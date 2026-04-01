package com.example.DoAn.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionBankResponseDTO {
    private Integer questionId;
    private String content;
    private String questionType;
    private String skill;
    private String cefrLevel;
    private String topic;
    private String tags;
    private String explanation;
    private String audioUrl;
    private String imageUrl;
    private String status;
    private String source;         // EXPERT_BANK | TEACHER_PRIVATE
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int usedInQuizCount;
    private List<AnswerOptionResponseDTO> options;
    // cho MATCHING: options chỉ hiện LEFT, này là RIGHT
    private List<AnswerOptionResponseDTO> matchRightOptions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AnswerOptionResponseDTO {
        private Integer answerOptionId;
        private String title;
        private Boolean correctAnswer;
        private Integer orderIndex;
        private String matchTarget;
    }
}
