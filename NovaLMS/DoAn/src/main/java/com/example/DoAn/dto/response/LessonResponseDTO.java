package com.example.DoAn.dto.response;

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
    private String videoEmbedUrl; // YouTube embed URL (auto-converted)
    private String contentText;
    private Integer quizId;
    private boolean isCompleted;
    private boolean isLocked;
}