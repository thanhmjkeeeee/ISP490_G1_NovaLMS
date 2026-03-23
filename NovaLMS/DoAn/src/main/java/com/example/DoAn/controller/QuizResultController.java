package com.example.DoAn.controller;

import com.example.DoAn.dto.response.QuizResultDetailDTO;
import com.example.DoAn.service.QuizResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class QuizResultController {

    private final QuizResultService quizResultService;

    private String getEmailFromPrincipal(Principal principal) {
        if (principal == null) return null;
        if (principal instanceof OAuth2AuthenticationToken token) {
            return token.getPrincipal().getAttribute("email");
        }
        return principal.getName();
    }

    @GetMapping("/student/quiz/result/{resultId}")
    public String showQuizResult(@PathVariable Integer resultId, Model model, Principal principal) {
        String email = getEmailFromPrincipal(principal);
        if (email == null) return "redirect:/login.html";

        try {
            QuizResultDetailDTO result = quizResultService.getQuizResult(resultId, email);
            model.addAttribute("result", result);
            return "student/quiz-result";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/student/my-courses";
        }
    }
}
