package com.example.DoAn.service.impl;

import com.example.DoAn.dto.request.ForgotPasswordRequest;
import com.example.DoAn.dto.request.LoginRequest;
import com.example.DoAn.dto.request.RegisterRequestDTO;
import com.example.DoAn.dto.request.ResetPasswordRequest;
import com.example.DoAn.dto.response.LoginResponse;
import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.exception.InvalidDataException;
import com.example.DoAn.exception.ResourceNotFoundException;
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
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j // Sử dụng Log thay vì System.out
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final SettingRepository settingRepository;
    private final EmailVerificationRepository verificationRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JavaMailSender mailSender;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    public ResponseData<LoginResponse> login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại."));

        if (!"Active".equalsIgnoreCase(user.getStatus())) {
            throw new DisabledException("Tài khoản của bạn đã bị khóa hoặc chưa kích hoạt.");
        }

        String token = jwtUtil.generateToken(user);

        return ResponseData.success("Đăng nhập thành công",
                LoginResponse.builder()
                        .token(token)
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .role(user.getRole().getValue())
                        .build());
    }

    @Override
    @Transactional
    public ResponseData<Void> registerUser(RegisterRequestDTO request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new InvalidDataException("Mật khẩu xác nhận không khớp!");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new InvalidDataException("Email này đã được sử dụng!");
        }

        // Kiểm tra mã xác minh
        EmailVerification verifyEntity = verificationRepository
                .findFirstByEmailOrderByExpiryTimeDesc(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mã xác minh cho email này."));

        if (!verifyEntity.getVerificationCode().equals(request.getVerificationCode())) {
            throw new InvalidDataException("Mã xác minh không chính xác!");
        }
        if (verifyEntity.isExpired()) {
            throw new InvalidDataException("Mã xác minh đã hết hạn.");
        }

        // Lấy Role mặc định từ bảng Setting
        Setting studentRole = settingRepository.findRoleByValue("ROLE_STUDENT")
                .orElseThrow(() -> new ResourceNotFoundException("Cấu hình hệ thống lỗi: Không tìm thấy Role học viên."));

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

        return ResponseData.success("Đăng ký tài khoản thành công!", null);
    }

    @Override
    @Transactional
    public ResponseData<Void> sendVerificationCode(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new InvalidDataException("Email này đã được đăng ký trong hệ thống.");
        }

        String code = String.format("%06d", new Random().nextInt(1000000));

        // Xóa mã cũ nếu có
        verificationRepository.deleteByEmail(email);

        EmailVerification verification = EmailVerification.builder()
                .email(email)
                .verificationCode(code)
                .expiryTime(LocalDateTime.now().plusMinutes(5))
                .build();

        verificationRepository.save(verification);

        sendEmail(email, "Mã xác minh Nova LMS", "Mã xác minh của bạn là: " + code + ". Hiệu lực trong 5 phút.");

        return ResponseData.success("Mã xác minh đã được gửi vào Email của bạn.", null);
    }

    @Override
    @Transactional
    public ResponseData<Void> forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Email không tồn tại trong hệ thống."));

        tokenRepository.deleteByUser(user);

        String tokenStr = String.format("%06d", new Random().nextInt(1000000));

        PasswordResetToken token = PasswordResetToken.builder()
                .user(user)
                .token(tokenStr) // Lưu mã 6 số này vào database đóng vai trò là token
                .expiryDatetime(LocalDateTime.now().plusMinutes(15))
                .isUsed(false)
                .build();

        tokenRepository.save(token);

        sendEmail(user.getEmail(), "[Nova LMS] Mã xác minh đặt lại mật khẩu",
                "Xin chào " + user.getFullName() + ",\n\n" +
                        "Mã xác minh để đặt lại mật khẩu của bạn là: " + tokenStr + "\n\n" +
                        "Vui lòng nhập mã này trên trang web để tiếp tục. Mã sẽ hết hạn sau 15 phút.\n" +
                        "Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.");

        return ResponseData.success("Mã xác minh đã được gửi đến email của bạn.", null);
    }

    @Override
    @Transactional
    public ResponseData<Void> resetPassword(ResetPasswordRequest request) {
        try {
            // 1. KIỂM TRA MẬT KHẨU XÁC NHẬN CÓ KHỚP KHÔNG (Tránh báo lỗi sai)
            if (request.getConfirmPassword() == null || !request.getNewPassword().equals(request.getConfirmPassword())) {
                return ResponseData.error(400, "Mật khẩu xác nhận không khớp!");
            }

            // 2. Tìm mã xác minh (Token) trong DB
            PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken()).orElse(null);

            if (resetToken == null) {
                return ResponseData.error(400, "Mã xác minh không hợp lệ hoặc không tồn tại!");
            }
            if (resetToken.isUsed() || resetToken.getExpiryDatetime().isBefore(LocalDateTime.now())) {
                return ResponseData.error(400, "Mã xác minh đã hết hạn hoặc đã được sử dụng!");
            }

            User user = resetToken.getUser();

            if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
                return ResponseData.error(400, "Mật khẩu mới không được trùng với mật khẩu hiện tại!");
            }

            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);

            resetToken.setUsed(true);
            passwordResetTokenRepository.save(resetToken);

            return ResponseData.success("Đặt lại mật khẩu thành công!");

        } catch (Exception e) {
            return ResponseData.error(500, "Lỗi hệ thống: " + e.getMessage());
        }
    }


    private void sendEmail(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        mailSender.send(message);
    }
}