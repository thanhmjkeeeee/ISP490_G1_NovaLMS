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
        private int score;
        private int totalPoints;
        private BigDecimal correctRate;
        private String suggestedLevel;
    }
}
