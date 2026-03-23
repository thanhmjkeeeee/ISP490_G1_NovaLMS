package com.example.DoAn.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnswerOptionPayloadDTO {
    private Integer answerOptionId;
    private String title;
    private String matchTarget;
}
