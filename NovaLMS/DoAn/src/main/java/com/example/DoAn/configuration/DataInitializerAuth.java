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
            Setting managerRole = settingRepository.findRoleByValue("ROLE_MANAGER")
                    .orElseGet(() -> settingRepository.save(
                            Setting.builder()
                                    .name("Manager")
                                    .value("ROLE_MANAGER")
                                    .settingType("USER_ROLE")
                                    .status("Active")
                                    .description("Course & Staff Manager")
                                    .orderIndex(4)
                                    .build()
                    ));
            if (!userRepository.existsByEmail("manager@novalms.edu.vn")) {
                User manager = User.builder()
                        .email("manager@novalms.edu.vn")
                        .password(passwordEncoder.encode("manager123"))
                        .fullName("Course Manager")
                        .role(managerRole)
                        .status("Active")
                        .authProvider("LOCAL")
                        .build();
                userRepository.save(manager);
                System.out.println(">>> Đã khởi tạo tài khoản MANAGER: manager@novalms.edu.vn / manager123");
            }

            if (!userRepository.existsByEmail("admin@novalms.edu.vn")) {
                User admin = User.builder()
                        .email("admin@novalms.edu.vn")
                        .password(passwordEncoder.encode("admin123"))
                        .fullName("System Administrator")
                        .role(adminRole)
                        .status("Active")
                        .authProvider("LOCAL")
                        .build();

                userRepository.save(admin);
                System.out.println(">>> Đã khởi tạo tài khoản ADMIN: admin@novalms.edu.vn / admin123");
            }
        };
    }
}