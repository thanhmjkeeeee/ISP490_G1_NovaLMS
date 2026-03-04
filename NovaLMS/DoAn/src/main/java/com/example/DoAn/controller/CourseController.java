package com.example.DoAn.controller;

import com.example.DoAn.service.CourseService;
import com.example.DoAn.service.SettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class CourseController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private SettingService settingService;

    @GetMapping("/courses")
    public String listCourses(@RequestParam(required = false) Integer categoryId, Model model) {
        model.addAttribute("courseList", courseService.getCoursesByFilter(categoryId));
        model.addAttribute("categories", settingService.getCourseCategories());
        model.addAttribute("selectedCat", categoryId);
        return "public/courses";
    }

    @GetMapping("/course/details/{id}")
    public String viewCourseDetails(@PathVariable Integer id, Model model) {
        return courseService.getCourseById(id)
                .map(course -> {
                    model.addAttribute("course", course);
                    model.addAttribute("studentCount", courseService.getStudentCount(id)); // Đã thêm lại dòng này
                    model.addAttribute("activeClasses", courseService.getActiveClasses(id));
                    model.addAttribute("curriculum", courseService.getCurriculum(id));
                    return "public/course-details";
                })
                .orElse("redirect:/courses");
    }

    @GetMapping("/courses/filter")
    public String filterCourses(String keyword, Integer categoryId, String sortBy, Model model) {
        model.addAttribute("courseList", courseService.searchAndFilterCourses(keyword, categoryId, sortBy));
        return "public/courses :: courseListFragment";
    }
}