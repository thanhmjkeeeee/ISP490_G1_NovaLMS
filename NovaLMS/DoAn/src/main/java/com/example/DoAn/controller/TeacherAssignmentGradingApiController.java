package com.example.DoAn.controller;

import com.example.DoAn.dto.request.AssignmentGradingRequestDTO;
import com.example.DoAn.dto.response.AssignmentGradingDetailDTO;
import com.example.DoAn.dto.response.AssignmentGradingQueueDTO;
import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.service.ITeacherAssignmentGradingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;

/**
 * REST API for teacher assignment grading.
 * Route: /api/v1/teacher/assignment-results
 */
@RestController
@RequestMapping("/api/v1/teacher/assignment-results")
@RequiredArgsConstructor
public class TeacherAssignmentGradingApiController {

    private final ITeacherAssignmentGradingService gradingService;

    private String getEmailFromPrincipal(Principal principal) {
        if (principal == null) return null;
        if (principal instanceof OAuth2AuthenticationToken token) {
            return token.getPrincipal().getAttribute("email");
        }
        return principal.getName();
    }

    /**
     * GET /api/v1/teacher/assignment-results
     * Returns paginated grading queue filtered by quizId, classId, status.
     */
    @GetMapping
    public ResponseData<Page<AssignmentGradingQueueDTO>> getQueue(
            @RequestParam(required = false) Integer quizId,
            @RequestParam(required = false) Integer classId,
            @RequestParam(required = false) List<String> status,
            @PageableDefault(size = 20) Pageable pageable,
            Principal principal) {

        String email = getEmailFromPrincipal(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");

        // --- ĐÃ FIX: XỬ LÝ LỖI PARSING STATUS ---
        List<String> processedStatus = status;
        if (status == null || status.isEmpty()) {
            // Mặc định: lấy tất cả các bài CHƯA CHẤM XONG
            processedStatus = Arrays.asList("PENDING_SPEAKING", "PENDING_WRITING", "PENDING_BOTH");
        } else if (status.size() == 1 && status.get(0).contains(",")) {
            processedStatus = Arrays.asList(status.get(0).split(","));
        }

        try {
            Page<AssignmentGradingQueueDTO> page = gradingService.getGradingQueue(
                    email, quizId, classId, processedStatus, pageable);
            return ResponseData.success("OK", page);
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    /**
     * GET /api/v1/teacher/assignment-results/{resultId}
     * Returns full grading detail for a single student result.
     */
    @GetMapping("/{resultId}")
    public ResponseData<AssignmentGradingDetailDTO> getDetail(
            @PathVariable Integer resultId,
            Principal principal) {

        String email = getEmailFromPrincipal(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");

        try {
            AssignmentGradingDetailDTO detail = gradingService.getGradingDetail(resultId, email);
            return ResponseData.success("OK", detail);
        } catch (Exception e) {
            return ResponseData.error(404, e.getMessage());
        }
    }

    /**
     * POST /api/v1/teacher/assignment-results/{resultId}/grade
     * Submits grading for an assignment result.
     */
    @PostMapping("/{resultId}/grade")
    public ResponseData<Void> grade(
            @PathVariable Integer resultId,
            @RequestBody AssignmentGradingRequestDTO request,
            Principal principal) {

        String email = getEmailFromPrincipal(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");

        try {
            gradingService.gradeAssignment(resultId, request, email);
            return ResponseData.success("Đã lưu điểm chấm thành công!");
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }
}