package com.example.DoAn.controller;

import com.example.DoAn.dto.request.QuestionRequestDTO;
import com.example.DoAn.dto.response.QuestionResponseDTO;
import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.service.IExpertQuestionService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/expert/questions")
@RequiredArgsConstructor
public class ExpertQuestionController {

    private final IExpertQuestionService questionService;

    private String getEmail(Principal principal) {
        if (principal instanceof OAuth2AuthenticationToken t) return t.getPrincipal().getAttribute("email");
        return principal.getName();
    }

    @Operation(summary = "Get all questions in a module")
    @GetMapping
    public ResponseData<List<QuestionResponseDTO>> getQuestions(
            @RequestParam Integer moduleId, Principal principal) {
        return ResponseData.success("Danh sách câu hỏi",
                questionService.getQuestionsByModule(moduleId, getEmail(principal)));
    }

    @Operation(summary = "Create a question with answer options")
    @PostMapping
    public ResponseData<QuestionResponseDTO> createQuestion(
            @Valid @RequestBody QuestionRequestDTO request, Principal principal) {
        return new ResponseData<>(HttpStatus.CREATED.value(), "Câu hỏi đã được tạo.",
                questionService.createQuestion(request, getEmail(principal)));
    }

    @Operation(summary = "Update a question and/or its answer options")
    @PutMapping("/{questionId}")
    public ResponseData<QuestionResponseDTO> updateQuestion(
            @PathVariable Integer questionId,
            @Valid @RequestBody QuestionRequestDTO request, Principal principal) {
        return ResponseData.success("Câu hỏi đã được cập nhật.",
                questionService.updateQuestion(questionId, request, getEmail(principal)));
    }

    @Operation(summary = "Delete a question and all its answer options")
    @DeleteMapping("/{questionId}")
    public ResponseData<Void> deleteQuestion(
            @PathVariable Integer questionId, Principal principal) {
        questionService.deleteQuestion(questionId, getEmail(principal));
        return ResponseData.success("Câu hỏi đã được xóa.");
    }
}
