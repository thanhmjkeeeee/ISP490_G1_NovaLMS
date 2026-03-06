package com.example.DoAn.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RegistrationResponseDTO {
    private Integer registrationId;
    private String courseName;
    private String className;
    private String status;
    private BigDecimal registrationPrice;
    private String note;
}