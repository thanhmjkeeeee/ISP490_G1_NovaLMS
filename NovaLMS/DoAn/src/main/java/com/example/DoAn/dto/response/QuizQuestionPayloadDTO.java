package com.example.DoAn.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class QuizQuestionPayloadDTO {
    private Integer questionId;
    private String content;
    private String questionType;
    private String skill;
    private String cefrLevel;
    private Integer points;
    private String imageUrl;
    private String audioUrl;
    private List<AnswerOptionPayloadDTO> options;
    // MATCHING: options chỉ LEFT, đây là RIGHT
    private List<AnswerOptionPayloadDTO> matchRightOptions;
}
