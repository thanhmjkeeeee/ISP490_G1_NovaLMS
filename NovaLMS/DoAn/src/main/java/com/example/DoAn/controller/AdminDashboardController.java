package com.example.DoAn.controller;

import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.model.Setting;
import com.example.DoAn.service.SettingService;
import com.example.DoAn.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final StudentService studentService;
    private final SettingService settingService;

    @GetMapping("/dashboard")
    public String managerDashboard(Model model) {
        model.addAttribute("pageTitle", "Admin Dashboard");
        return "admin/dashboard";
    }

    @GetMapping("/registrations")
    public String registrationsPage(Model model) {
        model.addAttribute("pageTitle", "Quản lý đăng ký");
        model.addAttribute("activePage", "registrations");
        model.addAttribute("categories", settingService.getCourseCategories());
        return "admin/registrations";
    }

    @GetMapping("/registrations/data")
    @ResponseBody
    public ResponseEntity<ResponseData> getRegistrationsData(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(studentService.getAllRegistrations(keyword, status, courseId, page, size));
    }

    @PutMapping("/registrations/{id}/status")
    @ResponseBody
    public ResponseEntity<ResponseData> updateRegistrationStatus(
            @PathVariable Integer id,
            @RequestParam String status,
            @RequestParam(required = false) String note) {
        return ResponseEntity.ok(studentService.updateRegistrationStatus(id, status, note));
    }

    // API lấy danh sách Category cho khóa học
    @GetMapping("/settings/categories")
    @ResponseBody
    public ResponseEntity<ResponseData<?>> getCourseCategories() {
        return ResponseEntity.ok(new ResponseData<>(HttpStatus.OK.value(), "Success", settingService.getCourseCategories()));
    }

    // API thêm mới Category cho khóa học
    @PostMapping("/settings/category")
    @ResponseBody
    public ResponseEntity<ResponseData<Setting>> addCourseCategory(@RequestBody CategoryRequest request) {
        try {
            Setting category = settingService.saveCourseCategory(request.getName(), request.getValue());
            return ResponseEntity.ok(new ResponseData<>(HttpStatus.CREATED.value(), "Thêm thành công!", category));
        } catch (Exception e) {
            return ResponseEntity.ok(new ResponseData<>(HttpStatus.BAD_REQUEST.value(), "Lỗi: " + e.getMessage()));
        }
    }

    // DTO for category request
    public static class CategoryRequest {
        private String name;
        private String value;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }
}