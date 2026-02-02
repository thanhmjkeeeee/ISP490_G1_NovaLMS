package com.example.DoAn.controller;


import com.example.DoAn.model.EmailVerification;
import com.example.DoAn.repository.EmailVerificationRepository;
import com.example.DoAn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Random;

@RestController
@RequestMapping("/api/verification")
@RequiredArgsConstructor
public class VerificationApiController {

    private final JavaMailSender mailSender;
    private final EmailVerificationRepository verificationRepository;
    private final UserRepository userRepository;

    @PostMapping("/send-code")
    public ResponseEntity<String> sendVerificationCode(@RequestParam String email) {
        // 1. Validate Email
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Vui lòng nhập email.");
        }
        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body("Email này đã được đăng ký! Vui lòng đăng nhập.");
        }

        // 2. Tạo mã Random 6 số
        String code = String.valueOf(new Random().nextInt(900000) + 100000);

        // 3. Xóa mã cũ & Lưu mã mới
        try {
            // Note: Cần có @Transactional ở service hoặc repository để delete hoạt động ổn định
            verificationRepository.deleteByEmail(email);
        } catch (Exception e) {
            // Ignore nếu chưa có mã cũ
        }

        EmailVerification verification = EmailVerification.builder()
                .email(email)
                .verificationCode(code)
                .expiryTime(LocalDateTime.now().plusMinutes(5))
                .build();
        verificationRepository.save(verification);

        // 4. Gửi Email
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Mã xác minh đăng ký Nova LMS");
            message.setText("Mã xác minh của bạn là: " + code + "\n\nMã này có hiệu lực trong 5 phút.");

            mailSender.send(message);
            return ResponseEntity.ok("Đã gửi mã thành công!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Lỗi gửi mail: " + e.getMessage());
        }
    }
}