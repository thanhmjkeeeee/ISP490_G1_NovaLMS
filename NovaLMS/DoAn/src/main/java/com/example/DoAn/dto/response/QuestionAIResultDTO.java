package com.example.DoAn.dto.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionAIResultDTO {
    private Integer answerId;
    private Integer questionId;
    private String questionType;  // WRITING or SPEAKING
    private String questionContent;
    private Boolean pendingAiReview;
    private Integer aiScore;
    private Integer maxPoints;
    private String aiFeedback;
    private String aiRubricJson;  // raw JSON string for JS parsing
    private Boolean isCorrect;
}
