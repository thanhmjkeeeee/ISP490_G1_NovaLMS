package com.example.DoAn.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentClassDetailResponse {
    private Integer classId;
    private Integer courseId;
    private String className;
    private String courseName;
    private String courseImage;
    private String startDate;
    private String endDate;
    private String status;
    private String meetLink;

    // Tiến độ học tập (%)
    private Integer progressPercent;
    private Integer completedSessions;
    private Integer totalSessions;

    // Các Tabs
    private List<MemberDTO> members;
    private List<SessionDetailDTO> sessions;
}