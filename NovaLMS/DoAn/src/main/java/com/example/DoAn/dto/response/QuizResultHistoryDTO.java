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
public class QuizResultHistoryDTO {
    private Integer resultId;
    private Integer quizId;
    private String quizTitle;
    private String courseName;
    private LocalDateTime submittedAt;
    private Integer score;
    private Integer maxScore;
    private Boolean passed;
}
