package com.example.DoAn.controller;

import com.example.DoAn.dto.response.AssignmentSectionDTO;
import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.service.IStudentAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/teacher/assignment/preview")
@RequiredArgsConstructor
public class TeacherAssignmentPreviewApiController {

    private final IStudentAssignmentService assignmentService;

    @GetMapping("/session/{sessionId}/section/{skill}")
    public ResponseData<AssignmentSectionDTO> getSection(
            @PathVariable Long sessionId,
            @PathVariable String skill,
            Authentication auth) {
        try {
            AssignmentSectionDTO section = assignmentService.getSection(sessionId, skill, auth.getName());
            section.setIsPreview(true);
            return ResponseData.success(section);
        } catch (Exception e) {
            return ResponseData.error(400, e.getMessage());
        }
    }

    @PatchMapping("/session/{sessionId}/section/{skill}")
    public ResponseData<Boolean> saveAnswers(
            @PathVariable Long sessionId,
            @PathVariable String skill,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        try {
            // In preview, we still allow saving to the teacher's preview session
            @SuppressWarnings("unchecked")
            Map<Integer, Object> answers = (Map<Integer, Object>) body.get("answers");
            assignmentService.saveAnswers(sessionId, skill, answers, auth.getName());
            return ResponseData.success(true);
        } catch (Exception e) {
            return ResponseData.error(400, e.getMessage());
        }
    }

    @PostMapping("/session/{sessionId}/section/{skill}/submit")
    public ResponseData<Map<String, Object>> submitSection(
            @PathVariable Long sessionId,
            @PathVariable String skill,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        try {
            @SuppressWarnings("unchecked")
            Map<Integer, Object> answers = (Map<Integer, Object>) body.get("answers");
            Map<String, Object> result = assignmentService.submitSection(
                    sessionId, skill, answers, auth.getName());
            
            // In preview, we might want to redirect back or show a message
            // But for now, just let it proceed like a normal assignment
            return ResponseData.success(result);
        } catch (Exception e) {
            return ResponseData.error(400, e.getMessage());
        }
    }

    @PostMapping("/session/{sessionId}/complete")
    public ResponseData<Integer> complete(
            @PathVariable Long sessionId,
            Authentication auth) {
        try {
            Integer resultId = assignmentService.completeAssignment(sessionId, auth.getName());
            return ResponseData.success(resultId);
        } catch (Exception e) {
            return ResponseData.error(400, e.getMessage());
        }
    }
}
