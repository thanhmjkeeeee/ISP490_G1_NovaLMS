package com.example.DoAn.service.impl;

import com.example.DoAn.dto.request.AIGenerateGroupRequestDTO;
import com.example.DoAn.dto.request.AIGenerateRequestDTO;
import com.example.DoAn.dto.response.AIGenerateGroupResponseDTO;
import com.example.DoAn.dto.response.AIGenerateResponseDTO;
import com.example.DoAn.dto.response.AIGenerateResponseDTO.QuestionDTO;
import com.example.DoAn.model.Lesson;
import com.example.DoAn.model.Module;
import com.example.DoAn.repository.LessonRepository;
import com.example.DoAn.repository.ModuleRepository;
import com.example.DoAn.service.AIQuestionService;
import com.example.DoAn.util.AIQuestionPromptBuilder;
import com.example.DoAn.util.RateLimitWindowStore;
import com.example.DoAn.service.FileUploadService;
import com.example.DoAn.service.ITextToSpeechService;
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
    private final ITextToSpeechService ttsService;
    private final FileUploadService fileUploadService;

    @Value("${ai.api.key:}")
    private String aiApiKey;

    @Value("${ai.model:apilms}")
    private String aiModel;

    @Value("${ai.api.url:http://localhost:20128/v1/chat/completions}")
    private String aiApiUrlBase;

    // OkHttpClient — uses OS DNS resolver, works on all environments
    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    @Override
    public AIGenerateResponseDTO generate(AIGenerateRequestDTO request, String userEmail) {
        if (!rateLimitStore.isAllowed(userEmail)) {
            throw new RateLimitExceededException(
                    "Bạn đã vượt giới hạn 10 yêu cầu/phút. Vui lòng chờ một chút.");
        }

        String mode = (request.getMode() != null && "ADVANCED".equalsIgnoreCase(request.getMode()))
                ? "ADVANCED" : "NORMAL";

        String prompt;
        if ("ADVANCED".equals(mode)) {
            String cefr = request.getCefrLevel() != null ? request.getCefrLevel() : "5.0";
            if (request.hasModuleId()) {
                Optional<Module> moduleOpt = moduleRepository.findById(request.getModuleId());
                if (moduleOpt.isEmpty()) {
                    throw new IllegalArgumentException("Module không tồn tại: " + request.getModuleId());
                }
                Module module = moduleOpt.get();
                String lessonSummary = fetchLessonSummary(module.getModuleId());
                prompt = promptBuilder.buildAdvancedContextPrompt(
                        (String) module.getModuleName(), (String) lessonSummary,
                        (int) request.getQuantity().intValue(),
                        (java.util.List<String>) request.getQuestionTypes(),
                        cefr,
                        request.getSkill(),
                        request.getAdvancedOptions());
            } else {
                prompt = promptBuilder.buildAdvancedQuickPrompt(
                        (String) request.getTopic(),
                        (int) request.getQuantity().intValue(),
                        (java.util.List<String>) request.getQuestionTypes(),
                        cefr,
                        request.getSkill(),
                        request.getAdvancedOptions());
            }
        } else {
            prompt = buildPrompt(request);
        }

        log.info("[AI_GENERATE] Sending prompt to AI (mode={}):\n{}", mode, prompt);

        try {
            String rawJson = callAI(prompt);
            log.info("[AI_GENERATE] Received raw JSON from AI (mode={}):\n{}", mode, rawJson);

            List<QuestionDTO> questions = parseQuestions(rawJson, request.getQuantity());

            // Handle TTS for independent questions
            if ("LISTENING".equalsIgnoreCase(request.getSkill())) {
                for (QuestionDTO q : questions) {
                    if (q.getAudioUrl() == null || q.getAudioUrl().isBlank()) {
                        generateAudioForQuestion(q);
                    }
                }
            }

            String warning = null;
            if (questions.isEmpty()) {
                warning = "AI không sinh được câu hỏi nào. Kiểm tra server logs để biết chi tiết.";
            } else if (questions.size() < request.getQuantity()) {
                warning = String.format("AI chỉ sinh được %d/%d câu hỏi.", questions.size(), request.getQuantity());
            }

            return AIGenerateResponseDTO.builder()
                    .questions(questions)
                    .warning(warning)
                    .build();
        } catch (Exception e) {
            log.error("[AI_GENERATE] Error generating questions: {}", e.getMessage());
            return AIGenerateResponseDTO.builder()
                    .questions(new java.util.ArrayList<>())
                    .warning("Lỗi AI: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public AIGenerateGroupResponseDTO generateGroup(AIGenerateGroupRequestDTO request, String userEmail) {
        if (!rateLimitStore.isAllowed(userEmail)) {
            throw new RateLimitExceededException(
                    "Bạn đã vượt giới hạn 10 yêu cầu/phút. Vui lòng chờ một chút.");
        }

        String topic = request.hasTopic() ? request.getTopic()
                : (request.hasModuleId() ? "Module-based content" : "");
        String skill = request.getSkill() != null ? request.getSkill() : "READING";
        String cefr = request.getCefrLevel() != null ? request.getCefrLevel() : "5.0";
        int qty = request.getQuantity() != null ? request.getQuantity() : 5;
        boolean isAdvanced = request.getMode() != null && "ADVANCED".equalsIgnoreCase(request.getMode());

        String prompt;
        if (isAdvanced) {
            prompt = promptBuilder.buildAdvancedGroupPrompt(topic, qty, request.getQuestionTypes(), cefr, skill, request.getAdvancedOptions());
        } else {
            prompt = promptBuilder.buildGroupPrompt(topic, skill, cefr, qty, request.getQuestionTypes(), request.getAdvancedOptions());
        }
        
        log.info("[AI_GENERATE_GROUP] Sending prompt to AI:\n{}", prompt);

        try {
            String rawJson = callAI(prompt);
            log.info("[AI_GENERATE_GROUP] Received raw JSON from AI:\n{}", rawJson);

            AIGenerateGroupResponseDTO.AIGenerateGroupResponseDTOBuilder builder = AIGenerateGroupResponseDTO.builder()
                    .skill(skill)
                    .cefrLevel(cefr)
                    .topic(topic);

            String cleaned = cleanAiJson(rawJson);
            Map<String, Object> parsed = objectMapper.readValue(cleaned, new TypeReference<>() {});
            if (parsed.get("passage") != null) builder.passage((String) parsed.get("passage"));
            if (parsed.get("audioUrl") != null) builder.audioUrl((String) parsed.get("audioUrl"));
            if (parsed.get("imageUrl") != null) builder.imageUrl((String) parsed.get("imageUrl"));
            if (parsed.get("explanation") != null) builder.explanation((String) parsed.get("explanation"));
            if (parsed.get("skill") != null) builder.skill((String) parsed.get("skill"));
            if (parsed.get("cefrLevel") != null) builder.cefrLevel((String) parsed.get("cefrLevel"));
            if (parsed.get("topic") != null) builder.topic((String) parsed.get("topic"));

            // Handle TTS for passage-based Listening
            if ("LISTENING".equalsIgnoreCase(skill)) {
                String passage = (String) parsed.get("passage");
                if (passage != null && !passage.isBlank()) {
                    byte[] audioBytes = ttsService.synthesizeDialogue(passage);
                    if (audioBytes != null && audioBytes.length > 0) {
                        String audioUrl = fileUploadService.uploadBytes(audioBytes, "ai_listening", "video");
                        builder.audioUrl(audioUrl);
                    }
                }
            }

            List<AIGenerateResponseDTO.QuestionDTO> questions = new ArrayList<>();
            List<?> rawQuestions = (List<?>) parsed.get("questions");
            if (rawQuestions != null) {
                for (Object raw : rawQuestions) {
                    if (raw instanceof Map<?, ?> m) {
                        Map<String, Object> m2 = new HashMap<>();
                        for (Map.Entry<?, ?> e : m.entrySet()) {
                            if (e.getKey() != null) m2.put(e.getKey().toString(), e.getValue());
                        }
                        // Inject context-level fields into each question so isValid() can pass
                        if (!m2.containsKey("skill") || m2.get("skill") == null) m2.put("skill", skill);
                        if (!m2.containsKey("cefrLevel") || m2.get("cefrLevel") == null) m2.put("cefrLevel", cefr);
                        if (!m2.containsKey("topic") || m2.get("topic") == null) m2.put("topic", topic);
                        AIGenerateResponseDTO.QuestionDTO dto = toQuestionDTO(m2);
                        if (dto != null && isValid(dto)) {
                            if (dto.getSkill() == null) dto.setSkill(skill);
                            if (dto.getCefrLevel() == null) dto.setCefrLevel(cefr);
                            if (dto.getTopic() == null) dto.setTopic(topic);
                            
                            // Generate individual audio for Listening question if not present
                            if ("LISTENING".equalsIgnoreCase(skill) && (dto.getAudioUrl() == null || dto.getAudioUrl().isBlank())) {
                                generateAudioForQuestion(dto);
                            }
                            
                            questions.add(dto);
                        }
                    }
                }
            }
            builder.questions(questions);
            if (questions.isEmpty()) {
                log.warn("[GROUP] No valid questions parsed.");
                builder.warning("AI không sinh được câu hỏi hợp lệ. Vui lòng thử lại.");
            } else if (questions.size() < qty) {
                builder.warning(String.format("AI chỉ sinh được %d/%d câu hỏi.", questions.size(), qty));
            }
            return builder.build();
        } catch (Exception e) {
            log.error("[AI_GENERATE_GROUP] Error: {}", e.getMessage());
            return AIGenerateGroupResponseDTO.builder()
                    .questions(new ArrayList<>())
                    .warning("Lỗi AI: " + e.getMessage())
                    .build();
        }
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
                    module.getModuleName(), lessonSummary,
                    request.getQuantity().intValue(),
                    request.getQuestionTypes(),
                    request.getSkill(),
                    request.getCefrLevel(),
                    request.getAdvancedOptions());
        } else {
            return promptBuilder.buildQuickPrompt(
                    request.getTopic(), 
                    request.getQuantity().intValue(),
                    request.getQuestionTypes(),
                    request.getSkill(),
                    request.getCefrLevel(),
                    request.getAdvancedOptions());
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

    private String callAI(String prompt) {
        boolean isGemini = aiApiUrlBase != null && aiApiUrlBase.contains("googleapis.com");

        Map<String, Object> body = new LinkedHashMap<>();
        String fullUrl;
        Request.Builder requestBuilder = new Request.Builder()
                .addHeader("Content-Type", "application/json");

        if (isGemini) {
            // Google Gemini Format
            fullUrl = aiApiUrlBase + (aiApiUrlBase.endsWith("/") ? "" : "/") + aiModel + ":generateContent?key=" + aiApiKey;
            body.put("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));
        } else {
            // OpenAI Compatible Format (Kyma, Groq, 9Router, etc.)
            fullUrl = aiApiUrlBase.endsWith("/chat/completions") ? aiApiUrlBase 
                    : aiApiUrlBase + (aiApiUrlBase.endsWith("/") ? "" : "/") + "chat/completions";
            body.put("model", aiModel);
            body.put("temperature", 0.7);
            body.put("max_tokens", 4000);
            body.put("stream", false);
            body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
            requestBuilder.addHeader("Authorization", "Bearer " + aiApiKey);
        }

        int maxRetries = 3;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                String jsonBody = objectMapper.writeValueAsString(body);
                Request request = requestBuilder
                        .url(fullUrl)
                        .post(okhttp3.RequestBody.create(jsonBody,
                                okhttp3.MediaType.parse("application/json; charset=utf-8")))
                        .build();

                try (Response resp = httpClient.newCall(request).execute()) {
                    int code = resp.code();
                    String respBody = resp.body() != null ? resp.body().string() : "";

                    // Retry on 429 (rate limit) or 5xx server errors
                    if ((code == 429 || code >= 500) && attempt < maxRetries - 1) {
                        long waitMs = (long) Math.pow(2, attempt) * 1000;
                        log.warn("AI HTTP {}, retrying in {}ms (attempt {}/{})",
                                code, waitMs, attempt + 1, maxRetries);
                        Thread.sleep(waitMs);
                        continue;
                    }

                    if (code == 413) {
                        throw new AIException("Yêu cầu quá lớn. Vui lòng giảm số lượng câu hỏi hoặc độ dài bài đọc.", null);
                    }
                    if (code == 429) {
                        throw new AIException("Bạn đang yêu cầu quá nhanh. Vui lòng đợi một lát rồi thử lại (Giới hạn tài khoản AI).", null);
                    }

                    if (!resp.isSuccessful()) {
                        throw new AIException("AI API error: HTTP " + code + " — " + respBody, null);
                    }

                    if (respBody == null || respBody.isBlank()) {
                        throw new AIException("AI trả về response trống.", null);
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
                    throw new AIException("AI request failed: " + e.getMessage(), e);
                }
                try { Thread.sleep((long) Math.pow(2, attempt) * 1000); }
                catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new AIException("Interrupted while retrying.", ie);
                }
            }
        }

        throw new AIException("AI: unexpected exit from retry loop.", null);
    }

    private String extractContent(String response) {
        try {
            String trimmed = response != null ? response.trim() : "";
            Map<String, Object> respMap = objectMapper.readValue(trimmed, new TypeReference<>() {});
            
            // Check for Gemini format
            if (respMap.containsKey("candidates")) {
                List<?> candidates = (List<?>) respMap.get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map<?, ?> candidate = (Map<?, ?>) candidates.get(0);
                    Map<?, ?> content = (Map<?, ?>) candidate.get("content");
                    if (content != null) {
                        List<?> parts = (List<?>) content.get("parts");
                        if (parts != null && !parts.isEmpty()) {
                            Map<?, ?> part = (Map<?, ?>) parts.get(0);
                            return (String) part.get("text");
                        }
                    }
                }
                throw new AIException("Gemini trả về response không có nội dung.", null);
            }

            // Check for OpenAI format
            List<?> choices = (List<?>) respMap.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<?, ?> choice = (Map<?, ?>) choices.get(0);
                Map<?, ?> message = (Map<?, ?>) choice.get("message");
                if (message != null) {
                    return (String) message.get("content");
                }
            }
            
            // Fallback: Nếu không có format OpenAI nhưng là JSON hợp lệ, trả thẳng
            if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                return trimmed;
            }

            throw new AIException("AI trả về danh sách rỗng hoặc định dạng không nhận dạng được.", null);
        } catch (ClassCastException | NullPointerException e) {
            String preview = (response != null && response.length() > 500) ? response.substring(0, 500) : response;
            log.error("Failed to parse AI response: {}. Error: {}", preview, e.getMessage());
            throw new AIException("Không parse được response từ AI. Định dạng không mong đợi.", e);
        } catch (JsonProcessingException e) {
            log.error("Lỗi parse OpenAI Format. Raw response từ API: \n{}", response);
            
            // Fallback (Chữa cháy): Nếu bắt đầu bằng { hoặc [ thì trả về luôn
            String trimmed = response != null ? response.trim() : "";
            if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                return trimmed;
            }
            throw new AIException("Không parse được response từ AI.", e);
        }
    }

    private String cleanAiJson(String rawJson) {
        if (rawJson == null) return "";
        String cleaned = rawJson.trim();
        if (cleaned.startsWith("```json")) cleaned = cleaned.substring(7);
        else if (cleaned.startsWith("```")) cleaned = cleaned.substring(3);
        if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length() - 3);
        return cleaned.trim();
    }

    private List<QuestionDTO> parseQuestions(String rawJson, int requested) {
        String cleaned = cleanAiJson(rawJson);

        try {
            List<Map<String, Object>> rawList = objectMapper.readValue(cleaned, new TypeReference<>() {});
            List<QuestionDTO> valid = new ArrayList<>();
            for (Map<String, Object> raw : rawList) {
                QuestionDTO dto = toQuestionDTO(raw);
                String reason = getRejectionReason(raw, dto);
                if (dto != null && isValid(dto)) {
                    valid.add(dto);
                } else {
                    log.warn("[AI] Question rejected: {}", reason);
                }
            }
            if (valid.isEmpty()) {
                log.warn("[AI] All {} questions rejected. Sample: {}",
                        rawList.size(),
                        rawJson.length() > 500 ? rawJson.substring(0, 500) : rawJson);
            }
            // Ensure we don't return more than requested
            if (valid.size() > requested) {
                return valid.subList(0, requested);
            }
            return valid;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse AI JSON. Raw length: {}. Preview: {}",
                    cleaned.length(), cleaned.substring(0, Math.min(500, cleaned.length())));
            throw new AIException("AI trả về định dạng không hợp lệ hoặc bị cắt cụt. Vui lòng thử lại với số lượng ít hơn.", e);
        }
    }

    private String getRejectionReason(Map<String, Object> raw, QuestionDTO dto) {
        if (dto == null) return "toQuestionDTO returned null: " + raw;
        String content = (String) raw.get("content");
        String qt = (String) raw.get("questionType");
        String skill = (String) raw.get("skill");
        String cefr = (String) raw.get("cefrLevel");
        Object opts = raw.get("options");
        String ca = (String) raw.get("correctAnswer");
        return String.format(
            "content=%s, type=%s (valid=%s), skill=%s (valid=%s), cefr=%s (valid=%s), " +
            "options=%s, correctAnswer=%s, matchLeft=%s, matchRight=%s, correctPairs=%s",
            content, qt, promptBuilder.isValidQuestionType(qt),
            skill, promptBuilder.isValidSkill(skill),
            cefr, promptBuilder.isValidCefr(cefr),
            opts, ca,
            raw.get("matchLeft"), raw.get("matchRight"), raw.get("correctPairs"));
    }

    // Uses raw m2 map directly (DTO setters for skill/cefrLevel/topic may not have run yet)
    private String getGroupRejectionReason(Map<String, Object> raw, AIGenerateResponseDTO.QuestionDTO dto) {
        String qt = (String) raw.get("questionType");
        String skill = (String) raw.get("skill");
        String cefr = (String) raw.get("cefrLevel");
        Object opts = raw.get("options");
        return String.format(
            "type=%s(valid=%s) skill=%s(valid=%s) cefr=%s(valid=%s) options=%s ca=%s matchL=%s matchR=%s pairs=%s",
            qt, promptBuilder.isValidQuestionType(qt),
            skill, promptBuilder.isValidSkill(skill),
            cefr, promptBuilder.isValidCefr(cefr),
            opts,
            raw.get("correctAnswer"),
            raw.get("matchLeft"), raw.get("matchRight"), raw.get("correctPairs"));
    }

    private QuestionDTO toQuestionDTO(Map<String, Object> raw) {
        try {
            String ca = (String) raw.get("correctAnswer");
            return QuestionDTO.builder()
                    .content((String) raw.get("content"))
                    .transcript((String) raw.get("transcript"))
                    .questionType((String) raw.get("questionType"))
                    .skill((String) raw.get("skill"))
                    .cefrLevel((String) raw.get("cefrLevel"))
                    .topic((String) raw.get("topic"))
                    .explanation((String) raw.get("explanation"))
                    .audioUrl((String) raw.get("audioUrl"))
                    .imageUrl((String) raw.get("imageUrl"))
                    .correctAnswer(ca)
                    .options(parseOptions(raw.get("options"), ca))
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
    private List<AIGenerateResponseDTO.OptionDTO> parseOptions(Object optionsObj, String correctAnswer) {
        if (optionsObj == null) return null;
        try {
            List<?> raw = (List<?>) optionsObj;
            List<AIGenerateResponseDTO.OptionDTO> opts = new ArrayList<>();
            for (Object item : raw) {
                if (item instanceof Map<?, ?> m) {
                    String optionText = (String) m.get("title");
                    if (optionText == null || optionText.isBlank()) optionText = (String) m.get("text");
                    if (optionText == null || optionText.isBlank()) optionText = (String) m.get("content");
                    Boolean isCorrect = (Boolean) m.get("correct");
                    if (isCorrect == null) isCorrect = (Boolean) m.get("isCorrect");
                    if (isCorrect == null) isCorrect = (Boolean) m.get("is_correct");

                    opts.add(AIGenerateResponseDTO.OptionDTO.builder()
                            .title(optionText)
                            .correct(isCorrect != null ? isCorrect : false)
                            .build());
                } else if (item instanceof String s) {
                    // Handle simple string list from AI
                    boolean isCorrect = (correctAnswer != null && s.trim().equalsIgnoreCase(correctAnswer.trim()));
                    opts.add(AIGenerateResponseDTO.OptionDTO.builder()
                            .title(s)
                            .correct(isCorrect)
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
        List<Integer> result = new ArrayList<>();
        try {
            if (obj instanceof List<?>) {
                List<?> raw = (List<?>) obj;
                for (Object o : raw) {
                    if (o == null) continue;
                    if (o instanceof Number n) {
                        result.add(n.intValue());
                    } else {
                        String s = o.toString().trim();
                        // Handle "1-3" or "3"
                        if (s.contains("-")) {
                            String[] parts = s.split("-");
                            result.add(Integer.parseInt(parts[parts.length - 1].trim()));
                        } else {
                            result.add(Integer.parseInt(s));
                        }
                    }
                }
            } else if (obj instanceof String s) {
                // Handle "1, 3, 2" or "[1, 3, 2]"
                String clean = s.replace("[", "").replace("]", "").trim();
                if (!clean.isEmpty()) {
                    String[] parts = clean.split(",");
                    for (String p : parts) {
                        String item = p.trim();
                        if (item.contains("-")) {
                            String[] subParts = item.split("-");
                            result.add(Integer.parseInt(subParts[subParts.length - 1].trim()));
                        } else {
                            result.add(Integer.parseInt(item));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Extensive parseIntegerList failure for object: {}. Error: {}", obj, e.getMessage());
        }
        return result.isEmpty() ? null : result;
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

    private void generateAudioForQuestion(QuestionDTO q) {
        try {
            String textToSpeak = (q.getTranscript() != null && !q.getTranscript().isBlank()) 
                    ? q.getTranscript() : q.getContent();
            if (textToSpeak == null || textToSpeak.isBlank()) return;
            
            byte[] audioBytes = ttsService.synthesizeDialogue(textToSpeak);
            if (audioBytes != null && audioBytes.length > 0) {
                String audioUrl = fileUploadService.uploadBytes(audioBytes, "ai_q_listening", "video");
                q.setAudioUrl(audioUrl);
            }
        } catch (Exception e) {
            log.warn("Failed to generate audio for question: {}", e.getMessage());
        }
    }
}
