package com.example.DoAn.service.impl;

import com.example.DoAn.model.Notification;
import com.example.DoAn.repository.NotificationRepository;
import com.example.DoAn.service.INotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements INotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public void send(Long userId, String type, String title, String message, String link) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setLink(link);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Override
    public void sendQuestionApproved(Long teacherUserId, String questionContentPreview, Long quizId) {
        String content = questionContentPreview != null && questionContentPreview.length() > 80
            ? questionContentPreview.substring(0, 80) + "..."
            : (questionContentPreview != null ? questionContentPreview : "");
        send(teacherUserId, "QUESTION_APPROVED",
            "Cau hoi cua ban da duoc phe duyet",
            "Cau hoi \"" + content + "\" da duoc Expert phe duyet va xuat ban.",
            "/teacher/my-questions");
    }

    @Override
    public void sendQuestionRejected(Long teacherUserId, String questionContentPreview, String reviewNote) {
        String content = questionContentPreview != null && questionContentPreview.length() > 80
            ? questionContentPreview.substring(0, 80) + "..."
            : (questionContentPreview != null ? questionContentPreview : "");
        String note = (reviewNote != null && !reviewNote.isBlank())
            ? " Ly do: " + reviewNote
            : "";
        send(teacherUserId, "QUESTION_REJECTED",
            "Cau hoi cua ban bi tu choi",
            "Cau hoi \"" + content + "\" da bi tu choi." + note,
            "/teacher/my-questions");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Notification> getInbox(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getTopUnread(Long userId) {
        return notificationRepository.findTop5ByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    @Override
    public void markAsRead(Long notificationId) {
        notificationRepository.markAsRead(notificationId);
    }

    @Override
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    // ─── Assignment ───────────────────────────────────────────────────────────

    @Override
    public void sendAssignmentPublished(Long userId, String assignmentTitle, String className) {
        send(userId, "ASSIGNMENT_PUBLISHED",
            "Bai tap moi duoc gan",
            "Bai tap \"" + nullToEmpty(assignmentTitle) + "\" da duoc gan cho lop " + nullToEmpty(className) + ".",
            "/student/assignments");
    }

    @Override
    public void sendAssignmentDeadlineReminder(Long userId, String assignmentTitle, String className, String deadline) {
        send(userId, "ASSIGNMENT_DEADLINE_REMINDER",
            "Nhac nho han nop bai tap",
            "Bai tap \"" + nullToEmpty(assignmentTitle) + "\" (lop " + nullToEmpty(className) + ") can nop truoc " + nullToEmpty(deadline) + ".",
            "/student/assignments");
    }

    @Override
    public void sendAssignmentGraded(Long userId, String assignmentTitle, String className, String score, String passedStatus) {
        send(userId, "ASSIGNMENT_GRADED",
            "Bai tap da duoc cham diem",
            "Bai tap \"" + nullToEmpty(assignmentTitle) + "\" (lop " + nullToEmpty(className) + ") da duoc cham. Diem: " + nullToEmpty(score) + ". Ket qua: " + nullToEmpty(passedStatus) + ".",
            "/student/assignments");
    }

    // ─── Quiz ──────────────────────────────────────────────────────────────────

    @Override
    public void sendQuizPublished(Long userId, String quizTitle, String className) {
        send(userId, "QUIZ_PUBLISHED",
            "Quiz moi duoc xuat ban",
            "Quiz \"" + nullToEmpty(quizTitle) + "\" da co cho lop " + nullToEmpty(className) + ".",
            "/student/quiz");
    }

    @Override
    public void sendQuizDeadlineReminder(Long userId, String quizTitle, String className, String deadline) {
        send(userId, "QUIZ_DEADLINE_REMINDER",
            "Nhac nho han lam quiz",
            "Quiz \"" + nullToEmpty(quizTitle) + "\" (lop " + nullToEmpty(className) + ") can hoan thanh truoc " + nullToEmpty(deadline) + ".",
            "/student/quiz");
    }

    @Override
    public void sendQuizResult(Long userId, String quizTitle, String className, String score, String passedStatus) {
        send(userId, "QUIZ_RESULT",
            "Ket qua quiz da co",
            "Quiz \"" + nullToEmpty(quizTitle) + "\" (lop " + nullToEmpty(className) + ") da co ket qua. Diem: " + nullToEmpty(score) + ". Ket qua: " + nullToEmpty(passedStatus) + ".",
            "/student/quiz/history");
    }

    @Override
    public void sendQuizPendingManualGrading(Long userId, String quizTitle, String studentName, String className) {
        send(userId, "QUIZ_PENDING_MANUAL_GRADING",
            "Co bai quiz can cham tay",
            "Bai quiz \"" + nullToEmpty(quizTitle) + "\" cua hoc sinh " + nullToEmpty(studentName) + " (lop " + nullToEmpty(className) + ") can ban cham diem thu cong (Writing/Speaking).",
            "/teacher/quiz/grading");
    }

    @Override
    public void sendManualGradingResult(Long userId, String quizTitle, String className, String finalScore, String passedStatus) {
        send(userId, "MANUAL_GRADING_RESULT",
            "Ket qua cham diem thu cong da co",
            "Bai quiz \"" + nullToEmpty(quizTitle) + "\" (lop " + nullToEmpty(className) + ") da duoc cham diem thu cong. Diem cuoi: " + nullToEmpty(finalScore) + ". Ket qua: " + nullToEmpty(passedStatus) + ".",
            "/student/quiz/history");
    }


    // ─── Class / Session ──────────────────────────────────────────────────────

    @Override
    public void sendClassEnrollment(Long userId, String className, String courseName) {
        send(userId, "CLASS_ENROLLMENT",
            "Ban da duoc them vao lop hoc",
            "Ban da duoc them vao lop \"" + nullToEmpty(className) + "\" (khoa hoc: " + nullToEmpty(courseName) + ").",
            "/student/class/" + nullToEmpty(className));
    }

    @Override
    public void sendSessionReminder(Long userId, String className, String sessionTopic, String sessionDate, String meetLink) {
        String msg = "Buoi hoc lop \"" + nullToEmpty(className) + "\" sap toi. Chude: " + nullToEmpty(sessionTopic) + ". Ngay: " + nullToEmpty(sessionDate) + ".";
        if (meetLink != null && !meetLink.isBlank()) {
            msg += " Link hoc online da duoc cap nhat.";
        }
        send(userId, "SESSION_REMINDER",
            "Nhac nho buoi hoc sap toi - " + nullToEmpty(className),
            msg,
            "/student/class/" + nullToEmpty(className));
    }

    @Override
    public void sendSessionRescheduled(Long userId, String className, String newDate, String newTime, String reason) {
        String msg = "Buoi hoc lop \"" + nullToEmpty(className) + "\" da duoc hoan doi. Ngay moi: " + nullToEmpty(newDate) + ", gio moi: " + nullToEmpty(newTime) + ".";
        if (reason != null && !reason.isBlank()) {
            msg += " Ly do: " + reason;
        }
        send(userId, "SESSION_RESCHEDULED",
            "Buoi hoc bi hoan doi - " + nullToEmpty(className),
            msg,
            "/student/class/" + nullToEmpty(className));
    }

    @Override
    public void sendSessionCancelled(Long userId, String className, String sessionDate, String reason) {
        String msg = "Buoi hoc lop \"" + nullToEmpty(className) + "\" ngay " + nullToEmpty(sessionDate) + " da bi huy.";
        if (reason != null && !reason.isBlank()) {
            msg += " Ly do: " + reason;
        }
        send(userId, "SESSION_CANCELLED",
            "Buoi hoc bi huy - " + nullToEmpty(className),
            msg,
            "/student/class/" + nullToEmpty(className));
    }

    // ─── Enrollment / Registration ───────────────────────────────────────────

    @Override
    public void sendEnrollmentPendingApproval(Long managerUserId, String studentName, String className, String courseName) {
        send(managerUserId, "ENROLLMENT_PENDING",
            "Co yeu cau ghi danh moi",
            "Hoc sinh " + nullToEmpty(studentName) + " yeu cau ghi danh lop \"" + nullToEmpty(className) + "\" (khoa: " + nullToEmpty(courseName) + ").",
            "/manager/registrations");
    }

    @Override
    public void sendEnrollmentApproved(Long userId, String className, String courseName) {
        send(userId, "ENROLLMENT_APPROVED",
            "Ghi danh duoc duyet",
            "Yeu cau ghi danh lop \"" + nullToEmpty(className) + "\" (khoa: " + nullToEmpty(courseName) + ") da duoc duyet.",
            "/student/my-courses");
    }

    @Override
    public void sendEnrollmentRejected(Long userId, String className, String courseName, String reason) {
        String msg = "Yeu cau ghi danh lop \"" + nullToEmpty(className) + "\" (khoa: " + nullToEmpty(courseName) + ") da bi tu choi.";
        if (reason != null && !reason.isBlank()) {
            msg += " Ly do: " + reason;
        }
        send(userId, "ENROLLMENT_REJECTED",
            "Ghi danh bi tu choi",
            msg,
            "/student/my-courses");
    }

    // ─── Payment ──────────────────────────────────────────────────────────────

    @Override
    public void sendPaymentSuccess(Long userId, String courseName, String className) {
        send(userId, "PAYMENT_SUCCESS",
            "Thanh toan thanh cong",
            "Thanh toan khoa hoc \"" + nullToEmpty(courseName) + "\" (lop " + nullToEmpty(className) + ") thanh cong.",
            "/student/my-courses");
    }

    @Override
    public void sendPaymentFailed(Long userId, String courseName, String className) {
        send(userId, "PAYMENT_FAILED",
            "Thanh toan that bai",
            "Thanh toan khoa hoc \"" + nullToEmpty(courseName) + "\" (lop " + nullToEmpty(className) + ") khong thanh cong.",
            "/student/my-courses");
    }

    // ─── Expert Review ─────────────────────────────────────────────────────────

    @Override
    public void sendQuizPendingReview(Long userId, String quizTitle, String teacherName) {
        send(userId, "QUIZ_PENDING_REVIEW",
            "Co quiz can Expert duyet",
            "Quiz \"" + nullToEmpty(quizTitle) + "\" tu giao vien " + nullToEmpty(teacherName) + " can ban xem xet va phan cong.",
            "/expert/review");
    }

    // ─── Announcement ─────────────────────────────────────────────────────────

    @Override
    public void sendAnnouncement(List<Long> userIds, String title, String content) {
        if (userIds == null || userIds.isEmpty()) return;
        for (Long userId : userIds) {
            send(userId, "ANNOUNCEMENT",
                title != null ? title : "Thong bao",
                content != null ? content : "Co mot thong bao moi tu Nova LMS.",
                "/");
        }
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private String nullToEmpty(String s) {
        return s != null ? s : "";
    }
}
