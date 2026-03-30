package com.example.DoAn.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HybridSessionCreateDTO {

    private String guestName;
    private String guestEmail;

    @NotEmpty(message = "Phải chọn ít nhất 1 kỹ năng")
    @Size(max = 4, message = "Tối đa 4 kỹ năng")
    private List<SkillSelection> selections;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SkillSelection {
        @NotNull(message = "Skill không được null")
        private String skill;

        @NotNull(message = "Quiz ID không được null")
        private Integer quizId;
    }
}
