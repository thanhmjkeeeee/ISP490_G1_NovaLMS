package com.example.DoAn.controller;

import com.example.DoAn.dto.response.AssignmentInfoDTO;
import com.example.DoAn.service.IStudentAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * View routes for student assignment flow (SPEC 004).
 * Routes: /student/assignment/{quizId}, /student/assignment/session/{sessionId}/section/{skill}, /student/assignment/complete/{resultId}
 */
@Controller
@RequiredArgsConstructor
public class StudentAssignmentController {

    private final IStudentAssignmentService assignmentService;

    private static final List<String> SKILL_ORDER = List.of("LISTENING", "READING", "SPEAKING", "WRITING");

    /**
     * Entry point — validates access and redirects to current section.
     * GET /student/assignment/{quizId}
     */
    @GetMapping("/student/assignment/{quizId}")
    public String assignmentHome(
            @PathVariable Integer quizId,
            Authentication auth,
            Model model) {
        try {
            AssignmentInfoDTO info = assignmentService.getAssignmentInfo(quizId, auth.getName());
            model.addAttribute("info", info);

            if (Boolean.TRUE.equals(info.getAttemptsExceeded())) {
                return "student/assignment-expired";
            }

            // Determine redirect target
            String skill = info.getSkillOrder().get(info.getCurrentSkillIndex());
            return "redirect:/student/assignment/session/" + info.getSessionId() + "/section/" + skill;

        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error/403";
        }
    }

    /**
     * Section page — delegates to either assignment-section or assignment-speaking.
     * GET /student/assignment/session/{sessionId}/section/{skill}
     */
    @GetMapping("/student/assignment/session/{sessionId}/section/{skill}")
    public String sectionPage(
            @PathVariable Long sessionId,
            @PathVariable String skill,
            Authentication auth,
            Model model) {
        model.addAttribute("sessionId", sessionId);
        model.addAttribute("skill", skill);
        model.addAttribute("skillOrder", SKILL_ORDER);

        if ("SPEAKING".equals(skill)) {
            return "student/assignment-speaking";
        }
        return "student/assignment-section";
    }

    /**
     * Completion page.
     * GET /student/assignment/complete/{resultId}
     */
    @GetMapping("/student/assignment/complete/{resultId}")
    public String completePage(
            @PathVariable Integer resultId,
            Authentication auth,
            Model model) {
        model.addAttribute("resultId", resultId);
        try {
            var detail = assignmentService.getAssignmentResultDetail(resultId, auth.getName());
            model.addAttribute("classId", detail.getClassId());
        } catch (Exception e) {
            // fallback
        }
        return "student/assignment-complete";
    }

    /**
     * Result detail page.
     * GET /student/assignment/result/{resultId}
     */
    @GetMapping("/student/assignment/result/{resultId}")
    public String showResultDetailPage(
            @PathVariable Integer resultId,
            Authentication auth,
            Model model) {
        model.addAttribute("resultId", resultId);
        return "student/assignment-result";
    }
}
