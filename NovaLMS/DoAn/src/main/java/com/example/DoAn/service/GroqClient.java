package com.example.DoAn.service;

import com.example.DoAn.dto.GradingResponse;
import com.example.DoAn.model.AIPromptConfig;
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
  private final String apiKey;
  private final String aiModel;
  private final IAIPromptConfigService aiPromptConfigService;

  @Value("${groq.api.key:}")
  private String groqApiKey;

  public GroqClient(
      @Value("${ai.api.key:${groq.api.key:}}") String apiKey,
      @Value("${ai.api.url:https://api.groq.com/openai/v1}") String apiUrl,
      @Value("${ai.model:${groq.model:llama-3.3-70b-versatile}}") String aiModel,
      WebClient.Builder webClientBuilder,
      IAIPromptConfigService aiPromptConfigService) {
    this.apiKey = apiKey;
    this.aiModel = aiModel;
    this.aiPromptConfigService = aiPromptConfigService;
    // Ensure baseUrl doesn't include specific endpoints so .uri() works correctly
    String baseUrl = apiUrl.endsWith("/chat/completions")
        ? apiUrl.substring(0, apiUrl.length() - "/chat/completions".length())
        : apiUrl;
    if (baseUrl.endsWith("/"))
      baseUrl = baseUrl.substring(0, baseUrl.length() - 1);

    this.webClient = webClientBuilder
        .baseUrl(baseUrl)
        .defaultHeader("Authorization", "Bearer " + apiKey)
        .build();
  }

  /**
   * Transcribe audio via Whisper Large V3.
   * Sử dụng RestTemplate thay vì WebClient để cô lập API Key thật của Groq,
   * tránh việc bị ghi đè/nhồi thêm header của Local Gateway gây lỗi 401.
   */
  public String transcribe(String audioUrl) {
    // 1. Kiểm tra Key
    if (groqApiKey == null || groqApiKey.isBlank()) {
      throw new RuntimeException("Missing Groq API Key in properties.");
    }
    String cleanKey = groqApiKey.trim().replace("\"", "").replace("'", "");

    try {
      // 2. Download file từ Cloudinary
      log.info("Downloading audio from {}...", audioUrl);
      RestTemplate downloader = new RestTemplate();
      byte[] audioBytes = downloader.getForObject(URI.create(audioUrl.replace("\"", "")), byte[].class);

      if (audioBytes == null)
        throw new RuntimeException("Empty audio file.");

      // 3. Gọi Groq bằng OkHttp (Cực kỳ an toàn cho Auth Header)
      okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();

      okhttp3.RequestBody fileBody = okhttp3.RequestBody.create(
          audioBytes,
          okhttp3.MediaType.parse("audio/webm"));

      okhttp3.RequestBody requestBody = new okhttp3.MultipartBody.Builder()
          .setType(okhttp3.MultipartBody.FORM)
          .addFormDataPart("model", "whisper-large-v3")
          .addFormDataPart("file", "recording.webm", fileBody)
          .build();

      okhttp3.Request request = new okhttp3.Request.Builder()
          .url("https://api.groq.com/openai/v1/audio/transcriptions")
          .post(requestBody)
          .header("Authorization", "Bearer " + cleanKey) // Ép cứng Key thật của Groq ở đây
          .build();

      try (okhttp3.Response response = client.newCall(request).execute()) {
        String respBody = response.body() != null ? response.body().string() : "";
        if (!response.isSuccessful()) {
          log.error("Groq Whisper Error: {} - {}", response.code(), respBody);
          throw new RuntimeException("Groq API returned " + response.code());
        }

        JsonNode node = mapper.readTree(respBody);
        return node.path("text").asText("");
      }
    } catch (Exception e) {
      log.error("Transcription failed: {}", e.getMessage());
      throw new RuntimeException("Transcription failed: " + e.getMessage());
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
      int maxPoints) {
    String rubricJson = buildRubricJson(questionType, cefrLevel);
    String systemPrompt = buildSystemPrompt(questionType, rubricJson);
    String userPrompt = buildUserPrompt(questionPrompt, skill, cefrLevel, studentAnswer);

    try {
      List<Map<String, String>> messages = List.of(
          Map.of("role", "system", "content", systemPrompt),
          Map.of("role", "user", "content", userPrompt));
      Map<String, Object> body = Map.of(
          "model", aiModel,
          "messages", messages,
          "temperature", 0.3,
          "max_tokens", 2048, // Tăng lên để tránh bị cắt cụt JSON (Unexpected end-of-input)
          "stream", false);

      JsonNode response = webClient.post()
          .uri("/chat/completions")
          .bodyValue(body)
          .retrieve()
          .bodyToMono(JsonNode.class)
          .timeout(Duration.ofSeconds(3000))
          .block();

      log.info("[GROQ] LLaMA grading response: {}", response);
      String content = response.path("choices").get(0)
          .path("message").path("content").asText();

      // Robust JSON extraction: Find the first '{' and the last '}'
      int firstBrace = content.indexOf("{");
      int lastBrace = content.lastIndexOf("}");
      if (firstBrace >= 0 && lastBrace > firstBrace) {
          content = content.substring(firstBrace, lastBrace + 1);
      }

      // AUTO-PATCH if truncated
      if (content.contains("\"rubric\":") && !content.endsWith("}")) {
        log.warn("[AI-FIX] JSON bị cắt cụt, đang cố gắng đóng ngoặc...");
        if (!content.endsWith("}")) content += "}";
        if (!content.endsWith("}}")) content += "}";
      }

      try {
          return mapper.readValue(content, GradingResponse.class);
      } catch (Exception parseEx) {
          log.error("[GROQ-PARSE-FAIL] Raw content: {}", content);
          throw parseEx;
      }
    } catch (Exception e) {
      log.error("Groq LLaMA grading failed: {}", e.getMessage());
      throw new RuntimeException("Grading failed: " + e.getMessage(), e);
    }
  }

  private String mapCefrToBucket(String cefr) {
    if (cefr == null)
      return "advanced";
    String c = cefr.toUpperCase();
    if (c.contains("A1") || c.contains("A2") || c.contains("BEGINNER"))
      return "beginner";
    if (c.contains("B1") || c.contains("B2") || c.contains("INTERMEDIATE"))
      return "intermediate";
    return "advanced";
  }

  private String buildRubricJson(String questionType, String cefrLevel) {
    try {
      String bucket = mapCefrToBucket(cefrLevel);
      AIPromptConfig config = aiPromptConfigService.getConfigByBucket(bucket);
      if (config != null) {
        if ("WRITING".equals(questionType) && config.getWritingRubricJson() != null) {
          return config.getWritingRubricJson();
        } else if ("SPEAKING".equals(questionType) && config.getSpeakingRubricJson() != null) {
          return config.getSpeakingRubricJson();
        }
      }
    } catch (Exception e) {
      log.warn("Lỗi khi fetch rubric config từ DB: {}, sử dụng fallback mặc định", e.getMessage());
    }

    if ("WRITING".equals(questionType)) {
      return """
          {
            "task_achievement": {
              "label": "Task Achievement",
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
              "label": "Lexical resource",
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
              "label": "Grammatical range and accuracy",
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
              "label": "Coherence and cohesion",
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
      return String.format(
          """
              Bạn là một chuyên gia soát lỗi (Proofreader) và giáo viên tiếng Anh IELTS.
              Nhiệm vụ của bạn là hỗ trợ giáo viên tìm ra các LỖI SAI trong bài viết của học sinh dựa trên RUBRIC sau:
              
              RUBRIC CẤU HÌNH (Sử dụng các tiêu chí này để phân loại lỗi):
              %s
              
              HÃY TẬP TRUNG VÀO:
              1. Lỗi chính tả (Spelling).
              2. Lỗi ngữ pháp và cấu trúc câu (Grammar & Sentence Structure).
              3. Lỗi dùng từ không phù hợp (Inappropriate Vocabulary).
              4. Lỗi logic và liên kết (Logic & Cohesion).

              YÊU CẦU QUAN TRỌNG: 
              - Trả về ĐÚNG ĐỊNH DẠNG JSON bên dưới.
              - Phần "feedback": Liệt kê danh sách các lỗi sai cụ thể (Sử dụng tiếng Việt).
              - Trong mỗi phần "aiReasoning" của rubric: Hãy chỉ ra các lỗi cụ thể dựa trên mô tả Band trong Rubric ở trên.
              
              {
                "overallBand": <dự đoán band tham khảo>,
                "displayScore": <overallBand>,
                "maxScore": 9,
                "feedback": "- Lỗi chính tả: ... \n- Lỗi ngữ pháp: ... \n- Từ vựng: ...",
                "overallBandDescriptor": "Proofreading Result",
                "rubric": {
                  "task_achievement": {
                    "score": <0-9>, "max": 9, "bandLabel": "...", "bandDescription": "...", 
                    "aiReasoning": "Lỗi về nội dung so với Rubric: [liệt kê lỗi]"
                  },
                  "lexical_resource": { 
                    "score": <0-9>, "max": 9, "bandLabel": "...", "bandDescription": "...", 
                    "aiReasoning": "Lỗi từ vựng/chính tả so với Rubric: [liệt kê lỗi]"
                  },
                  "grammatical_range": { 
                    "score": <0-9>, "max": 9, "bandLabel": "...", "bandDescription": "...", 
                    "aiReasoning": "Lỗi ngữ pháp/cấu trúc so với Rubric: [liệt kê lỗi]"
                  },
                  "coherence_cohesion": { 
                    "score": <0-9>, "max": 9, "bandLabel": "...", "bandDescription": "...", 
                    "aiReasoning": "Lỗi liên kết/mạch lạc so với Rubric: [liệt kê lỗi]"
                  }
                }
              }
              """,
          rubricJson);
    } else {
      return String.format(
          """
              Bạn là giáo viên tiếng Anh chuyên IELTS, chấm bài SPEAKING theo thang điểm IELTS 9-band.
              Rubric (mỗi tiêu chí 0-9 điểm):
              %s

              Hãy nghe/nhìn câu trả lời và CHẤM theo rubric trên.
              Trả về DUY NHẤT JSON, không có markdown hay text nào khác:
              {
                "overallBand": <(fluency_cohesion + lexical_resource + grammatical_range + pronunciation) / 4>,
                "displayScore": <overallBand>,
                "maxScore": 9,
                "feedback": "<nhận xét tổng 2-3 câu bằng tiếng Việt, gợi ý cải thiện>",
                "overallBandDescriptor": "<VD: 'Good User (7.0)'>",
                "rubric": {
                  "fluency_cohesion": { "score": <>, "max": 9, "bandLabel": "...", "bandDescription": "...", "aiReasoning": "..." },
                  "lexical_resource": { "score": <>, "max": 9, "bandLabel": "...", "bandDescription": "...", "aiReasoning": "..." },
                  "grammatical_range": { "score": <>, "max": 9, "bandLabel": "...", "bandDescription": "...", "aiReasoning": "..." },
                  "pronunciation": { "score": <>, "max": 9, "bandLabel": "...", "bandDescription": "...", "aiReasoning": "..." }
                }
              }
              """,
          rubricJson);
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
