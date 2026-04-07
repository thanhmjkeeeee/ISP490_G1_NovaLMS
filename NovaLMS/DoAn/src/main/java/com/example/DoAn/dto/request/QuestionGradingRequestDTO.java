package com.example.DoAn.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionGradingRequestDTO {
    private Integer questionId;
    private BigDecimal pointsAwarded;
    private String teacherNote;
}
