package com.example.DoAn.controller;

import com.example.DoAn.dto.response.ProfileResponseDTO;
import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ProfileViewController {

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

        ResponseData<ProfileResponseDTO> result = userService.getUserProfile(getEmailFromPrincipal(principal));

        if (result.getStatus() != 200) {
            return "redirect:/login.html";
        }

        model.addAttribute("userProfile", result.getData());
        return "user/account-details";
    }
}