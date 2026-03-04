package com.example.DoAn.service;

import com.example.DoAn.dto.request.CourseRequestDTO;
import com.example.DoAn.dto.response.CourseDetailResponse;
import com.example.DoAn.dto.response.PageResponse;

public interface ICourseService {
    Integer saveCourse(CourseRequestDTO request);
    void updateCourse(Integer id, CourseRequestDTO request);
    CourseDetailResponse getById(Integer id);
    PageResponse<?> getAllCourses(int pageNo, int pageSize);
    void deleteCourse(Integer id);
    PageResponse<?> getAllCoursesWithFilter(int pageNo, int pageSize, String search, String status);
}