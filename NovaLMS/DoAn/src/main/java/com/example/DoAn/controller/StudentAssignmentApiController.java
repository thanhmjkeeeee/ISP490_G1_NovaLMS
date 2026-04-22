package com.example.DoAn.controller;

import com.example.DoAn.dto.response.AssignmentGradingDetailDTO;
import com.example.DoAn.dto.response.AssignmentInfoDTO;
import com.example.DoAn.dto.response.AssignmentSectionDTO;
import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.service.IStudentAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API for student assignment flow (SPEC 004).
 * Base path: /api/v1/student/assignment
 */
@RestController
@RequestMapping("/api/v1/student/assignment")
@RequiredArgsConstructor
    public class StudentAssignmentApiController {

    private final IStudentAssignmentService assignmentService;

    /**
     * GET /api/v1/student/assignment/{quizId}
     * Returns assignment info, creates/resumes session.
     */
    @GetMapping("/{quizId}")
    public ResponseEntity<ResponseData<AssignmentInfoDTO>> getInfo(
            @PathVariable Integer quizId,
            Authentication auth) {
        try {
            AssignmentInfoDTO info = assignmentService.getAssignmentInfo(quizId, auth.getName());
            return ResponseEntity.ok(ResponseData.success(info));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ResponseData.error(400, e.getMessage()));
        }
    }

    /**
     * GET /api/v1/student/assignment/session/{sessionId}/section/{skill}
     * Returns section data with questions.
     */
    @GetMapping("/session/{sessionId}/section/{skill}")
    public ResponseEntity<ResponseData<AssignmentSectionDTO>> getSection(
            @PathVariable Long sessionId,
            @PathVariable String skill,
            Authentication auth) {
        try {
            AssignmentSectionDTO section = assignmentService.getSection(sessionId, skill, auth.getName());
            return ResponseEntity.ok(ResponseData.success(section));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ResponseData.error(400, e.getMessage()));
        }
    }

    /**
     * PATCH /api/v1/student/assignment/session/{sessionId}/section/{skill}
     * Auto-save answers.
     */
    @PatchMapping("/session/{sessionId}/section/{skill}")
    public ResponseEntity<ResponseData<Boolean>> saveAnswers(
            @PathVariable Long sessionId,
            @PathVariable String skill,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        try {
            @SuppressWarnings("unchecked")
            Map<Integer, Object> answers = (Map<Integer, Object>) body.get("answers");
            assignmentService.saveAnswers(sessionId, skill, answers, auth.getName());
            return ResponseEntity.ok(ResponseData.success(true));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ResponseData.error(400, e.getMessage()));
        }
    }

    /**
     * POST /api/v1/student/assignment/session/{sessionId}/section/{skill}/submit
     * Submit LISTENING/READING/WRITING section.
     */
    @PostMapping("/session/{sessionId}/section/{skill}/submit")
    public ResponseEntity<ResponseData<Map<String, Object>>> submitSection(
            @PathVariable Long sessionId,
            @PathVariable String skill,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        try {
            @SuppressWarnings("unchecked")
            Map<Integer, Object> answers = (Map<Integer, Object>) body.get("answers");
            Map<String, Object> result = assignmentService.submitSection(
                    sessionId, skill, answers, auth.getName());
            return ResponseEntity.ok(ResponseData.success(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ResponseData.error(400, e.getMessage()));
        }
    }

    /**
     * POST /api/v1/student/assignment/session/{sessionId}/section/SPEAKING/submit
     * Submit SPEAKING section with audio URLs.
     */
    @PostMapping("/session/{sessionId}/section/SPEAKING/submit")
    public ResponseEntity<ResponseData<Map<String, Object>>> submitSpeaking(
            @PathVariable Long sessionId,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        try {
            // body: { "questionId": "cloudinary_url", ... }
            @SuppressWarnings("unchecked")
            Map<Integer, String> audioUrls = (Map<Integer, String>) (Map<?, ?>) body;
            Map<String, Object> result = assignmentService.submitSpeakingSection(
                    sessionId, audioUrls, auth.getName());
            return ResponseEntity.ok(ResponseData.success(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ResponseData.error(400, e.getMessage()));
        }
    }

    /**
     * POST /api/v1/student/assignment/session/{sessionId}/complete
     * Final completion (called after WRITING).
     */
    @PostMapping("/session/{sessionId}/complete")
    public ResponseEntity<ResponseData<Integer>> complete(
            @PathVariable Long sessionId,
            Authentication auth) {
        try {
            Integer resultId = assignmentService.completeAssignment(sessionId, auth.getName());
            return ResponseEntity.ok(ResponseData.success(resultId));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ResponseData.error(400, e.getMessage()));
        }
    }

    /**
     * PATCH /api/v1/student/assignment/session/{sessionId}/external-submission
     * Save external link (Google Drive, etc.).
     */
    @PatchMapping("/session/{sessionId}/external-submission")
    public ResponseEntity<ResponseData<Boolean>> saveExternal(
            @PathVariable Long sessionId,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        try {
            String link = body.get("link");
            String note = body.get("note");
            assignmentService.saveExternalSubmission(sessionId, link, note, auth.getName());
            return ResponseEntity.ok(ResponseData.success(true));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ResponseData.error(400, e.getMessage()));
        }
    }

    /**
     * POST /api/v1/student/assignment/session/{sessionId}/auto-submit
     * Timer expired — force submit all remaining sections.
     */
    @PostMapping("/session/{sessionId}/auto-submit")
    public ResponseEntity<ResponseData<Boolean>> autoSubmit(
            @PathVariable Long sessionId,
            Authentication auth) {
        try {
            assignmentService.autoSubmit(sessionId, auth.getName());
            return ResponseEntity.ok(ResponseData.success(true));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ResponseData.error(400, e.getMessage()));
        }
    }

    /**
     * GET /api/v1/student/assignment/result/{resultId}
     * Returns detailed result for a single student assignment.
     */
    @GetMapping("/result/{resultId}")
    public ResponseEntity<ResponseData<AssignmentGradingDetailDTO>> getResultDetail(
            @PathVariable Integer resultId,
            Authentication auth) {
        try {
            AssignmentGradingDetailDTO detail = assignmentService.getAssignmentResultDetail(resultId, auth.getName());
            return ResponseEntity.ok(ResponseData.success(detail));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ResponseData.error(400, e.getMessage()));
        }
    }
}
