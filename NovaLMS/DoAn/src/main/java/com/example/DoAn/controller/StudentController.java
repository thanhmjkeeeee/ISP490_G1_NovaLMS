package com.example.DoAn.controller;

import com.example.DoAn.model.*;
import com.example.DoAn.repository.ClassRepository;
import com.example.DoAn.repository.CourseRepository;
import com.example.DoAn.repository.RegistrationRepository;
import com.example.DoAn.repository.UserRepository;
import com.example.DoAn.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.DoAn.dto.MyCourseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

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
    private final StudentService studentService;

    // ĐÃ FIX: Hàm dùng chung để lấy User an toàn cho cả Google và form thường
    private User getCurrentUser(Principal principal) {
        if (principal == null) return null;

        String email;
        if (principal instanceof OAuth2AuthenticationToken token) {
            email = token.getPrincipal().getAttribute("email");
        } else {
            email = principal.getName();
        }

        return userRepository.findByEmail(email).orElse(null);
    }

    // ======================================================
    // 1. ENROLL COURSE FLOW
    // ======================================================

    @GetMapping("/enroll/{courseId}")
    public String showEnrollPage(@PathVariable Integer courseId, Model model, Principal principal) {
        if (principal == null) return "redirect:/login.html";

        Course course = courseRepository.findById(courseId).orElseThrow();
        List<Clazz> openClasses = classRepository.findByCourse_CourseIdAndStatus(courseId, "Open");

        model.addAttribute("course", course);
        model.addAttribute("classes", openClasses);
        return "student/enroll-class";
    }

    @PostMapping("/enroll")
    public String processEnroll(@RequestParam Integer classId, Principal principal, RedirectAttributes ra) {
        User user = getCurrentUser(principal);
        if (user == null) return "redirect:/login.html";

        Clazz clazz = classRepository.findById(classId).orElseThrow();

        boolean exists = registrationRepository.existsByUser_UserIdAndClazz_ClassIdAndStatusNot(
                user.getUserId(), classId, "Cancelled");

        if (exists) {
            ra.addFlashAttribute("error", "Bạn đã đăng ký lớp này rồi!");
            return "redirect:/student/enroll/" + clazz.getCourse().getCourseId();
        }

        Registration reg = Registration.builder()
                .user(user)
                .clazz(clazz)
                .course(clazz.getCourse())
                .status("Submitted")
                .registrationPrice(new BigDecimal("5000000"))
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
        User user = getCurrentUser(principal);
        if (user == null) return "redirect:/login.html";

        List<Registration> list = registrationRepository.findByUser_UserIdOrderByRegistrationTimeDesc(user.getUserId());
        model.addAttribute("registrations", list);
        return "student/my-enrollments";
    }

    // ======================================================
    // 3. MY COURSES (LEARNING DASHBOARD)
    // ======================================================

    @GetMapping("/my-courses")
    public String viewMyCourses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(defaultValue = "registrationTime_desc") String sort,
            Principal principal, Model model) {

        User user = getCurrentUser(principal);
        if (user == null) return "redirect:/login.html";

        try {
            // Setup phân trang và sắp xếp
            String[] sortParams = sort.split("_");
            Sort.Direction direction = sortParams[1].equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));

            // Gọi Service
            Page<MyCourseDTO> coursePage = studentService.getMyCourses(user.getUserId(), keyword, categoryId, pageable);
            model.addAttribute("coursePage", coursePage);

            // Giữ lại trạng thái UI
            model.addAttribute("keyword", keyword);
            model.addAttribute("categoryId", categoryId);
            model.addAttribute("currentSort", sort);

        } catch (Exception e) {
            e.printStackTrace(); // In lỗi ra Console để Dev đọc
            model.addAttribute("error", "Lỗi tải dữ liệu: " + e.getMessage());
        }

        return "student/my-courses";
    }
}