package com.example.DoAn.dto.response;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionResponseDTO {
    private Integer questionId;
    private Integer moduleId;
    private String moduleName;
    private String content;
    private String status;
    private int optionCount;
    private int correctOptionCount;
    private List<AnswerOptionResponseDTO> options;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AnswerOptionResponseDTO {
        private Integer answerOptionId;
        private String title;
        private Boolean correctAnswer;
    }
}
