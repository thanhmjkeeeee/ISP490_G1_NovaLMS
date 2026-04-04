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
    private List<UpcomingEventDTO> upcomingEvents;
    private List<RecentQuizHistoryDTO> recentQuizHistory;

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

    @Data
    @Builder
    public static class UpcomingEventDTO {
        private String day;       // e.g. "15"
        private String month;     // e.g. "Thg 4"
        private String title;     // e.g. "Lớp IELTS-K01"
        private String subtitle;  // e.g. "19:30 – 21:00 • Live Zoom"
        private String type;      // "NORMAL" or "WARNING"
    }

    @Data
    @Builder
    public static class RecentQuizHistoryDTO {
        private Integer quizId;
        private Integer resultId;
        private String quizTitle;
        private String courseName;
        private String submittedAt;    // formatted: "HH:mm dd/MM/yyyy"
        private Integer score;
        private Integer maxScore;
        private String statusLabel;    // "Chờ chấm điểm" | "Đạt" | "Không đạt"
        private String statusClass;    // "badge-warn" | "badge-success" | "badge-danger"
        private String iconBg;         // "#eff6ff" | "#dcfce7" | "#fee2e2"
        private String iconColor;      // "#1d6de5" | "#16a34a" | "#dc2626"
        private String iconClass;      // "bi-patch-question-fill" | "bi-check-circle-fill" | "bi-x-circle-fill"
    }
}