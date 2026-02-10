package com.example.DoAn.configuration;

import com.example.DoAn.model.Setting;
import com.example.DoAn.model.User;
import com.example.DoAn.repository.SettingRepository;
import com.example.DoAn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataInitializerAuth {

    private final SettingRepository settingRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initAuthData() {
        return args -> {
            // 1. Tạo Roles (Là Setting với type = 'USER_ROLE')
            // Role Admin
            Setting adminRole = settingRepository.findRoleByValue("ROLE_ADMIN")
                    .orElseGet(() -> settingRepository.save(
                            Setting.builder()
                                    .name("Admin")
                                    .value("ROLE_ADMIN")
                                    .settingType("USER_ROLE")
                                    .status("Active")
                                    .description("System Administrator")
                                    .orderIndex(1)
                                    .build()
                    ));

            // Role Student
            Setting studentRole = settingRepository.findRoleByValue("ROLE_STUDENT")
                    .orElseGet(() -> settingRepository.save(
                            Setting.builder()
                                    .name("Student")
                                    .value("ROLE_STUDENT")
                                    .settingType("USER_ROLE")
                                    .status("Active")
                                    .description("Learner")
                                    .orderIndex(2)
                                    .build()
                    ));

            // Role Teacher (Thêm nếu cần)
            Setting teacherRole = settingRepository.findRoleByValue("ROLE_TEACHER")
                    .orElseGet(() -> settingRepository.save(
                            Setting.builder()
                                    .name("Teacher")
                                    .value("ROLE_TEACHER")
                                    .settingType("USER_ROLE")
                                    .status("Active")
                                    .description("Instructor")
                                    .orderIndex(3)
                                    .build()
                    ));

            // 2. Tạo Default Admin
            if (!userRepository.existsByEmail("admin@novalms.edu.vn")) {
                User admin = User.builder()
                        .email("admin@novalms.edu.vn")
                        .password(passwordEncoder.encode("admin123")) // DB mới dùng 'password'
                        .fullName("System Administrator")
                        .role(adminRole) // Role là Setting
                        .status("Active")
                        .authProvider("LOCAL") // Mặc định là LOCAL
                        .build();

                userRepository.save(admin);
                System.out.println(">>> Đã khởi tạo tài khoản ADMIN: admin@novalms.edu.vn / admin123");
            }
        };
    }
}