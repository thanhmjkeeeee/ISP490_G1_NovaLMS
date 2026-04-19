package com.example.DoAn.controller;

import com.example.DoAn.model.User;
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
            User user = userRepository.findByEmailWithRole(principal.getName()).orElse(null);
            if (user != null) {
                model.addAttribute("userProfile", user);
                
                // Redirect based on role
                if (user.getRole() != null) {
                    String roleVal = user.getRole().getValue();
                    if ("ROLE_ADMIN".equals(roleVal) || "ADMIN".equals(roleVal)) {
                        return "redirect:/admin/dashboard";
                    } else if ("ROLE_MANAGER".equals(roleVal) || "MANAGER".equals(roleVal)) {
                        return "redirect:/manager/dashboard";
                    } else if ("ROLE_EXPERT".equals(roleVal) || "EXPERT".equals(roleVal)) {
                        return "redirect:/expert/dashboard";
                    }
                }
            }
        }

        model.addAttribute("featuredCourses", homeService.getFeaturedCourses());
        model.addAttribute("teachers", homeService.getFeaturedTeachers());
        model.addAttribute("upcomingClasses", homeService.getUpcomingClasses());
        return "public/index";
    }
}