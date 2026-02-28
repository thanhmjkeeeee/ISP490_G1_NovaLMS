package com.example.DoAn.controller;

import com.example.DoAn.dto.UserProfileDTO;
import com.example.DoAn.service.FileUploadService;
import com.example.DoAn.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;
    @Autowired
    private FileUploadService fileUploadService;

    private String getEmailFromPrincipal(Principal principal) {
        if (principal instanceof OAuth2AuthenticationToken token) {
            return token.getPrincipal().getAttribute("email");
        }
        return principal.getName();
    }

    //HIỂN THỊ MÀN HÌNH PROFILE
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
        model.addAttribute("userProfile", userProfile);
        return "user/account-details";
    }

    //
    @PostMapping("/profile/update")
    public String updateProfile(@ModelAttribute("userProfile") UserProfileDTO dto,
                                Principal principal,
                                RedirectAttributes ra) {
        if (principal == null) return "redirect:/login.html";

        try {
            String email = getEmailFromPrincipal(principal);
            userService.updateUserProfile(email, dto);

            ra.addFlashAttribute("successMsg", "Cập nhật hồ sơ thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Có lỗi xảy ra: " + e.getMessage());
        }

        return "redirect:/profile";
    }
    @PostMapping("/profile/upload-avatar")
    public String uploadAvatar(@RequestParam("file") MultipartFile file, Principal principal, RedirectAttributes ra) {
        if (file.isEmpty()) {
            ra.addFlashAttribute("error", "Vui lòng chọn một file ảnh hợp lệ.");
            return "redirect:/profile";
        }

        try {
            String email = principal.getName();

            String uploadedAvatarUrl = fileUploadService.uploadImage(file);

            userService.updateAvatar(email, uploadedAvatarUrl);

            ra.addFlashAttribute("success", "Đã cập nhật ảnh đại diện thành công!");
        } catch (Exception e) {
            e.printStackTrace();
            ra.addFlashAttribute("error", "Có lỗi xảy ra trong quá trình tải ảnh lên.");
        }

        return "redirect:/profile";
    }
}