package com.example.DoAn.config;

import com.example.DoAn.service.MaintenanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataMigrationConfig {

    private final MaintenanceService maintenanceService;

    @Bean
    public CommandLineRunner runMigration() {
        return args -> {
            File flagFile = new File(".migration_to_bands_done");
            if (flagFile.exists()) {
                log.info("[MIGRATION] Migration already performed. Skipping.");
                return;
            }

            log.info("[MIGRATION] Starting automatic migration to IELTS Bands...");
            try {
                var result = maintenanceService.migrateToIELTSBands();
                if (result.getStatus() == 200) {
                    log.info("[MIGRATION] Migration successful: {}", result.getMessage());
                    flagFile.createNewFile();
                } else {
                    log.error("[MIGRATION] Migration failed: {}", result.getMessage());
                }
            } catch (Exception e) {
                log.error("[MIGRATION] Unexpected error during migration", e);
            }
        };
    }
}
