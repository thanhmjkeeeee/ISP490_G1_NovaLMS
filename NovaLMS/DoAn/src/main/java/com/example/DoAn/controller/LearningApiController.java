package com.example.DoAn.controller;

import com.example.DoAn.dto.response.CourseLearningInfoDTO;
import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.service.LearningService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

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
    public ResponseEntity<ResponseData<CourseLearningInfoDTO>> getCourseInfo(@PathVariable Long courseId, Principal principal) {
        String email = getEmailFromPrincipal(principal);
        if (email == null) return ResponseEntity.status(401).body(ResponseData.error(401, "Unauthorized"));

        ResponseData<CourseLearningInfoDTO> response = learningService.getCourseLearningInfo(courseId, email);
        if (response.getStatus() != 200) {
            return ResponseEntity.status(response.getStatus()).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/track-time")
    public ResponseEntity<ResponseData<Void>> trackTime(@RequestBody Map<String, Integer> payload, Principal principal) {
        String email = getEmailFromPrincipal(principal);
        if (email == null) return ResponseEntity.status(401).body(ResponseData.error(401, "Unauthorized"));

        Integer seconds = payload.get("seconds");
        if (seconds == null) seconds = 0;

        ResponseData<Void> response = learningService.trackTime(email, seconds);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}