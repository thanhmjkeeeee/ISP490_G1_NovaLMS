package com.example.DoAn.controller;

import com.example.DoAn.service.CourseService;
import com.example.DoAn.service.SettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class CoursePublicViewController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private SettingService settingService;

    @GetMapping("/courses")
    public String listCoursesPage(@RequestParam(required = false) Integer categoryId, Model model) {
        model.addAttribute("courseList", courseService.getCoursesByFilter(categoryId));
        model.addAttribute("categories", settingService.getCourseCategories());
        model.addAttribute("selectedCat", categoryId);
        return "public/courses"; // Đảm bảo file là templates/public/courses.html
    }

    @GetMapping("/course/details/{id}")
    public String viewCourseDetails(@PathVariable Integer id, Model model) {
        // Trong CoursePublicResponseDTO đã có sẵn studentCount, activeClasses, curriculum
        // Bạn không cần gọi lẻ 3 hàm kia nữa!
        return courseService.getCourseDetail(id)
                .map(courseDto -> {
                    model.addAttribute("course", courseDto);
                    return "public/course-details"; // Kiểm tra tên file là course-detail hay course-details
                })
                .orElse("redirect:/courses");
    }
}