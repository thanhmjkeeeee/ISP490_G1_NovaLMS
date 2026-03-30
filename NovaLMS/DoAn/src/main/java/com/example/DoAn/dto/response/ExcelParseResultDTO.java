package com.example.DoAn.dto.response;

import lombok.*;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExcelParseResultDTO {

    private List<ValidRowDTO> valid;
    private List<ErrorRowDTO> errors;
    private int totalRows;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ValidRowDTO {
        private int rowIndex;
        private String content;
        private String questionType;
        private String skill;
        private String cefrLevel;
        private String topic;
        private String explanation;
        private String audioUrl;
        private List<OptionDTO> options;
        private String correctAnswer;
        private List<String> matchLeft;
        private List<String> matchRight;
        private List<Integer> correctPairs;
        private boolean selected;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ErrorRowDTO {
        private int rowIndex;
        private String message;
        private Map<String, String> rawData;
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
