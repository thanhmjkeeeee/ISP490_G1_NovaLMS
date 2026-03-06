package com.example.DoAn.controller;

import com.example.DoAn.service.LearningService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
@RequestMapping("/student/lesson")
@RequiredArgsConstructor
public class LessonViewController {

    private final LearningService learningService;

    private String getEmailFromPrincipal(Principal principal) {
        if (principal == null) return null;
        if (principal instanceof OAuth2AuthenticationToken token) {
            return token.getPrincipal().getAttribute("email");
        }
        return principal.getName();
    }

    @GetMapping("/{courseId}/continue")
    public String continueLearning(@PathVariable Integer courseId, Principal principal) {
        String email = getEmailFromPrincipal(principal);
        if (email == null) {
            return "redirect:/login.html";
        }

        Integer nextLessonId = learningService.getLessonIdToContinue(courseId, email);

        if (nextLessonId == null) {
            return "redirect:/student/learn/" + courseId + "?error=empty_course";
        }

        return "redirect:/student/lesson/view/" + nextLessonId;
    }

    @GetMapping("/view/{lessonId}")
    public String viewLesson(@PathVariable Integer lessonId) {
        return "student/lesson-view";
    }
}