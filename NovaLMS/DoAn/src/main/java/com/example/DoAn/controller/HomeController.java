package com.example.DoAn.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    // Khi người dùng vào trang chủ (http://localhost:8080/)
    @GetMapping("/")
    public String home() {
        // Trả về tên file trong thư mục templates (không cần đuôi .html)
        return "index";
    }
}