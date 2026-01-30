package com.example.DoAn.service;

import com.example.DoAn.dto.*;
import com.example.DoAn.model.PasswordResetToken;
import com.example.DoAn.model.User;
import com.example.DoAn.repository.PasswordResetTokenRepository;
import com.example.DoAn.repository.UserRepository;
import com.example.DoAn.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // Inject trình gửi mail của Spring Boot
    private final JavaMailSender mailSender;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    // 1. Normal Login
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found or Invalid credentials"));

        if (!"Active".equals(user.getStatus())) {
            throw new RuntimeException("Account is inactive or banned");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(user);
        return new LoginResponse(token, user.getEmail(), user.getRole().getName());
    }

    // 3. Forgot Password (ĐÃ CẬP NHẬT GỬI MAIL THẬT)
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        // Bảo mật: Nếu email không tồn tại, vẫn return OK để hacker không dò được email nào đã đăng ký
        if (user == null) {
            System.out.println("Email not found: " + request.getEmail());
            return;
        }

        // Xóa token cũ nếu có để tránh rác DB
        tokenRepository.deleteByUser(user);

        // Tạo token mới
        String tokenStr = UUID.randomUUID().toString();
        PasswordResetToken token = PasswordResetToken.builder()
                .user(user)
                .token(tokenStr)
                .expiryDatetime(LocalDateTime.now().plusMinutes(15))
                .isUsed(false)
                .build();

        tokenRepository.save(token);

        // Gửi Email
        sendResetEmail(user.getEmail(), tokenStr);
    }

    private void sendResetEmail(String toEmail, String token) {
        try {
            // Trong thực tế bạn nên tạo trang reset-password.html riêng
            // Ở đây mình tạm dùng link demo trỏ về frontend
            String resetLink = frontendUrl + "/reset-password.html?token=" + token;

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@novalms.com");
            message.setTo(toEmail);
            message.setSubject("Nova LMS - Yêu cầu đặt lại mật khẩu");
            message.setText("Xin chào,\n\n"
                    + "Bạn vừa yêu cầu đặt lại mật khẩu cho tài khoản tại Nova LMS.\n"
                    + "Vui lòng nhấp vào liên kết dưới đây để đặt lại mật khẩu:\n\n"
                    + resetLink + "\n\n"
                    + "Liên kết này sẽ hết hạn sau 15 phút.\n"
                    + "Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email này.\n\n"
                    + "Trân trọng,\nNova LMS Team");

            mailSender.send(message);
            System.out.println(">>> Đã gửi mail reset password đến: " + toEmail);

        } catch (Exception e) {
            System.err.println(">>> LỖI GỬI MAIL: " + e.getMessage());
            e.printStackTrace();
            // Lưu ý: Không throw exception ra ngoài để tránh crash luồng chính
        }
    }

    // 3. Reset Password
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = tokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (token.isExpired() || Boolean.TRUE.equals(token.getIsUsed())) {
            throw new RuntimeException("Token expired or already used");
        }

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));

        token.setIsUsed(true);
    }
}