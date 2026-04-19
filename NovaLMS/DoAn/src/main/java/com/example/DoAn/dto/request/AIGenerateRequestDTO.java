package com.example.DoAn.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIGenerateRequestDTO {

    private String topic;

    private Integer moduleId;

    @NotNull(message = "Số lượng câu hỏi là bắt buộc")
    @Max(value = 25, message = "Số lượng câu hỏi không được vượt quá 25 mỗi lần sinh để đảm bảo ổn định")
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
