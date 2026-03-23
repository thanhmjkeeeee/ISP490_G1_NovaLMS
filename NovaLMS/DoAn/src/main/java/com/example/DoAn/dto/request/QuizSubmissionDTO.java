package com.example.DoAn.dto.request;

import lombok.Data;
import java.util.Map;

@Data
public class QuizSubmissionDTO {
    private Integer quizId;
    private Map<Integer, Object> answers;
}
