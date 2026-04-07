package com.example.DoAn.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentPreviewDTO {
    private Integer quizId;
    private String title;
    private String description;
    private String quizCategory;
    private List<SkillSectionSummaryDTO> sections;
    private long totalQuestions;
    private BigDecimal totalPoints;
    private Map<String, Integer> timeLimitsPerSkill; // {"SPEAKING": 2, "WRITING": 30}
    private BigDecimal passScore;
    private Integer maxAttempts;
    private Boolean showAnswerAfterSubmit;
    private List<String> missingSkills; // empty = ready to publish
    private Boolean canPublish;
}
