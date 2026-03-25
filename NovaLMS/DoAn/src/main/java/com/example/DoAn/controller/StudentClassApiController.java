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
    public ResponseData<List<MyClassDTO>> getMyClasses(Principal principal) {
        String email = getEmailFromPrincipal(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");
        return studentClassService.getMyClasses(email);
    }

    @GetMapping("/{classId}")
    public ResponseData<ClassDetailDTO> getClassDetail(@PathVariable Integer classId, Principal principal) {
        String email = getEmailFromPrincipal(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");
        return studentClassService.getClassDetail(email, classId);
    }
}
