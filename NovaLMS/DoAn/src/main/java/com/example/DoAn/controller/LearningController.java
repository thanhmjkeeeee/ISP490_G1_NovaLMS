package com.example.DoAn.controller;

import com.example.DoAn.dto.CourseLearningInfoDTO;
import com.example.DoAn.service.LearningService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
@RequestMapping("/student/learn")
@RequiredArgsConstructor
public class LearningController {

    private final LearningService learningService;

    @GetMapping("/{courseId}")
    public String viewCourseInfo(@PathVariable Long courseId, Model model, Principal principal) {

        String userEmail = principal.getName();


        CourseLearningInfoDTO courseInfo = learningService.getCourseLearningInfo(courseId, userEmail);


        model.addAttribute("course", courseInfo);

        return "student/course-learning-info";
    }
}