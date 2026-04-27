package com.example.DoAn.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentGradingRequestDTO {
    // {"LISTENING": 8.0, "READING": 7.5, "SPEAKING": 7.0, "WRITING": 7.5}
    private Map<String, BigDecimal> sectionScores;
    private List<QuestionGradingItem> gradingItems;
    private String overallNote;
    private Boolean isFinal; // true = GRADED, false/null = GRADING

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionGradingItem {
        private Integer questionId;
        private BigDecimal pointsAwarded;
        private String teacherNote;

        // Writing criteria
        private BigDecimal writingTaskAchievement;
        private BigDecimal writingCoherenceCohesion;
        private BigDecimal writingLexicalResource;
        private BigDecimal writingGrammarAccuracy;
    }
}
