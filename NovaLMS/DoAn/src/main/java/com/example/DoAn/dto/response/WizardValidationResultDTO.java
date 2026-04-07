package com.example.DoAn.dto.response;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WizardValidationResultDTO {
    private List<ValidatedQuestionDTO> questions;
    private List<ValidationErrorDTO> groupErrors;
    private List<ValidationErrorDTO> groupWarnings;
    private boolean isClean;
    private int totalQuestions;
    private int errorCount;
    private int warningCount;
}
