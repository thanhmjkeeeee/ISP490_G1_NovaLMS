package com.example.DoAn.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

/**
 * Extended grading request for Lesson Quiz.
 * Wraps the existing List<QuestionGradingRequestDTO> with
 * per-skill scores for AI pre-population and overall teacher note.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizGradingRequestDTO {
    private List<QuestionGradingRequestDTO> gradingItems;
    private Map<String, java.math.BigDecimal> skillScores;
    private String overallNote;
}
