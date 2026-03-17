package com.example.DoAn.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class RegistrationResponseDTO {
    private Integer registrationId;
    private String courseName;
    private String className;
    private String status;
    private BigDecimal registrationPrice;
    private String note;
    private String userName;
    private String userEmail;
    private LocalDateTime registrationTime;
}