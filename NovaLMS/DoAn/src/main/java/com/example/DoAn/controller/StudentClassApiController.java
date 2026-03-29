package com.example.DoAn.controller;

import com.example.DoAn.dto.response.*;
import com.example.DoAn.service.StudentClassService;
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
        if (email == null) return ResponseData.error(401, "Unauthorized");

        return studentClassService.getMyClasses(email, keyword, status, page, size);
    }

    @GetMapping("/{classId}")
    public ResponseData<ClassDetailDTO> getClassDetail(@PathVariable Integer classId, Principal principal) {
        String email = getEmailFromPrincipal(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");
        return studentClassService.getClassDetail(email, classId);
    }
}
