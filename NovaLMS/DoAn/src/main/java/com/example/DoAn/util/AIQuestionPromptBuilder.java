package com.example.DoAn.util;

import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@lombok.RequiredArgsConstructor
public class AIQuestionPromptBuilder {

    private final com.example.DoAn.service.IAIPromptConfigService aiPromptConfigService;

    private static final Set<String> VALID_QUESTION_TYPES = Set.of(
            "MULTIPLE_CHOICE_SINGLE", "MULTIPLE_CHOICE_MULTI",
            "FILL_IN_BLANK", "MATCHING", "WRITING", "SPEAKING");
    private static final Set<String> VALID_SKILLS = Set.of(
            "LISTENING", "READING", "WRITING", "SPEAKING");
    private static final Set<String> VALID_CEFR = Set.of(
            "3.0", "3.5", "4.0", "4.5", "5.0", "5.5", "6.0", "6.5", "7.0", "7.5", "8.0", "8.5", "9.0");

    public String buildQuickPrompt(String topic, int quantity, List<String> questionTypes, String skill,
            String cefr, Map<String, Object> advancedOptions) {
        String types = buildTypesClause(questionTypes);
        String constraints = buildAdvancedConstraints(advancedOptions);
        String skillConstraint = (skill != null && !skill.equalsIgnoreCase("MIXED"))
                ? "- Skills: ONLY " + skill.toUpperCase()
                : "- Skills: mix LISTENING, READING, WRITING, SPEAKING";

        return """
                You are a professional English teacher. Generate exactly %d English questions
                about the topic "%s" for students.

                CRITICAL: The final response MUST be a JSON array containing EXACTLY %d question objects. No more, no less.

                IMPORTANT: ALL question content, answer options, and explanations MUST be in ENGLISH only. No Vietnamese.

                Requirements:
                - Question types: %s
                - IELTS Band: EXACTLY %s. DO NOT use any other level.
                - STRICTNESS: The complexity of vocabulary, grammar, and sentence structure MUST be strictly appropriate for IELTS Band %s.
                %s
                - Every question must have: content (The question text), transcript (REQUIRED for LISTENING skill: a natural dialogue between 2-3 characters with SPECIFIC English names like 'Sarah', 'Robert', 'Michael'. FORBIDDEN: 'Man', 'Woman', 'Speaker', 'A', 'B'. Each line MUST start with 'Name [Male/Female]: ...', e.g., 'Robert [Male]: Hello!\\nSarah [Female]: Hi!'), questionType, skill, cefrLevel (IELTS Band), topic, explanation (can be null)
                - MULTIPLE_CHOICE_SINGLE: 4 options, each with "title" (ENGLISH text) and "correct" (true/false), exactly 1 correct = true
                - MULTIPLE_CHOICE_MULTI: 4 options, each with "title" (ENGLISH text) and "correct", 2-3 correct = true
                - FILL_IN_BLANK: content contains "___", correctAnswer is the answer (For LISTENING, focus on short 1-3 word answers or numbers)
                - MATCHING: matchLeft (3-5 English words), matchRight (3-5 English meanings), correctPairs (1-based index order)
                - WRITING, SPEAKING: no options needed

                Return ONLY a JSON array, no other text:
                [
                  {
                    "content": "...",
                    "transcript": "Sarah [Female]: Hello Robert.\\nRobert [Male]: Hi Sarah...",
                    "questionType": "...",
                    "skill": "...",
                    "cefrLevel": "...",
                    "topic": "%s",
                    "explanation": "...",
                    "options": [...],
                    "correctAnswer": "...",
                    "matchLeft": [...],
                    "matchRight": [...],
                    "correctPairs": [...]
                  }
                ]
                """
                .formatted(quantity, topic, quantity, types, cefr, cefr, skillConstraint, topic);
    }

