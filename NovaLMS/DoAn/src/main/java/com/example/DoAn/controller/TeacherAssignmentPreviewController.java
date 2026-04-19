package com.example.DoAn.controller;

import com.example.DoAn.dto.response.AssignmentInfoDTO;
import com.example.DoAn.service.IStudentAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/teacher/assignment")
@RequiredArgsConstructor
public class TeacherAssignmentPreviewController {

    private final IStudentAssignmentService assignmentService;
    private static final List<String> SKILL_ORDER = List.of("LISTENING", "READING", "SPEAKING", "WRITING");

    @GetMapping("/preview/{quizId}")
    public String previewHome(@PathVariable Integer quizId, Authentication auth, Model model) {
        try {
            AssignmentInfoDTO info = assignmentService.getAssignmentPreviewInfo(quizId, auth.getName());
            model.addAttribute("info", info);
            model.addAttribute("isPreview", true);

            String skill = info.getSkillOrder().get(info.getCurrentSkillIndex());
            return "redirect:/teacher/assignment/preview/session/" + info.getSessionId() + "/section/" + skill;
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error/403";
        }
    }

    @GetMapping("/preview/session/{sessionId}/section/{skill}")
    public String previewSection(@PathVariable Long sessionId, @PathVariable String skill, Model model) {
        model.addAttribute("sessionId", sessionId);
        model.addAttribute("skill", skill);
        model.addAttribute("skillOrder", SKILL_ORDER);
        model.addAttribute("isPreview", true);

        if ("SPEAKING".equals(skill)) {
            return "student/assignment-speaking";
        }
        return "student/assignment-section";
    }
}
