package com.example.DoAn.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonRequestDTO {

    @NotNull(message = "moduleId is required")
    private Integer moduleId;

    @NotBlank(message = "lessonName is required")
    private String lessonName;

    @NotBlank(message = "type is required (VIDEO, DOC, or QUIZ)")
    private String type;

    private String videoUrl;
    private String contentText;
    private Integer quizId;
    private String duration;
    private Integer orderIndex;
}
