package com.example.DoAn.controller;

import com.example.DoAn.dto.response.RegistrationResponseDTO;
import com.example.DoAn.dto.response.DashboardResponseDTO;
import com.example.DoAn.dto.request.EnrollRequestDTO;
import com.example.DoAn.dto.response.MyCourseDTO;
import com.example.DoAn.dto.response.PageResponse;
import com.example.DoAn.dto.response.ResponseData;
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

    /**
     * Legacy enroll endpoint — creates PENDING registration.
     * For actual payment, use POST /api/v1/enroll-with-payment instead.
     */
    @PostMapping("/enroll")
    public ResponseData<?> processEnroll(@Valid @RequestBody EnrollRequestDTO request, Principal principal) {
        String email = getEmailFromPrincipal(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");
        ResponseData<Integer> result = studentService.enrollCourse(email, request);

        if (result.getStatus() == 200 || result.getStatus() == 201) {
            return new ResponseData<>(result.getStatus(),
                    "Đăng ký thành công! Vui lòng gọi POST /api/v1/enroll-with-payment để tạo link thanh toán PayOS.",
                    result.getData());
        }

        // Nếu đã đăng ký rồi (status != Cancelled) → gợi ý retry thanh toán hoặc liên hệ admin
        if (result.getStatus() == 400) {
            return new ResponseData<>(result.getStatus(),
                    "Bạn đã có đăng ký chờ xử lý cho lớp này. "
                    + "Vui lòng thanh toán hoặc liên hệ quản trị viên để được hỗ trợ.",
                    null);
        }

        return result;
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