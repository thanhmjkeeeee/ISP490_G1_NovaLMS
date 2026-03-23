package com.example.DoAn.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizResponseDTO {
    private Integer quizId;
    private String title;
    private String description;
    private String quizCategory;
    private Integer courseId;
    private String courseName;
    private String status;
    private Integer timeLimitMinutes;
    private BigDecimal passScore;
    private Integer maxAttempts;
    private String questionOrder;
    private Boolean showAnswerAfterSubmit;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int totalQuestions;
    private boolean hasAttempts;

    private List<QuizQuestionResponseDTO> questions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuizQuestionResponseDTO {
        private Integer quizQuestionId;
        private Integer questionId;
        private String questionContent;
        private String questionType;
        private String skill;
        private String cefrLevel;
        private Integer orderIndex;
        private BigDecimal points;
    }
}
