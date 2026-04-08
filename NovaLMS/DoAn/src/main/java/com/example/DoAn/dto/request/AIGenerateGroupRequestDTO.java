package com.example.DoAn.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIGenerateGroupRequestDTO {

    private String topic;

    private Integer moduleId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 20, message = "Quantity cannot exceed 20")
    private Integer quantity;

    private String skill;

    private String cefrLevel;

    private java.util.List<String> questionTypes;

    private String mode = "NORMAL"; // "NORMAL" | "ADVANCED"

    public boolean hasTopic() {
        return topic != null && !topic.isBlank();
    }

    public boolean hasModuleId() {
        return moduleId != null;
    }

    public boolean isValid() {
        return hasTopic() || hasModuleId();
    }
}
