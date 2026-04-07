package com.example.DoAn.controller;

import com.example.DoAn.dto.request.QuizSubmissionDTO;
import com.example.DoAn.dto.response.QuizTakingDTO;
import com.example.DoAn.model.QuizAnswer;
import com.example.DoAn.repository.QuizAnswerRepository;
import com.example.DoAn.service.FileUploadService;
import com.example.DoAn.service.QuizResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class StudentQuizTakingController {

    private final QuizResultService quizResultService;
    private final FileUploadService fileUploadService;
    private final QuizAnswerRepository quizAnswerRepository;

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
            model.addAttribute("classId", dto.getClassId());
            model.addAttribute("sessionId", dto.getSessionId());
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

    @PostMapping("/api/v1/student/quiz/audio")
    @ResponseBody
    public ResponseEntity<?> uploadAudio(@RequestParam("file") MultipartFile file) {
        try {
            String audioUrl = fileUploadService.uploadFile(file, "audio");
            return ResponseEntity.ok(Map.of("status", 200, "audioUrl", audioUrl));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", 400, "message", e.getMessage()));
        }
    }

    @GetMapping("/api/v1/student/quiz/result/{resultId}/ai-status")
    @ResponseBody
    public ResponseEntity<?> getAiStatus(@PathVariable Integer resultId) {
        List<QuizAnswer> answers = quizAnswerRepository.findByQuizResultResultId(resultId);
        List<Map<String, Object>> data = answers.stream()
                .filter(a -> {
                    String qt = a.getQuestion().getQuestionType();
                    return "WRITING".equals(qt) || "SPEAKING".equals(qt);
                })
                .map(a -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("questionId", a.getQuestion().getQuestionId());
                    map.put("aiGradingStatus", a.getAiGradingStatus() != null ? a.getAiGradingStatus() : "PENDING");
                    map.put("aiScore", a.getAiScore());
                    map.put("aiFeedback", a.getAiFeedback());
                    map.put("aiRubricJson", a.getAiRubricJson());
                    String displayScore = a.getTeacherOverrideScore() != null
                            ? a.getTeacherOverrideScore()
                            : a.getAiScore();
                    map.put("displayScore", displayScore);
                    return map;
                })
                .toList();
        return ResponseEntity.ok(Map.of("status", 200, "data", data));
    }
}
