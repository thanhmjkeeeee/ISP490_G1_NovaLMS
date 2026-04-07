package com.example.DoAn.dto.response;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidatedQuestionDTO {
    private int index;
    private String content;
    private String questionType;
    private String skill;
    private String cefrLevel;
    private String explanation;
    private List<OptionDTO> options;
    private List<String> matchingPairs;
    private List<ValidationErrorDTO> errors;
    private List<ValidationErrorDTO> warnings;
    private boolean isValid;
    private List<String> tags;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OptionDTO {
        private String title;
        private Boolean correct;
        private String matchTarget;
    }
}
