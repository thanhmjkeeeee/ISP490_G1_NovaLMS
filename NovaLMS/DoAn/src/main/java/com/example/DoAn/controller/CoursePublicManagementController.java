package com.example.DoAn.controller;

import com.example.DoAn.dto.response.CoursePublicResponseDTO;
import com.example.DoAn.dto.response.PageResponse;
import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * RestController cung cấp API cho khách (Guest/Public).
 * Tránh trùng lặp với các API quản trị bằng cách sử dụng tiền tố /public/.
 */
@RestController
@RequestMapping("/api/v1/public/courses")
@RequiredArgsConstructor
public class CoursePublicManagementController {

    @Autowired
    private CourseService courseService;

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }

    /**
     * API Lọc và tìm kiếm khóa học cho Guest (AJAX) có phân trang.
     * URL: /api/v1/public/courses/filter
     */
    @GetMapping("/filter")
    public ResponseEntity<ResponseData<PageResponse<CoursePublicResponseDTO>>> filterCourses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false, defaultValue = "newest") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size) {

        // Gọi service lấy dữ liệu đã được map sang DTO có phân trang
        PageResponse<CoursePublicResponseDTO> data = courseService.searchAndFilterCourses(keyword, categoryId, sortBy, page, size);

        return ResponseEntity.ok(new ResponseData<>(200, "Tải danh sách thành công", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseData<CoursePublicResponseDTO>> getCourseDetail(@PathVariable Integer id) {
        return courseService.getCourseDetail(id)
                .map(dto -> ResponseEntity.ok(new ResponseData<>(200, "Thành công", dto)))
                .orElse(ResponseEntity.notFound().build());
    }
}