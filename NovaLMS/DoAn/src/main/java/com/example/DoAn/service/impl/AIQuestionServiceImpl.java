package com.example.DoAn.service.impl;

import com.example.DoAn.dto.request.AIGenerateRequestDTO;
import com.example.DoAn.dto.response.AIGenerateResponseDTO;
import com.example.DoAn.dto.response.AIGenerateResponseDTO.QuestionDTO;
import com.example.DoAn.model.Lesson;
import com.example.DoAn.model.Module;
import com.example.DoAn.repository.LessonRepository;
import com.example.DoAn.repository.ModuleRepository;
import com.example.DoAn.service.AIQuestionService;
import com.example.DoAn.util.AIQuestionPromptBuilder;
import com.example.DoAn.util.RateLimitWindowStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIQuestionServiceImpl implements AIQuestionService {

    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final AIQuestionPromptBuilder promptBuilder;
    private final RateLimitWindowStore rateLimitStore;
    private final ObjectMapper objectMapper;

    @Value("${groq.api.key:}")
    private String groqApiKey;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String groqModel;

    // Groq uses OpenAI-compatible endpoint
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    // OkHttpClient — uses OS DNS resolver, works on all environments
    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    @Override
    public AIGenerateResponseDTO generate(AIGenerateRequestDTO request, String userEmail) {
        if (!rateLimitStore.isAllowed(userEmail)) {
            throw new RateLimitExceededException(
                    "Bạn đã vượt giới hạn 10 yêu cầu/phút. Vui lòng chờ một chút.");
        }

        String prompt = buildPrompt(request);
        String rawJson = callGroq(prompt);

        List<QuestionDTO> questions = parseQuestions(rawJson, request.getQuantity());

        String warning = null;
        if (questions.size() < request.getQuantity()) {
            warning = String.format("AI chỉ sinh được %d/%d câu hỏi.", questions.size(), request.getQuantity());
        }

        return AIGenerateResponseDTO.builder()
                .questions(questions)
                .warning(warning)
                .build();
    }

    private String buildPrompt(AIGenerateRequestDTO request) {
        if (request.hasModuleId()) {
            Optional<Module> moduleOpt = moduleRepository.findById(request.getModuleId());
            if (moduleOpt.isEmpty()) {
                throw new IllegalArgumentException("Module không tồn tại: " + request.getModuleId());
            }
            Module module = moduleOpt.get();
            String lessonSummary = fetchLessonSummary(module.getModuleId());
            return promptBuilder.buildContextPrompt(
                    (String) module.getModuleName(), (String) lessonSummary,
                    (int) request.getQuantity().intValue(),
                    (java.util.List<String>) request.getQuestionTypes());
        } else {
            return promptBuilder.buildQuickPrompt(
                    (String) request.getTopic(), (int) request.getQuantity().intValue(),
                    (java.util.List<String>) request.getQuestionTypes());
        }
    }

    private String fetchLessonSummary(Integer moduleId) {
        List<Lesson> lessons = lessonRepository.findByModule_ModuleIdOrderByOrderIndexAsc(moduleId);
        if (lessons == null || lessons.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Lesson lesson : lessons) {
            sb.append("- Bài: ").append(lesson.getLessonName() != null ? lesson.getLessonName() : "");
            if (lesson.getContent_text() != null) {
                sb.append("\n  Nội dung: ").append(lesson.getContent_text());
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String callGroq(String prompt) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", groqModel);
        body.put("temperature", 0.7);
        body.put("max_tokens", 4096);

        List<Map<String, String>> messages = List.of(
                Map.of("role", "user", "content", prompt)
        );
        body.put("messages", messages);

        int maxRetries = 3;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                String jsonBody = objectMapper.writeValueAsString(body);
                Request request = new Request.Builder()
                        .url(GROQ_URL)
                        .post(okhttp3.RequestBody.create(jsonBody,
                                okhttp3.MediaType.parse("application/json; charset=utf-8")))
                        .addHeader("Authorization", "Bearer " + groqApiKey)
                        .addHeader("Content-Type", "application/json")
                        .build();

                try (Response resp = httpClient.newCall(request).execute()) {
                    int code = resp.code();
                    String respBody = resp.body() != null ? resp.body().string() : "";

                    // Retry on 429 (rate limit) or 5xx server errors
                    if ((code == 429 || code >= 500) && attempt < maxRetries - 1) {
                        long waitMs = (long) Math.pow(2, attempt) * 1000;
                        log.warn("Groq HTTP {}, retrying in {}ms (attempt {}/{})",
                                code, waitMs, attempt + 1, maxRetries);
                        Thread.sleep(waitMs);
                        continue;
                    }

                    if (!resp.isSuccessful()) {
                        throw new AIException("Groq API error: HTTP " + code + " — " + respBody, null);
                    }

                    if (respBody == null || respBody.isBlank()) {
                        throw new AIException("Groq trả về response trống.", null);
                    }

                    return extractContent(respBody);
                }
            } catch (AIException e) {
                if (attempt == maxRetries - 1) throw e;
                try { Thread.sleep((long) Math.pow(2, attempt) * 1000); }
                catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new AIException("Interrupted while retrying.", ie);
                }
            } catch (Exception e) {
                if (attempt == maxRetries - 1) {
                    throw new AIException("Groq request failed: " + e.getMessage(), e);
                }
                try { Thread.sleep((long) Math.pow(2, attempt) * 1000); }
                catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new AIException("Interrupted while retrying.", ie);
                }
            }
        }

        throw new AIException("Groq: unexpected exit from retry loop.", null);
    }

    // Parse OpenAI-compatible response: { choices: [{ message: { content: "..." } }] }
    private String extractContent(String response) {
        try {
            Map<String, Object> respMap = objectMapper.readValue(response, new TypeReference<>() {});
            List<?> choices = (List<?>) respMap.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new AIException("Groq trả về danh sách rỗng.", null);
            }
            Map<?, ?> choice = (Map<?, ?>) choices.get(0);
            Map<?, ?> message = (Map<?, ?>) choice.get("message");
            return (String) message.get("content");
        } catch (ClassCastException | NullPointerException e) {
            log.error("Failed to parse Groq response: {}", response.substring(0, Math.min(300, response.length())));
            throw new AIException("Không parse được response từ Groq.", e);
        } catch (JsonProcessingException e) {
            throw new AIException("Không parse được response từ Groq.", e);
        }
    }

    private List<QuestionDTO> parseQuestions(String rawJson, int requested) {
        String cleaned = rawJson != null ? rawJson.trim() : "";
        if (cleaned.startsWith("```json")) cleaned = cleaned.substring(7);
        if (cleaned.startsWith("```")) cleaned = cleaned.substring(3);
        if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length() - 3);
        cleaned = cleaned.trim();

        try {
            List<Map<String, Object>> rawList = objectMapper.readValue(cleaned, new TypeReference<>() {});
            List<QuestionDTO> valid = new ArrayList<>();
            for (Map<String, Object> raw : rawList) {
                QuestionDTO dto = toQuestionDTO(raw);
                if (dto != null && isValid(dto)) {
                    valid.add(dto);
                }
            }
            return valid;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse AI JSON: {}", cleaned.substring(0, Math.min(200, cleaned.length())));
            throw new AIException("AI trả về định dạng không hợp lệ.", e);
        }
    }

    private QuestionDTO toQuestionDTO(Map<String, Object> raw) {
        try {
            return QuestionDTO.builder()
                    .content((String) raw.get("content"))
                    .questionType((String) raw.get("questionType"))
                    .skill((String) raw.get("skill"))
                    .cefrLevel((String) raw.get("cefrLevel"))
                    .topic((String) raw.get("topic"))
                    .explanation((String) raw.get("explanation"))
                    .audioUrl((String) raw.get("audioUrl"))
                    .imageUrl((String) raw.get("imageUrl"))
                    .correctAnswer((String) raw.get("correctAnswer"))
                    .options(parseOptions(raw.get("options")))
                    .matchLeft(parseStringList(raw.get("matchLeft")))
                    .matchRight(parseStringList(raw.get("matchRight")))
                    .correctPairs(parseIntegerList(raw.get("correctPairs")))
                    .build();
        } catch (Exception e) {
            log.warn("Failed to convert AI raw to DTO: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<AIGenerateResponseDTO.OptionDTO> parseOptions(Object optionsObj) {
        if (optionsObj == null) return null;
        try {
            List<?> raw = (List<?>) optionsObj;
            List<AIGenerateResponseDTO.OptionDTO> opts = new ArrayList<>();
            for (Object item : raw) {
                if (item instanceof Map<?, ?> m) {
                    opts.add(AIGenerateResponseDTO.OptionDTO.builder()
                            .title((String) m.get("title"))
                            .correct((Boolean) m.get("correct"))
                            .build());
                }
            }
            return opts.isEmpty() ? null : opts;
        } catch (Exception e) { return null; }
    }

    @SuppressWarnings("unchecked")
    private List<String> parseStringList(Object obj) {
        if (obj == null) return null;
        if (obj instanceof List<?>) {
            List<?> raw = (List<?>) obj;
            List<String> result = new ArrayList<>();
            for (Object o : raw) { if (o != null) result.add(o.toString()); }
            return result.isEmpty() ? null : result;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<Integer> parseIntegerList(Object obj) {
        if (obj == null) return null;
        if (obj instanceof List<?>) {
            List<?> raw = (List<?>) obj;
            List<Integer> result = new ArrayList<>();
            for (Object o : raw) {
                if (o != null) {
                    result.add(o instanceof Number n ? n.intValue() : Integer.parseInt(o.toString()));
                }
            }
            return result.isEmpty() ? null : result;
        }
        return null;
    }

    private boolean isValid(QuestionDTO dto) {
        if (dto.getContent() == null || dto.getContent().isBlank()) return false;
        if (!promptBuilder.isValidQuestionType(dto.getQuestionType())) return false;
        if (!promptBuilder.isValidSkill(dto.getSkill())) return false;
        if (!promptBuilder.isValidCefr(dto.getCefrLevel())) return false;

        String type = dto.getQuestionType();
        if ("MULTIPLE_CHOICE_SINGLE".equals(type) || "MULTIPLE_CHOICE_MULTI".equals(type)) {
            List<AIGenerateResponseDTO.OptionDTO> opts = dto.getOptions();
            if (opts == null || opts.size() < 2) return false;
            // Every option must have a non-null, non-blank title
            for (AIGenerateResponseDTO.OptionDTO opt : opts) {
                if (opt.getTitle() == null || opt.getTitle().isBlank()) return false;
            }
            long correctCount = opts.stream().filter(o -> Boolean.TRUE.equals(o.getCorrect())).count();
            if (correctCount == 0) return false;
            if ("MULTIPLE_CHOICE_MULTI".equals(type) && correctCount == opts.size()) return false;
        }
        if ("FILL_IN_BLANK".equals(type)) {
            if (dto.getCorrectAnswer() == null || dto.getCorrectAnswer().isBlank()) return false;
        }
        if ("MATCHING".equals(type)) {
            List<String> left = dto.getMatchLeft();
            List<String> right = dto.getMatchRight();
            List<Integer> pairs = dto.getCorrectPairs();
            if (left == null || right == null || pairs == null) return false;
            if (left.size() < 2 || left.size() != right.size()) return false;
        }
        return true;
    }
}
