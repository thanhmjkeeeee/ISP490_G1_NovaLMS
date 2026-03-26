package com.example.DoAn.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseLearningInfoDTO {

    private Long courseId;
    private String courseName; // Bổ sung để map từ Entity Course
    private String title;
    private String description;
    private int progressPercent;

    private String teacherName;
    private String teacherAvatar;

    private String className;
    private String schedule;
    private String liveMeetingLink;

    private List<ModuleDTO> modules;

    private List<QuizInfoDTO> quizzes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModuleDTO {

        private Long moduleId;
        private String moduleName; // Bổ sung để map từ Entity Module
        private String moduleTitle;
        private int totalLessons;

        private List<LessonDTO> lessons;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LessonDTO {

        private Long lessonId;
        private String lessonName; // Bổ sung để map từ Entity Lesson
        private String lessonTitle;
        private String type;
        private String duration;
        private String videoUrl;   // Bổ sung URL gốc
        private String videoEmbedUrl; // YouTube embed URL
        private boolean completed;
        private boolean locked;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizInfoDTO {
        private Integer quizId;
        private String title;
        private Integer totalQuestions;
        private Integer timeLimitMinutes;
        private Integer maxAttempts;   // null = không giới hạn
        private Integer attemptCount;  // số lần đã làm
        private Boolean isOpen;        // teacher mở/đóng quiz
    }
}