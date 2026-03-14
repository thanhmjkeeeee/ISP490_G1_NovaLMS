package com.example.DoAn.controller;

import com.example.DoAn.dto.response.CoursePublicResponseDTO;
import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RestController cung cấp API cho khách (Guest/Public).
 * Tránh trùng lặp với các API quản trị bằng cách sử dụng tiền tố /public/.
 */
@RestController
@RequestMapping("/api/v1/public/courses")
public class CoursePublicManagementController {

    @Autowired
    private CourseService courseService;

    /**
     * API Lọc và tìm kiếm khóa học cho Guest (AJAX).
     * URL: /api/v1/public/courses/filter
     */
    @GetMapping("/filter")
    public ResponseEntity<ResponseData<List<CoursePublicResponseDTO>>> filterCourses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false, defaultValue = "newest") String sortBy) {

        // Gọi service lấy dữ liệu đã được map sang DTO
        List<CoursePublicResponseDTO> data = courseService.searchAndFilterCourses(keyword, categoryId, sortBy);

        return ResponseEntity.ok(new ResponseData<>(200, "Tải danh sách thành công", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseData<CoursePublicResponseDTO>> getCourseDetail(@PathVariable Integer id) {
        return courseService.getCourseDetail(id)
                .map(dto -> ResponseEntity.ok(new ResponseData<>(200, "Thành công", dto)))
                .orElse(ResponseEntity.notFound().build());
    }
}