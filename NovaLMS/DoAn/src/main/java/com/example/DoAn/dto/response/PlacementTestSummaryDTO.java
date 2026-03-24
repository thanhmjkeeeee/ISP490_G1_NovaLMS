package com.example.DoAn.dto.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlacementTestSummaryDTO {
    private Integer quizId;
    private String title;
    private String description;
    private Integer totalQuestions;
    private Integer timeLimitMinutes;
}
