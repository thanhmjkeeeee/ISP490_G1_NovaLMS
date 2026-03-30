package com.example.DoAn.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HybridResultSectionDTO {
    private Integer resultId;
    private String quizTitle;
    private String skill;
    private int score;
    private int totalPoints;
    private BigDecimal correctRate;
    private String suggestedLevel;

    // AI-aware scores (include WRITING/SPEAKING after AI grading)
    private Integer aiScore;
    private Integer aiTotalPoints;
    private BigDecimal aiCorrectRate;
    private String aiSuggestedLevel;

    // Per-question AI details for WRITING/SPEAKING
    private List<QuestionAIResultDTO> aiQuestions;
}
