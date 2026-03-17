package com.example.DoAn.service.impl;

import com.example.DoAn.dto.request.CourseRequestDTO;
import com.example.DoAn.dto.response.CourseDetailResponse;
import com.example.DoAn.dto.response.PageResponse;
import com.example.DoAn.model.Course;
import com.example.DoAn.repository.CourseRepository;
import com.example.DoAn.service.ICourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CourseServiceImpl implements ICourseService {

    private final CourseRepository courseRepository;

    @Override
    public Integer saveCourse(CourseRequestDTO request) {
        Course course = Course.builder()
                .title(request.getCourseCode())
                .courseName(request.getCourseName())
                .description(request.getDescription())
                .price(request.getPrice())
                .sale(request.getSale())
                .avatar(request.getAvatar())
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .build();

        courseRepository.save(course);
        log.info("Course added successfully, courseId={}", course.getCourseId());
        return course.getCourseId();
    }

    @Override
    public void updateCourse(Integer id, CourseRequestDTO request) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        course.setTitle(request.getCourseCode());
        course.setCourseName(request.getCourseName());
        course.setDescription(request.getDescription());
        course.setPrice(request.getPrice());
        course.setSale(request.getSale());
        course.setAvatar(request.getAvatar());
        course.setStatus(request.getStatus());

        courseRepository.save(course);
        log.info("Course updated successfully, courseId={}", id);
    }

    @Override
    public CourseDetailResponse getById(Integer id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        return mapToResponse(course);
    }

    @Override
    public PageResponse<?> getAllCourses(int pageNo, int pageSize) {
        Page<Course> page = courseRepository.findAll(PageRequest.of(pageNo, pageSize));

        List<CourseDetailResponse> list = page.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return PageResponse.<CourseDetailResponse>builder()
                .pageNo(pageNo)
                .pageSize(pageSize)
                .totalPages(page.getTotalPages())
                .items(list)
                .build();
    }

    @Override
    public void deleteCourse(Integer id) {
        courseRepository.deleteById(id);
        log.info("Course deleted successfully, id={}", id);
    }

    public PageResponse<?> getAllCoursesWithFilter(int pageNo, int pageSize, String search, String status) {
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("courseId").descending());
        Specification<Course> spec = Specification.where(null);

        if (search != null && !search.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.or(
                            cb.like(root.get("courseName"), "%" + search + "%"),
                            cb.like(root.get("title"), "%" + search + "%")
                    )
            );
        }

        if (status != null && !status.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        Page<Course> page = courseRepository.findAll(spec, pageable);

        List<CourseDetailResponse> list = page.getContent().stream()
                .map(this::mapToResponse) // Sử dụng hàm map đã viết ở các bước trước
                .toList();

        return PageResponse.<CourseDetailResponse>builder()
                .pageNo(pageNo)
                .pageSize(pageSize)
                .totalPages(page.getTotalPages())
                .items(list)
                .build();
    }

    private CourseDetailResponse mapToResponse(Course course) {
        return CourseDetailResponse.builder()
                .courseId(course.getCourseId())
                .courseCode(course.getTitle())
                .courseName(course.getCourseName())
                .description(course.getDescription())
                .price(course.getPrice())
                .sale(course.getSale())
                .avatar(course.getAvatar())
                .status(course.getStatus())
                .build();
    }
}