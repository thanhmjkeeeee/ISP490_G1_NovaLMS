package com.example.DoAn.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizItemGradingRequestDTO {
    private Integer resultId;
    private Integer questionId;
    private BigDecimal score;
    private String note;
    
    // Writing criteria
    private BigDecimal writingTaskAchievement;
    private BigDecimal writingCoherenceCohesion;
    private BigDecimal writingLexicalResource;
    private BigDecimal writingGrammarAccuracy;
}
