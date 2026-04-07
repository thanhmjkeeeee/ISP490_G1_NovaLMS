package com.example.DoAn.dto.request;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExcelQuestionGroupDTO {

    private String passage;
    @Builder.Default
    private String skill = "READING";
    @Builder.Default
    private String cefrLevel = "B1";
    private String topic;
    private String audioUrl;
    private String imageUrl;
    private String explanation;
    private List<ExcelImportRequestDTO.ExcelQuestionDTO> questions;
}
