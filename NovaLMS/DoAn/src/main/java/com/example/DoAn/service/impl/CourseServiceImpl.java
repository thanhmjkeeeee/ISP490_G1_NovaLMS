package com.example.DoAn.service.impl;

import com.example.DoAn.dto.CourseDTO;
import com.example.DoAn.model.Course;
import com.example.DoAn.repository.CourseRepository;
import com.example.DoAn.service.ICourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements ICourseService {

    private final CourseRepository courseRepository;

    @Override
    public Page<CourseDTO> getAllCourses(Pageable pageable) {
        return courseRepository.findAll(pageable)
                .map(this::convertToDTO);
    }

    @Override
    public void save(CourseDTO dto) {
        Course entity;
        if (dto.getCourseId() != null) {
            entity = courseRepository.findById(dto.getCourseId())
                    .orElseThrow(() -> new RuntimeException("Course not found"));
        } else {
            entity = new Course();
        }

        entity.setCourseName(dto.getCourseName());
        entity.setTitle(dto.getCourseCode());
        entity.setPrice(dto.getPrice());
        entity.setStatus(dto.getStatus());
        entity.setDescription(dto.getDescription());

        courseRepository.save(entity);
    }

    @Override
    public CourseDTO getById(Integer id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course ID " + id + " not found"));
        return convertToDTO(course);
    }

    private CourseDTO convertToDTO(Course course) {
        return CourseDTO.builder()
                .courseId(course.getCourseId())
                .courseCode(course.getTitle() != null ? course.getTitle() : "N/A")
                .courseName(course.getCourseName() != null ? course.getCourseName() : "No Name")
                .status(course.getStatus() != null ? course.getStatus() : "draft")
                .price(course.getPrice() != null ? course.getPrice() : 0.0)
                .build();
    }
}