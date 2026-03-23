package com.example.DoAn.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class StudentDashboardDTO {
    private String fullName;
    private String avatarUrl;
    private String roleName;
    private List<EnrolledCourseDTO> enrolledCourses;
}
