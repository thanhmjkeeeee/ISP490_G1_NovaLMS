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
    // THÊM categoryId VÀO ĐÂY ĐỂ HẾT LỖI BIÊN DỊCH
    public String listOpenClasses(@RequestParam(required = false) Integer categoryId, Model model) {

        // Lấy danh sách category để hiển thị lên Dropdown
        model.addAttribute("categories", settingService.getCourseCategories());

        // Gửi categoryId ngược lại View để thẻ <select> giữ được trạng thái đang chọn
        model.addAttribute("selectedCat", categoryId);

        return "public/classes";
    }
}