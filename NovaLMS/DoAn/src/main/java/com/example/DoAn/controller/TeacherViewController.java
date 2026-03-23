package com.example.DoAn.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/teacher")
@RequiredArgsConstructor
public class TeacherViewController {

    @GetMapping("/dashboard")
    public String dashboard() {
        return "teacher/dashboard"; // Trả về template teacher/dashboard.html
    }
}
