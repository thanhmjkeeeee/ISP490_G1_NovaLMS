package com.example.DoAn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDTO {
    private Integer userId;
    private String fullName;
    private String email;
    private String mobile;
    private Integer roleId;
    private String roleName;
    private String status;
}