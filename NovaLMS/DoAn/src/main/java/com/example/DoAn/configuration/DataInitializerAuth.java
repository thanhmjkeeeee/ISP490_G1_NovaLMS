package com.example.DoAn.configuration;

import com.example.DoAn.model.Role;
import com.example.DoAn.model.User;
import com.example.DoAn.repository.RoleRepository;
import com.example.DoAn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataInitializerAuth {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initAuthData() {
        return args -> {
            // 1. Tạo Roles (Sử dụng Builder thay vì new Role để tránh lỗi constructor)
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseGet(() -> roleRepository.save(
                            Role.builder()
                                    .name("ROLE_ADMIN")
                                    .description("System Administrator")
                                    .build()
                    ));

            Role studentRole = roleRepository.findByName("ROLE_STUDENT")
                    .orElseGet(() -> roleRepository.save(
                            Role.builder()
                                    .name("ROLE_STUDENT")
                                    .description("Learner")
                                    .build()
                    ));

            // 2. Tạo Default Admin
            // Lưu ý: SQL users dùng 'password_hash', nên entity User field là 'passwordHash'
            if (!userRepository.existsByEmail("admin@novalms.edu.vn")) {
                User admin = User.builder()
                        .email("admin@novalms.edu.vn")
                        .passwordHash(passwordEncoder.encode("admin123")) // Sửa .password() thành .passwordHash()
                        .role(adminRole)
                        .status("Active") // SQL yêu cầu status
                        .build();

                userRepository.save(admin);
                System.out.println(">>> Đã khởi tạo tài khoản ADMIN: admin@novalms.edu.vn / admin123");
            }
        };
    }
}