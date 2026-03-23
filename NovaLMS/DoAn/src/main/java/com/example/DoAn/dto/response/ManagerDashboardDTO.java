package com.example.DoAn.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ManagerDashboardDTO {
    private Long totalStudents;
    private Long totalCourses;
    private Long totalClasses;
    private Long newRegistrationsThisWeek;
    private List<RecentRegistrationDTO> recentRegistrations;
}
