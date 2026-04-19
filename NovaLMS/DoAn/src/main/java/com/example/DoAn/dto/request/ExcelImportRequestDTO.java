package com.example.DoAn.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExcelImportRequestDTO {

    @NotEmpty(message = "Cần ít nhất một câu hỏi")
    @Valid
    private List<ExcelQuestionDTO> questions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExcelQuestionDTO {
        private String content;
        private String questionType;
        private String skill;
        private String cefrLevel;
        private String topic;
        private String explanation;
        private String audioUrl;
        private String imageUrl;
        @Valid
        private List<OptionDTO> options;
        private String correctAnswer;
        private List<String> matchLeft;
        private List<String> matchRight;
        private List<Integer> correctPairs;
        private Integer rowIndex;
        private Boolean selected;
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
