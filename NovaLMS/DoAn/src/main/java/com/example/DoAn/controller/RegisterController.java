package com.example.DoAn.controller;


import com.example.DoAn.model.Role;
import com.example.DoAn.model.User;
import com.example.DoAn.repository.RoleRepository;
import com.example.DoAn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class RegisterController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public String registerUser(@RequestParam String email,
                               @RequestParam String password,
                               @RequestParam String confirmPassword,
                               // Nhận các tham số từ form register.html
                               @RequestParam(required = false) String fullName,
                               @RequestParam(required = false) String phone,
                               @RequestParam(required = false) String gender,
                               @RequestParam(required = false) String city,
                               RedirectAttributes redirectAttributes) {

        // 1. Validation cơ bản
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu xác nhận không khớp!");
            return "redirect:/register.html";
        }

        if (userRepository.existsByEmail(email)) {
            redirectAttributes.addFlashAttribute("error", "Email đã tồn tại trong hệ thống!");
            return "redirect:/register.html";
        }

        // 2. Tạo User mới và lưu vào DB
        try {
            Role studentRole = roleRepository.findByName("ROLE_STUDENT")
                    .orElseThrow(() -> new RuntimeException("Error: Role Student not found. Hãy chạy DataInitializer."));

            User newUser = User.builder()
                    .email(email)
                    .passwordHash(passwordEncoder.encode(password))
                    // Map các trường mới vào Entity
                    .fullName(fullName)
                    .phone(phone)
                    .gender(gender)
                    .city(city)
                    // Các trường mặc định
                    .role(studentRole)
                    .status("Active")
                    .authProvider("LOCAL")
                    .build();

            userRepository.save(newUser);

            // Vì ta dùng ddl-auto=update nên khi chạy lệnh save(),
            // Hibernate sẽ tự kiểm tra xem bảng users có cột fullName, phone... chưa.
            // Nếu chưa, nó tự chạy lệnh ALTER TABLE để thêm vào.

            redirectAttributes.addFlashAttribute("success", "Đăng ký thành công! Vui lòng đăng nhập.");
            return "redirect:/login.html";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Lỗi hệ thống: " + e.getMessage());
            return "redirect:/register.html";
        }
    }
}