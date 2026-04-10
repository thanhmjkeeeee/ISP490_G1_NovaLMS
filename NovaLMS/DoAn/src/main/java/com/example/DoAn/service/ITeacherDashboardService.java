package com.example.DoAn.service;

import com.example.DoAn.dto.response.TeacherDashboardResponseDTO;

public interface ITeacherDashboardService {
    TeacherDashboardResponseDTO getDashboardData(String email);
}
