package com.example.DoAn.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ClassQuizDTO {
    private Integer quizId;
    private String title;
    private String description;
    private Integer timeLimitMinutes;
    private BigDecimal passScore;
    private Integer maxAttempts;
    private String status;
    private LocalDateTime createdAt;
    private Boolean hasAttempted;
    private Integer attemptCount;
    private BigDecimal bestScore;
}
