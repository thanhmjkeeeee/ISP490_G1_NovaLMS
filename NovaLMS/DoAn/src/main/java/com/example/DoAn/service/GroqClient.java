package com.example.DoAn.service;

import com.example.DoAn.dto.GradingResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayOutputStream;
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

            JsonNode node = mapper.readTree(responseBody);
            return node.path("text").asText("");

        } catch (Exception e) {
            log.error("Groq Whisper transcription failed for url={}: {}", audioUrl, e.getMessage());
            throw new RuntimeException("Transcription failed: " + e.getMessage(), e);
        }
    }

    /**
     * Grade WRITING or SPEAKING answer via LLaMA 3.3 70B Versatile.
     * Returns JSON: {"totalScore": 8, "maxScore": 10, "feedback": "...", "rubric": {...}}
     */
    public GradingResponse gradeWritingOrSpeaking(
            String questionPrompt,
            String skill,
            String cefrLevel,
            String studentAnswer,
            String questionType,
            int maxPoints
    ) {
        String rubricJson = buildRubricJson(questionType, maxPoints);
        String systemPrompt = buildSystemPrompt(questionType, rubricJson, maxPoints);
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

            String content = response.path("choices").get(0)
                    .path("message").path("content").asText();
            content = content.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            return mapper.readValue(content, GradingResponse.class);

        } catch (Exception e) {
            log.error("Groq LLaMA grading failed: {}", e.getMessage());
            throw new RuntimeException("Grading failed: " + e.getMessage(), e);
        }
    }

    // ─── Private helpers ───────────────────────────────────────────

    private String buildRubricJson(String questionType, int maxPoints) {
        int perCriterion = maxPoints / 4;
        if ("WRITING".equals(questionType)) {
            return String.format("""
                {
                  "task_achievement": {"max": %d, "desc": "Hoàn thành đúng yêu cầu đề bài, đủ độ dài"},
                  "lexical_resource": {"max": %d, "desc": "Vốn từ vựng phong phú, phù hợp CEFR"},
                  "grammar": {"max": %d, "desc": "Độ chính xác ngữ pháp, cấu trúc câu"},
                  "coherence_cohesion": {"max": %d, "desc": "Liên kết câu/đoạn mạch lạc, từ nối"}
                }
                """, perCriterion, perCriterion, perCriterion, perCriterion);
        } else {
            return String.format("""
                {
                  "task_achievement": {"max": %d, "desc": "Trả lời đúng câu hỏi, đủ ý"},
                  "lexical_resource": {"max": %d, "desc": "Từ vựng phong phú, phù hợp CEFR"},
                  "pronunciation": {"max": %d, "desc": "Phát âm rõ ràng, đúng trọng âm"},
                  "fluency": {"max": %d, "desc": "Lưu loát, tự nhiên, tốc độ phù hợp"}
                }
                """, perCriterion, perCriterion, perCriterion, perCriterion);
        }
    }

    private String buildSystemPrompt(String questionType, String rubricJson, int maxPoints) {
        int perCriterion = maxPoints / 4;
        if ("WRITING".equals(questionType)) {
            return String.format("""
                Bạn là giáo viên tiếng Anh chấm bài WRITING theo rubric CEFR.
                Rubric: %s
                Hãy chấm bài dựa trên 4 tiêu chí trên.
                Trả về DUY NHẤT JSON, không có markdown nào khác:
                {
                  "totalScore": <tổng điểm>,
                  "maxScore": <tổng điểm tối đa>,
                  "feedback": "<nhận xét ngắn 2-3 câu bằng tiếng Việt>",
                  "rubric": {
                    "task_achievement": <điểm 0-%d>,
                    "lexical_resource": <điểm 0-%d>,
                    "grammar": <điểm 0-%d>,
                    "coherence_cohesion": <điểm 0-%d>
                  }
                }
                """, rubricJson, perCriterion, perCriterion, perCriterion, perCriterion);
        } else {
            return String.format("""
                Bạn là giáo viên tiếng Anh chấm bài SPEAKING theo rubric CEFR.
                Rubric: %s
                Hãy chấm bài dựa trên 4 tiêu chí trên.
                Trả về DUY NHẤT JSON, không có markdown nào khác:
                {
                  "totalScore": <tổng điểm>,
                  "maxScore": <tổng điểm tối đa>,
                  "feedback": "<nhận xét ngắn 2-3 câu bằng tiếng Việt>",
                  "rubric": {
                    "task_achievement": <điểm 0-%d>,
                    "lexical_resource": <điểm 0-%d>,
                    "pronunciation": <điểm 0-%d>,
                    "fluency": <điểm 0-%d>
                  }
                }
                """, rubricJson, perCriterion, perCriterion, perCriterion, perCriterion);
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