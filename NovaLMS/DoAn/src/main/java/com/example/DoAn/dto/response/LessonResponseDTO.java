package com.example.DoAn.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LessonResponseDTO {
    private Integer lessonId;
    private Integer classId;
    private Integer sessionId;
    private String type;
    private String lessonTitle;
    private String lessonName;
    private String duration;
    private String videoUrl;
    private String videoEmbedUrl; // YouTube embed URL (auto-converted)
    private String contentText;
    private Boolean allowDownload;
    private Integer quizId;

    @JsonProperty("isCompleted")
    private boolean isCompleted;

    @JsonProperty("isLocked")
    private boolean isLocked;

    private String title;
    private Double score;
    private String status;

    private Integer latestResultId;
    private String gradingStatus;
    private Boolean passed;
    private boolean isSequential;
    private Boolean canRetake;
    private Integer attemptsLeft;
    private Integer maxAttempts;
}