package com.example.DoAn.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PlacementTestResultDTO {
    private Integer resultId;
    private String quizTitle;
    private LocalDateTime submittedAt;
    private Integer score;
    private Integer totalPoints;
    private Double correctRate;
    private Boolean passed;
    private String suggestedLevel;       // A1, A2, B1, B2, C1, C2
    private String levelDescription;     // Mô tả trình độ
    private List<QuestionResultDTO> questions;  // Reuse DTO có sẵn
    private List<CoursePublicResponseDTO> suggestedCourses; // Danh sách khóa học gợi ý
}
