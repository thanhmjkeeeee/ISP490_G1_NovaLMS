package com.example.DoAn.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WizardSaveDTO {

    @NotNull(message = "Status is required")
    private String status; // "DRAFT" or "PUBLISHED"

    @NotBlank(message = "Source is required")
    private String source; // "EXPERT_BANK" or "TEACHER_PRIVATE"
}
