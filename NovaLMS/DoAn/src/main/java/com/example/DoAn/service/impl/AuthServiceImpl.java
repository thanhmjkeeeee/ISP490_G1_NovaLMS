package com.example.DoAn.service.impl;

import com.example.DoAn.dto.*;
import com.example.DoAn.model.EmailVerification;
import com.example.DoAn.model.PasswordResetToken;
import com.example.DoAn.model.Setting;
import com.example.DoAn.model.User;
import com.example.DoAn.repository.EmailVerificationRepository;
import com.example.DoAn.repository.PasswordResetTokenRepository;
import com.example.DoAn.repository.SettingRepository;
import com.example.DoAn.repository.UserRepository;
import com.example.DoAn.service.AuthService;
import com.example.DoAn.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final SettingRepository settingRepository;
    private final EmailVerificationRepository verificationRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JavaMailSender mailSender;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    @Transactional(readOnly = true)
    public ResponseData<LoginResponse> login(LoginRequest request) {
        try {
            User user = userRepository.findByEmail(request.getEmail()).orElse(null);
            if (user == null) return ResponseData.error(400, "Không tìm thấy người dùng hoặc sai mật khẩu.");
            if (!"Active".equals(user.getStatus())) return ResponseData.error(403, "Tài khoản bị khóa.");
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) return ResponseData.error(400, "Sai mật khẩu.");

            String token = jwtUtil.generateToken(user);
            LoginResponse response = new LoginResponse(token, user.getEmail(), user.getRole().getName());
            return ResponseData.success("Đăng nhập thành công", response);
        } catch (Exception e) {
            return ResponseData.error(500, "Lỗi hệ thống: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ResponseData<Void> registerUser(RegisterRequestDTO request) {
        try {
            if (!request.getPassword().equals(request.getConfirmPassword())) return ResponseData.error(400, "Mật khẩu xác nhận không khớp!");
            if (userRepository.existsByEmail(request.getEmail())) return ResponseData.error(400, "Email đã tồn tại!");

            EmailVerification verifyEntity = verificationRepository.findFirstByEmailOrderByExpiryTimeDesc(request.getEmail()).orElse(null);
            if (verifyEntity == null || !verifyEntity.getVerificationCode().equals(request.getVerificationCode())) return ResponseData.error(400, "Mã xác minh không chính xác!");
            if (verifyEntity.isExpired()) return ResponseData.error(400, "Mã xác minh đã hết hạn.");

            Setting studentRole = settingRepository.findRoleByValue("ROLE_STUDENT").orElse(null);
            if (studentRole == null) return ResponseData.error(500, "Lỗi cấu hình Role");

            User newUser = User.builder()
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .fullName(request.getFullName())
                    .mobile(request.getPhone())
                    .gender(request.getGender())
                    .city(request.getCity())
                    .role(studentRole)
                    .status("Active")
                    .authProvider("LOCAL")
                    .build();

            userRepository.save(newUser);
            verificationRepository.deleteByEmail(request.getEmail());
            return ResponseData.success("Đăng ký thành công!");
        } catch (Exception e) {
            return ResponseData.error(500, "Lỗi hệ thống: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ResponseData<Void> sendVerificationCode(String email) {
        try {
            if (email == null || email.trim().isEmpty()) return ResponseData.error(400, "Vui lòng nhập email.");
            if (userRepository.existsByEmail(email)) return ResponseData.error(400, "Email đã đăng ký.");

            String code = String.valueOf(new Random().nextInt(900000) + 100000);
            try { verificationRepository.deleteByEmail(email); } catch (Exception ignored) {}

            EmailVerification verification = EmailVerification.builder()
                    .email(email).verificationCode(code).expiryTime(LocalDateTime.now().plusMinutes(5)).build();
            verificationRepository.save(verification);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Mã xác minh Nova LMS");
            message.setText("Mã xác minh: " + code);
            mailSender.send(message);

            return ResponseData.success("Đã gửi mã thành công!");
        } catch (Exception e) {
            return ResponseData.error(500, "Lỗi gửi mail: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ResponseData<Void> forgotPassword(ForgotPasswordRequest request) {
        try {
            User user = userRepository.findByEmail(request.getEmail()).orElse(null);
            if (user == null) return ResponseData.error(400, "Email không tồn tại!");

            tokenRepository.deleteByUser(user);
            String tokenStr = UUID.randomUUID().toString();
            PasswordResetToken token = PasswordResetToken.builder()
                    .user(user).token(tokenStr).expiryDatetime(LocalDateTime.now().plusMinutes(15)).isUsed(false).build();

            tokenRepository.save(token);
            sendResetEmail(user.getEmail(), tokenStr);

            return ResponseData.success("Đã gửi link reset password!");
        } catch (Exception e) {
            return ResponseData.error(500, "Lỗi hệ thống: " + e.getMessage());
        }
    }

    private void sendResetEmail(String toEmail, String token) {
        String resetLink = "http://localhost:8080/reset-password.html?token=" + token;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("[Nova LMS] Đặt lại mật khẩu");
        message.setText("Link đặt lại mật khẩu (có tác dụng 15 phút): \n" + resetLink);
        mailSender.send(message);
    }

    @Override
    @Transactional
    public ResponseData<Void> resetPassword(ResetPasswordRequest request) {
        try {
            PasswordResetToken token = tokenRepository.findByToken(request.getToken()).orElse(null);
            if (token == null) return ResponseData.error(400, "Token không hợp lệ!");
            if (token.isExpired() || Boolean.TRUE.equals(token.isUsed())) return ResponseData.error(400, "Link đã hết hạn!");

            User user = token.getUser();
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);

            token.setUsed(true);
            tokenRepository.save(token);

            return ResponseData.success("Cập nhật mật khẩu thành công!");
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }
}