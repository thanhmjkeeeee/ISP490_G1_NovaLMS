package com.example.DoAn.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TeacherDashboardResponseDTO {
    private String fullName;
    private Integer todayClasses;
    private Integer pendingGrading;
    private Integer activeClasses;
    private Integer totalStudents;
    private Integer unlockRequests; 
    private List<AlertStudentItem> alertStudents;

    private List<ScheduleItem> todaySchedule;
    private List<RecentSubmissionItem> recentSubmissions;

    @Data
    public static class AlertStudentItem {
        private String name;
        private String initials;
        private String avatarColor; // red, orange, yellow, purple, blue, teal
        private String reason;
        private String badgeText;
        private String type; // score, absent, submit, request, late, drop
    }

    @Data
    public static class ScheduleItem {
        private String className;
        private String startTime;
        private String endTime;
        private LocalDateTime sessionDate;
        private String slotName;
        private Integer sessionNumber;
        private Boolean isToday;
    }

    @Data
    public static class RecentSubmissionItem {
        private String studentName;
        private String studentEmail;
        private String initials;
        private String quizTitle;
        private String className;
        private LocalDateTime submittedAt;
        private Double score;
        private Integer totalScore;
        private String status;
        private Integer resultId;
    }
}