    public String buildContextPrompt(String moduleName, String lessonSummary,
            int quantity, List<String> questionTypes, String skill, String cefr, Map<String, Object> advancedOptions) {
        String types = buildTypesClause(questionTypes);
        String constraints = buildAdvancedConstraints(advancedOptions);
        String skillConstraint = (skill != null && !skill.equalsIgnoreCase("MIXED"))
                ? "- Skills: ONLY " + skill.toUpperCase()
                : "- Skills: mix LISTENING, READING, WRITING, SPEAKING";
        String truncated = lessonSummary.length() > 8000
                ? lessonSummary.substring(0, 8000)
                : lessonSummary;
        return """
                You are a professional English teacher. The module "%s" has the following lesson content:
                %s

                Generate exactly %d English questions appropriate for this content.

                CRITICAL: The final response MUST be a JSON array containing EXACTLY %d question objects. No more, no less.

                IMPORTANT: ALL question content, answer options, and explanations MUST be in ENGLISH only. No Vietnamese.

                Requirements:
                - Question types: %s
                - IELTS Band: EXACTLY %s. DO NOT use any other level.
                %s
                - Every question must have: content, transcript (REQUIRED for LISTENING skill: a natural dialogue between 2-3 characters with REAL English names like 'Emily', 'David', 'Chris'. FORBIDDEN: 'Man', 'Woman', 'Speaker'. Each line MUST start with 'Name [Male/Female]: ...', e.g., 'David [Male]: Welcome...'), questionType, skill, cefrLevel (IELTS Band), topic, explanation (can be null)
                - MULTIPLE_CHOICE_SINGLE: 4 options, each with "title" (ENGLISH text) and "correct" (true/false), exactly 1 correct = true
                - MULTIPLE_CHOICE_MULTI: 4 options, each with "title" (ENGLISH text) and "correct", 2-3 correct = true
                - FILL_IN_BLANK: content contains "___", correctAnswer is the answer
                - MATCHING: matchLeft (3-5 English words), matchRight (3-5 English meanings), correctPairs (1-based index)

                Return ONLY a JSON array, no other text.
                """
                .formatted(moduleName, truncated, quantity, quantity, types, cefr, skillConstraint, constraints);
    }

