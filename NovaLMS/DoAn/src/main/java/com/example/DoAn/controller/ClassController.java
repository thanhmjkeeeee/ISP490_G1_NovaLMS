package com.example.DoAn.controller;

import com.example.DoAn.repository.ClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class ClassController {

    private final ClassRepository classRepository;

    @GetMapping("/class/details/{id}")
    public String viewClassDetails(@PathVariable Integer id, Model model) {
        return classRepository.findById(id)
                .map(clazz -> {
                    model.addAttribute("clazz", clazz);
                    model.addAttribute("currentPage", "courses");
                    return "public/class-details";
                })
                .orElse("redirect:/courses");
    }
}