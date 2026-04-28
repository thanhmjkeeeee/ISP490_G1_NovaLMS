package com.example.DoAn.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizResultHistoryDTO {
    private Integer resultId;
    private Integer quizId;
    private String quizTitle;
    private String courseName;
    private String quizCategory; // ENTRY_TEST | COURSE_QUIZ
    private LocalDateTime submittedAt;
    private LocalDateTime startedAt;
    private Integer score;
    private Integer maxScore;
    private BigDecimal overallBand;   // IELTS band score (e.g., 4.5, 7.0)
    private Boolean passed;
    private String status;
    private String violationLog;
    private Boolean isUnlockRequested;
}

