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

    @NotNull(message = "moduleId is required")
    private Integer moduleId;

    private String content;

    @NotEmpty(message = "At least one answer option is required")
    @Valid
    private List<AnswerOptionDTO> options;

    private String status;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AnswerOptionDTO {
        @NotBlank(message = "Option title is required")
        private String title;

        private Boolean correct;
    }
}
