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

        String tokenStr = UUID.randomUUID().toString();
        PasswordResetToken token = PasswordResetToken.builder()
                .user(user)
                .token(tokenStr)
                .expiryDatetime(LocalDateTime.now().plusMinutes(15))
                .isUsed(false)
                .build();

        tokenRepository.save(token);

        String resetLink = frontendUrl + "/reset-password.html?token=" + tokenStr;
        sendEmail(user.getEmail(), "[Nova LMS] Đặt lại mật khẩu",
                "Vui lòng nhấn vào link sau để đặt lại mật khẩu (Hết hạn sau 15 phút): \n" + resetLink);

        return ResponseData.success("Link đặt lại mật khẩu đã được gửi.", null);
    }

    @Override
    @Transactional
    public ResponseData<Void> resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = tokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new ResourceNotFoundException("Token không hợp lệ hoặc đã bị xóa."));

        if (token.isExpired() || token.isUsed()) {
            throw new InvalidDataException("Link đặt lại mật khẩu đã hết hạn hoặc đã được sử dụng.");
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);

        return ResponseData.success("Mật khẩu đã được cập nhật thành công!", null);
    }
    private void sendEmail(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        mailSender.send(message);
    }
}