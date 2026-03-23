package com.example.DoAn.controller;

import com.example.DoAn.dto.request.QuestionGradingRequestDTO;
import com.example.DoAn.dto.response.PageResponse;
import com.example.DoAn.dto.response.QuizResultPendingDTO;
import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.service.QuizResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/teacher/quiz-results")
@RequiredArgsConstructor
public class TeacherQuizGradingApiController {

    private final QuizResultService quizResultService;

    private String getEmailFromPrincipal(Principal principal) {
        if (principal == null) return null;
        if (principal instanceof OAuth2AuthenticationToken token) {
            return token.getPrincipal().getAttribute("email");
        }
        return principal.getName();
    }

    @GetMapping("/pending")
    public ResponseData<PageResponse<QuizResultPendingDTO>> getPendingGradingList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {
        String email = getEmailFromPrincipal(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");

        try {
            PageResponse<QuizResultPendingDTO> list = quizResultService.getPendingGradingList(email, page, size);
            return ResponseData.success("Success", list);
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    @PostMapping("/{resultId}/grade")
    public ResponseData<Void> gradeQuizResult(
            @PathVariable Integer resultId,
            @RequestBody List<QuestionGradingRequestDTO> gradingItems,
            Principal principal) {
        String email = getEmailFromPrincipal(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");

        try {
            quizResultService.gradeQuizResult(resultId, gradingItems, email);
            return ResponseData.success("Đã lưu điểm thành công!");
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }
}
