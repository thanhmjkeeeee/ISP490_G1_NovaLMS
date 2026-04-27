package com.example.DoAn.dto.response;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WizardStep2ResultDTO {
    private List<ValidatedQuestionDTO> questions;
    private List<ValidationErrorDTO> errors;
    private int totalQuestions;
    private int errorCount;
    private String sourceType;
    private String passage;
    private String audioUrl;
}
