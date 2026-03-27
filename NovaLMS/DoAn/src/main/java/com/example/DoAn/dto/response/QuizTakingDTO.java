package com.example.DoAn.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class QuizTakingDTO {
    private Integer quizId;
    private String title;
    private String description;
    private Integer timeLimitMinutes;
    private Integer totalQuestions;
    private String questionOrder;
    private List<QuizQuestionPayloadDTO> questions;
    private Integer classId; // dùng để redirect về lớp học sau khi submit
    private Integer sessionId; // dùng để auto-open modal session khi redirect về class
}
