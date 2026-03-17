package com.example.DoAn.controller;

import com.example.DoAn.dto.response.ClassDetailResponse;
import com.example.DoAn.service.IClassService;
import com.example.DoAn.service.SettingService;
import com.example.DoAn.service.impl.ClassServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class ClassPublicViewController {

    private final SettingService settingService;
    private final IClassService classService;

    @GetMapping("/classes")
    public String listOpenClasses(@RequestParam(required = false) Integer categoryId, Model model) {
        model.addAttribute("categories", settingService.getCourseCategories());
        model.addAttribute("selectedCat", categoryId);

        return "public/classes";
    }

    @GetMapping("/enroll/{classId}")
    public String enrollPage(@PathVariable Integer classId, Model model) {
        ClassDetailResponse classInfo = classService.getClassById(classId);
        model.addAttribute("classInfo", classInfo);
        return "public/enroll-class";
    }
}