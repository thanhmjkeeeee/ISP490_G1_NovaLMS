package com.example.DoAn.controller;

import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.service.TeacherClassSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/teacher/class-sessions")
@RequiredArgsConstructor
public class TeacherClassSessionApiController {

    private final TeacherClassSessionService sessionService;

    private String getEmail(Principal principal) {
        if (principal instanceof OAuth2AuthenticationToken token) {
            return token.getPrincipal().getAttribute("email");
        }
        return principal != null ? principal.getName() : null;
    }

    @GetMapping("/{classId}")
    public ResponseData<List<Map<String, Object>>> getSessions(@PathVariable Integer classId, Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");
        return sessionService.getSessionsByClass(email, classId);
    }

    @GetMapping("/{classId}/detail")
    public ResponseData<Map<String, Object>> getClassDetail(@PathVariable Integer classId, Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");
        return sessionService.getClassSessionsDetail(email, classId);
    }

    @GetMapping("/{classId}/quizzes")
    public ResponseData<List<Map<String, Object>>> getQuizzes(@PathVariable Integer classId, Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");
        return sessionService.getAvailableQuizzes(email, classId);
    }

    @PostMapping("/{classId}")
    public ResponseData<Integer> createSession(
            @PathVariable Integer classId,
            @RequestParam Integer sessionNumber,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime sessionDate,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) Integer quizId,
            Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");
        return sessionService.createSession(email, classId, sessionNumber, sessionDate, startTime, endTime, topic, notes, quizId);
    }

    @PutMapping("/update/{sessionId}")
    public ResponseData<Void> updateSession(
            @PathVariable Integer sessionId,
            @RequestParam(required = false) Integer sessionNumber,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime sessionDate,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) Integer quizId,
            Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");
        return sessionService.updateSession(email, sessionId, sessionNumber, sessionDate, startTime, endTime, topic, notes, quizId);
    }

    @DeleteMapping("/{sessionId}")
    public ResponseData<Void> deleteSession(@PathVariable Integer sessionId, Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");
        return sessionService.deleteSession(email, sessionId);
    }
}
