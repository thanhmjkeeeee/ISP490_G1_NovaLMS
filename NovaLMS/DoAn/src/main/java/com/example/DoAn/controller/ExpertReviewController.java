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
        if (email == null) return ResponseData.error(401, "Vui lòng đăng nhập.");
        return expertReviewService.getPendingQuestions();
    }

    /**
     * Duyệt câu hỏi: PENDING_REVIEW -> PUBLISHED
     * POST /api/v1/expert/question-review/{questionId}/approve
     * Body (optional): { "reviewNote": "..." }
     */
    @PostMapping("/{questionId}/approve")
    public ResponseData<ExpertReviewService.PendingQuestionDTO> approveQuestion(
            @PathVariable Integer questionId,
            @RequestBody(required = false) java.util.Map<String, String> body,
            Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Vui lòng đăng nhập.");
        String reviewNote = (body != null) ? body.get("reviewNote") : null;
        return expertReviewService.approveQuestion(questionId, email, reviewNote);
    }

    /**
     * Từ chối: trả về bản nháp hoặc xóa
     * POST /api/v1/expert/question-review/{questionId}/reject
     * Body: { "reviewNote": "...", "delete": false }
     */
    @PostMapping("/{questionId}/reject")
    public ResponseData<Void> rejectQuestion(
            @PathVariable Integer questionId,
            @RequestBody(required = false) java.util.Map<String, Object> body,
            Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Vui lòng đăng nhập.");
        String reviewNote = (body != null && body.get("reviewNote") != null)
                ? String.valueOf(body.get("reviewNote")) : null;
        boolean delete = (body != null && body.get("delete") != null)
                && Boolean.TRUE.equals(body.get("delete"));
        return expertReviewService.rejectQuestion(questionId, email, delete, reviewNote);
    }

    /**
     * Lấy chi tiết một câu hỏi đang chờ duyệt
     * GET /api/v1/expert/question-review/{questionId}
     */
    @GetMapping("/{questionId}")
    public ResponseData<ExpertReviewService.PendingQuestionDTO> getQuestion(
            @PathVariable Integer questionId) {
        return expertReviewService.getQuestionById(questionId);
    }
}
