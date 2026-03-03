package com.example.DoAn.service;

import com.example.DoAn.dto.CourseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ICourseService {
    Page<CourseDTO> getAllCourses(Pageable pageable);

    void save(CourseDTO dto);

    CourseDTO getById(Integer id);
}