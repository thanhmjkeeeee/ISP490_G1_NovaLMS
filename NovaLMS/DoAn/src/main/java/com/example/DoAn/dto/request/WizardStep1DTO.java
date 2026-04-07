package com.example.DoAn.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WizardStep1DTO {

    @NotNull(message = "Mode is required")
    private String mode; // "PASSAGE_BASED" or "TOPIC_BASED"

    @NotBlank(message = "Skill is required")
    private String skill; // "LISTENING", "READING", "WRITING", "SPEAKING"

    @NotBlank(message = "CEFR level is required")
    private String cefrLevel; // "A1", "A2", "B1", "B2", "C1", "C2"

    @NotBlank(message = "Topic is required")
    @Size(min = 2, max = 200, message = "Topic must be 2-200 characters")
    private String topic;

    private java.util.List<@Size(max = 50) String> tags; // max 10 items

    @Size(min = 10, max = 5000, message = "Passage must be 10-5000 characters")
    private String passageContent; // required when mode == PASSAGE_BASED

    @Size(max = 500, message = "Audio URL too long")
    private String audioUrl;

    @Size(max = 500, message = "Image URL too long")
    private String imageUrl;
}
