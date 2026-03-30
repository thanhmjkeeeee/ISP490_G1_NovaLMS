package com.example.DoAn.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIImportRequestDTO {

    @NotEmpty(message = "At least one question is required")
    @Valid
    private List<AIQuestionDTO> questions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AIQuestionDTO {
        private String content;
        private String questionType;
        private String skill;
        private String cefrLevel;
        private String topic;
        private String explanation;
        private String audioUrl;
        private String imageUrl;
        @Valid
        private List<AIOptionDTO> options;
        private String correctAnswer;
        private List<String> matchLeft;
        private List<String> matchRight;
        private List<Integer> correctPairs;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AIOptionDTO {
        private String title;
        private Boolean correct;
    }
}
