package com.example.DoAn.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/expert/questions")
@RequiredArgsConstructor
public class ExpertQuestionGroupWizardController {

    private static final String SESSION_STEP1 = "wizard_step1";
    private static final String SESSION_QUESTIONS = "wizard_step2_questions";

    @GetMapping("/wizard")
    public String wizardPage(HttpSession session, Model model) {
        // Clear stale session data on fresh load
        session.removeAttribute(SESSION_STEP1);
        session.removeAttribute(SESSION_QUESTIONS);
        model.addAttribute("currentStep", 1);
        return "expert/question-group-wizard";
    }
}
