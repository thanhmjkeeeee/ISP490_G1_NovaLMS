package com.example.DoAn.controller;

import com.example.DoAn.model.Clazz;
import com.example.DoAn.repository.ClassRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

@Controller
public class ClassController {

    @Autowired
    private ClassRepository classRepository;

    // Endpoint xem chi tiết lớp học
    @GetMapping("/class/details/{id}")
    public String viewClassDetails(@PathVariable("id") Integer id, Model model) {

        // 1. Tìm lớp học trong DB theo ID
        // Lưu ý: Clazz là tên Entity lớp học của bạn (tránh trùng từ khóa class của Java)
        Optional<Clazz> classOptional = classRepository.findById(id);

        // 2. Nếu không tìm thấy lớp học, quay về trang danh sách khóa học
        if (classOptional.isEmpty()) {
            return "redirect:/courses";
        }

        Clazz clazz = classOptional.get();

        // 3. Đẩy dữ liệu lớp học sang HTML
        model.addAttribute("clazz", clazz);

        // 4. Giữ trạng thái Active cho menu "Khóa học" (để có gạch chân trắng)
        model.addAttribute("currentPage", "courses");

        // 5. Trả về view: public/class-details.html
        // (Lưu ý: Dựa theo CourseController của bạn, file html nằm trong thư mục public)
        return "public/class-details";
    }
}