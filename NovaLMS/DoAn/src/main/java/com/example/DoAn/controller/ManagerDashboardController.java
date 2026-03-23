package com.example.DoAn.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.DoAn.service.ManagerDashboardService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/manager")
@RequiredArgsConstructor
public class ManagerDashboardController {

    private final ManagerDashboardService managerDashboardService;

    @GetMapping("/dashboard")
    public String managerDashboard(Model model) {
        model.addAttribute("pageTitle", "Manager Dashboard");
        return "manager/dashboard";
    }

    @GetMapping("/api/v1/dashboard/data")
    @org.springframework.web.bind.annotation.ResponseBody
    public org.springframework.http.ResponseEntity<?> getDashboardData() {
        try {
            return org.springframework.http.ResponseEntity.ok(managerDashboardService.getDashboardData());
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        }
    }
}