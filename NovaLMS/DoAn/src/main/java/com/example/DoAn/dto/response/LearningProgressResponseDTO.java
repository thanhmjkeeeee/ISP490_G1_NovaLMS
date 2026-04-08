package com.example.DoAn.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LearningProgressResponseDTO {
    private List<CourseProgressDTO> courses;

    @Data
    @Builder
    public static class CourseProgressDTO {
        private Integer courseId;
        private String courseName;
        private String courseImage;
        private Integer totalLessons;
        private Integer completedLessons;
        private Integer progressPercent;
        private Integer totalQuizzes;
        private Integer completedQuizzes;
        private Double averageScore;
        private String teacherName;
        private String status; // e.g. "In Progress", "Completed"
    }
}
