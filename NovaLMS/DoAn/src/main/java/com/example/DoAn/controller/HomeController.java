package com.example.DoAn.controller;

import com.example.DoAn.service.HomeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @Autowired
    private HomeService homeService;

    @GetMapping({"/", "/index", "/home"})
    public String index(Model model) {
        model.addAttribute("featuredCourses", homeService.getFeaturedCourses());
        model.addAttribute("teachers", homeService.getFeaturedTeachers());
        return "public/index";
    }
}