    /**
     * Builds prompt for generating a passage-based question group.
     * Groq returns flat JSON: { passage, audioUrl, imageUrl, skill, cefrLevel,
     * topic,
     * explanation, questions: [{content, questionType, options, ...}] }
     */
    public String buildGroupPrompt(String topic, String skill, String cefrLevel,
            int questionCount, List<String> questionTypes, Map<String, Object> advancedOptions) {
        String cefr = cefrLevel != null ? cefrLevel : "5.5";
        String topicVal = topic != null ? topic : "General English";
        String skillVal = skill != null ? skill : "READING";

        // Handle advanced options for constraints
        String passageConstraint = "150-400 words";
        String scenarioConstraint = "";
        String matchingConstraint = "";

        if (advancedOptions != null) {
            String readLen = (String) advancedOptions.get("readingLength");
            if ("SHORT".equalsIgnoreCase(readLen))
                passageConstraint = "100-150 words";
            else if ("MEDIUM".equalsIgnoreCase(readLen))
                passageConstraint = "200-300 words";
            else if ("LONG".equalsIgnoreCase(readLen))
                passageConstraint = "400-600 words";

            String listenType = (String) advancedOptions.get("listeningType");
            if (listenType != null) {
                scenarioConstraint = " Specifically, this MUST be a " + listenType + " scenario.";
            }

            Integer matchPairs = (Integer) advancedOptions.get("matchingPairs");
            if (matchPairs != null) {
                matchingConstraint = " For MATCHING questions, generate exactly " + matchPairs + " pairs.";
            }
        }

        // Xác định nhãn nội dung dựa trên skill
        String contentLabel = "LISTENING".equalsIgnoreCase(skillVal) ? "Transcript bài nghe" : "Đoạn văn đọc (Passage)";
        String listenType = (advancedOptions != null) ? (String) advancedOptions.get("listeningType") : null;
        boolean isDialogue = listenType == null || "Dialogue".equalsIgnoreCase(listenType);

        String taskInstruction = "LISTENING".equalsIgnoreCase(skillVal)
                ? "Đoạn văn này sẽ được dùng làm kịch bản (transcript) cho bài nghe. " +
                  (isDialogue 
                    ? "YÊU CẦU BẮT BUỘC: Đây PHẢI là một đoạn hội thoại (dialogue) tự nhiên giữa 2-3 nhân vật có TÊN RIÊNG cụ thể (ví dụ: Robert, Sarah, John, Mary, ...). " +
                      "KHÔNG ĐƯỢC sử dụng tên nhân vật chung chung như 'Man', 'Woman', 'Speaker 1', 'A', 'B'. " +
                      "Mỗi lời thoại PHẢI bắt đầu bằng dòng mới và BẮT BUỘC có nhãn giới tính ngay sau tên để hệ thống đổi giọng, ví dụ: 'Robert [Male]: Hello Sarah!\\nSarah [Female]: Hi Robert, how are you?'."
                    : "YÊU CẦU: Đây PHẢI là một bản " + listenType + " (monologue/announcement). " +
                      "Sử dụng tên nhân vật cụ thể và nhãn giới tính ở đầu, ví dụ: 'Professor Smith [Male]: ...'.")
                : "Đoạn văn này dùng cho bài đọc hiểu.";

        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert English language test designer.\n\n");
        sb.append("Generate a ").append(skillVal).append(" question group in JSON format.\n");
        sb.append("Requirement: ").append(taskInstruction).append("\n");
        sb.append("The content must be in English and EXCLUSIVELY appropriate for IELTS Band ").append(cefr).append(".\n");
        sb.append("STRICTNESS: Do NOT use advanced vocabulary or complex grammar that belongs to higher IELTS bands. Keep it strictly at Band ").append(cefr).append(" level.\n\n");
        sb.append("Return ONLY a valid JSON object with this exact structure:\n");
        sb.append("{\n");
        sb.append("  \"passage\": \"<").append(contentLabel).append(" text, ").append(passageConstraint).append(">\",\n");
        sb.append("  \"audioUrl\": null, \"imageUrl\": null,\n");
        sb.append("  \"skill\": \"").append(skillVal).append("\", \"cefrLevel\": \"").append(cefr).append("\", \"topic\": \"").append(topicVal).append("\",\n");
        sb.append("  \"explanation\": \"...\",\n");
        sb.append("  \"questions\": [\n");
        sb.append("    {\n");
        sb.append("      \"content\": \"...\", \"questionType\": \"MULTIPLE_CHOICE_SINGLE\",\n");
        sb.append("      \"options\": [{ \"title\": \"...\", \"correct\": true }, ...],\n");
        sb.append("      \"correctAnswer\": null, \"matchLeft\": null, \"matchRight\": null, \"correctPairs\": null,\n");
        sb.append("      \"cefrLevel\": \"").append(cefr).append("\", \"topic\": \"").append(topicVal).append("\", \"explanation\": \"...\"\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}\n\n");

        sb.append("Rules:\n");
        sb.append("- Generate EXACTLY ").append(questionCount).append(" questions in the 'questions' array.\n");
        sb.append("- For LISTENING, use 2-3 different characters with REAL names (e.g., Emily, Chris, David).\n");
        sb.append("- FORBIDDEN: Man, Woman, Speaker, A, B names.\n");
        sb.append("- All content MUST be in English only. No Vietnamese.\n");
        sb.append("- Return ONLY the JSON object, no markdown fences, no commentary.\n");

        return sb.toString();
    }

    private String buildAdvancedConstraints(Map<String, Object> advancedOptions) {
        if (advancedOptions == null || advancedOptions.isEmpty())
            return "";
        StringBuilder sb = new StringBuilder("\nAdvanced Constraints:\n");

        String listenType = (String) advancedOptions.get("listeningType");
        if (listenType != null)
            sb.append("- Listening Type: MUST be a ").append(listenType).append("\n");

        Integer matchPairs = (Integer) advancedOptions.get("matchingPairs");
        if (matchPairs != null)
            sb.append("- Matching Pairs: For MATCHING questions, generate exactly ").append(matchPairs)
                    .append(" pairs.\n");

        String readLen = (String) advancedOptions.get("readingLength");
        if (readLen != null)
            sb.append("- Reading Length: Passage should be ").append(readLen).append("\n");

        return sb.toString();
    }

    private String buildTypesClause(List<String> questionTypes) {
        if (questionTypes == null || questionTypes.isEmpty()) {
            return "MIXED: MULTIPLE_CHOICE_SINGLE, MULTIPLE_CHOICE_MULTI, FILL_IN_BLANK, MATCHING, WRITING, SPEAKING";
        }
        return String.join(", ", questionTypes);
    }

