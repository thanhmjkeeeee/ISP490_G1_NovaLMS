package com.example.DoAn.controller;

import com.example.DoAn.service.DatabaseSeederService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/debug")
@RequiredArgsConstructor
public class DatabaseSeederController {

    private final DatabaseSeederService databaseSeederService;

    @PostMapping("/seed-database")
    public ResponseEntity<Map<String, Long>> seedDatabase() {
        Map<String, Long> result = databaseSeederService.seed();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/seed-v2")
    public ResponseEntity<String> seedDatabaseV2() {
        databaseSeederService.seedFromSqlFile("massive_seed_v2.sql");
        return ResponseEntity.ok("Database seeded successfully from massive_seed_v2.sql");
    }
}
