package com.example.DoAn.service.impl;

import com.example.DoAn.dto.response.CoursePublicResponseDTO;
import com.example.DoAn.model.User;
import com.example.DoAn.repository.CourseRepository;
import com.example.DoAn.repository.UserRepository;
import com.example.DoAn.repository.SettingRepository;
import com.example.DoAn.service.HomeService;
import com.example.DoAn.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class HomeServiceImpl implements HomeService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SettingRepository settingRepository;

    @Autowired
    private CourseService courseService;

    /**
     * Sửa lỗi: Chuyển kiểu trả về từ List<Course> sang List<CoursePublicResponseDTO>
     * để đồng bộ với logic mới của CourseService.
     */
    @Override
    public List<CoursePublicResponseDTO> getFeaturedCourses() {
        // Fetch only top 6 courses ordered by student count directly from DB
        return courseRepository.findTopFeaturedCourses(org.springframework.data.domain.PageRequest.of(0, 6))
                .stream()
                // Use optimized summary mapper (no heavy join/lazy load)
                .map(course -> courseService.mapToSummaryDTO(course))
                .collect(Collectors.toList());
    }

    @Override
    public List<User> getFeaturedTeachers() {
        // Find Teacher role dynamically instead of hardcoded ID
        return settingRepository.findRoleByValue("ROLE_TEACHER")
                .map(role -> userRepository.findByRole_SettingIdAndStatus(role.getSettingId(), "Active"))
                .orElse(java.util.Collections.emptyList());
    }
}