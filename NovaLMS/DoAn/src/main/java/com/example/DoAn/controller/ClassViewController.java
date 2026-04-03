package com.example.DoAn.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/manager/class")
public class ClassViewController {

    @GetMapping("/list")
    public String listPage() {
        return "manager/class-list";
    }

    @GetMapping("/create")
    public String createPage() {
        return "manager/class-create";
    }

    @GetMapping("/detail/{id}")
    public String detailPage(@PathVariable Integer id) {
        return "manager/class-create";
    }

    @GetMapping("/edit/{id}")
    public String editPage(@PathVariable Integer id) {
        return "manager/class-create";
    }

    @GetMapping("/students/{id}")
    public String studentsPage(@PathVariable Integer id) {
        return "manager/class-students";
    }
}