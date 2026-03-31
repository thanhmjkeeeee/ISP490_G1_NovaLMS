package com.example.DoAn.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HybridTransitionDTO {
    private Integer sessionId;
    private int completedQuizzes;
    private int totalQuizzes;
    private SectionResult currentResult;
    private Integer nextQuizIndex;
    private boolean isLastQuiz;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SectionResult {
        private Integer resultId;
        private String quizTitle;
        private String skill;           // e.g. "WRITING", "SPEAKING", "READING"
        private boolean aiGraded;        // true if quiz has WRITING/SPEAKING questions
        private boolean allAiGraded;    // true if ALL questions are AI-graded (no MC score available)
        private int score;              // MC score only (0 if allAiGraded)
        private int totalPoints;
        private BigDecimal correctRate;
        private String suggestedLevel;
    }
}
