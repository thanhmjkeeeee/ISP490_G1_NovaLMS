package com.example.DoAn.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/courses")
public class CourseViewController {

    @GetMapping("")
    public String viewPage() {
        return "admin/course-list";
    }
    @GetMapping("/new")
    public String createPage() {
        return "admin/course-form";
    }

    @GetMapping("/detail/{id}")
    public String detailPage(@PathVariable Integer id) {
        return "admin/course-details-admin";
    }

    @GetMapping("/edit/{id}")
    public String editPage(@PathVariable Integer id) {
        return "admin/course-form";
    }
}