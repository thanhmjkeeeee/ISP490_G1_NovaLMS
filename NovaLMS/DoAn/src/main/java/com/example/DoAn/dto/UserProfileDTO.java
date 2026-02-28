package com.example.DoAn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {
    private String email;      // Read-only (Chỉ để hiển thị)
    private String fullName;   // Được phép sửa
    private String mobile;     // Được phép sửa
    private String note;       // Được phép sửa (Giới thiệu bản thân)
    private String avatarUrl;
}