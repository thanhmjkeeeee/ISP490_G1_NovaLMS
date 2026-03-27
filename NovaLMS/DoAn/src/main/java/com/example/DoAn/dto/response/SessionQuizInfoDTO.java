package com.example.DoAn.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SessionQuizInfoDTO {
    private Integer quizId;
    private String title;
    private String description;
    private Integer timeLimitMinutes;
    private BigDecimal passScore;
    private Integer maxAttempts;
    private String status;
    private Boolean isOpen;
    private Boolean hasAttempted;
    private Integer attemptCount;
    private BigDecimal bestScore;
}
