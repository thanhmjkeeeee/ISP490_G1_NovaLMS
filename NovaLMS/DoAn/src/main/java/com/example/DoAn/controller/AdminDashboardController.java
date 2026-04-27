package com.example.DoAn.controller;

import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.model.Setting;
import com.example.DoAn.service.ExcelExportService;
import com.example.DoAn.service.SettingService;
import com.example.DoAn.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.DoAn.repository.CourseRepository;
import com.example.DoAn.repository.PaymentRepository;
import com.example.DoAn.repository.QuizRepository;
import com.example.DoAn.repository.RegistrationRepository;
import com.example.DoAn.repository.UserRepository;

import org.springframework.data.domain.PageRequest;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import java.util.*;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final StudentService studentService;
    private final SettingService settingService;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final RegistrationRepository registrationRepository;
    private final PaymentRepository paymentRepository;
    private final QuizRepository quizRepository;
    private final ExcelExportService excelExportService;
    private final com.example.DoAn.repository.VisitorLogRepository visitorLogRepository;

    @GetMapping("/dashboard")
    public String managerDashboard(Model model) {
        model.addAttribute("pageTitle", "Tổng quan hệ thống - Admin Dashboard");
        model.addAttribute("activePage", "dashboard");
        
        long studentCount = userRepository.countByRoleName("ROLE_STUDENT");
        long courseCount = courseRepository.count();
        long registrationCount = registrationRepository.count();
        long teacherCount = userRepository.countByRoleName("ROLE_TEACHER");
        long quizCount = quizRepository.count();
        
        java.math.BigDecimal totalRevenue = paymentRepository.sumTotalRevenue();
        if (totalRevenue == null) totalRevenue = java.math.BigDecimal.ZERO;
        
        // Lấy 5 đăng ký gần đây nhất (Eager Fetch)
        var recentRegistrations = registrationRepository.findRecentRegistrationsWithAssociations(PageRequest.of(0, 5));

        model.addAttribute("studentCount", studentCount);
        model.addAttribute("courseCount", courseCount);
        model.addAttribute("registrationCount", registrationCount);
        model.addAttribute("teacherCount", teacherCount);
        model.addAttribute("quizCount", quizCount);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("recentRegistrations", recentRegistrations);

        // --- DỮ LIỆU CHUYỂN ĐỔI (Guest to Student) ---
        long totalStudentAccounts = userRepository.countByRoleName("ROLE_STUDENT");
        long convertedStudentsCount = paymentRepository.countConvertedStudents();
        long totalGuests = totalStudentAccounts - convertedStudentsCount;
        if (totalGuests < 0) totalGuests = 0; // Tránh số âm nếu data có chút lệch
        
        double conversionRate = 0;
        if (totalStudentAccounts > 0) {
            conversionRate = (double) convertedStudentsCount / totalStudentAccounts * 100;
        }

        model.addAttribute("totalGuests", totalGuests);
        model.addAttribute("convertedStudentsCount", convertedStudentsCount);
        model.addAttribute("conversionRate", Math.round(conversionRate * 10.0) / 10.0);

        // --- DỮ LIỆU CHUYỂN ĐỔI GUEST (VISITOR TO USER) ---
        long totalUniqueVisitors = visitorLogRepository.countTotalVisitors();
        long registeredFromGuest = visitorLogRepository.countRegisteredVisitors();
        double guestToUserRate = 0;
        if (totalUniqueVisitors > 0) {
            guestToUserRate = (double) registeredFromGuest / totalUniqueVisitors * 100;
        }
        model.addAttribute("totalUniqueVisitors", totalUniqueVisitors);
        model.addAttribute("registeredFromGuest", registeredFromGuest);
        model.addAttribute("guestToUserRate", Math.round(guestToUserRate * 10.0) / 10.0);

        // --- DỮ LIỆU BIỂU ĐỒ TRUY CẬP (Visitor Traffic) ---
        List<Object[]> visitorData = visitorLogRepository.getDailyVisitors();
        List<String> visitorLabels = new ArrayList<>();
        List<Long> visitorValues = new ArrayList<>();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM");
        for (Object[] row : visitorData) {
            java.sql.Date sqlDate = (java.sql.Date) row[0];
            visitorLabels.add(sqlDate.toLocalDate().format(dtf));
            visitorValues.add(((Number) row[1]).longValue());
        }
        model.addAttribute("visitorLabels", visitorLabels);
        model.addAttribute("visitorValues", visitorValues);

        // --- DỮ LIỆU BIỂU ĐỒ DOANH THU ---
        List<Object[]> revenueData = paymentRepository.getMonthlyRevenue();
        List<Integer> revenueLabels = new ArrayList<>();
        List<java.math.BigDecimal> revenueValues = new ArrayList<>();
        // Khởi tạo 12 tháng với giá trị 0
        Map<Integer, java.math.BigDecimal> monthlyMap = new HashMap<>();
        for (int i = 1; i <= 12; i++) monthlyMap.put(i, java.math.BigDecimal.ZERO);
        for (Object[] row : revenueData) {
            monthlyMap.put(((Number) row[0]).intValue(), (java.math.BigDecimal) row[1]);
        }
        for (int i = 1; i <= 12; i++) {
            revenueLabels.add(i);
            revenueValues.add(monthlyMap.get(i));
        }
        model.addAttribute("revenueLabels", revenueLabels);
        model.addAttribute("revenueValues", revenueValues);

        // --- DỮ LIỆU BIỂU ĐỒ TẢI TRỌNG KHÓA HỌC ---
        List<Object[]> categoryData = registrationRepository.countEnrollmentsByCategory();
        model.addAttribute("categoryLabels", categoryData.stream().map(row -> row[0]).collect(Collectors.toList()));
        model.addAttribute("categoryValues", categoryData.stream().map(row -> row[1]).collect(Collectors.toList()));

        return "admin/dashboard";
    }

    @GetMapping("/revenue/export")
    public ResponseEntity<InputStreamResource> exportRevenue() {
        ByteArrayInputStream in = excelExportService.exportRevenueToExcel();

        String filename = "Revenue_Report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".xlsx";

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=" + filename);

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }

    @GetMapping("/registrations")
    public String registrationsPage(Model model) {
        model.addAttribute("pageTitle", "Quản lý đăng ký");
        model.addAttribute("activePage", "registrations");
        model.addAttribute("categories", settingService.getCourseCategories());
        return "admin/registrations";
    }

    @GetMapping("/settings")
    public String settingsPage(Model model) {
        model.addAttribute("pageTitle", "Cài đặt hệ thống - NovaLMS");
        model.addAttribute("activePage", "settings");
        return "admin/settings";
    }

    @GetMapping("/registrations/data")
    @ResponseBody
    public ResponseEntity<ResponseData<?>> getRegistrationsData(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(studentService.getAllRegistrations(keyword, status, courseId, page, size));
    }

    @PutMapping("/registrations/{id}/status")
    @ResponseBody
    public ResponseEntity<ResponseData<?>> updateRegistrationStatus(
            @PathVariable Integer id,
            @RequestParam String status,
            @RequestParam(required = false) String note) {
        return ResponseEntity.ok(studentService.updateRegistrationStatus(id, status, note));
    }

    // API lấy danh sách Category cho khóa học
    @GetMapping("/settings/categories")
    @ResponseBody
    public ResponseEntity<ResponseData<?>> getCourseCategories() {
        return ResponseEntity.ok(new ResponseData<>(HttpStatus.OK.value(), "Thành công", settingService.getCourseCategories()));
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