    public boolean isValidQuestionType(String type) {
        return type != null && VALID_QUESTION_TYPES.contains(type);
    }

    public boolean isValidSkill(String skill) {
        return skill != null && VALID_SKILLS.contains(skill.toUpperCase());
    }

    public boolean isValidCefr(String cefr) {
        return cefr != null && VALID_CEFR.contains(cefr.toUpperCase());
    }

    // ─── ADVANCED MODE ─────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> getBucketConfig(String cefr) {
        String bucket = getBucket(cefr);
        Map<String, Object> config = aiPromptConfigService.getBucketConfigAsMap(bucket);
        if (config == null) {
            log.warn("[AI_CONFIG] No config found for bucket: {}. Using fallbacks.", bucket);
            return getFallbackBucketConfig(bucket);
        }
        
        // Log missing keys to help debug
        List<String> required = List.of("bloom_instruction", "grammar_focus", "skills", "writing_constraint", "speaking_constraint");
        for (String key : required) {
            if (!config.containsKey(key) || config.get(key) == null) {
                log.warn("[AI_CONFIG] Bucket '{}' is missing required key: {}. PromptBuilder will use defaults.", bucket, key);
            }
        }
        
        return config;
    }

    private String getBucket(String cefr) {
        if (cefr == null) return "intermediate";
        String u = cefr.toUpperCase();
        try {
            double band = Double.parseDouble(u);
            if (band <= 4.5) return "beginner";
            if (band <= 6.5) return "intermediate";
            return "advanced";
        } catch (NumberFormatException e) {
            return "intermediate";
        }
    }

    private Map<String, Object> getFallbackBucketConfig(String bucket) {
        return switch (bucket) {
            case "beginner" -> Map.of(
                    "bloom_instruction", "Use REMEMBER/UNDERSTAND/APPLY verbs.",
                    "grammar_focus", List.of("Present/Past Simple", "Basic collocations"),
                    "question_types_ratio",
                    Map.of("MULTIPLE_CHOICE_SINGLE", 0.35, "FILL_IN_BLANK", 0.30, "WRITING", 0.20, "SPEAKING", 0.15),
                    "skills", List.of("READING", "LISTENING", "WRITING"),
                    "writing_constraint", "50-80 words",
                    "speaking_constraint", "2-4 sentences");
            case "intermediate" -> Map.of(
                    "bloom_instruction", "Use ANALYZE verbs. Include conditionals, idioms.",
                    "grammar_focus", List.of("Conditionals", "Passive voice", "Idioms B1"),
                    "question_types_ratio",
                    Map.of("MULTIPLE_CHOICE_SINGLE", 0.20, "MULTIPLE_CHOICE_MULTI", 0.15, "FILL_IN_BLANK", 0.15,
                            "MATCHING", 0.10, "WRITING", 0.25, "SPEAKING", 0.15),
                    "skills", List.of("READING", "LISTENING", "WRITING", "SPEAKING"),
                    "writing_constraint", "80-150 words",
                    "speaking_constraint", "4-8 sentences");
            default -> Map.of(
                    "bloom_instruction", "Use ANALYZE/EVALUATE/CREATE verbs. Formal register.",
                    "grammar_focus", List.of("Subjunctive", "Complex structures", "Discourse markers"),
                    "question_types_ratio",
                    Map.of("MULTIPLE_CHOICE_MULTI", 0.10, "FILL_IN_BLANK", 0.05, "WRITING", 0.50, "SPEAKING", 0.35),
                    "skills", List.of("WRITING", "SPEAKING"),
                    "writing_constraint", "150-300 words, formal register",
                    "speaking_constraint", "Extended responses, justify opinions");
        };
    }

    public String buildAdvancedContextPrompt(String moduleName, String lessonSummary,
            int quantity, List<String> questionTypes,
            String cefrLevel, String targetSkill, Map<String, Object> advancedOptions) {
        String types = buildTypesClause(questionTypes);
        String truncated = lessonSummary.length() > 8000 ? lessonSummary.substring(0, 8000) : lessonSummary;
        String constraints = buildAdvancedConstraints(advancedOptions);
        Map<String, Object> cfg = getBucketConfig(cefrLevel);

        String bloomInstruction = String.valueOf(cfg.getOrDefault("bloom_instruction", "Use appropriate IELTS-level verbs (Analyze/Evaluate)."));
        List<String> grammarFocus = (List<String>) cfg.getOrDefault("grammar_focus", List.of("General IELTS grammar"));

        String skillsInstruction;
        if (targetSkill != null && !targetSkill.equalsIgnoreCase("MIXED") && isValidSkill(targetSkill)) {
            skillsInstruction = "ONLY " + targetSkill.toUpperCase();
        } else {
            List<String> bucketSkills = (List<String>) cfg.getOrDefault("skills", List.of("READING", "LISTENING", "WRITING", "SPEAKING"));
            skillsInstruction = "mix " + String.join(", ", bucketSkills);
        }

        String writingConstraint = String.valueOf(cfg.getOrDefault("writing_constraint", "80-150 words"));
        String speakingConstraint = String.valueOf(cfg.getOrDefault("speaking_constraint", "4-8 sentences"));

        return """
                You are a professional English teacher specializing in advanced question design for IELTS Band %s.
                The module "%s" has the following lesson content:
                %s

                CRITICAL: The final response MUST be a JSON array containing EXACTLY %d question objects. No more, no less.

                IMPORTANT: ALL question content, answer options, and explanations MUST be in ENGLISH only. No Vietnamese.

                ## ADVANCED MODE REQUIREMENTS

                ### Bloom's Taxonomy Focus
                %s

                ### Grammar & Language Focus
                Generate questions that demonstrate mastery of:
                %s

                ### Skills Distribution
                Prioritize these skills: %s

                ### Question Requirements
                - Generate exactly %d questions following these Bloom levels and grammar focus.
                - Question types: %s
                - CEFR level: %s
                %s
                - WRITING questions: %s
                - SPEAKING questions: %s

                Every question must have: content, questionType, skill, cefrLevel, topic, explanation (can be null).

                Question type specifics:
                - MULTIPLE_CHOICE_SINGLE: 4 options (ENGLISH text), exactly 1 correct = true
                - MULTIPLE_CHOICE_MULTI: 4 options, 2-3 correct = true (but not all)
                - FILL_IN_BLANK: content contains "___", correctAnswer required (For LISTENING, focus on short 1-3 word answers or numbers)
                - MATCHING: matchLeft (3-5 English words), matchRight (3-5 meanings), correctPairs (1-based indices)
                - WRITING, SPEAKING: no options needed

                IMPORTANT: Every "title" field must contain real ENGLISH text, not null, empty, or just a number.
                Do NOT generate questions that only test recall or simple comprehension. Focus on analysis, evaluation, or creation.
                
                CRITICAL: The field "cefrLevel" MUST contain a numerical IELTS Band (e.g. "6.0", "7.5"). DO NOT use "B2", "C1", etc.

                Return ONLY a JSON array, no other text:
                [
                  {
                    "content": "...",
                    "questionType": "...",
                    "skill": "%s",
                    "cefrLevel": "%s",
                    "topic": "%s",
                    "explanation": "...",
                    "options": [...],
                    "correctAnswer": "...",
                    "matchLeft": [...],
                    "matchRight": [...],
                    "correctPairs": [...]
                  }
                ]
                """
                .formatted(
                        cefrLevel, moduleName, truncated, quantity,
                        bloomInstruction,
                        "        - " + String.join("\n        - ", grammarFocus),
                        skillsInstruction,
                        quantity, types, cefrLevel,
                        constraints,
                        writingConstraint,
                        speakingConstraint,
                        targetSkill, cefrLevel, moduleName);
    }

    @SuppressWarnings("unchecked")
    public String buildAdvancedQuickPrompt(String topic, int quantity,
            List<String> questionTypes,
            String cefrLevel, String targetSkill, Map<String, Object> advancedOptions) {
        return buildAdvancedPromptInternal(false, topic, quantity, questionTypes, cefrLevel, targetSkill, advancedOptions);
    }

    @SuppressWarnings("unchecked")
    public String buildAdvancedGroupPrompt(String topic, int quantity,
            List<String> questionTypes,
            String cefrLevel, String targetSkill, Map<String, Object> advancedOptions) {
        return buildAdvancedPromptInternal(true, topic, quantity, questionTypes, cefrLevel, targetSkill, advancedOptions);
    }

    private String buildAdvancedPromptInternal(boolean isGroup, String topic, int quantity,
            List<String> questionTypes,
            String cefrLevel, String targetSkill, Map<String, Object> advancedOptions) {
        String types = buildTypesClause(questionTypes);
        String constraints = buildAdvancedConstraints(advancedOptions);
        Map<String, Object> cfg = getBucketConfig(cefrLevel);

        String bloomInstruction = String.valueOf(cfg.getOrDefault("bloom_instruction", "Use appropriate IELTS-level verbs (Analyze/Evaluate)."));
        List<String> grammarFocus = (List<String>) cfg.getOrDefault("grammar_focus", List.of("General IELTS grammar"));

        String skillsInstruction;
        if (targetSkill != null && !targetSkill.equalsIgnoreCase("MIXED") && isValidSkill(targetSkill)) {
            skillsInstruction = "ONLY " + targetSkill.toUpperCase();
        } else {
            List<String> bucketSkills = (List<String>) cfg.getOrDefault("skills", List.of("READING", "LISTENING", "WRITING", "SPEAKING"));
            skillsInstruction = "mix " + String.join(", ", bucketSkills);
        }

        String writingConstraint = String.valueOf(cfg.getOrDefault("writing_constraint", "80-150 words"));
        String speakingConstraint = String.valueOf(cfg.getOrDefault("speaking_constraint", "4-8 sentences"));

        String listenType = (advancedOptions != null) ? (String) advancedOptions.get("listeningType") : null;
        boolean isDialogue = listenType == null || "Dialogue".equalsIgnoreCase(listenType);

        String taskTypeInstruction;
        if (isGroup) {
            if ("LISTENING".equalsIgnoreCase(targetSkill)) {
                String subTypeInstruction;
                if ("Dialogue".equalsIgnoreCase(listenType) || "Interview".equalsIgnoreCase(listenType)) {
                    subTypeInstruction = "MANDATORY: This MUST be a " + (listenType != null ? listenType : "dialogue") + 
                        " between 2-3 characters with REAL English names (e.g., Sarah, Robert, Michael). FORBIDDEN: Man, Woman, Speaker, A, B. Every line MUST start with a speaker label and gender, e.g., 'David [Male]: ...' or 'Sarah [Female]: ...'.";
                } else if ("Lecture".equalsIgnoreCase(listenType) || "Announcement".equalsIgnoreCase(listenType) || "Monologue".equalsIgnoreCase(listenType)) {
                    subTypeInstruction = "MANDATORY: This MUST be a " + (listenType != null ? listenType : "monologue") + 
                        ". Use a REAL name at the start, e.g., 'Professor Smith [Male]: ...' or 'Dr. Miller [Female]: ...'. FORBIDDEN: Man, Woman, Speaker.";
                } else {
                    subTypeInstruction = "Generate a realistic listening scenario (dialogue between 2-3 people) with REAL English names and gender tags [Male]/[Female].";
                }
                taskTypeInstruction = "Generate a LISTENING task. The 'passage' field must contain the FULL transcript. " + subTypeInstruction + " Use diverse English names.";
            } else {
                taskTypeInstruction = "Generate a READING task. The 'passage' field must contain the reading text.";
            }
        } else {
            if ("LISTENING".equalsIgnoreCase(targetSkill)) {
                taskTypeInstruction = "Generate independent LISTENING questions. For each question, you MUST provide a 'transcript' field containing a natural dialogue between 2-3 characters (2-4 lines) with REAL English names. " +
                    "FORBIDDEN: Man, Woman, Speaker. Use speaker labels with gender, e.g., 'Robert [Male]: ...' or 'Emily [Female]: ...'. The 'content' field should contain ONLY the actual question text.";
            } else if ("WRITING".equalsIgnoreCase(targetSkill)) {
                taskTypeInstruction = "Generate independent WRITING questions. For each question, provide a clear writing task prompt in the 'content' field (e.g., IELTS Writing Task 1 or Task 2 style).";
            } else if ("SPEAKING".equalsIgnoreCase(targetSkill)) {
                taskTypeInstruction = "Generate independent SPEAKING questions. For each question, provide a clear speaking prompt or topic in the 'content' field (e.g., IELTS Speaking Part 1, 2, or 3 style).";
            } else {
                taskTypeInstruction = "Generate independent advanced English questions.";
            }
        }

        String structureHeader = isGroup
                ? "CRITICAL: The final response MUST be a single JSON object containing a 'passage' string and a 'questions' array. The 'questions' array MUST contain EXACTLY %d question objects based ON the passage.".formatted(quantity)
                : "CRITICAL: The final response MUST be a JSON array containing EXACTLY %d question objects. No more, no less.".formatted(quantity);

        String jsonStructure = isGroup ? """
                {
                  "passage": "<Reading passage or Listening transcript based on skill>",
                  "skill": "%s",
                  "cefrLevel": "%s",
                  "topic": "%s",
                  "explanation": "...",
                  "questions": [
                    {
                      "content": "The actual question text (e.g. Where are they?)",
                      "transcript": "Robert [Male]: Hello Sarah.\\nSarah [Female]: Hi Robert...",
                      "questionType": "...",
                      "options": [
                        { "title": "...", "correct": true },
                        { "title": "...", "correct": false }
                      ],
                      "correctAnswer": "text for fill_in_blank",
                      "matchLeft": ["item 1", "item 2"],
                      "matchRight": ["match 1", "match 2"],
                      "correctPairs": [1, 2],
                      "explanation": "..."
                    }
                  ]
                }
                """.formatted(targetSkill, cefrLevel, topic) : """
                [
                  {
                    "content": "The actual question text or writing/speaking prompt",
                    "questionType": "...",
                    "skill": "%s",
                    "cefrLevel": "%s",
                    "topic": "%s",
                    "explanation": "...",
                    "options": [...],
                    "correctAnswer": "...",
                    "matchLeft": [...],
                    "matchRight": [...],
                    "correctPairs": [...]
                  }
                ]
                """.formatted(targetSkill, cefrLevel, topic);

        return """
                You are a professional English teacher specializing in advanced question design.
                TARGET LEVEL: You MUST generate content strictly for IELTS Band %s.
                %s
                Topic: "%s"

                %s

                IMPORTANT: ALL question content, answer options, and explanations MUST be in ENGLISH only. No Vietnamese.

                ## ADVANCED MODE REQUIREMENTS

                ### Bloom's Taxonomy Focus
                %s

                ### Grammar & Language Focus
                Generate questions that demonstrate mastery of:
                %s

                ### Skills Distribution
                Prioritize these skills: %s

                ### Question Requirements
                - Generate exactly %d questions following these Bloom levels and grammar focus.
                - Question types: %s
                - CEFR level: %s
                %s
                - WRITING questions: %s
                - SPEAKING questions: %s

                Every question must have: content, questionType, skill, cefrLevel, topic, explanation (can be null).

                Question type specifics:
                - MULTIPLE_CHOICE_SINGLE: 4 options (ENGLISH text), exactly 1 correct = true
                - MULTIPLE_CHOICE_MULTI: 4 options, 2-3 correct = true (but not all)
                - FILL_IN_BLANK: content contains "___", correctAnswer required (For LISTENING, focus on short 1-3 word answers or numbers)
                - MATCHING: matchLeft (3-5 English words), matchRight (3-5 meanings), correctPairs (1-based indices)
                - WRITING, SPEAKING: no options needed

                IMPORTANT: Every "title" field must contain real ENGLISH text, not null, empty, or just a number.
                Do NOT generate questions that only test recall or simple comprehension. Focus on analysis, evaluation, or creation.
                
                CRITICAL: The complexity of ALL generated content (passage, questions, options) MUST NOT exceed IELTS Band %s.
                CRITICAL: The field "cefrLevel" MUST be exactly "%s". DO NOT use any other value.
                
                %s
                
                Return ONLY the JSON, no other text.
                """
                .formatted(
                        cefrLevel,
                        taskTypeInstruction,
                        topic,
                        structureHeader,
                        bloomInstruction,
                        "        - " + String.join("\n        - ", grammarFocus),
                        skillsInstruction,
                        quantity, types, cefrLevel,
                        constraints,
                        writingConstraint,
                        speakingConstraint,
                        cefrLevel,
                        cefrLevel,
                        jsonStructure);
    }
}
