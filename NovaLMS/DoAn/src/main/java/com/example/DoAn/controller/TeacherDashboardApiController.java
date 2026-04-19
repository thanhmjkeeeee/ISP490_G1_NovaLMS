package com.example.DoAn.controller;

import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.dto.response.TeacherDashboardResponseDTO;
import com.example.DoAn.service.ITeacherDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/teacher/dashboard")
@RequiredArgsConstructor
public class TeacherDashboardApiController {

    private final ITeacherDashboardService teacherDashboardService;

    @GetMapping
    public ResponseData<TeacherDashboardResponseDTO> getDashboard(Principal principal) {
        if (principal == null) {
            return ResponseData.error(401, "Vui lòng đăng nhập lại.");
        }
        String email = getEmailFromPrincipal(principal);
        if (email == null) {
            return ResponseData.error(400, "Không lấy được email từ tài khoản. Vui lòng thử lại.");
        }
        return ResponseData.success("Thành công", teacherDashboardService.getDashboardData(email));
    }

    private String getEmailFromPrincipal(Principal principal) {
        if (principal instanceof OAuth2AuthenticationToken token) {
            return (String) token.getPrincipal().getAttributes().get("email");
        }
        return principal.getName();
    }
}