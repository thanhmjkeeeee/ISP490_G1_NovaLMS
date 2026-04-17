package com.example.DoAn.service;

import com.example.DoAn.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface INotificationService {

    void send(Long userId, String type, String title, String message, String link);

    void sendQuestionApproved(Long teacherUserId, String questionContentPreview, Long quizId);

    void sendQuestionRejected(Long teacherUserId, String questionContentPreview, String reviewNote);

    Page<Notification> getInbox(Long userId, Pageable pageable);

    long getUnreadCount(Long userId);

    List<Notification> getTopUnread(Long userId);

    void markAsRead(Long notificationId);

    void markAllAsRead(Long userId);

    // ─── Assignment ───────────────────────────────────────────────────────────

    void sendAssignmentPublished(Long userId, String assignmentTitle, String className);

    void sendAssignmentDeadlineReminder(Long userId, String assignmentTitle, String className, String deadline);

    void sendAssignmentGraded(Long userId, String assignmentTitle, String className, String score, String passedStatus);

    // ─── Quiz ──────────────────────────────────────────────────────────────────

    void sendQuizPublished(Long userId, String quizTitle, String className);

    void sendQuizDeadlineReminder(Long userId, String quizTitle, String className, String deadline);

    void sendQuizResult(Long userId, String quizTitle, String className, String score, String passedStatus);

    void sendQuizPendingManualGrading(Long userId, String quizTitle, String studentName, String className);

    void sendManualGradingResult(Long userId, String quizTitle, String className, String finalScore, String passedStatus);

    // ─── Class / Session ──────────────────────────────────────────────────────

    void sendClassEnrollment(Long userId, String className, String courseName);

    void sendSessionReminder(Long userId, String className, String sessionTopic, String sessionDate, String meetLink);

    void sendSessionRescheduled(Long userId, String className, String newDate, String newTime, String reason);

    void sendSessionCancelled(Long userId, String className, String sessionDate, String reason);

    // ─── Enrollment / Registration ───────────────────────────────────────────

    void sendEnrollmentPendingApproval(Long managerUserId, String studentName, String className, String courseName);

    void sendEnrollmentApproved(Long userId, String className, String courseName);

    void sendEnrollmentRejected(Long userId, String className, String courseName, String reason);

    // ─── Payment ──────────────────────────────────────────────────────────────

    void sendPaymentSuccess(Long userId, String courseName, String className);

    void sendPaymentFailed(Long userId, String courseName, String className);

    // ─── Expert Review ─────────────────────────────────────────────────────────

    void sendQuizPendingReview(Long userId, String quizTitle, String teacherName);

    // ─── Announcement ─────────────────────────────────────────────────────────

    void sendAnnouncement(List<Long> userIds, String title, String content);

    // ─── Reschedule ───────────────────────────────────────────────────────────

    void sendRescheduleRequestForManager(Long managerUserId, String teacherName, String className, String newDate);
}
