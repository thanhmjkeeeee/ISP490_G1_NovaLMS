package com.example.DoAn.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ClassDetailDTO {
    private Integer classId;
    private String className;
    private Integer courseId;
    private String courseName;
    private String courseImage;
    private String teacherName;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String schedule;
    private String slotTime;
    private String status;
    private int studentCount;
    private Integer numberOfSessions;
    private List<ClassSessionDTO> sessions;
    private List<ClassQuizDTO> quizzes;
}
