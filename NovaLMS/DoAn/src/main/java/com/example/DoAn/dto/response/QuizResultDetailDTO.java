package com.example.DoAn.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class QuizResultDetailDTO {
    private Integer resultId;
    private Integer quizId;
    private String quizTitle;
    private String studentName;
    private String className;
    private String courseName;
    private LocalDateTime submittedAt;
    private Double score;
    private Double totalPoints;
    private Double correctRate;
    private Double overallBand;
    private Boolean passed;
    private Boolean showAnswer;
    private String passScoreDescription;
    private List<QuestionResultDTO> questions;
    private Integer maxAttempts;
    private Long usedAttempts;
    private Boolean canRetake;
    /** Distinct skills present in this quiz's questions — used for dynamic tab rendering */
    private List<String> skillsPresent;
    private String status;

    // IELTS specific fields
    private Map<String, Double> sectionScores;
    private String quizCategory;
    private Boolean isAssignment;
    private Map<String, String> criteriaLabels; // Dynamic labels for writing/speaking criteria
}
