package com.example.DoAn.service;

import com.example.DoAn.dto.*;

public interface AuthService {
    ResponseData<LoginResponse> login(LoginRequest request);
    ResponseData<Void> registerUser(RegisterRequestDTO request);
    ResponseData<Void> sendVerificationCode(String email);
    ResponseData<Void> forgotPassword(ForgotPasswordRequest request);
    ResponseData<Void> resetPassword(ResetPasswordRequest request);
}