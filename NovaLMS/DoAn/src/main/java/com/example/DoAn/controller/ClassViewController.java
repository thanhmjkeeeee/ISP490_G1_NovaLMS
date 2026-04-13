package com.example.DoAn.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/manager/class")
public class ClassViewController {

    @GetMapping("/list")
    public String listPage(org.springframework.ui.Model model) {
        model.addAttribute("pageTitle", "Danh Sách Lớp Học");
        model.addAttribute("isDashboard", true);
        return "manager/class-list";
    }

    @GetMapping("/create")
    public String createPage(org.springframework.ui.Model model) {
        model.addAttribute("pageTitle", "Tạo Lớp Học Mới");
        model.addAttribute("isDashboard", true);
        return "manager/class-create";
    }

    @GetMapping("/detail/{id}")
    public String detailPage(@PathVariable Integer id, org.springframework.ui.Model model) {
        model.addAttribute("pageTitle", "Chi Tiết Lớp Học");
        model.addAttribute("isDashboard", true);
        return "manager/class-create"; // Wait, is this correct? Re-check later.
    }

    @GetMapping("/edit/{id}")
    public String editPage(@PathVariable Integer id, org.springframework.ui.Model model) {
        model.addAttribute("pageTitle", "Chỉnh Sửa Lớp Học");
        model.addAttribute("isDashboard", true);
        return "manager/class-create";
    }

    @GetMapping("/students/{id}")
    public String studentsPage(@PathVariable Integer id, org.springframework.ui.Model model) {
        model.addAttribute("pageTitle", "Danh Sách Học Viên");
        model.addAttribute("isDashboard", true);
        return "manager/class-students";
    }
}