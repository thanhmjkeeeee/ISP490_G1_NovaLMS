package com.example.DoAn.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/teacher/quiz")
@RequiredArgsConstructor
public class TeacherQuizGradingController {

    @GetMapping("/grading")
    public String gradingList() {
        return "teacher/quiz-grading-list";
    }

    @GetMapping("/graded")
    public String gradedList() {
        return "teacher/quiz-graded-list";
    }

    /**
     * Redirect /teacher/quiz/grading/{resultId} → workspace grading tab.
     * Trang chi tiết standalone đã bị loại bỏ; toàn bộ chấm điểm thực hiện trong workspace.
     */
    @GetMapping("/grading/{resultId}")
    public String gradingDetail(@PathVariable Integer resultId) {
        return "redirect:/teacher/workspace?tab=grading&resultId=" + resultId;
    }
}
