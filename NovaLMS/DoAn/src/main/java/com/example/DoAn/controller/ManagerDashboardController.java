package com.example.DoAn.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/manager")
public class ManagerDashboardController {

    @GetMapping("/dashboard")
    public String managerDashboard(Model model) {
        model.addAttribute("pageTitle", "Manager Dashboard");
        return "manager/dashboard";
    }
}