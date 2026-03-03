package com.example.DoAn.controller;

import com.example.DoAn.dto.CourseDTO;
import com.example.DoAn.service.ICourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/courses")
public class CourseManagementController {

    @Autowired
    private ICourseService courseService;

    @GetMapping({"", "/", "/list"})
    public String viewList(Model model, @PageableDefault(size = 10) Pageable pageable) {
        Page<CourseDTO> coursePage = courseService.getAllCourses(pageable);

        model.addAttribute("courses", coursePage.getContent());
        model.addAttribute("totalPages", coursePage.getTotalPages());
        model.addAttribute("currentPage", pageable.getPageNumber());

        return "admin/course-list";
    }

    // ==> Form create <====
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("course", new CourseDTO());
        return "admin/course-form";
    }

    @PostMapping("/save")
    public String saveCourse(@ModelAttribute("course") CourseDTO courseDTO) {
        courseService.save(courseDTO);
        return "redirect:/admin/courses?success";
    }

    // ==> Detail <==

    @GetMapping("/detail/{id}")
    public String viewDetail(@PathVariable("id") Integer id, Model model) {
        CourseDTO course = courseService.getById(id);
        model.addAttribute("course", course);
        return "admin/course-details-admin";
    }
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id, Model model) {
        CourseDTO course = courseService.getById(id);
        model.addAttribute("course", course);
        return "admin/course-form";
    }
}