package com.example.DoAn.controller;

import com.example.DoAn.dto.response.AssignmentGradingDetailDTO;
import com.example.DoAn.service.ITeacherAssignmentGradingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * Handles teacher assignment grading views (Thymeleaf).
 * Route: /teacher/assignment/grading  →  assignment-grading-list.html
 * Route: /teacher/assignment/grading/{resultId}  →  assignment-grading-detail.html
 */
@Controller
@RequestMapping("/teacher/assignment")
@RequiredArgsConstructor
public class TeacherAssignmentGradingController {

    private final ITeacherAssignmentGradingService gradingService;

    private String getEmailFromPrincipal(Principal principal) {
        if (principal == null) return null;
        if (principal instanceof OAuth2AuthenticationToken token) {
            return token.getPrincipal().getAttribute("email");
        }
        return principal.getName();
    }

    // ─── View Routes ─────────────────────────────────────────────────────────

    @GetMapping("/grading")
    public String gradingList() {
        return "teacher/assignment-grading-list";
    }

    @GetMapping("/grading/{resultId}")
    public String gradingDetail(
            @PathVariable Integer resultId,
            Model model,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        String email = getEmailFromPrincipal(principal);
        if (email == null) return "redirect:/login";

        try {
            AssignmentGradingDetailDTO detail = gradingService.getGradingDetail(resultId, email);
            model.addAttribute("resultId", resultId);
            model.addAttribute("detail", detail);
            return "teacher/assignment-grading-detail";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg",
                    "Không tìm thấy bài nộp hoặc bạn không có quyền chấm điểm.");
            return "redirect:/teacher/assignment/grading";
        }
    }
}
