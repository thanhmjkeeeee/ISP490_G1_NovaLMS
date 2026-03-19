package com.example.DoAn.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class RegistrationResponseDTO {
    private Integer registrationId;
    private Integer userId;
    private String userName;
    private String userEmail;
    private Integer classId;
    private String className;
    private Integer courseId;
    private String courseName;
    private String status;
    private BigDecimal registrationPrice;
    private String note;
    private LocalDateTime registrationTime;
}