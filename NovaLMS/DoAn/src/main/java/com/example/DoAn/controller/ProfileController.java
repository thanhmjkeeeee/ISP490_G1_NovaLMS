package com.example.DoAn.controller;

import com.example.DoAn.dto.ServiceResult;
import com.example.DoAn.dto.UserProfileDTO;
import com.example.DoAn.service.UserService;
import lombok.RequiredArgsConstructor;
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

    private String getEmailFromPrincipal(Principal principal) {
        if (principal instanceof OAuth2AuthenticationToken token) {
            return token.getPrincipal().getAttribute("email");
        }
        return principal.getName();
    }

    @GetMapping("/profile")
    public String viewProfile(Principal principal, Model model) {
        if (principal == null) return "redirect:/login.html";

        String email = getEmailFromPrincipal(principal);
        ServiceResult<UserProfileDTO> result = userService.getUserProfile(email);

        if (!result.isSuccess()) {
            return "redirect:/login.html";
        }

        model.addAttribute("userProfile", result.getData());
        return "user/account-details";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@ModelAttribute("userProfile") UserProfileDTO dto,
                                Principal principal,
                                RedirectAttributes ra) {
        if (principal == null) return "redirect:/login.html";

        String email = getEmailFromPrincipal(principal);
        ServiceResult<Void> result = userService.updateUserProfile(email, dto);

        if (result.isSuccess()) {
            ra.addFlashAttribute("successMsg", result.getMessage());
        } else {
            ra.addFlashAttribute("errorMsg", result.getMessage());
        }

        return "redirect:/profile";
    }

    @PostMapping("/profile/upload-avatar")
    public String uploadAvatar(@RequestParam("file") MultipartFile file,
                               Principal principal,
                               RedirectAttributes ra) {
        if (principal == null) return "redirect:/login.html";

        String email = getEmailFromPrincipal(principal);
        ServiceResult<String> result = userService.updateAvatar(email, file);

        if (result.isSuccess()) {
            ra.addFlashAttribute("success", result.getMessage());
        } else {
            ra.addFlashAttribute("error", result.getMessage());
        }

        return "redirect:/profile";
    }
}