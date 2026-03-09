package com.example.DoAn.service;

import com.example.DoAn.dto.response.CoursePublicResponseDTO;
import com.example.DoAn.model.Course;
import com.example.DoAn.model.User;
import com.example.DoAn.model.Setting;
import java.util.List;

public interface HomeService {
    List<CoursePublicResponseDTO> getFeaturedCourses(); // Đổi từ List<Course> sang DTO
    List<User> getFeaturedTeachers();
}