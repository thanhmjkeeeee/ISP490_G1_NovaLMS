package com.example.DoAn.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

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



}