package com.example.DoAn.controller;

import com.example.DoAn.dto.response.*;
import com.example.DoAn.model.User;
import com.example.DoAn.repository.UserRepository;
import com.example.DoAn.service.StudentClassService;
import com.example.DoAn.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/student/classes")
@RequiredArgsConstructor
public class StudentClassApiController {

    private final StudentClassService studentClassService;
    private final StudentService studentService;
    private final UserRepository userRepository;

    private String getEmailFromPrincipal(Principal principal) {
        if (principal instanceof OAuth2AuthenticationToken token) {
            return token.getPrincipal().getAttribute("email");
        }
        return principal.getName();
    }

    @GetMapping
    public ResponseData<PageResponse<MyClassDTO>> getMyClasses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            Principal principal) {

        String email = getEmailFromPrincipal(principal);
        if (email == null) return ResponseData.error(401, "Vui lòng đăng nhập.");

        return studentClassService.getMyClasses(email, keyword, status, page, size);
    }

    @GetMapping("/{classId}/detail")
    public ResponseData<StudentClassDetailResponse> getClassDetail(@PathVariable Integer classId, Principal principal) {
        String email = getEmailFromPrincipal(principal);
        if (email == null) return ResponseData.error(401, "Vui lòng đăng nhập.");

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return ResponseData.error(404, "Không tìm thấy người dùng");

        try {
            return ResponseData.success("Chi tiết lớp học", studentService.getStudentClassDetail(classId, user.getUserId()));
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }
}
