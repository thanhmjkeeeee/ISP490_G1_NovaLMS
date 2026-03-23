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
public class QuizResultPendingDTO {
    private Integer resultId;
    private Integer quizId;
    private String quizTitle;
    private String studentName;
    private String studentEmail;
    private LocalDateTime submittedAt;
    private String courseName;
}
