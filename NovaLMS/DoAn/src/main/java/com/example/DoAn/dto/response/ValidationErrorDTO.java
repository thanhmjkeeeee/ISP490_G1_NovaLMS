package com.example.DoAn.dto.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationErrorDTO {
    private int questionIndex; // -1 for group-level errors
    private String field;     // "passageContent", "cefrLevel", etc.
    private String code;       // "PASSAGE_TOO_SHORT", "CEFR_MISMATCH", etc.
    private String message;    // human-readable message
    private String severity;   // "ERROR" or "WARNING"
}
