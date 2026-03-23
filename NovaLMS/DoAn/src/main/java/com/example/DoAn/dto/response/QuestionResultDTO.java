package com.example.DoAn.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class QuestionResultDTO {
    private Integer questionId;
    private String content;
    private String questionType;
    private String skill;
    private Integer points;
    private Boolean isCorrect;
    private String userAnswerDisplay;
    private String correctAnswerDisplay;
    private String explanation;
    private List<AnswerOptionDTO> options;
}
