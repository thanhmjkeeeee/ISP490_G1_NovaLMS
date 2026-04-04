package com.example.DoAn.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DashboardResponseDTO {
    private String fullName;
    private String email;
    private String avatarUrl;
    private String roleName;

    private Integer activeCourses;
    private Integer completedQuizzes;
    private List<QuizScoreDTO> recentQuizScores;
    private LastLessonDTO lastLesson;

    @Data
    @Builder
    public static class QuizScoreDTO {
        private String quizName;
        private Double score;
    }

    @Data
    @Builder
    public static class LastLessonDTO {
        private String courseName;
        private String chapterName;
        private Integer progress;
        private String lessonUrl;
        private String courseImage;
    }
}