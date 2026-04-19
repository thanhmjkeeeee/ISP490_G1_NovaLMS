package com.example.DoAn.controller;

import com.example.DoAn.dto.response.ClassDetailResponse;
import com.example.DoAn.service.IClassService;
import com.example.DoAn.service.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
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
    public String enrollPage(@PathVariable Integer classId, Model model, Authentication authentication) {
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream().noneMatch(a -> "ROLE_ANONYMOUS".equals(a.getAuthority()))
                && authentication.getAuthorities().stream().noneMatch(a -> "ROLE_STUDENT".equals(a.getAuthority()))) {
            return "redirect:/classes";
        }
        ClassDetailResponse classInfo = classService.getClassById(classId);
        model.addAttribute("classInfo", classInfo);
        return "public/enroll-class";
    }
}