package com.example.DoAn.util;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;
import lombok.extern.slf4j.Slf4j;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class AIQuestionPromptBuilder {

    private static final Set<String> VALID_QUESTION_TYPES = Set.of(
            "MULTIPLE_CHOICE_SINGLE", "MULTIPLE_CHOICE_MULTI",
            "FILL_IN_BLANK", "MATCHING", "WRITING", "SPEAKING"
    );
    private static final Set<String> VALID_SKILLS = Set.of(
            "LISTENING", "READING", "WRITING", "SPEAKING"
    );
    private static final Set<String> VALID_CEFR = Set.of(
            "A1", "A2", "B1", "B2", "C1", "C2"
    );

    public String buildQuickPrompt(String topic, int quantity, List<String> questionTypes) {
        String types = buildTypesClause(questionTypes);
        return """
                You are a professional English teacher. Generate exactly %d English questions
                about the topic "%s" for students.

                IMPORTANT: ALL question content, answer options, and explanations MUST be in ENGLISH only. No Vietnamese.

                Requirements:
                - Question types: %s
                - CEFR levels: mix from A1 to C2
                - Skills: mix LISTENING, READING, WRITING, SPEAKING
                - Every question must have: content, questionType, skill, cefrLevel, topic, explanation (can be null)
                - MULTIPLE_CHOICE_SINGLE: 4 options, each with "title" (ENGLISH text) and "correct" (true/false), exactly 1 correct = true
                - MULTIPLE_CHOICE_MULTI: 4 options, each with "title" (ENGLISH text) and "correct", 2-3 correct = true
                - FILL_IN_BLANK: content contains "___", correctAnswer is the answer to fill in
                - MATCHING: matchLeft (3-5 English words), matchRight (3-5 English meanings), correctPairs (1-based index order)
                - WRITING, SPEAKING: no options needed

                IMPORTANT: Every "title" field must contain real ENGLISH text. Do NOT leave title as null, empty, or a number.

                Return ONLY a JSON array, no other text:
                [
                  {
                    "content": "...",
                    "questionType": "...",
                    "skill": "...",
                    "cefrLevel": "...",
                    "topic": "%s",
                    "explanation": "...",
                    "options": [
                      {"title": "Paris", "correct": true},
                      {"title": "London", "correct": false},
                      {"title": "Berlin", "correct": false},
                      {"title": "Madrid", "correct": false}
                    ],
                    "correctAnswer": "...",
                    "matchLeft": [...],
                    "matchRight": [...],
                    "correctPairs": [...]
                  }
                ]
                """.formatted(quantity, topic, types, topic);
    }

    public String buildContextPrompt(String moduleName, String lessonSummary,
                                     int quantity, List<String> questionTypes) {
        String types = buildTypesClause(questionTypes);
        String truncated = lessonSummary.length() > 8000
                ? lessonSummary.substring(0, 8000)
                : lessonSummary;
        return """
                You are a professional English teacher. The module "%s" has the following lesson content:
                %s

                Generate exactly %d English questions appropriate for this content.

                IMPORTANT: ALL question content, answer options, and explanations MUST be in ENGLISH only. No Vietnamese.

                Requirements:
                - Question types: %s
                - CEFR levels: mix from A1 to C2
                - Skills: mix LISTENING, READING, WRITING, SPEAKING
                - Every question must have: content, questionType, skill, cefrLevel, topic, explanation (can be null)
                - MULTIPLE_CHOICE_SINGLE: 4 options, each with "title" (ENGLISH text) and "correct" (true/false), exactly 1 correct = true
                - MULTIPLE_CHOICE_MULTI: 4 options, each with "title" (ENGLISH text) and "correct", 2-3 correct = true
                - FILL_IN_BLANK: content contains "___", correctAnswer is the answer to fill in
                - MATCHING: matchLeft (3-5 English words), matchRight (3-5 English meanings), correctPairs (1-based index)
                - WRITING, SPEAKING: no options needed

                IMPORTANT: Every "title" field must contain real ENGLISH text. Do NOT leave title as null, empty, or a number.

                Return ONLY a JSON array, no other text:
                [
                  {
                    "content": "...",
                    "questionType": "...",
                    "skill": "...",
                    "cefrLevel": "...",
                    "topic": "%s",
                    "explanation": "...",
                    "options": [
                      {"title": "Paris", "correct": true},
                      {"title": "London", "correct": false},
                      {"title": "Berlin", "correct": false},
                      {"title": "Madrid", "correct": false}
                    ],
                    "correctAnswer": "...",
                    "matchLeft": [...],
                    "matchRight": [...],
                    "correctPairs": [...]
                  }
                ]
                """.formatted(moduleName, truncated, quantity, types, moduleName);
    }

    /**
     * Builds prompt for generating a passage-based question group.
     * Groq returns flat JSON: { passage, audioUrl, imageUrl, skill, cefrLevel, topic,
     *                             explanation, questions: [{content, questionType, options, ...}] }
     */
    public String buildGroupPrompt(String topic, String skill, String cefrLevel,
                                  int questionCount, List<String> questionTypes) {
        String types = buildTypesClause(questionTypes);
        String cefr = cefrLevel != null ? cefrLevel : "B1";
        String topicVal = topic != null ? topic : "General English";
        String skillVal = skill != null ? skill : "READING";

        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert English language test designer.\n\n");
        sb.append("Generate a passage-based question group in JSON format. ");
        sb.append("The passage must be in English and should be interesting, educational, and appropriate for CEFR level ").append(cefr).append(".\n\n");
        sb.append("Return ONLY a valid JSON object with this exact structure (no markdown, no explanation):\n");
        sb.append("{\n");
        sb.append("  \"passage\": \"<English passage text, 150-400 words>\",\n");
        sb.append("  \"audioUrl\": null,\n");
        sb.append("  \"imageUrl\": null,\n");
        sb.append("  \"skill\": \"").append(skillVal).append("\",\n");
        sb.append("  \"cefrLevel\": \"").append(cefr).append("\",\n");
        sb.append("  \"topic\": \"").append(topicVal).append("\",\n");
        sb.append("  \"explanation\": \"<brief explanation of the passage>\",\n");
        sb.append("  \"questions\": [\n");
        sb.append("    {\n");
        sb.append("      \"content\": \"<question text in English>\",\n");
        sb.append("      \"questionType\": \"<MULTIPLE_CHOICE_SINGLE or MULTIPLE_CHOICE_MULTI or FILL_IN_BLANK or MATCHING>\",\n");
        sb.append("      \"options\": [{ \"title\": \"<option text in English>\", \"correct\": <true or false> }, ...],\n");
        sb.append("      \"correctAnswer\": <null or answer text for FILL_IN_BLANK>,\n");
        sb.append("      \"matchLeft\": <null or [\"word1\",\"word2\"]>,\n");
        sb.append("      \"matchRight\": <null or [\"meaning1\",\"meaning2\"]>,\n");
        sb.append("      \"correctPairs\": <null or [1,2] where numbers are 1-based indices into matchRight>,\n");
        sb.append("      \"cefrLevel\": \"").append(cefr).append("\",\n");
        sb.append("      \"topic\": \"").append(topicVal).append("\",\n");
        sb.append("      \"explanation\": \"<explanation for this specific question>\"\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}\n\n");

        sb.append("Rules:\n");
        sb.append("- Generate exactly ").append(questionCount).append(" questions.\n");
        sb.append("- Question types allowed: ").append(types).append(".\n");
        sb.append("- MULTIPLE_CHOICE_SINGLE: exactly 1 correct answer, at least 3 distractors, all options in English.\n");
        sb.append("- MULTIPLE_CHOICE_MULTI: 2 or more correct answers (but not all).\n");
        sb.append("- FILL_IN_BLANK: must have a correctAnswer field.\n");
        sb.append("- MATCHING: matchLeft (words/phrases) and matchRight (definitions/meanings) must have the same size (3-5 items each).\n");
        sb.append("  CRITICAL - correctPairs format: a JSON array of 1-based INTEGER indices into the matchRight array.\n");
        sb.append("  The Nth value in correctPairs is the 1-based index in matchRight that matches the Nth item in matchLeft.\n");
        sb.append("  Example: matchLeft=[\"Libraries\",\"Laboratories\"] matchRight=[\"books\",\"experiments\"]\n");
        sb.append("    If Libraries→books (position 1) and Laboratories→experiments (position 2): correctPairs=[1,2]\n");
        sb.append("    If Libraries→experiments (position 2) and Laboratories→books (position 1): correctPairs=[2,1]\n");
        sb.append("  IMPORTANT: The passage MUST explicitly state each pair. If the passage only mentions 'Libraries provide books' then Libraries→books is valid. Do NOT guess pairs that are not clearly stated in the passage.\n");
        sb.append("- All content, options, explanations MUST be in English only. No Vietnamese.\n");
        sb.append("- Every 'title' must be real English text, not null, empty, or just a number.\n");
        sb.append("- Return ONLY the JSON object, no markdown fences, no commentary.\n");

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

    private static final String ADVANCED_CONFIG_PATH = "config/ai-prompt-advanced.yaml";
    private Map<String, Object> cachedAdvancedConfig;

    private Map<String, Object> loadAdvancedConfig() {
        if (cachedAdvancedConfig != null) return cachedAdvancedConfig;
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream(ADVANCED_CONFIG_PATH);
            if (is == null) {
                log.warn("Advanced config file not found: {}", ADVANCED_CONFIG_PATH);
                return null;
            }
            cachedAdvancedConfig = new Yaml().load(is);
            is.close();
            return cachedAdvancedConfig;
        } catch (Exception e) {
            log.warn("Failed to load advanced config: {}", e.getMessage());
            return null;
        }
    }

    private String getBucket(String cefr) {
        return switch (cefr == null ? "B1" : cefr.toUpperCase()) {
            case "A1", "A2" -> "beginner";
            case "B1", "B2" -> "intermediate";
            case "C1", "C2" -> "advanced";
            default          -> "intermediate";
        };
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getBucketConfig(String cefr) {
        Map<String, Object> config = loadAdvancedConfig();
        if (config == null) return getFallbackBucketConfig(getBucket(cefr));
        Map<String, Object> advanced = (Map<String, Object>) config.get("advanced");
        if (advanced == null) return getFallbackBucketConfig(getBucket(cefr));
        String bucket = getBucket(cefr);
        Map<String, Object> bucketConfig = (Map<String, Object>) advanced.get(bucket);
        if (bucketConfig == null) return getFallbackBucketConfig(bucket);
        return bucketConfig;
    }

    private Map<String, Object> getFallbackBucketConfig(String bucket) {
        return switch (bucket) {
            case "beginner" -> Map.of(
                "bloom_instruction", "Use REMEMBER/UNDERSTAND/APPLY verbs.",
                "grammar_focus", List.of("Present/Past Simple", "Basic collocations"),
                "question_types_ratio", Map.of("MULTIPLE_CHOICE_SINGLE", 0.35, "FILL_IN_BLANK", 0.30, "WRITING", 0.20, "SPEAKING", 0.15),
                "skills", List.of("READING", "LISTENING", "WRITING"),
                "writing_constraint", "50-80 words",
                "speaking_constraint", "2-4 sentences"
            );
            case "intermediate" -> Map.of(
                "bloom_instruction", "Use ANALYZE verbs. Include conditionals, idioms.",
                "grammar_focus", List.of("Conditionals", "Passive voice", "Idioms B1"),
                "question_types_ratio", Map.of("MULTIPLE_CHOICE_SINGLE", 0.20, "MULTIPLE_CHOICE_MULTI", 0.15, "FILL_IN_BLANK", 0.15, "MATCHING", 0.10, "WRITING", 0.25, "SPEAKING", 0.15),
                "skills", List.of("READING", "LISTENING", "WRITING", "SPEAKING"),
                "writing_constraint", "80-150 words",
                "speaking_constraint", "4-8 sentences"
            );
            default -> Map.of(
                "bloom_instruction", "Use ANALYZE/EVALUATE/CREATE verbs. Formal register.",
                "grammar_focus", List.of("Subjunctive", "Complex structures", "Discourse markers"),
                "question_types_ratio", Map.of("MULTIPLE_CHOICE_MULTI", 0.10, "FILL_IN_BLANK", 0.05, "WRITING", 0.50, "SPEAKING", 0.35),
                "skills", List.of("WRITING", "SPEAKING"),
                "writing_constraint", "150-300 words, formal register",
                "speaking_constraint", "Extended responses, justify opinions"
            );
        };
    }

    @SuppressWarnings("unchecked")
    public String buildAdvancedContextPrompt(String moduleName, String lessonSummary,
                                             int quantity, List<String> questionTypes,
                                             String cefrLevel) {
        String types = buildTypesClause(questionTypes);
        String truncated = lessonSummary.length() > 8000 ? lessonSummary.substring(0, 8000) : lessonSummary;
        Map<String, Object> cfg = getBucketConfig(cefrLevel);

        String bloomInstruction = cfg.get("bloom_instruction").toString();
        List<String> grammarFocus = (List<String>) cfg.get("grammar_focus");
        List<String> skills = (List<String>) cfg.get("skills");
        String writingConstraint = cfg.get("writing_constraint").toString();
        String speakingConstraint = cfg.get("speaking_constraint").toString();

        return """
            You are a professional English teacher specializing in advanced question design for CEFR level %s.
            The module "%s" has the following lesson content:
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
            - WRITING questions: %s
            - SPEAKING questions: %s

            Every question must have: content, questionType, skill, cefrLevel, topic, explanation (can be null).

            Question type specifics:
            - MULTIPLE_CHOICE_SINGLE: 4 options (ENGLISH text), exactly 1 correct = true
            - MULTIPLE_CHOICE_MULTI: 4 options, 2-3 correct = true (but not all)
            - FILL_IN_BLANK: content contains "___", correctAnswer required
            - MATCHING: matchLeft (3-5 English words), matchRight (3-5 meanings), correctPairs (1-based indices)
            - WRITING, SPEAKING: no options needed

            IMPORTANT: Every "title" field must contain real ENGLISH text, not null, empty, or just a number.
            Do NOT generate questions that only test recall or simple comprehension. Focus on analysis, evaluation, or creation.

            Return ONLY a JSON array, no other text:
            [
              {
                "content": "...",
                "questionType": "...",
                "skill": "...",
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
            """.formatted(
                cefrLevel, moduleName, truncated,
                bloomInstruction,
                "        - " + String.join("\n        - ", grammarFocus),
                String.join(", ", skills),
                quantity, types, cefrLevel,
                writingConstraint,
                speakingConstraint,
                cefrLevel, moduleName
            );
    }

    @SuppressWarnings("unchecked")
    public String buildAdvancedQuickPrompt(String topic, int quantity,
                                           List<String> questionTypes,
                                           String cefrLevel) {
        String types = buildTypesClause(questionTypes);
        Map<String, Object> cfg = getBucketConfig(cefrLevel);

        String bloomInstruction = cfg.get("bloom_instruction").toString();
        List<String> grammarFocus = (List<String>) cfg.get("grammar_focus");
        List<String> skills = (List<String>) cfg.get("skills");
        String writingConstraint = cfg.get("writing_constraint").toString();
        String speakingConstraint = cfg.get("speaking_constraint").toString();

        return """
            You are a professional English teacher specializing in advanced question design for CEFR level %s.
            Generate exactly %d advanced English questions about the topic "%s".

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
            - WRITING questions: %s
            - SPEAKING questions: %s

            Every question must have: content, questionType, skill, cefrLevel, topic, explanation (can be null).

            Question type specifics:
            - MULTIPLE_CHOICE_SINGLE: 4 options (ENGLISH text), exactly 1 correct = true
            - MULTIPLE_CHOICE_MULTI: 4 options, 2-3 correct = true (but not all)
            - FILL_IN_BLANK: content contains "___", correctAnswer required
            - MATCHING: matchLeft (3-5 English words), matchRight (3-5 meanings), correctPairs (1-based indices)
            - WRITING, SPEAKING: no options needed

            IMPORTANT: Every "title" field must contain real ENGLISH text, not null, empty, or just a number.
            Do NOT generate questions that only test recall or simple comprehension. Focus on analysis, evaluation, or creation.

            Return ONLY a JSON array, no other text:
            [
              {
                "content": "...",
                "questionType": "...",
                "skill": "...",
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
            """.formatted(
                cefrLevel, quantity, topic,
                bloomInstruction,
                "        - " + String.join("\n        - ", grammarFocus),
                String.join(", ", skills),
                quantity, types, cefrLevel,
                writingConstraint,
                speakingConstraint,
                cefrLevel, topic
            );
    }
}
