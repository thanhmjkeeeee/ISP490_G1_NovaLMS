package com.example.DoAn.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionRequestDTO {

    private Integer moduleId;

    @NotBlank(message = "Content is required")
    private String content;

    @NotBlank(message = "Question type is required")
    private String questionType;

    @NotBlank(message = "Skill is required")
    private String skill;

    @NotBlank(message = "CEFR level is required")
    private String cefrLevel;

    private String topic;
    private String tags;
    private String explanation;
    private String audioUrl;
    private String imageUrl;
    private String status;

    @NotEmpty(message = "At least one answer option is required")
    @Valid
    private List<AnswerOptionDTO> options;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AnswerOptionDTO {
        @NotBlank(message = "Option title is required")
        private String title;

        private Boolean correct;
        private String matchTarget;
    }
}
