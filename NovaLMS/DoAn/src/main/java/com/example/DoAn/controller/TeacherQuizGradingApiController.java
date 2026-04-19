package com.example.DoAn.controller;

import com.example.DoAn.dto.request.QuestionGradingRequestDTO;
import com.example.DoAn.dto.request.QuizGradingRequestDTO;
import com.example.DoAn.dto.response.PageResponse;
import com.example.DoAn.dto.response.QuizResultDetailDTO;
import com.example.DoAn.dto.response.QuizResultGradedDTO;
import com.example.DoAn.dto.response.QuizResultPendingDTO;
import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.model.QuizAnswer;
import com.example.DoAn.repository.QuizAnswerRepository;
import com.example.DoAn.service.QuizResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/teacher/quiz-results")
@RequiredArgsConstructor
public class TeacherQuizGradingApiController {

    private final QuizResultService quizResultService;
    private final QuizAnswerRepository quizAnswerRepository;

    private String getEmailFromPrincipal(Principal principal) {
        if (principal == null)
            return null;
        if (principal instanceof OAuth2AuthenticationToken token) {
            return token.getPrincipal().getAttribute("email");
        }
        return principal.getName();
    }

    @GetMapping("/pending")
    public ResponseData<PageResponse<QuizResultPendingDTO>> getPendingGradingList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer classId,
            Principal principal) {
        String email = getEmailFromPrincipal(principal);
        if (email == null)
            return ResponseData.error(401, "Vui lòng đăng nhập.");

        try {
            PageResponse<QuizResultPendingDTO> list = quizResultService.getPendingGradingList(email, classId, page,
                    size);
            return ResponseData.success("Thành công", list);
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    @GetMapping("/unlock-requests")
    public ResponseData<PageResponse<QuizResultPendingDTO>> getUnlockRequestsList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) Integer classId,
            Principal principal) {
        String email = getEmailFromPrincipal(principal);
        if (email == null)
            return ResponseData.error(401, "Vui lòng đăng nhập.");

        try {
            PageResponse<QuizResultPendingDTO> list = quizResultService.getUnlockRequests(email, classId, page, size);
            return ResponseData.success("Thành công", list);
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    @GetMapping("/graded")
    public ResponseData<PageResponse<QuizResultGradedDTO>> getGradedResults(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer classId,
            Principal principal) {
        String email = getEmailFromPrincipal(principal);
        if (email == null)
            return ResponseData.error(401, "Vui lòng đăng nhập.");

        try {
            PageResponse<QuizResultGradedDTO> list = quizResultService.getGradedResults(email, classId, page, size);
            return ResponseData.success("Thành công", list);
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
        if (email == null)
            return ResponseData.error(401, "Vui lòng đăng nhập.");

        try {
            quizResultService.gradeQuizResult(resultId, gradingItems, email);
            return ResponseData.success("Đã lưu điểm thành công!");
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    /**
     * POST /api/v1/teacher/quiz-results/{resultId}/grade-extended
     * Extended grading with skillScores + overallNote + teacherNote per question.
     */
    @PostMapping("/{resultId}/grade-extended")
    public ResponseData<Void> gradeQuizResultExtended(
            @PathVariable Integer resultId,
            @RequestBody QuizGradingRequestDTO request,
            Principal principal) {
        String email = getEmailFromPrincipal(principal);
        if (email == null)
            return ResponseData.error(401, "Vui lòng đăng nhập.");

        try {
            quizResultService.gradeQuizResult(resultId, request, email);
            return ResponseData.success("Đã lưu điểm chấm thành công!");
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    /**
     * GET /api/v1/teacher/quiz-results/{resultId}/detail
     * Returns grading detail with skillsPresent list for dynamic tab rendering.
     */
    @GetMapping("/{resultId}/detail")
    public ResponseData<QuizResultDetailDTO> getGradingDetail(
            @PathVariable Integer resultId,
            Principal principal) {
        String email = getEmailFromPrincipal(principal);
        if (email == null)
            return ResponseData.error(401, "Vui lòng đăng nhập.");

        try {
            QuizResultDetailDTO detail = quizResultService.getQuizResult(resultId, email);
            return ResponseData.success("Thành công", detail);
        } catch (Exception e) {
            return ResponseData.error(404, e.getMessage());
        }
    }

    /**
     * POST /api/v1/teacher/quiz-results/override-score
     * Teacher overrides AI score for a specific answer.
     */
    @PostMapping("/override-score")
    public ResponseData<Void> overrideScore(
            @RequestParam Integer answerId,
            @RequestParam String score,
            Principal principal) {
        String email = getEmailFromPrincipal(principal);
        if (email == null)
            return ResponseData.error(401, "Vui lòng đăng nhập.");

        try {
            QuizAnswer answer = quizAnswerRepository.findById(answerId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy câu trả lời."));
            answer.setTeacherOverrideScore(score);
            answer.setAiGradingStatus("REVIEWED");
            quizAnswerRepository.save(answer);

            // Tự động tính lại điểm IELTS cho cả bài
            quizResultService.recalculateQuizResult(answer.getQuizResult().getResultId());

            return ResponseData.success("Đã cập nhật điểm override!");
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    @PostMapping("/{resultId}/unlock")
    public ResponseData<Void> unlockQuizResult(
            @PathVariable Integer resultId,
            Principal principal) {
        String email = getEmailFromPrincipal(principal);
        if (email == null)
            return ResponseData.error(401, "Vui lòng đăng nhập.");

        try {
            quizResultService.unlockQuiz(resultId);
            return ResponseData.success("Đã mở khóa bài làm của học sinh thành công!");
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    @GetMapping("/completion-list")
    public ResponseData<List<Map<String, Object>>> getCompletionList(
            @RequestParam Integer classId,
            @RequestParam Integer quizId,
            Principal principal) {
        String email = getEmailFromPrincipal(principal);
        if (email == null)
            return ResponseData.error(401, "Unauthorized");

        try {
            List<Map<String, Object>> list = quizResultService.getQuizCompletionList(email, classId, quizId);
            return ResponseData.success("Success", list);
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }
}
