package com.example.DoAn.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class EnrolledCourseDTO {
    private Integer courseId;
    private String title;
    private String thumbnailUrl;
    private String teacherName;
    private String className;
    private Integer totalLessons;
    private Integer completedLessons;
    private Double progressPercent;
    private LocalDate enrolledAt;
    private String status;
    private Integer nextLessonId;
    private String nextLessonTitle;
}
