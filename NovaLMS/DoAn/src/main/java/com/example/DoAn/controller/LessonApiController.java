package com.example.DoAn.controller;

import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.service.LearningService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/learning/lesson")
@RequiredArgsConstructor
public class LessonApiController {

    private final LearningService learningService;

    private String getEmailFromPrincipal(Principal principal) {
        if (principal == null) return null;
        if (principal instanceof OAuth2AuthenticationToken token) {
            return token.getPrincipal().getAttribute("email");
        }
        return principal.getName();
    }

    @GetMapping("/view-data/{lessonId}")
    public ResponseData<Map<String, Object>> getLessonViewData(@PathVariable Integer lessonId, Principal principal) {
        String email = getEmailFromPrincipal(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");

        return learningService.getLessonViewData(lessonId, email);
    }

    @PostMapping("/complete")
    public ResponseData<Void> markLessonCompleted(@RequestBody Map<String, Integer> payload, Principal principal) {
        String email = getEmailFromPrincipal(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");

        Integer lessonId = payload.get("lessonId");
        return learningService.markLessonCompleted(lessonId, email);
    }
}