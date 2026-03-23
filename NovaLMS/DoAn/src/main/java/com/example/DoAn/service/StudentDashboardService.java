package com.example.DoAn.service;

import com.example.DoAn.dto.response.StudentDashboardDTO;

public interface StudentDashboardService {
    StudentDashboardDTO getDashboardData(String email);
}
