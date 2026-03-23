package com.example.DoAn.controller;

import com.example.DoAn.dto.response.EnrollPageResponseDTO;
import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

import com.example.DoAn.service.StudentDashboardService;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentViewController {

    private final StudentService studentService;
    private final StudentDashboardService studentDashboardService;

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

        ResponseData<EnrollPageResponseDTO> result = studentService.getEnrollPageData(email, courseId);
        if (result.getStatus() != 200) return "redirect:/courses.html";

        model.addAttribute("course", result.getData().getCourse());
        model.addAttribute("classes", result.getData().getClasses());
        return "student/enroll-class";
    }

    @GetMapping("/my-enrollments")
    public String viewMyEnrollments() {
        return "student/my-enrollments";
    }

    @GetMapping("/my-courses")
    public String viewMyCourses() {
        return "student/my-courses";
    }

    @GetMapping("/quiz")
    public String viewMyQuizzes() {
        return "student/my-quizzes";
    }

    @GetMapping("/dashboard")
    public String viewDashboard() {
        return "student/dashboard";
    }

    @GetMapping("/api/v1/dashboard/data")
    @org.springframework.web.bind.annotation.ResponseBody
    public org.springframework.http.ResponseEntity<?> getDashboardData(Principal principal) {
        String email = getEmailFromPrincipal(principal);
        if (email == null) return org.springframework.http.ResponseEntity.status(401).body(java.util.Map.of("message", "Unauthorized"));
        try {
            return org.springframework.http.ResponseEntity.ok(studentDashboardService.getDashboardData(email));
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        }
    }
}