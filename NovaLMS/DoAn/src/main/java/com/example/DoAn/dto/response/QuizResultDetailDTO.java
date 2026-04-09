package com.example.DoAn.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class QuizResultDetailDTO {
    private Integer resultId;
    private Integer quizId;
    private String quizTitle;
    private String courseName;
    private LocalDateTime submittedAt;
    private Integer score;
    private Integer totalPoints;
    private Double correctRate;
    private Boolean passed;
    private Boolean showAnswer;
    private String passScoreDescription;
    private List<QuestionResultDTO> questions;
    /** Distinct skills present in this quiz's questions — used for dynamic tab rendering */
    private List<String> skillsPresent;
}
