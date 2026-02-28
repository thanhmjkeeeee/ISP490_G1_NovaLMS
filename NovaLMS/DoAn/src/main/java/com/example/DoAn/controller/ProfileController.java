package com.example.DoAn.controller;

import com.example.DoAn.dto.UserProfileDTO;
import com.example.DoAn.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;

    // Hàm dùng chung để lấy Email từ Principal (Hỗ trợ cả Form Login & Google Login)
    private String getEmailFromPrincipal(Principal principal) {
        if (principal instanceof OAuth2AuthenticationToken token) {
            return token.getPrincipal().getAttribute("email");
        }
        return principal.getName();
    }

    // 1. HIỂN THỊ MÀN HÌNH PROFILE
    @GetMapping("/profile")
    public String viewProfile(Principal principal, Model model) {
        if (principal == null) return "redirect:/login.html";

        String email = getEmailFromPrincipal(principal);
        UserProfileDTO userProfile = userService.getUserProfile(email);

//        boolean isAdmin = authentication.getAuthorities().stream()
//                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
//
//        if (isAdmin) {
//            return "admin/account-details"; // Admin thì load layout admin
//        }

        // Gửi DTO ra giao diện
        model.addAttribute("userProfile", userProfile);
        return "user/account-details"; // Trỏ đúng tên file HTML của bạn
    }

    // 2. XỬ LÝ CẬP NHẬT PROFILE
    @PostMapping("/profile/update")
    public String updateProfile(@ModelAttribute("userProfile") UserProfileDTO dto,
                                Principal principal,
                                RedirectAttributes ra) {
        if (principal == null) return "redirect:/login.html";

        try {
            String email = getEmailFromPrincipal(principal);
            userService.updateUserProfile(email, dto);

            // Gửi thông báo thành công
            ra.addFlashAttribute("successMsg", "Cập nhật hồ sơ thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Có lỗi xảy ra: " + e.getMessage());
        }

        return "redirect:/profile";
    }
}