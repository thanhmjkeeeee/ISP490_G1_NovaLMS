package com.example.DoAn.controller;

import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.service.ExpertReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/expert/question-review")
@RequiredArgsConstructor
public class ExpertReviewController {

    private final ExpertReviewService expertReviewService;

    private String getEmail(Principal principal) {
        if (principal instanceof OAuth2AuthenticationToken token) {
            return token.getPrincipal().getAttribute("email");
        }
        return principal != null ? principal.getName() : null;
    }

    /**
     * Lấy danh sách câu hỏi TEACHER_PRIVATE đang chờ duyệt.
     * GET /api/v1/expert/question-review/pending
     */
    @GetMapping("/pending")
    public ResponseData<List<ExpertReviewService.PendingQuestionDTO>> getPendingQuestions(Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");
        return expertReviewService.getPendingQuestions();
    }

    /**
     * Duyệt câu hỏi: PENDING_REVIEW -> PUBLISHED
     * POST /api/v1/expert/question-review/{questionId}/approve
     */
    @PostMapping("/{questionId}/approve")
    public ResponseData<ExpertReviewService.PendingQuestionDTO> approveQuestion(
            @PathVariable Integer questionId, Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");
        return expertReviewService.approveQuestion(questionId, email);
    }

    /**
     * Từ chối: trả về bản nháp
     * POST /api/v1/expert/question-review/{questionId}/reject
     */
    @PostMapping("/{questionId}/reject")
    public ResponseData<Void> rejectQuestion(
            @PathVariable Integer questionId,
            @RequestParam(defaultValue = "false") boolean delete,
            Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");
        return expertReviewService.rejectQuestion(questionId, email, delete);
    }
}
