package com.example.DoAn.controller;

import com.example.DoAn.dto.*;
import com.example.DoAn.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/student")
@RequiredArgsConstructor
public class StudentApiController {

    private final StudentService studentService;

    private String getEmailFromPrincipal(Principal principal) {
        if (principal instanceof OAuth2AuthenticationToken token) {
            return token.getPrincipal().getAttribute("email");
        }
        return principal.getName();
    }

    @PostMapping("/enroll")
    public ResponseData<Integer> processEnroll(@Valid @RequestBody EnrollRequestDTO request, Principal principal) {
        String email = getEmailFromPrincipal(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");
        return studentService.enrollCourse(email, request);
    }

    @GetMapping("/my-enrollments/data")
    public ResponseData<List<RegistrationResponseDTO>> getMyEnrollmentsData(Principal principal) {
        String email = getEmailFromPrincipal(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");
        return studentService.getMyEnrollments(email);
    }

    @GetMapping("/my-courses/data")
    public ResponseData<PageResponse<MyCourseDTO>> getMyCoursesData(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(defaultValue = "registrationTime_desc") String sort,
            Principal principal) {

        String email = getEmailFromPrincipal(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");
        return studentService.getMyCourses(email, keyword, categoryId, page, size, sort);
    }

    @GetMapping("/dashboard/data")
    public ResponseData<DashboardResponseDTO> getDashboardData(Principal principal) {
        String email = getEmailFromPrincipal(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");
        return studentService.getDashboardData(email);
    }
}