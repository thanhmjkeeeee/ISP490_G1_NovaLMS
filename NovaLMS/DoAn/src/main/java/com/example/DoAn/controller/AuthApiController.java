package com.example.DoAn.controller;

import com.example.DoAn.dto.*;
import com.example.DoAn.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseData<LoginResponse> login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    public ResponseData<Void> registerUser(@RequestBody RegisterRequestDTO request) {
        return authService.registerUser(request);
    }

    @PostMapping("/send-code")
    public ResponseData<Void> sendVerificationCode(@RequestParam String email) {
        return authService.sendVerificationCode(email);
    }

    @PostMapping("/forgot-password")
    public ResponseData<Void> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    public ResponseData<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }
}