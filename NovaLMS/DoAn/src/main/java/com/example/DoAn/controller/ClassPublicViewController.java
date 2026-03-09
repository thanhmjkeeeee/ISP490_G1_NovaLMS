package com.example.DoAn.controller;

import com.example.DoAn.service.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class ClassPublicViewController {

    private final SettingService settingService;

    @GetMapping("/classes")
    public String listOpenClasses(@RequestParam(required = false) Integer categoryId, Model model) {
        model.addAttribute("categories", settingService.getCourseCategories());
        model.addAttribute("selectedCat", categoryId);

        return "public/classes";
    }
}