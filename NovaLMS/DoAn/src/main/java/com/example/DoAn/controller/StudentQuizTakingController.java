package com.example.DoAn.controller;

import com.example.DoAn.dto.request.QuizSubmissionDTO;
import com.example.DoAn.dto.response.QuizTakingDTO;
import com.example.DoAn.service.QuizResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class StudentQuizTakingController {

    private final QuizResultService quizResultService;

    private String getEmailFromPrincipal(Principal principal) {
        if (principal == null) return null;
        if (principal instanceof OAuth2AuthenticationToken token) {
            return token.getPrincipal().getAttribute("email");
        }
        return principal.getName();
    }

    @GetMapping("/student/quiz/take/{quizId}")
    public String showQuizTakingPage(@PathVariable Integer quizId, Model model, Principal principal) {
        String email = getEmailFromPrincipal(principal);
        if (email == null) return "redirect:/login.html";

        try {
            QuizTakingDTO dto = quizResultService.getQuizForTaking(quizId, email);
            model.addAttribute("quiz", dto);
            return "student/quiz-take";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/student/my-courses";
        }
    }

    @PostMapping("/api/v1/student/quiz/submit")
    @ResponseBody
    public ResponseEntity<?> submitQuiz(@RequestBody QuizSubmissionDTO request, Principal principal) {
        String email = getEmailFromPrincipal(principal);
        if (email == null) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        try {
            Integer resultId = quizResultService.submitQuiz(request.getQuizId(), email, request.getAnswers());
            return ResponseEntity.ok(Map.of("resultId", resultId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
