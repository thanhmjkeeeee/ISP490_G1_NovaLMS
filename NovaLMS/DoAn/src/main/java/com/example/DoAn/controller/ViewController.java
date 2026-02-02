package com.example.DoAn.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    // 1. Trang chủ
    @GetMapping({"/", "/index.html"})
    public String index() {
        return "index"; // Trả về templates/index.html
    }

    // 2. Trang Login
    @GetMapping("/login.html")
    public String login() {
        return "login"; // Trả về templates/login.html
    }

    // 3. Các trang khác (Mapping tương tự)
    @GetMapping("/courses.html")
    public String courses() {
        return "courses";
    }

    @GetMapping("/course-details.html")
    public String courseDetails() {
        return "course-details";
    }

    @GetMapping("/instructors.html")
    public String instructors() {
        return "instructors";
    }

    @GetMapping("/instructor-profile.html")
    public String instructorProfile() {
        return "instructor-profile";
    }

    @GetMapping("/about.html")
    public String about() {
        return "about";
    }

    @GetMapping("/contact.html")
    public String contact() {
        return "contact";
    }

    @GetMapping("/pricing.html")
    public String pricing() {
        return "pricing";
    }

    @GetMapping("/blog.html")
    public String blog() {
        return "blog";
    }

    @GetMapping("/404.html")
    public String notFound() {
        return "404";
    }

    // Thêm mapping cho Register nếu bạn có file register.html
    @GetMapping("/register.html")
    public String register() {
        return "register";
    }
    @GetMapping("/reset-password.html")
    public String resetPasswordPage() {
        return "reset-password"; // Trả về templates/reset-password.html
    }
}