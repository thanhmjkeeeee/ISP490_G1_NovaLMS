package com.example.DoAn.service;

import com.example.DoAn.dto.*;
import com.example.DoAn.model.EmailVerification;
import com.example.DoAn.model.PasswordResetToken;
import com.example.DoAn.model.Setting;
import com.example.DoAn.model.User;
import com.example.DoAn.repository.EmailVerificationRepository;
import com.example.DoAn.repository.PasswordResetTokenRepository;
import com.example.DoAn.repository.SettingRepository;
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
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final SettingRepository settingRepository;
    private final EmailVerificationRepository verificationRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JavaMailSender mailSender;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Transactional(readOnly = true)
    public ServiceResult<LoginResponse> login(LoginRequest request) {
        try {
            User user = userRepository.findByEmail(request.getEmail()).orElse(null);

            if (user == null) {
                return ServiceResult.failure("Không tìm thấy người dùng hoặc thông tin đăng nhập không hợp lệ.");
            }

            if (!"Active".equals(user.getStatus())) {
                return ServiceResult.failure("Tài khoản không hoạt động hoặc bị cấm.");
            }

            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                return ServiceResult.failure("Thông tin đăng nhập không hợp lệ");
            }

            String token = jwtUtil.generateToken(user);
            LoginResponse response = new LoginResponse(token, user.getEmail(), user.getRole().getName());

            return ServiceResult.success("Đăng nhập thành công", response);
        } catch (Exception e) {
            return ServiceResult.failure("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @Transactional
    public ServiceResult<Void> registerUser(String email, String password, String confirmPassword, String fullName, String phone, String gender, String city, String verificationCode) {
        try {
            if (!password.equals(confirmPassword)) {
                return ServiceResult.failure("Mật khẩu xác nhận không khớp!");
            }
            if (userRepository.existsByEmail(email)) {
                return ServiceResult.failure("Email đã tồn tại trong hệ thống!");
            }

            EmailVerification verifyEntity = verificationRepository.findFirstByEmailOrderByExpiryTimeDesc(email).orElse(null);

            if (verifyEntity == null || !verifyEntity.getVerificationCode().equals(verificationCode)) {
                return ServiceResult.failure("Mã xác minh không chính xác!");
            }
            if (verifyEntity.isExpired()) {
                return ServiceResult.failure("Mã xác minh đã hết hạn.");
            }

            Setting studentRole = settingRepository.findRoleByValue("ROLE_STUDENT").orElse(null);
            if (studentRole == null) {
                return ServiceResult.failure("Lỗi cấu hình Role 'ROLE_STUDENT'");
            }

            User newUser = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .fullName(fullName)
                    .mobile(phone)
                    .gender(gender)
                    .city(city)
                    .role(studentRole)
                    .status("Active")
                    .authProvider("LOCAL")
                    .build();

            userRepository.save(newUser);
            verificationRepository.deleteByEmail(email);

            return ServiceResult.success("Đăng ký thành công! Vui lòng đăng nhập.");
        } catch (Exception e) {
            return ServiceResult.failure("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @Transactional
    public ServiceResult<Void> sendVerificationCode(String email) {
        try {
            if (email == null || email.trim().isEmpty()) {
                return ServiceResult.failure("Vui lòng nhập email.");
            }
            if (userRepository.existsByEmail(email)) {
                return ServiceResult.failure("Email này đã được đăng ký! Vui lòng đăng nhập.");
            }

            String code = String.valueOf(new Random().nextInt(900000) + 100000);

            try {
                verificationRepository.deleteByEmail(email);
            } catch (Exception ignored) {}

            EmailVerification verification = EmailVerification.builder()
                    .email(email)
                    .verificationCode(code)
                    .expiryTime(LocalDateTime.now().plusMinutes(5))
                    .build();
            verificationRepository.save(verification);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Mã xác minh Nova LMS");
            message.setText("Mã xác minh của bạn là: " + code);
            mailSender.send(message);

            return ServiceResult.success("Đã gửi mã thành công!");
        } catch (Exception e) {
            return ServiceResult.failure("Lỗi gửi mail: " + e.getMessage());
        }
    }

    @Transactional
    public ServiceResult<Void> forgotPassword(ForgotPasswordRequest request) {
        try {
            User user = userRepository.findByEmail(request.getEmail()).orElse(null);

            if (user == null) {
                return ServiceResult.failure("Email không tồn tại!");
            }

            tokenRepository.deleteByUser(user);

            String tokenStr = UUID.randomUUID().toString();
            PasswordResetToken token = PasswordResetToken.builder()
                    .user(user)
                    .token(tokenStr)
                    .expiryDatetime(LocalDateTime.now().plusMinutes(15))
                    .isUsed(false)
                    .build();

            tokenRepository.save(token);
            sendResetEmail(user.getEmail(), tokenStr);

            return ServiceResult.success("Link reset password đã được gửi qua email!");
        } catch (Exception e) {
            return ServiceResult.failure("Lỗi hệ thống khi gửi mail: " + e.getMessage());
        }
    }

    private void sendResetEmail(String toEmail, String token) {
        String resetLink = "http://localhost:8080/reset-password.html?token=" + token;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("[Nova LMS] Đặt lại mật khẩu");
        message.setText("Click vào link để đặt lại mật khẩu (có tác dụng trong 15 phút): \n" + resetLink);
        mailSender.send(message);
    }

    @Transactional
    public ServiceResult<Void> resetPassword(ResetPasswordRequest request) {
        try {
            PasswordResetToken token = tokenRepository.findByToken(request.getToken()).orElse(null);

            if (token == null) {
                return ServiceResult.failure("Token không hợp lệ!");
            }

            if (token.isExpired() || Boolean.TRUE.equals(token.isUsed())) {
                return ServiceResult.failure("Link đã hết hạn hoặc đã được sử dụng!");
            }

            User user = token.getUser();
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);

            token.setUsed(true);
            tokenRepository.save(token);

            return ServiceResult.success("Mật khẩu đã được cập nhật thành công!");
        } catch (Exception e) {
            return ServiceResult.failure(e.getMessage());
        }
    }
}