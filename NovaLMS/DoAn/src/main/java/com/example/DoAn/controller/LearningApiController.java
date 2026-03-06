package com.example.DoAn.controller;

import com.example.DoAn.dto.CourseLearningInfoDTO;
import com.example.DoAn.dto.ResponseData;
import com.example.DoAn.service.LearningService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/learning")
@RequiredArgsConstructor
public class LearningApiController {

    private final LearningService learningService;

    private String getEmailFromPrincipal(Principal principal) {
        if (principal == null) return null;
        if (principal instanceof OAuth2AuthenticationToken token) {
            return token.getPrincipal().getAttribute("email");
        }
        return principal.getName();
    }

    @GetMapping("/course/{courseId}")
    public ResponseData<CourseLearningInfoDTO> getCourseData(@PathVariable Long courseId, Principal principal) {
        String email = getEmailFromPrincipal(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");
        return learningService.getCourseLearningInfo(courseId, email);
    }
}