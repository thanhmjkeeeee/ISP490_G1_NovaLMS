package com.example.DoAn.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import java.io.Serializable;

@Getter
public class AccountRequestDTO implements Serializable {
    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    @Email(message = "Định dạng email không hợp lệ")
    @NotBlank(message = "Email không được để trống")
    private String email;

    private String mobile;

    @NotNull(message = "Vai trò (role) là bắt buộc")
    private Integer roleId;

    // Password is optional - only required for create, optional for update
    private String password;

    private String status; // ACTIVE | INACTIVE
}