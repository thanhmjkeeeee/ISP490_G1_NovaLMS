package com.example.DoAn.controller;

import com.example.DoAn.model.Clazz;
import com.example.DoAn.model.Course;
import com.example.DoAn.model.User;
import com.example.DoAn.service.ClassService;
import com.example.DoAn.repository.CourseRepository;
import com.example.DoAn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/manager/class")
@RequiredArgsConstructor
public class ManagerClassController {

    private final ClassService classService;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    // View
    @GetMapping("/list")
    public String listClasses(Model model) {
        model.addAttribute("classes", classService.findAll());
        model.addAttribute("activePage", "classes");
        model.addAttribute("isDashboard", true);
        return "manager/class-list";
    }

    // Create
    @GetMapping("/create")
    public String createForm(Model model) {
        Clazz newClazz = new Clazz();
        newClazz.setCourse(new Course());
        newClazz.setTeacher(new User());

        model.addAttribute("clazz", new Clazz());
        model.addAttribute("courses", courseRepository.findAll());
        model.addAttribute("teachers", userRepository.findAll());
        model.addAttribute("activePage", "classes");
        model.addAttribute("isDashboard", true);
        return "manager/class-create";
    }

    // Xem và edit
    @GetMapping("/detail/{id}")
    public String showDetail(@PathVariable Integer id, Model model) {
        Clazz clazz = classService.findById(id);
        if (clazz == null) return "redirect:/manager/class/list";

        model.addAttribute("clazz", clazz);
        model.addAttribute("courses", courseRepository.findAll());
        model.addAttribute("teachers", userRepository.findAll());
        model.addAttribute("activePage", "classes");
        model.addAttribute("isDashboard", true);
        return "manager/class-detail";
    }



    // Save
    @PostMapping("/save")
    public String save(@ModelAttribute("clazz") Clazz clazz) {
        classService.save(clazz);
        return "redirect:/manager/class/list";
    }
}