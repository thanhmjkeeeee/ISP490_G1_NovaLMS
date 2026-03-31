package com.example.DoAn.dto;

import lombok.*;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradingResponse {
    private int totalScore;
    private int maxScore;
    private String feedback;
    private Map<String, Integer> rubric;  // {"task_achievement": 3, ...}
}
