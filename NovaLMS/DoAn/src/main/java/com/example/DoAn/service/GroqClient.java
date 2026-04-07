package com.example.DoAn.service;

import com.example.DoAn.dto.GradingResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GroqClient {

    private final WebClient webClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public GroqClient(
            @NonNull @Value("${groq.api.key}") String apiKey,
            @Value("${groq.api.url:https://api.groq.com/openai/v1}") String apiUrl,
            WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl(apiUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    /**
     * Transcribe audio via Whisper Large V3.
     * Groq's Whisper API only accepts a binary audio file (multipart/form-data, field name "file").
     * This method downloads the audio from audioUrl, then sends it to Groq.
     */
    public String transcribe(String audioUrl) {
        try {
            // Strip surrounding quotes if present (some frontends store URL with quotes)
            if (audioUrl != null && audioUrl.startsWith("\"")) {
                audioUrl = audioUrl.substring(1);
            }
            if (audioUrl != null && audioUrl.endsWith("\"")) {
                audioUrl = audioUrl.substring(0, audioUrl.length() - 1);
            }
            log.info("Downloading audio from {} for transcription", audioUrl);

            // Download audio from Cloudinary (or wherever it's hosted)
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<byte[]> audioResponse = restTemplate.getForEntity(
                    URI.create(audioUrl), byte[].class);

            if (audioResponse.getStatusCode() != HttpStatus.OK || audioResponse.getBody() == null) {
                throw new RuntimeException("Failed to download audio: HTTP " + audioResponse.getStatusCode());
            }
            byte[] audioBytes = audioResponse.getBody();

            // Determine content type from Content-Type header (default to audio/mpeg)
            MediaType mediaType = audioResponse.getHeaders().getContentType();
            String mimeType = (mediaType != null) ? mediaType.toString() : "audio/mpeg";

            // Determine file extension from mime type
            String ext = mimeType.contains("wav") ? "wav"
                    : mimeType.contains("webm") ? "webm"
                    : mimeType.contains("m4a") ? "m4a"
                    : "mp3";

            log.info("Sending {} bytes (type={}) to Groq Whisper", audioBytes.length, mimeType);

            // Build multipart body: model (text) + file (binary)
            MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
            formData.add("model", "whisper-large-v3");
            formData.add("file", new org.springframework.core.io.ByteArrayResource(audioBytes) {
                @Override
                public String getFilename() {
                    return "audio." + ext;
                }
            });

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(formData, headers);

            String responseBody = webClient.post()
                    .uri("/audio/transcriptions")
                    .headers(h -> h.putAll(headers))
                    .bodyValue(formData)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            log.info("[GROQ] Whisper transcription response: {}", responseBody);
            JsonNode node = mapper.readTree(responseBody);
            return node.path("text").asText("");

        } catch (Exception e) {
            log.error("Groq Whisper transcription failed for url={}: {}", audioUrl, e.getMessage());
            throw new RuntimeException("Transcription failed: " + e.getMessage(), e);
        }
    }

    /**
     * Grade WRITING or SPEAKING answer via LLaMA 3.3 70B Versatile.
     * Returns JSON with IELTS 9-band rubric breakdown.
     */
    public GradingResponse gradeWritingOrSpeaking(
            String questionPrompt,
            String skill,
            String cefrLevel,
            String studentAnswer,
            String questionType,
            int maxPoints
    ) {
        String rubricJson = buildRubricJson(questionType);
        String systemPrompt = buildSystemPrompt(questionType, rubricJson);
        String userPrompt = buildUserPrompt(questionPrompt, skill, cefrLevel, studentAnswer);

        try {
            List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            );
            Map<String, Object> body = Map.of(
                "model", "llama-3.3-70b-versatile",
                "messages", messages,
                "temperature", 0.3,
                "max_tokens", 1024
            );

            JsonNode response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();

            log.info("[GROQ] LLaMA grading response: {}", response);
            String content = response.path("choices").get(0)
                    .path("message").path("content").asText();
            content = content.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            return mapper.readValue(content, GradingResponse.class);

        } catch (Exception e) {
            log.error("Groq LLaMA grading failed: {}", e.getMessage());
            throw new RuntimeException("Grading failed: " + e.getMessage(), e);
        }
    }

    // ─── Private helpers ───────────────────────────────────────────────────────

    private String buildRubricJson(String questionType) {
        if ("WRITING".equals(questionType)) {
            return """
            {
              "task_achievement": {
                "max": 9,
                "bands": {
                  "0": "Content is irrelevant or does not communicate the message",
                  "1": "Barely communicates with occasional comprehensible sections",
                  "2": "Partially addresses the task; content may be largely irrelevant",
                  "3": "Task not fully achieved; coverage is inadequate",
                  "4": "Presents a position but development is insufficient or repetitive",
                  "5": "Presents some relevant ideas but development is limited",
                  "6": "Addresses the task adequately; relevant ideas with some development",
                  "7": "Covers all requirements; well-developed with clear progression",
                  "8": "Satisfies all requirements; very good development and cohesion",
                  "9": "Fully addresses all requirements with complete clarity and precision"
                }
              },
              "lexical_resource": {
                "max": 9,
                "bands": {
                  "0": "No appropriate lexical resource",
                  "1": "Rarely used appropriate vocabulary; comprehension is severely limited",
                  "2": "Limited vocabulary; frequent errors of word choice",
                  "3": "Limited vocabulary; inaccuracies impede meaning",
                  "4": "Adequate vocabulary; some inaccuracies but meaning largely clear",
                  "5": "Sufficient range; some vocabulary errors but meaning clear",
                  "6": "Wide enough vocabulary; occasional lexical errors",
                  "7": "Wide range; minor errors; effective communication",
                  "8": "Wide range; very few lexical errors; communicates flexibly",
                  "9": "Full flexibility and precision; sophisticated vocabulary"
                }
              },
              "grammatical_range": {
                "max": 9,
                "bands": {
                  "0": "No grammatical structures",
                  "1": "Rarely produces grammatical structures",
                  "2": "Few sentence structures; accuracy only in simplest forms",
                  "3": "Limited control; errors impede communication",
                  "4": "Some accuracy; limited range of structures",
                  "5": "Fair range; frequent errors but clear communication",
                  "6": "Good range; reasonable accuracy; complex structures attempted",
                  "7": "Wide range; good accuracy; minor errors",
                  "8": "Wide range; very good accuracy; rare errors",
                  "9": "Full range; high accuracy; sophisticated structures"
                }
              },
              "coherence_cohesion": {
                "max": 9,
                "bands": {
                  "0": "No organization or cohesion",
                  "1": "Unconnected ideas; minimal cohesion",
                  "2": "Lacks cohesion; organization unclear",
                  "3": "Cohesion inadequate; organization hard to follow",
                  "4": "Cohesion developed; organization sometimes unclear",
                  "5": "Uses some cohesive devices; organization generally clear",
                  "6": "Logically organized; appropriate cohesive devices",
                  "7": "Well-organized; clear progression; effective cohesion",
                  "8": "Very well organized; seamless cohesion",
                  "9": "Fluent and sophisticated; perfect cohesion and progression"
                }
              }
            }
            """;
        } else {
            // SPEAKING
            return """
            {
              "fluency_cohesion": {
                "max": 9,
                "bands": {
                  "0": "Cannot communicate",
                  "1": "Difficult to produce connected speech",
                  "2": "Long pauses; hesitant; communication is stilted",
                  "3": "Usually hesitant; some connected speech",
                  "4": "Shows hesitation; limited connected speech",
                  "5": "Able to sustain speech; some hesitation",
                  "6": "Speaks at reasonable speed; occasional repetition",
                  "7": "Speaks fluently with occasional self-correction",
                  "8": "Speaks fluently with rare hesitation or repetition",
                  "9": "Speaks with complete fluency like a native speaker"
                }
              },
              "lexical_resource": {
                "max": 9,
                "bands": {
                  "0": "No appropriate lexical resource",
                  "1": "Barely communicates; no evidence of lexical control",
                  "2": "Limited vocabulary; frequent word-searching",
                  "3": "Limited vocabulary; comprehension often breaks down",
                  "4": "Limited range; occasional word-finding difficulty",
                  "5": "Sufficient range; some word choice errors",
                  "6": "Wide enough vocabulary; occasional errors",
                  "7": "Wide range; minor lexical gaps; communicates effectively",
                  "8": "Wide range; very few lexical errors",
                  "9": "Full lexical sophistication; precise word choice"
                }
              },
              "grammatical_range": {
                "max": 9,
                "bands": {
                  "0": "No grammatical structures",
                  "1": "Rarely produces grammatical structures",
                  "2": "Few structures; frequent errors",
                  "3": "Limited control; frequent errors impede meaning",
                  "4": "Some accuracy in simple sentences; complex forms rare",
                  "5": "Fair range; frequent grammatical errors",
                  "6": "Good range; reasonable accuracy; complex structures attempted",
                  "7": "Wide range; good accuracy; minor errors",
                  "8": "Wide range; very good accuracy; rare errors",
                  "9": "Full range; sophisticated structures; high accuracy"
                }
              },
              "pronunciation": {
                "max": 9,
                "bands": {
                  "0": "No intelligible pronunciation",
                  "1": "Barely intelligible; constant breakdowns",
                  "2": "Severe pronunciation difficulties; frequent breakdowns",
                  "3": "Heavy accent; frequent mispronunciation; meaning obscured",
                  "4": "Pronunciation errors require listener effort",
                  "5": "Acceptable but with occasional mispronunciation",
                  "6": "Generally intelligible; some errors but clear",
                  "7": "Clear and intelligible; minor pronunciation slips",
                  "8": "Very clear; rare pronunciation errors",
                  "9": "Equivalent to an educated native speaker"
                }
              }
            }
            """;
        }
    }

    private String buildSystemPrompt(String questionType, String rubricJson) {
        if ("WRITING".equals(questionType)) {
            return String.format("""
            Bạn là giáo viên tiếng Anh chuyên IELTS, chấm bài WRITING theo thang điểm IELTS 9-band.
            Rubric (mỗi tiêu chí 0-9 điểm):
            %s

            Hãy đọc câu trả lời và CHẤM theo rubric trên.
            Trả về DUY NHẤT JSON, không có markdown hay text nào khác:
            {
              "overallBand": <(task_achievement + lexical_resource + grammatical_range + coherence_cohesion) / 4>,
              "displayScore": <overallBand * 2.5>,
              "maxScore": 10,
              "feedback": "<nhận xét tổng 2-3 câu bằng tiếng Việt, gợi ý cải thiện>",
              "overallBandDescriptor": "<VD: 'Good User (7.0)'>",
              "rubric": {
                "task_achievement": {
                  "score": <điểm 0-9>,
                  "max": 9,
                  "bandLabel": "<VD: 'Band 7.0' hoặc '7.5'>",
                  "bandDescription": "<lấy mô tả band tương ứng từ rubric ở trên>",
                  "aiReasoning": "<giải thích 1-2 câu bằng tiếng Việt tại sao đạt mức này>"
                },
                "lexical_resource": { "score": <>, "max": 9, "bandLabel": "...", "bandDescription": "...", "aiReasoning": "..." },
                "grammatical_range": { "score": <>, "max": 9, "bandLabel": "...", "bandDescription": "...", "aiReasoning": "..." },
                "coherence_cohesion": { "score": <>, "max": 9, "bandLabel": "...", "bandDescription": "...", "aiReasoning": "..." }
              }
            }
            """, rubricJson);
        } else {
            return String.format("""
            Bạn là giáo viên tiếng Anh chuyên IELTS, chấm bài SPEAKING theo thang điểm IELTS 9-band.
            Rubric (mỗi tiêu chí 0-9 điểm):
            %s

            Hãy nghe/nhìn câu trả lời và CHẤM theo rubric trên.
            Trả về DUY NHẤT JSON, không có markdown hay text nào khác:
            {
              "overallBand": <(fluency_cohesion + lexical_resource + grammatical_range + pronunciation) / 4>,
              "displayScore": <overallBand * 2.5>,
              "maxScore": 10,
              "feedback": "<nhận xét tổng 2-3 câu bằng tiếng Việt, gợi ý cải thiện>",
              "overallBandDescriptor": "<VD: 'Good User (7.0)'>",
              "rubric": {
                "fluency_cohesion": { "score": <>, "max": 9, "bandLabel": "...", "bandDescription": "...", "aiReasoning": "..." },
                "lexical_resource": { "score": <>, "max": 9, "bandLabel": "...", "bandDescription": "...", "aiReasoning": "..." },
                "grammatical_range": { "score": <>, "max": 9, "bandLabel": "...", "bandDescription": "...", "aiReasoning": "..." },
                "pronunciation": { "score": <>, "max": 9, "bandLabel": "...", "bandDescription": "...", "aiReasoning": "..." }
              }
            }
            """, rubricJson);
        }
    }

    private String buildUserPrompt(String prompt, String skill, String cefr, String answer) {
        return String.format("""
            Đề bài: %s
            Skill: %s | CEFR Level: %s
            Câu trả lời của học sinh:
            %s
            Hãy chấm và trả về JSON theo rubric.
            """, prompt, skill, cefr, answer);
    }
}
