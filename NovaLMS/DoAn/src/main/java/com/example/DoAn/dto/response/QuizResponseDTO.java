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
    private Integer moduleId;
    private String moduleName;
    private Integer lessonId;
    private String lessonName;
    private String status;
    private Boolean isOpen; // Teacher mở/đóng quiz cho học sinh
    private Integer timeLimitMinutes;
    private BigDecimal passScore;
    private Integer maxAttempts;
    private Integer numberOfQuestions;
    private String questionOrder;
    private Boolean showAnswerAfterSubmit;
    private Boolean isHybridEnabled;
    private String targetSkill;   // kỹ năng đích cho quiz hybrid
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int totalQuestions;
    private boolean hasAttempts;
    private String timeLimitPerSkill; // JSON string
    private String openAt;            // datetime strings for FE
    private String closeAt;
    private String deadline;
    private Boolean isSequential;
    private String skillOrder; // JSON string

    private List<QuizQuestionResponseDTO> questions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuizQuestionResponseDTO {
        private Integer quizQuestionId;
        private Integer questionId;
        private Integer groupId;
        private String groupContent;
        private String questionContent;
        private String questionType;
        private String skill;
        private String cefrLevel;
        private Integer orderIndex;
        private BigDecimal points;
    }
}
