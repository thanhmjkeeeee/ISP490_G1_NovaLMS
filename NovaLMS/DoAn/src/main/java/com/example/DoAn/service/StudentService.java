package com.example.DoAn.service;

import com.example.DoAn.dto.response.*;
import com.example.DoAn.dto.request.EnrollRequestDTO;

import java.util.List;

public interface StudentService {
    ResponseData<EnrollPageResponseDTO> getEnrollPageData(String email, Integer courseId);
    ResponseData<Integer> enrollCourse(String email, EnrollRequestDTO request);
    ResponseData<List<RegistrationResponseDTO>> getMyEnrollments(String email);
    ResponseData<PageResponse<MyCourseDTO>> getMyCourses(String email, String keyword, Integer categoryId, int page, int size, String sort);
    ResponseData<DashboardResponseDTO> getDashboardData(String email);
}