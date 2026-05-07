package com.example.DoAn.controller;

import com.example.DoAn.dto.response.QuizResultDetailDTO;
import com.example.DoAn.service.QuizResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/teacher/quiz")
@RequiredArgsConstructor
public class TeacherQuizGradingController {

    private final QuizResultService quizResultService;

    private String getEmailFromPrincipal(Principal principal) {
        if (principal == null) return null;
        if (principal instanceof OAuth2AuthenticationToken token) {
            return token.getPrincipal().getAttribute("email");
        }
        return principal.getName();
    }

    @GetMapping("/grading")
    public String gradingList() {
        return "teacher/quiz-grading-list";
    }

    @GetMapping("/graded")
    public String gradedList() {
        return "teacher/quiz-graded-list";
    }

    /**
     * Xem chi tiết kết quả/Chấm điểm tại Workspace (Dùng URL riêng).
     */
    @GetMapping("/grading/{resultId}")
    public String gradingDetail(@PathVariable Integer resultId, Model model) {
        model.addAttribute("initialTab", "grading");
        model.addAttribute("initialResultId", resultId);
        model.addAttribute("initialType", "QUIZ");
        return "teacher/workspace";
    }
}
