package com.example.DoAn.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MyClassDTO {
    private Integer classId;
    private String className;
    private Integer courseId;
    private String courseName;
    private String courseImage;
    private String teacherName;
    private String schedule;
    private String slotTime;
    private String status;
    private int studentCount;
    private int sessionCount;
}
