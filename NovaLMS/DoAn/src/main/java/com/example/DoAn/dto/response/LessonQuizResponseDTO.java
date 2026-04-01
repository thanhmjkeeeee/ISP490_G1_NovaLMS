package com.example.DoAn.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonQuizResponseDTO {

    private Integer quizId;
    private String title;
    private String description;
    private String quizCategory;
    private String status;            // AVAILABLE | LOCKED | COMPLETED
    private Integer orderIndex;
    private BigDecimal passScore;
    private Integer timeLimitMinutes;
    private Integer maxAttempts;
    private Integer numberOfQuestions;
    private Double bestScore;
    private Boolean bestPassed;
}
