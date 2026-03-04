package com.example.DoAn.controller;

import com.example.DoAn.dto.EnrollPageDTO;
import com.example.DoAn.dto.MyCourseDTO;
import com.example.DoAn.dto.ServiceResult;
import com.example.DoAn.model.Registration;
import com.example.DoAn.model.User;
import com.example.DoAn.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    private String getEmailFromPrincipal(Principal principal) {
        if (principal == null) return null;
        if (principal instanceof OAuth2AuthenticationToken token) {
            return token.getPrincipal().getAttribute("email");
        }
        return principal.getName();
    }

    @GetMapping("/enroll/{courseId}")
    public String showEnrollPage(@PathVariable Integer courseId, Model model, Principal principal) {
        String email = getEmailFromPrincipal(principal);
        if (email == null) return "redirect:/login.html";

        ServiceResult<EnrollPageDTO> result = studentService.getEnrollPageData(email, courseId);

        if (!result.isSuccess()) {
            return "redirect:/courses.html";
        }

        model.addAttribute("course", result.getData().getCourse());
        model.addAttribute("classes", result.getData().getClasses());
        return "student/enroll-class";
    }

    @PostMapping("/enroll")
    public String processEnroll(@RequestParam Integer classId,
                                @RequestParam Integer courseId,
                                Principal principal,
                                RedirectAttributes ra) {

        String email = getEmailFromPrincipal(principal);
        if (email == null) return "redirect:/login.html";

        ServiceResult<Integer> result = studentService.enrollCourse(email, classId);

        if (result.isSuccess()) {
            ra.addFlashAttribute("success", result.getMessage());
            return "redirect:/student/my-enrollments";
        } else {
            ra.addFlashAttribute("error", result.getMessage());
            Integer redirectCourseId = result.getData() != null ? result.getData() : courseId;
            return "redirect:/student/enroll/" + redirectCourseId;
        }
    }

    @GetMapping("/my-enrollments")
    public String viewMyEnrollments(Principal principal, Model model) {
        String email = getEmailFromPrincipal(principal);
        if (email == null) return "redirect:/login.html";

        ServiceResult<List<Registration>> result = studentService.getMyEnrollments(email);

        if (!result.isSuccess()) {
            model.addAttribute("error", result.getMessage());
        } else {
            model.addAttribute("registrations", result.getData());
        }

        return "student/my-enrollments";
    }

    @GetMapping("/my-courses")
    public String viewMyCourses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(defaultValue = "registrationTime_desc") String sort,
            Principal principal, Model model) {

        String email = getEmailFromPrincipal(principal);
        if (email == null) return "redirect:/login.html";

        String[] sortParams = sort.split("_");
        Sort.Direction direction = sortParams[1].equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));

        ServiceResult<Page<MyCourseDTO>> result = studentService.getMyCourses(email, keyword, categoryId, pageable);

        if (!result.isSuccess()) {
            model.addAttribute("error", result.getMessage());
        } else {
            model.addAttribute("coursePage", result.getData());
        }

        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("currentSort", sort);

        return "student/my-courses";
    }

    @GetMapping("/dashboard")
    public String viewDashboard(Principal principal, Model model) {
        String email = getEmailFromPrincipal(principal);
        if (email == null) return "redirect:/login.html";

        ServiceResult<User> result = studentService.getDashboardData(email);

        if (!result.isSuccess()) {
            return "redirect:/login.html";
        }

        model.addAttribute("user", result.getData());
        return "student/dashboard";
    }
}