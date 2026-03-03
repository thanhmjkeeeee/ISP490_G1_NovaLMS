package com.example.DoAn.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    // 1. Trang chủ
    @GetMapping({"/", "/index.html"})
    public String index() {
        return "public/index"; // Trả về templates/index.html
    }

    // 2. Trang Login
    @GetMapping("/login.html")
    public String login() {
        return "auth/login"; // Trả về templates/login.html
    }

//
//    @GetMapping("/courses.html")
//    public String courses() {
//        return "public/courses";
//    }
//
//    @GetMapping("/course-details.html")
//    public String courseDetails() {
//        return "course-details";
//    }

    @GetMapping("/instructors.html")
    public String instructors() {
        return "public/instructors";
    }

    @GetMapping("/instructor-profile.html")
    public String instructorProfile() {
        return "public/instructor-profile";
    }

    @GetMapping("/about.html")
    public String about() {
        return "public/about";
    }

    @GetMapping("/contact.html")
    public String contact() {
        return "public/contact";
    }

    @GetMapping("/pricing.html")
    public String pricing() {
        return "public/pricing";
    }

    @GetMapping("/blog.html")
    public String blog() {
        return "public/blog";
    }

    @GetMapping("/404.html")
    public String notFound() {
        return "public/404";
    }

    // Thêm mapping cho Register nếu bạn có file register.html
    @GetMapping("/register.html")
    public String register() {
        return "auth/register";
    }
    @GetMapping("/reset-password.html")
    public String resetPasswordPage() {
        return "auth/reset-password"; // Trả về templates/reset-password.html
    }

//    @GetMapping("/admin/dashboard")
//    public String admin() {
//        return "admin/dashboard";
//    }

}