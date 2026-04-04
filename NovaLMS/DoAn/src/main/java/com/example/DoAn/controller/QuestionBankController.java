package com.example.DoAn.controller;

import com.example.DoAn.dto.request.QuestionBankRequestDTO;
import com.example.DoAn.dto.response.PageResponse;
import com.example.DoAn.dto.response.QuestionBankItemDTO;
import com.example.DoAn.dto.response.QuestionBankResponseDTO;
import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.service.IQuestionBankService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/expert/question-bank")
@RequiredArgsConstructor
@Tag(name = "Expert - Question Bank", description = "Quản lý câu hỏi trong Master Question Bank")
public class QuestionBankController {

    private final IQuestionBankService questionBankService;

    private String getEmail(Principal principal) {
        if (principal instanceof OAuth2AuthenticationToken t) return t.getPrincipal().getAttribute("email");
        return principal.getName();
    }

    // ─── LIST + FILTER ──────────────────────────────────────────────────────

    @Operation(summary = "Lấy danh sách câu hỏi với filter và phân trang")
    @GetMapping
    public ResponseData<PageResponse<QuestionBankItemDTO>> getQuestions(
            @RequestParam(required = false) String skill,
            @RequestParam(required = false) String cefrLevel,
            @RequestParam(required = false) String questionType,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseData.success("Danh sách câu hỏi",
                questionBankService.getQuestions(skill, cefrLevel, questionType, topic, status, keyword, page, size));
    }

    // ─── GET BY ID ──────────────────────────────────────────────────────────

    @Operation(summary = "Xem chi tiết một câu hỏi")
    @GetMapping("/{questionId}")
    public ResponseData<QuestionBankResponseDTO> getQuestionById(@PathVariable Integer questionId) {
        return ResponseData.success("Chi tiết câu hỏi", questionBankService.getQuestionById(questionId));
    }

    // ─── CREATE ─────────────────────────────────────────────────────────────

    @Operation(summary = "Tạo câu hỏi mới trong Question Bank")
    @PostMapping
    public ResponseData<QuestionBankResponseDTO> createQuestion(
            @Valid @RequestBody QuestionBankRequestDTO request, Principal principal) {
        return new ResponseData<>(HttpStatus.CREATED.value(), "Câu hỏi đã được tạo.",
                questionBankService.createQuestion(request, getEmail(principal)));
    }

    // ─── UPDATE ─────────────────────────────────────────────────────────────

    @Operation(summary = "Cập nhật câu hỏi")
    @PutMapping("/{questionId}")
    public ResponseData<QuestionBankResponseDTO> updateQuestion(
            @PathVariable Integer questionId,
            @Valid @RequestBody QuestionBankRequestDTO request, Principal principal) {
        return ResponseData.success("Câu hỏi đã được cập nhật.",
                questionBankService.updateQuestion(questionId, request, getEmail(principal)));
    }

    // ─── DELETE ─────────────────────────────────────────────────────────────

    @Operation(summary = "Xóa câu hỏi (chỉ khi chưa sử dụng trong quiz)")
    @DeleteMapping("/{questionId}")
    public ResponseData<Void> deleteQuestion(
            @PathVariable Integer questionId, Principal principal) {
        questionBankService.deleteQuestion(questionId, getEmail(principal));
        return ResponseData.success("Câu hỏi đã được xóa.");
    }

    // ─── CHANGE STATUS ──────────────────────────────────────────────────────

    @Operation(summary = "Đổi trạng thái câu hỏi (DRAFT → PUBLISHED → ARCHIVED)")
    @PatchMapping("/{questionId}/status")
    public ResponseData<QuestionBankResponseDTO> changeStatus(
            @PathVariable Integer questionId,
            @RequestBody Map<String, String> body,
            Principal principal) {
        String newStatus = body.get("status");
        String type = body.get("type"); // SINGLE or GROUP
        return ResponseData.success("Trạng thái đã được cập nhật.",
                questionBankService.changeStatus(questionId, type, newStatus, getEmail(principal)));
    }
}
