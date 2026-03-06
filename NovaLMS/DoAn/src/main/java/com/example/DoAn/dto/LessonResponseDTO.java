package com.example.DoAn.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LessonResponseDTO {
    private Integer lessonId;
    private String lessonTitle;
    private String type;
    private String duration;
    private String videoUrl;
    private String contentText;
    private boolean isCompleted;
    private boolean isLocked;
}