package com.example.DoAn.dto;

import lombok.Data;

@Data
public class RegisterRequestDTO {
    private String email;
    private String password;
    private String confirmPassword;
    private String fullName;
    private String phone;
    private String gender;
    private String city;
    private String verificationCode;
}