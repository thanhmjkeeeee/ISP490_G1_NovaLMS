package com.example.DoAn.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClassPublicResponseDTO {
    private Integer classId;
    private String courseTitle;
    private String categoryName;
    private String className;
    private String teacherName;
    private String schedule;
    private String slotTime;
    private String startDate; // Đã format sang kiểu String (dd/MM/yyyy)
}