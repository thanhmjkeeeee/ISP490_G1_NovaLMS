package com.example.DoAn.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnswerOptionDTO {
    private Integer answerOptionId;
    private String title;
    private String matchTarget;
    private Boolean isCorrect;
}
