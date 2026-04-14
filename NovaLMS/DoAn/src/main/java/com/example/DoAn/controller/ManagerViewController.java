package com.example.DoAn.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/manager")
@RequiredArgsConstructor
public class ManagerViewController {




    @GetMapping("/registrations/pending")
    public String viewPendingRegistrations(Model model) {
        model.addAttribute("pageTitle", "Duyệt Đăng Ký");
        model.addAttribute("isDashboard", true);
        // We reuse the admin registrations view but set the context for manager
        return "admin/registrations";
    }

    @GetMapping("/reports")
    public String viewReports(Model model) {
        model.addAttribute("pageTitle", "Báo Cáo Thống Kê");
        model.addAttribute("isDashboard", true);
        return "manager/reports";
    }
}
