package com.example.DoAn.controller;

import com.example.DoAn.dto.CourseLearningInfoDTO;
import com.example.DoAn.model.Lesson;
import com.example.DoAn.service.LearningService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
@RequestMapping("/student/lesson")
@RequiredArgsConstructor
public class LessonController {

    private final LearningService learningService;


    @GetMapping("/{courseId}/continue")
    public String continueLearning(@PathVariable Integer courseId, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        String email = principal.getName();
        Integer nextLessonId = learningService.getLessonIdToContinue(courseId, email);

        if (nextLessonId == null) {
            return "redirect:/student/learn/" + courseId + "?error=empty_course";
        }


        return "redirect:/student/lesson/view/" + nextLessonId;
    }

    @GetMapping("/view/{lessonId}")
    public String viewLesson(@PathVariable Integer lessonId, Model model, Principal principal) {
        String email = principal.getName();

        Lesson currentLesson = learningService.getLessonEntity(lessonId);
        Integer courseId = currentLesson.getModule().getCourse().getCourseId();
        CourseLearningInfoDTO courseInfo = learningService.getCourseLearningInfo(courseId.longValue(), email);

        model.addAttribute("currentLesson", currentLesson);
        model.addAttribute("courseInfo", courseInfo);

        return "student/lesson-view";
    }
}