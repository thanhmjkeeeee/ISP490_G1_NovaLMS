package com.example.DoAn.controller;

import com.example.DoAn.repository.UserRepository;
import com.example.DoAn.service.HomeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @Autowired
    private HomeService homeService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping({"/", "/index", "/home"})
    public String index(Model model, java.security.Principal principal) {
        if (principal != null) {
            userRepository.findByEmail(principal.getName()).ifPresent(user -> {
                model.addAttribute("userProfile", user);
            });
        }

        model.addAttribute("featuredCourses", homeService.getFeaturedCourses());
        model.addAttribute("teachers", homeService.getFeaturedTeachers());
        model.addAttribute("upcomingClasses", homeService.getUpcomingClasses());
        return "public/index";
    }
}