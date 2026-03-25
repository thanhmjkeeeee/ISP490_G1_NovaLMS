package com.example.DoAn.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ClassSessionDTO {
    private Integer sessionId;
    private Integer sessionNumber;
    private LocalDateTime sessionDate;
    private String startTime;
    private String endTime;
    private String topic;
    private String notes;

    // Quiz gắn với buổi học này
    private Integer quizId;
    private String quizTitle;
    private String quizDescription;
    private Integer timeLimitMinutes;
    private BigDecimal passScore;
    private Integer maxAttempts;
    private String quizStatus; // DRAFT | PUBLISHED | ARCHIVED
    private Boolean hasAttempted;
    private Integer attemptCount;
    private BigDecimal bestScore;
}
