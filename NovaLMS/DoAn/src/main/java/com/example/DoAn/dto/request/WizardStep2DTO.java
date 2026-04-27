package com.example.DoAn.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WizardStep2DTO {

    @NotBlank(message = "Loại nguồn là bắt buộc")
    private String sourceType; // "AI_GENERATE", "EXCEL_IMPORT", "MANUAL"

    // AI_GENERATE fields
    @Size(min = 2, max = 200)
    private String aiTopic;

    @Min(1) @Max(50)
    private Integer aiQuantity;

    private List<String> aiQuestionTypes; // e.g. ["MULTIPLE_CHOICE_SINGLE", "FILL_IN_BLANK"]

    @Builder.Default
    private String aiMode = "NORMAL"; // "NORMAL" | "ADVANCED"

    private java.util.Map<String, Object> aiAdvancedOptions;

    // EXCEL_IMPORT fields
    private MultipartFile excelFile;
    private String excelQuestionType; // the question type for the uploaded Excel

    // MANUAL fields
    private List<ManualQuestionDTO> manualQuestions;
}
