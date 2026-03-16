package com.example.DoAn.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ClassDetailResponse {
    private Integer classId;
    private String className;
    private Integer courseId;
    private String courseName;
    private Integer teacherId;
    private String teacherName;
    private String startDate;
    private String endDate;
    private String status;
    private String schedule;
    private String slotTime;
}