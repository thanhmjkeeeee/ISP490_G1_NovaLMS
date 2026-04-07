package com.example.DoAn.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizSkillSummaryDTO {
    private String skill;
    private long totalCount;
    private long publishedCount;
    private long pendingCount;
    private long totalPoints;
}
