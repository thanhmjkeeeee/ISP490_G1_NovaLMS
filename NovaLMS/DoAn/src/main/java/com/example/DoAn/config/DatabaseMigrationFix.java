package com.example.DoAn.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@Slf4j
public class DatabaseMigrationFix {

    @Bean
    public CommandLineRunner fixRegistrationTableSchema(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                log.info("[MIGRATION] Checking if registration.class_id needs to be nullable...");
                
                // MySQL command to make class_id nullable
                // We use MODIFY to ensure the column type remains INT but allows NULL
                jdbcTemplate.execute("ALTER TABLE registration MODIFY class_id INT NULL;");
                
                log.info("[MIGRATION] registration.class_id is now nullable.");
            } catch (Exception e) {
                log.error("[MIGRATION] Error updating registration table schema: {}", e.getMessage());
                // We don't throw the exception to avoid blocking app startup if the table/column doesn't exist yet
            }
        };
    }
}
