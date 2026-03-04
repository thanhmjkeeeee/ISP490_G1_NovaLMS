package com.example.DoAn.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccountDetailResponse {
    private Integer userId;
    private String fullName;
    private String email;
    private String mobile;
    private Integer roleId;
    private String roleName;
    private String status;
}