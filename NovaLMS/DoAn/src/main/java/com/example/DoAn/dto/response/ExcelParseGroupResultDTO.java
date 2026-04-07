package com.example.DoAn.dto.response;

import com.example.DoAn.dto.request.ExcelQuestionGroupDTO;
import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExcelParseGroupResultDTO {

    private List<ValidGroupRowDTO> valid;
    private List<ErrorRowDTO> errors;
    private int totalRows;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ValidGroupRowDTO {
        private int rowIndex;
        private ExcelQuestionGroupDTO group;
        private List<ExcelParseResultDTO.ValidRowDTO> questions;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ErrorRowDTO {
        private int rowIndex;
        private String message;
        private java.util.Map<String, String> rawData;
    }
}
