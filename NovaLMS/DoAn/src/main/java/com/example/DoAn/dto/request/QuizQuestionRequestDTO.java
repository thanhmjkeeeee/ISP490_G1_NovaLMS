package com.example.DoAn.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizQuestionRequestDTO {

    @NotNull(message = "questionId không được để trống")
    private Integer questionId;

    private String itemType; // "SINGLE" hoặc "GROUP"
    private Integer orderIndex;
    private BigDecimal points;
}
