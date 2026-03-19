package com.example.DoAn.controller;

import com.example.DoAn.dto.request.ChangePasswordRequest;
import com.example.DoAn.dto.request.ProfileRequestDTO;
import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileApiController {

    private final UserService userService;

    private String getEmailFromPrincipal(Principal principal) {
        if (principal instanceof OAuth2AuthenticationToken token) {
            return token.getPrincipal().getAttribute("email");
        }
        return principal.getName();
    }

    @PutMapping("/update")
    public ResponseData<Void> updateProfile(@RequestBody ProfileRequestDTO dto, Principal principal) {
        if (principal == null) return ResponseData.error(401, "Unauthorized");
        return userService.updateUserProfile(getEmailFromPrincipal(principal), dto);
    }

    @PostMapping("/upload-avatar")
    public ResponseData<String> uploadAvatar(@RequestParam("file") MultipartFile file, Principal principal) {
        if (principal == null) return ResponseData.error(401, "Unauthorized");
        return userService.updateAvatar(getEmailFromPrincipal(principal), file);
    }
    @PutMapping("/change-password")
    public ResponseData<Void> changePassword(@RequestBody ChangePasswordRequest request, Principal principal) {
        String email = getEmailFromPrincipal(principal); // Hoặc principal.getName()
        return userService.changePassword(email, request);
    }
}