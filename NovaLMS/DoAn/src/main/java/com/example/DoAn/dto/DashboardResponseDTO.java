package com.example.DoAn.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardResponseDTO {
    private String fullName;
    private String email;
    private String avatarUrl;
    private String roleName;
}