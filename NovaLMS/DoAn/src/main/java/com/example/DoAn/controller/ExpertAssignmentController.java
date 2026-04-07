package com.example.DoAn.controller;

import com.example.DoAn.dto.request.AssignmentQuestionRequestDTO;
import com.example.DoAn.dto.request.AssignmentPublishDTO;
import com.example.DoAn.dto.request.QuizRequestDTO;
import com.example.DoAn.dto.response.AssignmentPreviewDTO;
import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.dto.response.SkillSectionSummaryDTO;
import com.example.DoAn.exception.InvalidDataException;
import com.example.DoAn.model.Quiz;
import com.example.DoAn.model.QuizCategory;
import com.example.DoAn.service.IExpertAssignmentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/expert/assignments")
@RequiredArgsConstructor
public class ExpertAssignmentController {

    private final IExpertAssignmentService assignmentService;

    @PostMapping
    public ResponseEntity<ResponseData<Integer>> create(
            @Valid @RequestBody QuizRequestDTO dto,
            Authentication auth) throws JsonProcessingException {
        Quiz quiz = assignmentService.createAssignment(dto, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ResponseData.success(quiz.getQuizId()));
    }

    @GetMapping("/{quizId}")
    public ResponseEntity<ResponseData<Quiz>> get(@PathVariable Integer quizId, Authentication auth) {
        Quiz quiz = assignmentService.getAssignment(quizId, auth.getName());
        return ResponseEntity.ok(ResponseData.success(quiz));
    }

    @GetMapping("/{quizId}/skills")
    public ResponseEntity<ResponseData<Map<String, SkillSectionSummaryDTO>>> getSkills(
            @PathVariable Integer quizId) {
        Map<String, SkillSectionSummaryDTO> summaries = assignmentService.getSkillSummaries(quizId);
        return ResponseEntity.ok(ResponseData.success(summaries));
    }

    @PostMapping("/{quizId}/questions")
    public ResponseEntity<ResponseData<Integer>> addQuestions(
            @PathVariable Integer quizId,
            @Valid @RequestBody AssignmentQuestionRequestDTO dto,
            Authentication auth) {
        assignmentService.addQuestionsToSection(quizId, dto, auth.getName());
        return ResponseEntity.ok(ResponseData.success(dto.getQuestionIds().size()));
    }

    @DeleteMapping("/{quizId}/questions/{questionId}")
    public ResponseEntity<ResponseData<Boolean>> removeQuestion(
            @PathVariable Integer quizId,
            @PathVariable Integer questionId,
            Authentication auth) {
        assignmentService.removeQuestion(quizId, questionId);
        return ResponseEntity.ok(ResponseData.success(true));
    }

    @GetMapping("/{quizId}/preview")
    public ResponseEntity<ResponseData<AssignmentPreviewDTO>> preview(@PathVariable Integer quizId) {
        AssignmentPreviewDTO preview = assignmentService.getPreview(quizId);
        return ResponseEntity.ok(ResponseData.success(preview));
    }

    @PatchMapping("/{quizId}/publish")
    public ResponseEntity<ResponseData<Boolean>> publish(@PathVariable Integer quizId, Authentication auth) {
        try {
            assignmentService.publishAssignment(quizId);
            return ResponseEntity.ok(ResponseData.success(true));
        } catch (InvalidDataException e) {
            return ResponseEntity.badRequest()
                .body(ResponseData.error(400, e.getMessage()));
        }
    }

    @PatchMapping("/{quizId}/status")
    public ResponseEntity<ResponseData<Boolean>> changeStatus(
            @PathVariable Integer quizId,
            @RequestBody AssignmentPublishDTO dto) {
        assignmentService.changeStatus(quizId, dto.getStatus());
        return ResponseEntity.ok(ResponseData.success(true));
    }

    @GetMapping
    public ResponseEntity<ResponseData<List<Quiz>>> list(Authentication auth) {
        List<Quiz> assignments = assignmentService.getAssignments(auth.getName());
        return ResponseEntity.ok(ResponseData.success(assignments));
    }
}
