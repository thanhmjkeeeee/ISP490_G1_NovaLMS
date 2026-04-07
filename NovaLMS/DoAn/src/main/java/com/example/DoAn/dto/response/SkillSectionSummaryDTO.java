package com.example.DoAn.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkillSectionSummaryDTO {
    private String skill;
    private long questionCount;
    private long totalPoints;
    private String status; // DRAFT, READY (>=1 question)
}
