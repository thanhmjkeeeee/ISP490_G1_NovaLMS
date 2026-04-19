package com.example.DoAn.controller;

import com.example.DoAn.dto.response.LessonQuizResponseDTO;
import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.service.LessonQuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/learning/lessons")
@RequiredArgsConstructor
public class LearningLessonQuizController {

    private final LessonQuizService lessonQuizService;

    private String getEmailFromPrincipal(Principal principal) {
        if (principal == null) return null;
        if (principal instanceof OAuth2AuthenticationToken t) return t.getPrincipal().getAttribute("email");
        return principal.getName();
    }

    // GET /api/v1/learning/lessons/{lessonId}/quizzes
    @GetMapping("/{lessonId}/quizzes")
    public ResponseData<List<LessonQuizResponseDTO>> getLessonQuizzes(
            @PathVariable Integer lessonId, Principal principal) {
        String email = getEmailFromPrincipal(principal);
        if (email == null) return ResponseData.error(401, "Vui lòng đăng nhập.");
        return ResponseData.success("Danh sách quiz",
                lessonQuizService.getLessonQuizzesForStudent(lessonId, email));
    }

    // GET /api/v1/learning/lessons/{lessonId}/quizzes/{quizId}
    @GetMapping("/{lessonId}/quizzes/{quizId}")
    public ResponseData<?> validateAndGetQuiz(
            @PathVariable Integer lessonId,
            @PathVariable Integer quizId,
            Principal principal) {
        String email = getEmailFromPrincipal(principal);
        if (email == null) return ResponseData.error(401, "Vui lòng đăng nhập.");
        lessonQuizService.validateQuizAvailableForStudent(lessonId, quizId, email);
        return ResponseData.success("Quiz sẵn sàng để làm");
    }
}
