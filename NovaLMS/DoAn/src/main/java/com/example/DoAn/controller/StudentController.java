package com.example.DoAn.controller;


import com.example.DoAn.model.*;
import com.example.DoAn.repository.ClassRepository;
import com.example.DoAn.repository.CourseRepository;
import com.example.DoAn.repository.RegistrationRepository;
import com.example.DoAn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {

    private final CourseRepository courseRepository;
    private final ClassRepository classRepository;
    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;

    // Helper: Lấy User hiện tại từ Session
    private User getCurrentUser(Principal principal) {
        if (principal == null) return null;
        return userRepository.findByEmail(principal.getName()).orElse(null);
    }

    // ======================================================
    // 1. ENROLL COURSE FLOW
    // ======================================================

    // Màn hình chọn lớp để đăng ký (Từ trang Course Details bấm vào)
    @GetMapping("/enroll/{courseId}")
    public String showEnrollPage(@PathVariable Integer courseId, Model model) {
        Course course = courseRepository.findById(courseId).orElseThrow();
        // Chỉ lấy các lớp đang mở (Open)
        List<Clazz> openClasses = classRepository.findByCourse_CourseIdAndStatus(courseId, "Open");

        model.addAttribute("course", course);
        model.addAttribute("classes", openClasses);
        return "student/enroll-class"; // Trả về view chọn lớp
    }

    // Xử lý hành động Đăng ký
    @PostMapping("/enroll")
    public String processEnroll(@RequestParam Integer classId, Principal principal, RedirectAttributes ra) {
        User user = getCurrentUser(principal);
        Clazz clazz = classRepository.findById(classId).orElseThrow();

        // Check: Đã đăng ký chưa?
        boolean exists = registrationRepository.existsByUser_UserIdAndClazz_ClassIdAndStatusNot(
                user.getUserId(), classId, "Cancelled");

        if (exists) {
            ra.addFlashAttribute("error", "Bạn đã đăng ký lớp này rồi!");
            return "redirect:/student/enroll/" + clazz.getCourse().getCourseId();
        }

        // Tạo Registration mới
        Registration reg = Registration.builder()
                .user(user)
                .clazz(clazz)
                .course(clazz.getCourse())
                .status("Submitted") // Trạng thái chờ duyệt/thanh toán
                .registrationPrice(new BigDecimal("5000000")) // Giả định giá, thực tế lấy từ Class/Course
                .note("Đăng ký trực tuyến")
                .build();

        registrationRepository.save(reg);

        ra.addFlashAttribute("success", "Đăng ký thành công! Vui lòng chờ duyệt hoặc thanh toán.");
        return "redirect:/student/my-enrollments";
    }

    // ======================================================
    // 2. MY ENROLLMENTS & CANCEL FLOW
    // ======================================================

    @GetMapping("/my-enrollments")
    public String viewMyEnrollments(Principal principal, Model model) {
        if (principal == null) return "redirect:/login.html";

        // 1. Lấy Email chuẩn từ Principal (Xử lý cả OAuth2 và Local)
        String email;
        if (principal instanceof OAuth2AuthenticationToken token) {
            email = token.getPrincipal().getAttribute("email");
        } else {
            email = principal.getName();
        }

        // 2. Tìm User trong DB
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Tài khoản chưa tồn tại trong hệ thống!"));

        // 3. Lấy dữ liệu đăng ký
        List<Registration> list = registrationRepository.findByUser_UserIdOrderByRegistrationTimeDesc(user.getUserId());

        model.addAttribute("registrations", list);
        return "student/enroll-class"; // Trả về templates/enroll-class.html
    }

    @PostMapping("/cancel-enrollment")
    public String cancelEnrollment(@RequestParam Integer registrationId, Principal principal, RedirectAttributes ra) {
        User user = getCurrentUser(principal);
        Registration reg = registrationRepository.findById(registrationId).orElseThrow();

        // Security check: Phải là đơn của chính mình
        if (!reg.getUser().getUserId().equals(user.getUserId())) {
            ra.addFlashAttribute("error", "Không có quyền thực hiện!");
            return "redirect:/student/my-enrollments";
        }

        // Logic check: Chỉ cho hủy khi mới Submitted
        if ("Submitted".equals(reg.getStatus()) || "Pending".equals(reg.getStatus())) {
            reg.setStatus("Cancelled");
            registrationRepository.save(reg);
            ra.addFlashAttribute("success", "Đã hủy đăng ký thành công.");
        } else {
            ra.addFlashAttribute("error", "Không thể hủy khi lớp đã duyệt hoặc đang học.");
        }

        return "redirect:/student/my-enrollments";
    }

    // ======================================================
    // 3. MY COURSES (LEARNING DASHBOARD)
    // ======================================================

    @GetMapping("/my-courses")
    public String viewMyCourses(Principal principal, Model model) {
        User user = getCurrentUser(principal);
        // Chỉ lấy những khóa đã Approved (Đã đóng tiền/được duyệt)
        List<Registration> activeRegs = registrationRepository.findActiveRegistrations(user.getUserId());
        model.addAttribute("myCourses", activeRegs);
        return "student/my-courses";
    }
}