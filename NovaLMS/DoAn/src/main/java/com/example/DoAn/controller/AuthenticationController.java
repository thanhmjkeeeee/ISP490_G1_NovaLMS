package com.example.DoAn.controller;

import com.example.DoAn.dto.*;
import com.example.DoAn.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthService authService;

    @PostMapping("/login")
    @ResponseBody
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        ServiceResult<LoginResponse> result = authService.login(request);
        return result.isSuccess() ? ResponseEntity.ok(result.getData()) : ResponseEntity.badRequest().body(result.getMessage());
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam String email,
                               @RequestParam String password,
                               @RequestParam String confirmPassword,
                               @RequestParam(required = false) String fullName,
                               @RequestParam(required = false) String phone,
                               @RequestParam(required = false) String gender,
                               @RequestParam(required = false) String city,
                               @RequestParam String verificationCode,
                               RedirectAttributes redirectAttributes) {

        ServiceResult<Void> result = authService.registerUser(email, password, confirmPassword, fullName, phone, gender, city, verificationCode);

        if (result.isSuccess()) {
            redirectAttributes.addFlashAttribute("success", result.getMessage());
            return "redirect:/login.html";
        } else {
            redirectAttributes.addFlashAttribute("error", result.getMessage());
            return "redirect:/register.html";
        }
    }

    @PostMapping("/api/verification/send-code")
    @ResponseBody
    public ResponseEntity<String> sendVerificationCode(@RequestParam String email) {
        ServiceResult<Void> result = authService.sendVerificationCode(email);
        return result.isSuccess() ? ResponseEntity.ok(result.getMessage()) : ResponseEntity.badRequest().body(result.getMessage());
    }

    @PostMapping("/forgot-password")
    @ResponseBody
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        ServiceResult<Void> result = authService.forgotPassword(request);
        return result.isSuccess() ? ResponseEntity.ok(result.getMessage()) : ResponseEntity.badRequest().body(result.getMessage());
    }

    @PostMapping("/reset-password")
    @ResponseBody
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        ServiceResult<Void> result = authService.resetPassword(request);
        return result.isSuccess() ? ResponseEntity.ok(result.getMessage()) : ResponseEntity.badRequest().body(result.getMessage());
    }
}