package com.example.DoAn.service;

import com.example.DoAn.dto.*;
import java.util.List;

public interface StudentService {
    ResponseData<EnrollPageDTO> getEnrollPageData(String email, Integer courseId);
    ResponseData<Integer> enrollCourse(String email, EnrollRequestDTO request);
    ResponseData<List<RegistrationResponseDTO>> getMyEnrollments(String email);
    ResponseData<PageResponse<MyCourseDTO>> getMyCourses(String email, String keyword, Integer categoryId, int page, int size, String sort);
    ResponseData<DashboardResponseDTO> getDashboardData(String email);
}