package com.example.DoAn.controller;

import com.example.DoAn.dto.*;
import com.example.DoAn.model.EmailVerification;
import com.example.DoAn.model.PasswordResetToken;
import com.example.DoAn.model.Setting;
import com.example.DoAn.model.User;
import com.example.DoAn.repository.EmailVerificationRepository;
import com.example.DoAn.repository.PasswordResetTokenRepository;
import com.example.DoAn.repository.SettingRepository;
import com.example.DoAn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@RestController // Thay @Controller bằng @RestController để trả về JSON thay vì tìm file HTML
@RequestMapping("/api/auth") // Đảm bảo prefix này đúng với path bạn gọi
@RequiredArgsConstructor
public class AuthenticationController {

    private final UserRepository userRepository;
    private final SettingRepository settingRepository;
    private final EmailVerificationRepository verificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    @Autowired private PasswordResetTokenRepository tokenRepository;
    // ============================================================
    // 1. XỬ LÝ ĐĂNG KÝ (REGISTER)
    // ============================================================

    @PostMapping("/register")
    @Transactional
    public String registerUser(@RequestParam String email,
                               @RequestParam String password,
                               @RequestParam String confirmPassword,
                               @RequestParam(required = false) String fullName,
                               @RequestParam(required = false) String phone,
                               @RequestParam(required = false) String gender,
                               @RequestParam(required = false) String city,
                               @RequestParam String verificationCode,
                               RedirectAttributes redirectAttributes) {

        // 1. Validate Password
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu xác nhận không khớp!");
            return "redirect:/register.html";
        }

        // 2. Validate Email tồn tại
        if (userRepository.existsByEmail(email)) {
            redirectAttributes.addFlashAttribute("error", "Email đã tồn tại trong hệ thống!");
            return "redirect:/register.html";
        }

        // 3. Validate Mã xác minh
        EmailVerification verifyEntity = verificationRepository.findFirstByEmailOrderByExpiryTimeDesc(email)
                .orElse(null);

        if (verifyEntity == null || !verifyEntity.getVerificationCode().equals(verificationCode)) {
            redirectAttributes.addFlashAttribute("error", "Mã xác minh không chính xác!");
            return "redirect:/register.html";
        }

        if (verifyEntity.isExpired()) {
            redirectAttributes.addFlashAttribute("error", "Mã xác minh đã hết hạn.");
            return "redirect:/register.html";
        }

        try {
            // 4. Lấy Role 'ROLE_STUDENT' từ bảng Setting
            Setting studentRole = settingRepository.findRoleByValue("ROLE_STUDENT")
                    .orElseThrow(() -> new RuntimeException("Lỗi: Role 'ROLE_STUDENT' chưa được cấu hình trong bảng Setting."));

            // 5. Tạo User Mới
            User newUser = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(password)) // Lưu mật khẩu đã mã hóa
                    .fullName(fullName)
                    .mobile(phone)
                    .gender(gender)
                    .city(city)
                    .role(studentRole)
                    .status("Active")
                    .authProvider("LOCAL")
                    .build();

            userRepository.save(newUser);

            // Xóa mã sau khi dùng
            verificationRepository.deleteByEmail(email);

            redirectAttributes.addFlashAttribute("success", "Đăng ký thành công! Vui lòng đăng nhập.");
            return "redirect:/login.html";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Lỗi hệ thống: " + e.getMessage());
            return "redirect:/register.html";
        }
    }

    // ============================================================
    // 2. API GỬI MÃ XÁC MINH (AJAX)
    // ============================================================

    @PostMapping("/api/verification/send-code")
    @ResponseBody // Trả về JSON/String cho AJAX
    public ResponseEntity<String> sendVerificationCode(@RequestParam String email) {
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Vui lòng nhập email.");
        }
        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body("Email này đã được đăng ký! Vui lòng đăng nhập.");
        }

        String code = String.valueOf(new Random().nextInt(900000) + 100000);

        try {
            verificationRepository.deleteByEmail(email); // Xóa mã cũ
        } catch (Exception ignored) {}

        EmailVerification verification = EmailVerification.builder()
                .email(email)
                .verificationCode(code)
                .expiryTime(LocalDateTime.now().plusMinutes(5))
                .build();
        verificationRepository.save(verification);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Mã xác minh Nova LMS");
            message.setText("Mã xác minh của bạn là: " + code);
            mailSender.send(message);
            return ResponseEntity.ok("Đã gửi mã thành công!");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi gửi mail: " + e.getMessage());
        }
    }

    // ============================================================
    // 3. XỬ LÝ QUÊN MẬT KHẨU & RESET PASSWORD
    // ============================================================



    @PostMapping("/forgot-password")
    @Transactional
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        // Tìm user theo email chuẩn DB mới
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email không tồn tại!"));

        // Tạo Token duy nhất
        String token = UUID.randomUUID().toString();

        // Xóa token cũ để đảm bảo không bị rác dữ liệu
        tokenRepository.deleteByUser(user);

        // Lưu Token vào bảng mới bổ sung
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setToken(token);
        resetToken.setExpiryDatetime(LocalDateTime.now().plusMinutes(15)); // Hết hạn sau 15p
        resetToken.setUsed(false);
        tokenRepository.save(resetToken);

        // Gửi Mail thật
        try {
            sendResetEmail(user.getEmail(), token);
            return ResponseEntity.ok("Link reset password đã được gửi qua email!");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi hệ thống khi gửi mail: " + e.getMessage());
        }
    }

    @PostMapping("/reset-password")
    @ResponseBody // Trả về JSON cho AJAX từ trang reset-password.html
    @Transactional
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        PasswordResetToken resetToken = tokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Token không hợp lệ!"));

        if (resetToken.isUsed() || resetToken.getExpiryDatetime().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("Link đã hết hạn hoặc đã được sử dụng!");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        return ResponseEntity.ok("Mật khẩu đã được cập nhật thành công!");
    }

    private void sendResetEmail(String email, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[Nova LMS] Đặt lại mật khẩu");
        // Đảm bảo link này khớp với trang reset-password.html của bạn [cite: 131]
        message.setText("Click vào link để đặt lại mật khẩu (có tác dụng trong 15 phút): \n"
                + "http://localhost:8080/reset-password.html?token=" + token);
        mailSender.send(message);
    }


}