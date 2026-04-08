package com.example.DoAn.service;

public interface EmailService {

    // ─── Account ──────────────────────────────────────────────────────────────

    void sendAccountCreatedEmail(String toEmail, String fullName, String roleName, String password);

    void sendRoleUpdatedEmail(String toEmail, String fullName, String oldRoleName, String newRoleName);

    void sendAccountStatusEmail(String toEmail, String fullName, String newStatus);

    // ─── Assignment ───────────────────────────────────────────────────────────

    /**
     * Email thông báo bài tập (assignment) mới được giao cho học sinh.
     */
    void sendAssignmentPublishedEmail(String toEmail, String studentName,
            String assignmentTitle, String className, String deadline);

    /**
     * Email thông báo hạn nộp bài tập sắp đến (reminder).
     */
    void sendAssignmentDeadlineReminderEmail(String toEmail, String studentName,
            String assignmentTitle, String className, String deadline);

    /**
     * Email thông báo bài tập đã được chấm điểm & trả lại cho học sinh.
     */
    void sendAssignmentGradedEmail(String toEmail, String studentName,
            String assignmentTitle, String className, String score, String passedStatus);

    // ─── Quiz ──────────────────────────────────────────────────────────────────

    /**
     * Email thông báo quiz mới được publish.
     */
    void sendQuizPublishedEmail(String toEmail, String studentName,
            String quizTitle, String className, String deadline);

    /**
     * Email thông báo hạn làm quiz sắp đến (reminder).
     */
    void sendQuizDeadlineReminderEmail(String toEmail, String studentName,
            String quizTitle, String className, String deadline);

    /**
     * Email thông báo kết quả quiz đã có (auto-grade xong).
     */
    void sendQuizResultEmail(String toEmail, String studentName,
            String quizTitle, String className, String score, String passedStatus);

    /**
     * Email thông báo có phần Writing/Speaking cần chấm tay (gửi giáo viên).
     */
    void sendQuizPendingManualGradingEmail(String toEmail, String teacherName,
            String quizTitle, String studentName, String className);

    /**
     * Email thông báo kết quả chấm Writing/Speaking (manual grading done) gửi học sinh.
     */
    void sendManualGradingResultEmail(String toEmail, String studentName,
            String quizTitle, String className, String finalScore, String passedStatus);

    /**
     * Email cảnh báo hành vi vi phạm (gian lận) khi làm quiz (gửi giáo viên).
     */
    void sendQuizLockedEmail(String toEmail, String teacherName, String studentName,
            String quizTitle, String reason, int violationCount, String violationDetails);

    // ─── Class / Session ──────────────────────────────────────────────────────

    /**
     * Email thông báo lớp học mới được tạo/thêm vào.
     */
    void sendClassEnrollmentEmail(String toEmail, String userName,
            String className, String courseName, String startDate, String schedule);

    /**
     * Email thông báo buổi học sắp tới (session reminder).
     */
    void sendSessionReminderEmail(String toEmail, String userName,
            String className, String sessionTopic, String sessionDate, String sessionTime, String meetLink);

    /**
     * Email thông báo buổi học bị hoãn/reschedule.
     */
    void sendSessionRescheduledEmail(String toEmail, String userName,
            String className, String oldDate, String oldTime,
            String newDate, String newTime, String reason);

    /**
     * Email thông báo buổi học bị hủy.
     */
    void sendSessionCancelledEmail(String toEmail, String userName,
            String className, String sessionDate, String sessionTime, String reason);

    // ─── Enrollment / Registration ───────────────────────────────────────────

    /**
     * Email thông báo yêu cầu ghi danh đang chờ duyệt (gửi Manager).
     */
    void sendEnrollmentPendingApprovalEmail(String toEmail, String managerName,
            String studentName, String studentEmail, String className, String courseName);

    /**
     * Email thông báo ghi danh đã được duyệt.
     */
    void sendEnrollmentApprovedEmail(String toEmail, String studentName,
            String className, String courseName, String startDate);

    /**
     * Email thông báo ghi danh bị từ chối.
     */
    void sendEnrollmentRejectedEmail(String toEmail, String studentName,
            String className, String courseName, String reason);

    // ─── Payment ──────────────────────────────────────────────────────────────

    /**
     * Email thông báo thanh toán khóa học thành công — kèm file PDF hóa đơn.
     */
    void sendPaymentSuccessEmail(String toEmail, String studentName,
            String courseName, String className, String amount,
            String paymentId, String orderCode, String paidAt, byte[] invoicePdf);

    /**
     * Email thông báo thanh toán thất bại.
     */
    void sendPaymentFailedEmail(String toEmail, String studentName,
            String courseName, String className);

    // ─── Expert Review ─────────────────────────────────────────────────────────

    /**
     * Email thông báo quiz/module mới cần Expert duyệt.
     */
    void sendQuizPendingReviewEmail(String toEmail, String expertName,
            String quizTitle, String teacherName, String courseName);

    /**
     * Email thông báo câu hỏi đã được Expert phê duyệt (gửi giáo viên).
     */
    void sendQuestionApprovedEmail(String toEmail, String teacherName, String questionContent, String reviewNote);

    /**
     * Email thông báo câu hỏi bị Expert từ chối (gửi giáo viên).
     */
    void sendQuestionRejectedEmail(String toEmail, String teacherName, String questionContent, String reviewNote);

    // ─── Announcement ─────────────────────────────────────────────────────────

    /**
     * Email thông báo/quảng cáo mới từ Admin/Manager.
     */
    void sendAnnouncementEmail(String toEmail, String userName,
            String announcementTitle, String announcementContent);
}
