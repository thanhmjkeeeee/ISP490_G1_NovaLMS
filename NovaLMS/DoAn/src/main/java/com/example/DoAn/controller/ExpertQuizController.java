package com.example.DoAn.controller;

import com.example.DoAn.dto.request.QuizQuestionRequestDTO;
import com.example.DoAn.dto.request.QuizRequestDTO;
import com.example.DoAn.dto.response.PageResponse;
import com.example.DoAn.dto.response.QuizResponseDTO;
import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.service.IExpertQuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/expert/quizzes")
@RequiredArgsConstructor
@Tag(name = "Expert - Quiz Management", description = "Quản lý Quiz cho Entry Test và Course")
public class ExpertQuizController {

    private final IExpertQuizService quizService;

    private String getEmail(Principal principal) {
        if (principal instanceof OAuth2AuthenticationToken t) return t.getPrincipal().getAttribute("email");
        return principal.getName();
    }

    // ─── LIST + FILTER ──────────────────────────────────────────────────────

    @Operation(summary = "Lấy danh sách Quiz với filter và phân trang")
    @GetMapping
    public ResponseData<PageResponse<QuizResponseDTO>> getQuizzes(
            @RequestParam(required = false) Integer courseId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseData.success("Danh sách quiz",
                quizService.getQuizzes(courseId, category, status, keyword, page, size));
    }

    // ─── GET BY ID ──────────────────────────────────────────────────────────

    @Operation(summary = "Xem chi tiết Quiz (bao gồm danh sách câu hỏi)")
    @GetMapping("/{quizId}")
    public ResponseData<QuizResponseDTO> getQuizById(@PathVariable Integer quizId) {
        return ResponseData.success("Chi tiết quiz", quizService.getQuizById(quizId));
    }

    // ─── CREATE ─────────────────────────────────────────────────────────────

    @Operation(summary = "Tạo Quiz mới")
    @PostMapping
    public ResponseData<QuizResponseDTO> createQuiz(
            @Valid @RequestBody QuizRequestDTO request, Principal principal) {
        return new ResponseData<>(HttpStatus.CREATED.value(), "Quiz đã được tạo.",
                quizService.createQuiz(request, getEmail(principal)));
    }

    // ─── UPDATE ─────────────────────────────────────────────────────────────

    @Operation(summary = "Cập nhật Quiz config")
    @PutMapping("/{quizId}")
    public ResponseData<QuizResponseDTO> updateQuiz(
            @PathVariable Integer quizId,
            @Valid @RequestBody QuizRequestDTO request, Principal principal) {
        return ResponseData.success("Quiz đã được cập nhật.",
                quizService.updateQuiz(quizId, request, getEmail(principal)));
    }

    // ─── DELETE ─────────────────────────────────────────────────────────────

    @Operation(summary = "Xóa Quiz (chỉ khi chưa có học viên làm)")
    @DeleteMapping("/{quizId}")
    public ResponseData<Void> deleteQuiz(
            @PathVariable Integer quizId, Principal principal) {
        quizService.deleteQuiz(quizId, getEmail(principal));
        return ResponseData.success("Quiz đã được xóa.");
    }

    // ─── CHANGE STATUS ──────────────────────────────────────────────────────

    @Operation(summary = "Đổi trạng thái Quiz (DRAFT → PUBLISHED → ARCHIVED)")
    @PatchMapping("/{quizId}/status")
    public ResponseData<QuizResponseDTO> changeStatus(
            @PathVariable Integer quizId,
            @RequestBody Map<String, String> body,
            Principal principal) {
        String newStatus = body.get("status");
        return ResponseData.success("Trạng thái quiz đã được cập nhật.",
                quizService.changeStatus(quizId, newStatus, getEmail(principal)));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  QUESTION MANAGEMENT WITHIN QUIZ
    // ═══════════════════════════════════════════════════════════════════════

    @Operation(summary = "Thêm câu hỏi vào Quiz")
    @PostMapping("/{quizId}/questions")
    public ResponseData<QuizResponseDTO> addQuestionToQuiz(
            @PathVariable Integer quizId,
            @Valid @RequestBody QuizQuestionRequestDTO request,
            Principal principal) {
        return ResponseData.success("Câu hỏi đã được thêm vào quiz.",
                quizService.addQuestionToQuiz(quizId, request, getEmail(principal)));
    }

    @Operation(summary = "Gỡ câu hỏi khỏi Quiz")
    @DeleteMapping("/{quizId}/questions/{questionId}")
    public ResponseData<QuizResponseDTO> removeQuestionFromQuiz(
            @PathVariable Integer quizId,
            @PathVariable Integer questionId,
            Principal principal) {
        return ResponseData.success("Câu hỏi đã được gỡ khỏi quiz.",
                quizService.removeQuestionFromQuiz(quizId, questionId, getEmail(principal)));
    }

    @Operation(summary = "Gỡ cả bộ câu hỏi (Passage) khỏi Quiz")
    @DeleteMapping("/{quizId}/groups/{groupId}")
    public ResponseData<QuizResponseDTO> removeGroupFromQuiz(
            @PathVariable Integer quizId,
            @PathVariable Integer groupId,
            Principal principal) {
        return ResponseData.success("Bộ câu hỏi đã được gỡ khỏi quiz.",
                quizService.removeGroupFromQuiz(quizId, groupId, getEmail(principal)));
    }

    @Operation(summary = "Sắp xếp lại thứ tự câu hỏi trong Quiz")
    @PutMapping("/{quizId}/questions/reorder")
    public ResponseData<QuizResponseDTO> reorderQuestions(
            @PathVariable Integer quizId,
            @RequestBody List<QuizQuestionRequestDTO> orderedList,
            Principal principal) {
        return ResponseData.success("Thứ tự câu hỏi đã được cập nhật.",
                quizService.reorderQuestions(quizId, orderedList, getEmail(principal)));
    }
}
