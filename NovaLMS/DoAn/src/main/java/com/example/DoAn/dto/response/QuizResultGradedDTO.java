package com.example.DoAn.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizResultGradedDTO {
    private Integer resultId;
    private Integer quizId;
    private String quizTitle;
    private String studentName;
    private String studentEmail;
    private LocalDateTime submittedAt;
    private LocalDateTime gradedAt;
    private String courseName;
    private String quizType;     // LESSON_QUIZ | ASSIGNMENT
    private Integer score;
    private Integer maxScore;
    private Double percentage;
    private Boolean passed;

}
