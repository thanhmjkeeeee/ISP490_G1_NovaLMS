package com.example.DoAn.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import java.io.Serializable;

@Getter
public class AccountRequestDTO implements Serializable {
    @NotBlank(message = "Full name must not be blank")
    private String fullName;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email must not be blank")
    private String email;

    private String mobile;

    @NotNull(message = "Role ID must not be null")
    private Integer roleId;

    // Password is optional - only required for create, optional for update
    private String password;

    private String status; // ACTIVE | INACTIVE
}