package com.example.DoAn.service;

import com.example.DoAn.dto.response.CoursePublicResponseDTO;
import com.example.DoAn.dto.response.PageResponse;
import com.example.DoAn.model.Course;
import java.util.List;
import java.util.Optional;

public interface CourseService {
    // Logic cho trang Public
    List<CoursePublicResponseDTO> getCoursesByFilter(Integer categoryId);

    Optional<CoursePublicResponseDTO> getCourseDetail(Integer id);

    // Logic cho tìm kiếm/lọc nâng cao (AJAX) có phân trang
    PageResponse<CoursePublicResponseDTO> searchAndFilterCourses(String keyword, Integer categoryId, String sortBy, int page, int size);

    // Mapper chuyển đổi Entity sang DTO
    CoursePublicResponseDTO mapToPublicDTO(Course course);

    // Mapper tối ưu cho danh sách (không load curriculum/classes)
    CoursePublicResponseDTO mapToSummaryDTO(Course course);
}