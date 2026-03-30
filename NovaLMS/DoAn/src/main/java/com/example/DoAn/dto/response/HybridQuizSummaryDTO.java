package com.example.DoAn.dto.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HybridQuizSummaryDTO {
    private Integer quizId;
    private String title;
    private String description;
    private int totalQuestions;
    private Integer timeLimitMinutes;
    private String skill;
}
