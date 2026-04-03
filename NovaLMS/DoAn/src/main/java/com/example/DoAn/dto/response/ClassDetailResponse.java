package com.example.DoAn.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ClassDetailResponse {
    private Integer classId;
    private String className;
    private Integer courseId;
    private String courseName;
    private String courseImageUrl;
    private String courseDescription;
    private java.math.BigDecimal coursePrice;
    private java.math.BigDecimal courseSale;
    private String expertAvatar;
    private String expertName;
    private Integer teacherId;
    private String teacherName;
    private String startDate;
    private String endDate;
    private String status;
    private String schedule;
    private String slotTime;
    private Integer numberOfSessions;
    private String meetLink;
    private String description;
    private List<RegistrationResponseDTO> registrations;
}