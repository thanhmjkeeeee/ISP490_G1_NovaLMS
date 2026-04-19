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

    @NotNull(message = "Số lượng câu hỏi là bắt buộc")
    @Min(value = 1, message = "Số lượng tối thiểu là 1")
    @Max(value = 20, message = "Số lượng không được vượt quá 20")
    private Integer quantity;

    private String skill;

    private String cefrLevel;

    private java.util.List<String> questionTypes;

    @Builder.Default
    private String mode = "NORMAL"; // "NORMAL" | "ADVANCED"

    private java.util.Map<String, Object> advancedOptions;

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
