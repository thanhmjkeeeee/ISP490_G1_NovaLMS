package com.example.DoAn.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonRequestDTO {

    @NotNull(message = "moduleId is required")
    private Integer moduleId;

    @NotBlank(message = "lessonName is required")
    @Size(max = 255, message = "Tên bài học không được vượt quá 255 ký tự")
    private String lessonName;

    @NotBlank(message = "type is required (VIDEO or DOC)")
    private String type;

    private String videoUrl;
    private String contentText;
    private String duration;
    private Integer orderIndex;
}
