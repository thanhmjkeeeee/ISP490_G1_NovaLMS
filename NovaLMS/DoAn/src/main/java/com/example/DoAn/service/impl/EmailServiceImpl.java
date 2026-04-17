package com.example.DoAn.service.impl;

import com.example.DoAn.service.EmailService;
import com.example.DoAn.service.InvoicePdfService;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final InvoicePdfService invoicePdfService;

    public EmailServiceImpl(JavaMailSender mailSender, InvoicePdfService invoicePdfService) {
        this.mailSender = mailSender;
        this.invoicePdfService = invoicePdfService;
    }

    private void sendEmail(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    private void sendHtmlEmailWithAttachment(String to, String subject,
            String htmlContent, byte[] pdfBytes, String fileName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            if (pdfBytes != null && pdfBytes.length > 0) {
                helper.addAttachment(fileName, new ByteArrayResource(pdfBytes));
            }
            mailSender.send(message);
            log.info("HTML email with PDF attachment sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send HTML email with attachment to {}: {}", to, e.getMessage());
        }
    }

    // ─── Account ──────────────────────────────────────────────────────────────

    @Override
    public void sendAccountCreatedEmail(String toEmail, String fullName, String roleName, String password) {
        String subject = "[Nova LMS] Thong bao tao tai khoan";
        String content = buildAccountCreatedContent(fullName, roleName, password);
        sendEmail(toEmail, subject, content);
    }

    @Override
    public void sendRoleUpdatedEmail(String toEmail, String fullName, String oldRoleName, String newRoleName) {
        String subject = "[Nova LMS] Thong bao thay doi vai tro";
        String content = buildRoleUpdatedContent(fullName, oldRoleName, newRoleName);
        sendEmail(toEmail, subject, content);
    }

    @Override
    public void sendAccountStatusEmail(String toEmail, String fullName, String newStatus) {
        String subject = "[Nova LMS] Thong bao thay doi trang thai tai khoan";
        String content = buildStatusUpdatedContent(fullName, newStatus);
        sendEmail(toEmail, subject, content);
    }

    // ─── Assignment ───────────────────────────────────────────────────────────

    @Override
    public void sendAssignmentPublishedEmail(String toEmail, String studentName,
            String assignmentTitle, String className, String deadline) {
        String subject = "[Nova LMS] Bai tap moi duoc gan - " + assignmentTitle;
        StringBuilder sb = new StringBuilder();
        sb.append("Xin chao ").append(nullToEmpty(studentName)).append(",\n\n");
        sb.append("Ban co mot bai tap moi can hoan thanh tren Nova LMS.\n\n");
        sb.append("Chi tiet:\n");
        sb.append("- Bai tap: ").append(nullToEmpty(assignmentTitle)).append("\n");
        sb.append("- Lop hoc: ").append(nullToEmpty(className)).append("\n");
        sb.append("- Han nop: ").append(nullToEmpty(deadline)).append("\n\n");
        sb.append("Vui long dang nhap Nova LMS de lam bai.\n\n");
        sb.append("Tran trong,\n");
        sb.append("Nova LMS");
        sendEmail(toEmail, subject, sb.toString());
    }

    @Override
    public void sendAssignmentDeadlineReminderEmail(String toEmail, String studentName,
            String assignmentTitle, String className, String deadline) {
        String subject = "[Nova LMS] Nhac nho han nop bai tap - " + assignmentTitle;
        StringBuilder sb = new StringBuilder();
        sb.append("Xin chao ").append(nullToEmpty(studentName)).append(",\n\n");
        sb.append("Day la email nhac nho han nop bai tap cua ban.\n\n");
        sb.append("Chi tiet:\n");
        sb.append("- Bai tap: ").append(nullToEmpty(assignmentTitle)).append("\n");
        sb.append("- Lop hoc: ").append(nullToEmpty(className)).append("\n");
        sb.append("- Han nop: ").append(nullToEmpty(deadline)).append("\n\n");
        sb.append("Vui long hoan thanh va nop bai truoc khi het han.\n\n");
        sb.append("Tran trong,\n");
        sb.append("Nova LMS");
        sendEmail(toEmail, subject, sb.toString());
    }

    @Override
    public void sendAssignmentGradedEmail(String toEmail, String studentName,
            String assignmentTitle, String className, String score, String passedStatus) {
        String subject = "[Nova LMS] Bai tap da duoc cham diem - " + assignmentTitle;
        StringBuilder sb = new StringBuilder();
        sb.append("Xin chao ").append(nullToEmpty(studentName)).append(",\n\n");
        sb.append("Bai tap cua ban da duoc cham diem.\n\n");
        sb.append("Chi tiet:\n");
        sb.append("- Bai tap: ").append(nullToEmpty(assignmentTitle)).append("\n");
        sb.append("- Lop hoc: ").append(nullToEmpty(className)).append("\n");
        sb.append("- Diem: ").append(nullToEmpty(score)).append("\n");
        sb.append("- Ket qua: ").append(nullToEmpty(passedStatus)).append("\n\n");
        sb.append("Vui long dang nhap Nova LMS de xem chi tiet.\n\n");
        sb.append("Tran trong,\n");
        sb.append("Nova LMS");
        sendEmail(toEmail, subject, sb.toString());
    }

    // ─── Quiz ──────────────────────────────────────────────────────────────────

    @Override
    public void sendQuizPublishedEmail(String toEmail, String studentName,
            String quizTitle, String className, String deadline) {
        String subject = "[Nova LMS] Quiz moi duoc xuat ban - " + quizTitle;
        StringBuilder sb = new StringBuilder();
        sb.append("Xin chao ").append(nullToEmpty(studentName)).append(",\n\n");
        sb.append("Mot bai quiz moi da duoc xuat ban tren Nova LMS.\n\n");
        sb.append("Chi tiet:\n");
        sb.append("- Quiz: ").append(nullToEmpty(quizTitle)).append("\n");
        sb.append("- Lop hoc: ").append(nullToEmpty(className)).append("\n");
        if (deadline != null && !deadline.isBlank()) {
            sb.append("- Han lam: ").append(deadline).append("\n");
        }
        sb.append("\nVui long dang nhap Nova LMS de lam bai.\n\n");
        sb.append("Tran trong,\n");
        sb.append("Nova LMS");
        sendEmail(toEmail, subject, sb.toString());
    }

    @Override
    public void sendQuizDeadlineReminderEmail(String toEmail, String studentName,
            String quizTitle, String className, String deadline) {
        String subject = "[Nova LMS] Nhac nho han lam quiz - " + quizTitle;
        StringBuilder sb = new StringBuilder();
        sb.append("Xin chao ").append(nullToEmpty(studentName)).append(",\n\n");
        sb.append("Day la email nhac nho han lam quiz cua ban.\n\n");
        sb.append("Chi tiet:\n");
        sb.append("- Quiz: ").append(nullToEmpty(quizTitle)).append("\n");
        sb.append("- Lop hoc: ").append(nullToEmpty(className)).append("\n");
        sb.append("- Han lam: ").append(nullToEmpty(deadline)).append("\n\n");
        sb.append("Vui long hoan thanh truoc khi het han.\n\n");
        sb.append("Tran trong,\n");
        sb.append("Nova LMS");
        sendEmail(toEmail, subject, sb.toString());
    }

    @Override
    public void sendQuizResultEmail(String toEmail, String studentName,
            String quizTitle, String className, String score, String passedStatus) {
        String subject = "[Nova LMS] Ket qua quiz - " + quizTitle;
        StringBuilder sb = new StringBuilder();
        sb.append("Xin chao ").append(nullToEmpty(studentName)).append(",\n\n");
        sb.append("Ket qua bai quiz cua ban da co.\n\n");
        sb.append("Chi tiet:\n");
        sb.append("- Quiz: ").append(nullToEmpty(quizTitle)).append("\n");
        sb.append("- Lop hoc: ").append(nullToEmpty(className)).append("\n");
        sb.append("- Diem: ").append(nullToEmpty(score)).append("\n");
        sb.append("- Ket qua: ").append(nullToEmpty(passedStatus)).append("\n\n");
        sb.append("Vui long dang nhap Nova LMS de xem chi tiet.\n\n");
        sb.append("Tran trong,\n");
        sb.append("Nova LMS");
        sendEmail(toEmail, subject, sb.toString());
    }

    @Override
    public void sendQuizPendingManualGradingEmail(String toEmail, String teacherName,
            String quizTitle, String studentName, String className) {
        String subject = "[Nova LMS] Co bai quiz can cham tay - " + quizTitle;
        StringBuilder sb = new StringBuilder();
        sb.append("Xin chao ").append(nullToEmpty(teacherName)).append(",\n\n");
        sb.append("Co mot bai quiz can ban cham diem thu cong (Writing/Speaking).\n\n");
        sb.append("Chi tiet:\n");
        sb.append("- Quiz: ").append(nullToEmpty(quizTitle)).append("\n");
        sb.append("- Hoc sinh: ").append(nullToEmpty(studentName)).append("\n");
        sb.append("- Lop hoc: ").append(nullToEmpty(className)).append("\n\n");
        sb.append("Vui long dang nhap Nova LMS de cham diem.\n\n");
        sb.append("Tran trong,\n");
        sb.append("Nova LMS");
        sendEmail(toEmail, subject, sb.toString());
    }

    @Override
    public void sendManualGradingResultEmail(String toEmail, String studentName,
            String quizTitle, String className, String finalScore, String passedStatus) {
        String subject = "[Nova LMS] Ket qua cham diem thu cong - " + quizTitle;
        StringBuilder sb = new StringBuilder();
        sb.append("Xin chao ").append(nullToEmpty(studentName)).append(",\n\n");
        sb.append("Bai quiz cua ban da duoc cham diem thu cong boi giao vien.\n\n");
        sb.append("Chi tiet:\n");
        sb.append("- Quiz: ").append(nullToEmpty(quizTitle)).append("\n");
        sb.append("- Lop hoc: ").append(nullToEmpty(className)).append("\n");
        sb.append("- Diem cuoi: ").append(nullToEmpty(finalScore)).append("\n");
        sb.append("- Ket qua: ").append(nullToEmpty(passedStatus)).append("\n\n");
        sb.append("Vui long dang nhap Nova LMS de xem chi tiet.\n\n");
        sb.append("Tran trong,\n");
        sb.append("Nova LMS");
        sendEmail(toEmail, subject, sb.toString());
    }

    @Override
    public void sendQuizLockedEmail(String toEmail, String teacherName, String studentName,
            String quizTitle, String reason, int violationCount, String violationDetails) {
        String subject = "[Nova LMS] CẢNH BÁO VI PHẠM: Bài kiểm tra đã bị khóa - " + quizTitle;

        String now = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy"));

        StringBuilder sb = new StringBuilder();
        sb.append("Kính gửi Quý thầy/cô ").append(nullToEmpty(teacherName)).append(",\n\n");
        sb.append(
                "Hệ thống Nova LMS xin thông báo về một trường hợp vi phạm quy chế nghiêm trọng trong bài kiểm tra.\n\n");
        sb.append("THÔNG TIN CHI TIẾT SỰ VIỆC:\n");
        sb.append("--------------------------------------------------\n");
        sb.append("- Học sinh: ").append(nullToEmpty(studentName)).append("\n");
        sb.append("- Bài kiểm tra: ").append(nullToEmpty(quizTitle)).append("\n");
        sb.append("- Tổng số lần vi phạm: ").append(violationCount).append("\n");
        sb.append("- Thời điểm ghi nhận khóa: ").append(now).append("\n");
        sb.append("- Lý do khóa cuối: ").append(nullToEmpty(reason)).append("\n");
        sb.append("--------------------------------------------------\n\n");

        if (violationDetails != null && !violationDetails.isBlank()) {
            sb.append("NHẬT KÝ VI PHẠM CHI TIẾT:\n");
            sb.append(violationDetails).append("\n");
            sb.append("--------------------------------------------------\n\n");
        }

        sb.append("HÀNH ĐỘNG CỦA HỆ THỐNG:\n");
        sb.append("Học sinh đã tabbing out vượt ngưỡng cho phép (3 lần). Hệ thống đã tự động KHÓA bài thi.\n\n");
        sb.append("Quý thầy/cô vui lòng đăng nhập để xem xét giải trình của học sinh (nếu có):\n");
        sb.append("Đường dẫn quản lý: http://localhost:8080/teacher/quiz/grading\n\n");
        sb.append("Trân trọng,\n");
        sb.append("Ban quản trị hệ thống Nova LMS");

        sendEmail(toEmail, subject, sb.toString());
    }

    // ─── Class / Session ──────────────────────────────────────────────────────

    @Override
    public void sendClassEnrollmentEmail(String toEmail, String userName,
            String className, String courseName, String startDate, String schedule) {
        String subject = "[Nova LMS] Ban da duoc them vao lop hoc - " + className;
        StringBuilder sb = new StringBuilder();
        sb.append("Xin chao ").append(nullToEmpty(userName)).append(",\n\n");
        sb.append("Chuc mung! Ban da duoc them vao mot lop hoc tren Nova LMS.\n\n");
        sb.append("Chi tiet lop hoc:\n");
        sb.append("- Lop: ").append(nullToEmpty(className)).append("\n");
        sb.append("- Khoa hoc: ").append(nullToEmpty(courseName)).append("\n");
        sb.append("- Khai giang: ").append(nullToEmpty(startDate)).append("\n");
        sb.append("- Lich hoc: ").append(nullToEmpty(schedule)).append("\n\n");
        sb.append("Vui long dang nhap Nova LMS de xem chi tiet.\n\n");
        sb.append("Tran trong,\n");
        sb.append("Nova LMS");
        sendEmail(toEmail, subject, sb.toString());
    }

    @Override
    public void sendSessionReminderEmail(String toEmail, String userName,
            String className, String sessionTopic, String sessionDate, String sessionTime, String meetLink) {
        String subject = "[Nova LMS] Nhac nho buoi hoc - " + className;
        StringBuilder sb = new StringBuilder();
        sb.append("Xin chao ").append(nullToEmpty(userName)).append(",\n\n");
        sb.append("Day la email nhac nho buoi hoc sap toi.\n\n");
        sb.append("Chi tiet:\n");
        sb.append("- Lop hoc: ").append(nullToEmpty(className)).append("\n");
        sb.append("- Chude: ").append(nullToEmpty(sessionTopic)).append("\n");
        sb.append("- Ngay: ").append(nullToEmpty(sessionDate)).append("\n");
        sb.append("- Gio: ").append(nullToEmpty(sessionTime)).append("\n");
        if (meetLink != null && !meetLink.isBlank()) {
            sb.append("- Link hoc online: ").append(meetLink).append("\n");
        }
        sb.append("\nVui long tham gia dung gio.\n\n");
        sb.append("Tran trong,\n");
        sb.append("Nova LMS");
        sendEmail(toEmail, subject, sb.toString());
    }

    @Override
    public void sendSessionRescheduledEmail(String toEmail, String userName,
            String className, String oldDate, String oldTime,
            String newDate, String newTime, String reason) {
        String subject = "[Nova LMS] Buoi hoc bi hoan doi - " + className;
        StringBuilder sb = new StringBuilder();
        sb.append("Xin chao ").append(nullToEmpty(userName)).append(",\n\n");
        sb.append("Mot buoi hoc da duoc hoan doi lich.\n\n");
        sb.append("Chi tiet:\n");
        sb.append("- Lop hoc: ").append(nullToEmpty(className)).append("\n");
        sb.append("- Lich cu: ").append(nullToEmpty(oldDate)).append(" luc ").append(nullToEmpty(oldTime)).append("\n");
        sb.append("- Lich moi: ").append(nullToEmpty(newDate)).append(" luc ").append(nullToEmpty(newTime))
                .append("\n");
        if (reason != null && !reason.isBlank()) {
            sb.append("- Ly do: ").append(reason).append("\n");
        }
        sb.append("\nVui long kiem tra lich hoc moi.\n\n");
        sb.append("Tran trong,\n");
        sb.append("Nova LMS");
        sendEmail(toEmail, subject, sb.toString());
    }

    @Override
    public void sendSessionCancelledEmail(String toEmail, String userName,
            String className, String sessionDate, String sessionTime, String reason) {
        String subject = "[Nova LMS] Buoi hoc bi huy - " + className;
        StringBuilder sb = new StringBuilder();
        sb.append("Xin chao ").append(nullToEmpty(userName)).append(",\n\n");
        sb.append("Mot buoi hoc da bi huy.\n\n");
        sb.append("Chi tiet:\n");
        sb.append("- Lop hoc: ").append(nullToEmpty(className)).append("\n");
        sb.append("- Ngay: ").append(nullToEmpty(sessionDate)).append("\n");
        sb.append("- Gio: ").append(nullToEmpty(sessionTime)).append("\n");
        if (reason != null && !reason.isBlank()) {
            sb.append("- Ly do: ").append(reason).append("\n");
        }
        sb.append("\nVui long lien he giao vien de biet them chi tiet.\n\n");
        sb.append("Tran trong,\n");
        sb.append("Nova LMS");
        sendEmail(toEmail, subject, sb.toString());
    }

    // ─── Enrollment / Registration ───────────────────────────────────────────

    @Override
    public void sendEnrollmentPendingApprovalEmail(String toEmail, String managerName,
            String studentName, String studentEmail, String className, String courseName) {
        String subject = "[Nova LMS] Co yeu cau ghi danh moi cho xu ly";
        StringBuilder sb = new StringBuilder();
        sb.append("Xin chao ").append(nullToEmpty(managerName)).append(",\n\n");
        sb.append("Co mot yeu cau ghi danh moi can duyet tren Nova LMS.\n\n");
        sb.append("Chi tiet:\n");
        sb.append("- Hoc sinh: ").append(nullToEmpty(studentName)).append("\n");
        sb.append("- Email: ").append(nullToEmpty(studentEmail)).append("\n");
        sb.append("- Lop hoc: ").append(nullToEmpty(className)).append("\n");
        sb.append("- Khoa hoc: ").append(nullToEmpty(courseName)).append("\n\n");
        sb.append("Vui long dang nhap Nova LMS de xu ly.\n\n");
        sb.append("Tran trong,\n");
        sb.append("Nova LMS");
        sendEmail(toEmail, subject, sb.toString());
    }

    @Override
    public void sendEnrollmentApprovedEmail(String toEmail, String studentName,
            String className, String courseName, String startDate) {
        String subject = "[Nova LMS] Ghi danh duoc duyet - " + className;
        StringBuilder sb = new StringBuilder();
        sb.append("Xin chao ").append(nullToEmpty(studentName)).append(",\n\n");
        sb.append("Chuc mung! Yeu cau ghi danh cua ban da duoc duyet.\n\n");
        sb.append("Chi tiet:\n");
        sb.append("- Lop hoc: ").append(nullToEmpty(className)).append("\n");
        sb.append("- Khoa hoc: ").append(nullToEmpty(courseName)).append("\n");
        sb.append("- Khai giang: ").append(nullToEmpty(startDate)).append("\n\n");
        sb.append("Vui long dang nhap Nova LMS de bat dau hoc.\n\n");
        sb.append("Tran trong,\n");
        sb.append("Nova LMS");
        sendEmail(toEmail, subject, sb.toString());
    }

    @Override
    public void sendEnrollmentRejectedEmail(String toEmail, String studentName,
            String className, String courseName, String reason) {
        String subject = "[Nova LMS] Ghi danh bi tu choi - " + className;
        StringBuilder sb = new StringBuilder();
        sb.append("Xin chao ").append(nullToEmpty(studentName)).append(",\n\n");
        sb.append("Yeu cau ghi danh cua ban da bi tu choi.\n\n");
        sb.append("Chi tiet:\n");
        sb.append("- Lop hoc: ").append(nullToEmpty(className)).append("\n");
        sb.append("- Khoa hoc: ").append(nullToEmpty(courseName)).append("\n");
        if (reason != null && !reason.isBlank()) {
            sb.append("- Ly do: ").append(reason).append("\n");
        }
        sb.append("\nNeu ban co thac mac, vui long lien he nhan vien.\n\n");
        sb.append("Tran trong,\n");
        sb.append("Nova LMS");
        sendEmail(toEmail, subject, sb.toString());
    }

    // ─── Payment ──────────────────────────────────────────────────────────────

    @Override
    public void sendPaymentSuccessEmail(String toEmail, String studentName,
            String courseName, String className, String amount,
            String paymentId, String orderCode, String paidAt, byte[] invoicePdf) {
        String subject = "[Nova LMS] Thanh toan thanh cong - " + courseName;
        String htmlContent = buildPaymentSuccessHtml(studentName, courseName, className, amount, orderCode);
        String fileName = "HoaDon_NovaLMS_" + (orderCode != null ? orderCode : "unknown") + ".pdf";
        sendHtmlEmailWithAttachment(toEmail, subject, htmlContent, invoicePdf, fileName);
    }

    private String buildPaymentSuccessHtml(String studentName, String courseName,
            String className, String amount, String orderCode) {
        return "<!DOCTYPE html>"
                + "<html>"
                + "<head><meta charset='UTF-8'></head>"
                + "<body style='font-family: Arial, sans-serif; background-color: #f4f6f9; margin: 0; padding: 20px;'>"
                + "  <div style='max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 8px; "
                + "         overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1);'>"
                + "    <div style='background: linear-gradient(135deg, #1e50a2, #2980b9); padding: 30px; text-align: center;'>"
                + "      <h1 style='color: #ffffff; margin: 0; font-size: 28px;'>NovaLMS</h1>"
                + "      <p style='color: #d0e8ff; margin: 5px 0 0;'>Hoc tieng Anh - Tao tuong lai</p>"
                + "    </div>"
                + "    <div style='padding: 30px;'>"
                + "      <h2 style='color: #2c3e50; margin-top: 0;'>Thanh toan thanh cong!</h2>"
                + "      <p>Xin chao <strong>" + nullToEmpty(studentName) + "</strong>,</p>"
                + "      <p>Chuc mung! Thanh toan khoa hoc cua ban da duoc xu ly thanh cong. "
                + "         Vui long kiem tra file PDF dinh kem de xem chi tiet hoa don.</p>"
                + "      <div style='background: #f8f9fa; border-left: 4px solid #1e50a2; "
                + "           border-radius: 4px; padding: 15px; margin: 20px 0;'>"
                + "        <p style='margin: 0;'><strong>Khoa hoc:</strong> " + nullToEmpty(courseName) + "</p>"
                + "        <p style='margin: 8px 0 0;'><strong>Lop hoc:</strong> " + nullToEmpty(className) + "</p>"
                + "        <p style='margin: 8px 0 0;'><strong>So tien:</strong> "
                + "           <span style='color: #c0392b; font-weight: bold;'>" + nullToEmpty(amount)
                + " VND</span></p>"
                + "      </div>"
                + "      <p>Vui long dang nhap <strong>NovaLMS</strong> de bat dau hoc.</p>"
                + "      <p style='margin-top: 30px;'>Tran trong,<br><strong>NovaLMS Team</strong></p>"
                + "    </div>"
                + "    <div style='background: #f1f1f1; padding: 15px; text-align: center; font-size: 12px; color: #888;'>"
                + "      NovaLMS - He thong quan ly hoc tap truc tuyen<br>"
                + "      Email nay duoc gui tu dong boi he thong NovaLMS."
                + "    </div>"
                + "  </div>"
                + "</body>"
                + "</html>";
    }

    @Override
    public void sendPaymentFailedEmail(String toEmail, String studentName,
            String courseName, String className) {
        String subject = "[Nova LMS] Thanh toan that bai - " + courseName;
        StringBuilder sb = new StringBuilder();
        sb.append("Xin chao ").append(nullToEmpty(studentName)).append(",\n\n");
        sb.append("Rất tiếc, thanh toán khóa học của bạn không thành công.\n\n");
        sb.append("Chi tiet:\n");
        sb.append("- Khoa hoc: ").append(nullToEmpty(courseName)).append("\n");
        sb.append("- Lop hoc: ").append(nullToEmpty(className)).append("\n\n");
        sb.append("Vui long thu lai hoac lien he ho tro.\n\n");
        sb.append("Tran trong,\n");
        sb.append("Nova LMS");
        sendEmail(toEmail, subject, sb.toString());
    }

    // ─── Expert Review ─────────────────────────────────────────────────────────

    @Override
    public void sendQuizPendingReviewEmail(String toEmail, String expertName,
            String quizTitle, String teacherName, String courseName) {
        String subject = "[Nova LMS] Co quiz can Expert duyet - " + quizTitle;
        StringBuilder sb = new StringBuilder();
        sb.append("Xin chao ").append(nullToEmpty(expertName)).append(",\n\n");
        sb.append("Co mot quiz moi can ban xem xet va phan cong.\n\n");
        sb.append("Chi tiet:\n");
        sb.append("- Quiz: ").append(nullToEmpty(quizTitle)).append("\n");
        sb.append("- Giao vien: ").append(nullToEmpty(teacherName)).append("\n");
        sb.append("- Khoa hoc: ").append(nullToEmpty(courseName)).append("\n\n");
        sb.append("Vui long dang nhap Nova LMS de xu ly.\n\n");
        sb.append("Tran trong,\n");
        sb.append("Nova LMS");
        sendEmail(toEmail, subject, sb.toString());
    }

    @Override
    public void sendQuestionApprovedEmail(String toEmail, String teacherName, String questionContent,
            String reviewNote) {
        String subject = "[Nova LMS] Cau hoi cua ban da duoc phe duyet";
        StringBuilder sb = new StringBuilder();
        sb.append("Xin chao ").append(nullToEmpty(teacherName)).append(",\n\n");
        sb.append("Chuc mung! Cau hoi cua ban da duoc Expert phe duyet.\n\n");
        if (questionContent != null && !questionContent.isBlank()) {
            sb.append("Noi dung cau hoi: ").append(questionContent).append("\n\n");
        }
        if (reviewNote != null && !reviewNote.isBlank()) {
            sb.append("Ghi chu tu Expert: ").append(reviewNote).append("\n\n");
        }
        sb.append("Vui long dang nhap Nova LMS de xem chi tiet.\n\n");
        sb.append("Tran trong,\n");
        sb.append("Nova LMS");
        sendEmail(toEmail, subject, sb.toString());
    }

    @Override
    public void sendQuestionRejectedEmail(String toEmail, String teacherName, String questionContent,
            String reviewNote) {
        String subject = "[Nova LMS] Cau hoi cua ban bi tu choi";
        StringBuilder sb = new StringBuilder();
        sb.append("Xin chao ").append(nullToEmpty(teacherName)).append(",\n\n");
        sb.append("Rất tiếc, cau hoi cua ban da bi Expert tu choi.\n\n");
        if (questionContent != null && !questionContent.isBlank()) {
            sb.append("Noi dung cau hoi: ").append(questionContent).append("\n\n");
        }
        if (reviewNote != null && !reviewNote.isBlank()) {
            sb.append("Ly do: ").append(reviewNote).append("\n\n");
        }
        sb.append("Vui long chinh sua va gui lai cau hoi.\n\n");
        sb.append("Tran trong,\n");
        sb.append("Nova LMS");
        sendEmail(toEmail, subject, sb.toString());
    }

    // ─── Announcement ─────────────────────────────────────────────────────────

    @Override
    public void sendAnnouncementEmail(String toEmail, String userName,
            String announcementTitle, String announcementContent) {
        String subject = "[Nova LMS] " + nullToEmpty(announcementTitle);
        StringBuilder sb = new StringBuilder();
        sb.append("Xin chao ").append(nullToEmpty(userName)).append(",\n\n");
        if (announcementContent != null && !announcementContent.isBlank()) {
            sb.append(announcementContent).append("\n\n");
        } else {
            sb.append("Co mot thong bao moi tu Nova LMS.\n\n");
        }
        sb.append("Tran trong,\n");
        sb.append("Nova LMS");
        sendEmail(toEmail, subject, sb.toString());
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private String nullToEmpty(String s) {
        return s != null ? s : "";
    }

    // ─── Legacy template methods (kept for backward compat) ──────────────────

    private String buildAccountCreatedContent(String fullName, String roleName, String password) {
        StringBuilder sb = new StringBuilder();
        sb.append("Xin chao ").append(fullName).append(",\n\n");
        sb.append("Tai khoan cua ban tren he thong Nova LMS da duoc tao thanh cong.\n\n");
        sb.append("Thong tin tai khoan:\n");
        sb.append("- Ho ten: ").append(fullName).append("\n");
        sb.append("- Vai tro: ").append(roleName).append("\n");
        sb.append("- Mat khau: ").append(password).append("\n\n");
        sb.append("Vui long dang nhap tai: http://localhost:8080\n");
        sb.append("Xin vui long doi mat khau ngay sau khi dang nhap lan dau.\n\n");
        sb.append("Tran trong,\n");
        sb.append("Nova LMS");
        return sb.toString();
    }

    private String buildRoleUpdatedContent(String fullName, String oldRoleName, String newRoleName) {
        StringBuilder sb = new StringBuilder();
        sb.append("Xin chao ").append(fullName).append(",\n\n");
        sb.append("Vai tro cua ban tren he thong Nova LMS da duoc thay doi.\n\n");
        sb.append("Thong tin thay doi:\n");
        sb.append("- Vai tro cu: ").append(oldRoleName).append("\n");
        sb.append("- Vai tro moi: ").append(newRoleName).append("\n\n");
        sb.append("Neu ban co bat ky thac mac nao, vui long lien he quan tri vien.\n\n");
        sb.append("Tran trong,\n");
        sb.append("Nova LMS");
        return sb.toString();
    }

    private String buildStatusUpdatedContent(String fullName, String newStatus) {
        String statusText = "Active".equalsIgnoreCase(newStatus) ? "hoat dong" : "khong hoat dong";
        StringBuilder sb = new StringBuilder();
        sb.append("Xin chao ").append(fullName).append(",\n\n");
        sb.append("Trang thai tai khoan tren he thong Nova LMS cua ban da duoc cap nhat.\n\n");
        sb.append("Trang thai moi: ").append(statusText).append("\n\n");
        sb.append("Neu ban co bat ky thac mac nao, vui long lien he quan tri vien.\n\n");
        sb.append("Tran trong,\n");
        sb.append("Nova LMS");
        return sb.toString();
    }

    @Async
    @Override
    public void sendClassScheduleUpdatedEmail(String toEmail, String userName,
            String className, String newStartDate, String newSchedule, String newSlotTime) {
        String subject = "[Nova LMS] Thong bao thay doi lich hoc - " + className;
        StringBuilder sb = new StringBuilder();
        sb.append("Xin chao ").append(nullToEmpty(userName)).append(",\n\n");
        sb.append("Lop hoc cua ban tren Nova LMS vua co thay doi ve lich hoc.\n\n");
        sb.append("Chi tiet lich hoc moi:\n");
        sb.append("- Lop hoc: ").append(nullToEmpty(className)).append("\n");
        sb.append("- Ngay khai giang moi: ").append(nullToEmpty(newStartDate)).append("\n");
        sb.append("- Lich hoc moi: ").append(nullToEmpty(newSchedule)).append("\n");
        sb.append("- Ca hoc moi: ").append(nullToEmpty(newSlotTime)).append("\n\n");
        sb.append("Vui long dang nhap Nova LMS de kiem tra thoi khoa bieu chi tiet.\n\n");
        sb.append("Tran trong,\n");
        sb.append("Nova LMS");
        sendEmail(toEmail, subject, sb.toString());
    }
}
