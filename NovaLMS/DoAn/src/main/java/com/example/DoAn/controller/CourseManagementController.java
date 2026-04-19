package com.example.DoAn.controller;

import com.example.DoAn.dto.request.CourseRequestDTO;
import com.example.DoAn.dto.response.CourseDetailResponse;
import com.example.DoAn.dto.response.PageResponse;
import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.service.ICourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/courses")
@Validated
@Slf4j
@Tag(name = "Course Management Controller")
@RequiredArgsConstructor
public class CourseManagementController {

    private final ICourseService courseService;

    @Operation(summary = "Get course list with Filter")
    @GetMapping("/list")
    public ResponseData<PageResponse<?>> getCourses(
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") @Min(1) int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        try {
            PageResponse<?> courses = courseService.getAllCoursesWithFilter(pageNo, pageSize, search, status);
            return new ResponseData<>(HttpStatus.OK.value(), "Thành công", courses);
        } catch (Exception e) {
            return ResponseData.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }

    @Operation(summary = "Add new course")
    @PostMapping("/")
    public ResponseData<Integer> addCourse(@Valid @RequestBody CourseRequestDTO request) {
        log.info("Request add course: {}", request.getCourseName());
        try {
            Integer courseId = courseService.saveCourse(request);
            return new ResponseData<>(HttpStatus.CREATED.value(), "Course created successfully", courseId);
        } catch (Exception e) {
            return ResponseData.error(HttpStatus.BAD_REQUEST.value(), "Add course fail: " + e.getMessage());
        }
    }

    @Operation(summary = "Update course")
    @PutMapping("/{id}")
    public ResponseData<Void> updateCourse(@PathVariable Integer id, @Valid @RequestBody CourseRequestDTO request) {
        try {
            courseService.updateCourse(id, request);
            return new ResponseData<>(HttpStatus.ACCEPTED.value(), "Course updated successfully");
        } catch (Exception e) {
            return ResponseData.error(HttpStatus.BAD_REQUEST.value(), "Update fail");
        }
    }

    @Operation(summary = "Get course detail")
    @GetMapping("/{id}")
    public ResponseData<CourseDetailResponse> getCourse(@PathVariable Integer id) {
        try {
            CourseDetailResponse response = courseService.getById(id);
            return new ResponseData<>(HttpStatus.OK.value(), "Thành công", response);
        } catch (Exception e) {
            return ResponseData.error(HttpStatus.NOT_FOUND.value(), "Course not found");
        }
    }

    @Operation(summary = "Delete course")
    @DeleteMapping("/{id}")
    public ResponseData<Void> deleteCourse(@PathVariable Integer id) {
        try {
            courseService.deleteCourse(id);
            return new ResponseData<>(HttpStatus.NO_CONTENT.value(), "Course deleted");
        } catch (Exception e) {
            return ResponseData.error(HttpStatus.BAD_REQUEST.value(), "Delete fail");
        }
    }

    @Operation(summary = "Get lesson count of course")
    @GetMapping("/{id}/lessons/count")
    public ResponseData<Long> getLessonCount(@PathVariable Integer id) {
        try {
            long count = courseService.getLessonCount(id);
            return new ResponseData<>(HttpStatus.OK.value(), "Thành công", count);
        } catch (Exception e) {
            return ResponseData.error(HttpStatus.NOT_FOUND.value(), "Course not found");
        }
    }
}