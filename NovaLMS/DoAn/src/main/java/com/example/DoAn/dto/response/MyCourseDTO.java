package com.example.DoAn.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MyCourseDTO {
    private Integer courseId;
    private String title;
    private String description;
    private String imageUrl;
    private String className;
    
    // Thống kê tiến độ
    private String teacherName;
    private Integer progressPercent;
    private Integer completedLessons;
    private Integer totalLessons;
    private Integer completedQuizzes;
    private Integer totalQuizzes;
    private Double averageScore;
}