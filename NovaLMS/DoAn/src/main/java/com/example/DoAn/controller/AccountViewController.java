package com.example.DoAn.controller;

import com.example.DoAn.service.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AccountViewController {

    private final SettingService settingService;

    @GetMapping("/accounts")
    public String accountsPage(Model model) {
        model.addAttribute("roleSettings", settingService.getSettingsByType("ROLE", true));
        return "admin/account-list";
    }

    @GetMapping("/list")
    public String listPage(Model model) {
        model.addAttribute("roleSettings", settingService.getSettingsByType("ROLE", true));
        return "admin/account-list";
    }

    @GetMapping("/accounts/add")
    public String addPage() {
        return "admin/account-form";
    }

    @GetMapping("/accounts/detail/{id}")
    public String detailPage(@PathVariable Integer id) {
        return "admin/account-details";
    }
}