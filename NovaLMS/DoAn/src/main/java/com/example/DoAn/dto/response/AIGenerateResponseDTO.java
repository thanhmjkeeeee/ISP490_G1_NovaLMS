package com.example.DoAn.dto.response;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIGenerateResponseDTO {

    private List<QuestionDTO> questions;
    private String warning;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuestionDTO {
        private String content;
        private String transcript; // For Listening scripts
        private String questionType;
        private String skill;
        private String cefrLevel;
        private String topic;
        private String explanation;
        private String audioUrl;
        private String imageUrl;
        private List<OptionDTO> options;
        private String correctAnswer;
        private List<String> matchLeft;
        private List<String> matchRight;
        private List<Integer> correctPairs;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OptionDTO {
        private String title;
        private Boolean correct;
    }
}
