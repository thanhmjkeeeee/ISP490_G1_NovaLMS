package com.example.DoAn.controller;

import com.example.DoAn.dto.*;
import com.example.DoAn.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 1. Normal Login
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // 3. Forgot Password
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok("If the email exists, a reset token has been generated (Check Console).");
    }

    // 3. Reset Password
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok("Password reset successfully.");
    }

    // 4. Logout
    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        // Stateless JWT logout is handled on client side by deleting the token.
        return ResponseEntity.ok("Logged out successfully.");
    }
}