package com.example.DoAn.service.impl;

import com.example.DoAn.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendAccountCreatedEmail(String toEmail, String fullName, String roleName, String password) {
        String subject = "[Nova LMS] Thông báo tạo tài khoản";
        String content = buildAccountCreatedContent(fullName, roleName, password);
        sendEmail(toEmail, subject, content);
    }

    @Override
    public void sendRoleUpdatedEmail(String toEmail, String fullName, String oldRoleName, String newRoleName) {
        String subject = "[Nova LMS] Thông báo thay đổi vai trò";
        String content = buildRoleUpdatedContent(fullName, oldRoleName, newRoleName);
        sendEmail(toEmail, subject, content);
    }

    @Override
    public void sendAccountStatusEmail(String toEmail, String fullName, String newStatus) {
        String subject = "[Nova LMS] Thông báo thay đổi trạng thái tài khoản";
        String content = buildStatusUpdatedContent(fullName, newStatus);
        sendEmail(toEmail, subject, content);
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
}
