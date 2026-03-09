package com.example.DoAn.service;

import com.example.DoAn.dto.request.ForgotPasswordRequest;
import com.example.DoAn.dto.request.LoginRequest;
import com.example.DoAn.dto.request.RegisterRequestDTO;
import com.example.DoAn.dto.request.ResetPasswordRequest;
import com.example.DoAn.dto.response.LoginResponse;
import com.example.DoAn.dto.response.ResponseData;

public interface AuthService {
    ResponseData<LoginResponse> login(LoginRequest request);
    ResponseData<Void> registerUser(RegisterRequestDTO request);
    ResponseData<Void> sendVerificationCode(String email);
    ResponseData<Void> forgotPassword(ForgotPasswordRequest request);
    ResponseData<Void> resetPassword(ResetPasswordRequest request);
}