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

    private Integer lessonId;
    @NotNull(message = "Mã chương (module) là bắt buộc")
    private Integer moduleId;

    @NotBlank(message = "Tên bài học là bắt buộc")
    @Size(max = 255, message = "Tên bài học không được vượt quá 255 ký tự")
    private String lessonName;

    @NotBlank(message = "Loại bài học là bắt buộc (VIDEO hoặc DOC)")
    private String type;

    private String videoUrl;
    private String contentText;
    private String duration;
    private Boolean allowDownload;
    private Integer orderIndex;
}
