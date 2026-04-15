package com.example.DoAn.controller;

import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.service.DatabaseSeederService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/debug")
@RequiredArgsConstructor
@Slf4j
public class DebugApiController {

    private final DatabaseSeederService databaseSeederService;

    @GetMapping("/seed-v2")
    public ResponseData<String> seedV2() {
        log.info("Received debug seed request for massive_seed_v2.sql");
        try {
            databaseSeederService.seedFromSqlFile("massive_seed_v2.sql");
            return ResponseData.success("Database seeded successfully from massive_seed_v2.sql", null);
        } catch (Exception e) {
            log.error("Failed to seed database: {}", e.getMessage());
            return ResponseData.error(500, "Seed failed: " + e.getMessage());
        }
    }

    private final com.example.DoAn.repository.UserRepository userRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @GetMapping("/reset-all-passwords")
    public ResponseData<String> resetAllPasswords() {
        List<com.example.DoAn.model.User> users = userRepository.findAll();
        String encodedPassword = passwordEncoder.encode("123456");
        
        for (com.example.DoAn.model.User user : users) {
            user.setPassword(encodedPassword);
            user.setStatus("Active");
        }
        
        userRepository.saveAll(users);
        return ResponseData.success("Successfully reset passwords for " + users.size() + " users to '123456'!", null);
    }
}
