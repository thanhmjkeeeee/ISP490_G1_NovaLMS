package com.example.DoAn.util;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Set;

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
}
