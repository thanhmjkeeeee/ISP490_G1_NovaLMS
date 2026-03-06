package com.example.DoAn.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthViewController {

    @GetMapping("/login.html")
    public String loginPage() {
        return "auth/Login";
    }

    @GetMapping("/register.html")
    public String registerPage() { return "auth/register"; }

    @GetMapping("/reset-password.html")
    public String resetPasswordPage() { return "auth/reset-password"; }
}