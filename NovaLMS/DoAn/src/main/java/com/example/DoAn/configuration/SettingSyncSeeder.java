package com.example.DoAn.configuration;

import com.example.DoAn.model.Setting;
import com.example.DoAn.repository.SettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SettingSyncSeeder {

    private final SettingRepository settingRepository;
    private final JdbcTemplate jdbcTemplate;

    @Bean
    public CommandLineRunner syncSettingsFromQuestions() {
        return args -> {
            try {
                // 1. Sync CEFR_LEVEL
                List<String> cefrLevels = jdbcTemplate.queryForList("SELECT DISTINCT cefr_level FROM question WHERE cefr_level IS NOT NULL", String.class);
                for (String cefr : cefrLevels) {
                    if (settingRepository.findBySettingType("CEFR_LEVEL").stream().noneMatch(s -> s.getValue().equalsIgnoreCase(cefr))) {
                        settingRepository.save(Setting.builder()
                                .settingType("CEFR_LEVEL")
                                .name(cefr.toUpperCase())
                                .value(cefr.toUpperCase())
                                .status("Active")
                                .description("Auto-synced from question table")
                                .orderIndex(0)
                                .build());
                    }
                }

                // 2. Sync SKILL
                List<String> skills = jdbcTemplate.queryForList("SELECT DISTINCT skill FROM question WHERE skill IS NOT NULL", String.class);
                for (String skill : skills) {
                    if (settingRepository.findBySettingType("SKILL").stream().noneMatch(s -> s.getValue().equalsIgnoreCase(skill))) {
                        String name = skill.substring(0, 1).toUpperCase() + skill.substring(1).toLowerCase();
                        settingRepository.save(Setting.builder()
                                .settingType("SKILL")
                                .name(name)
                                .value(skill.toUpperCase())
                                .status("Active")
                                .description("Auto-synced from question table")
                                .orderIndex(0)
                                .build());
                    }
                }

                // 3. Sync QUESTION_TYPE
                List<String> qTypes = jdbcTemplate.queryForList("SELECT DISTINCT question_type FROM question WHERE question_type IS NOT NULL", String.class);
                for (String qType : qTypes) {
                    if (settingRepository.findBySettingType("QUESTION_TYPE").stream().noneMatch(s -> s.getValue().equalsIgnoreCase(qType))) {
                        String name = qType.replace("_", " ");
                        settingRepository.save(Setting.builder()
                                .settingType("QUESTION_TYPE")
                                .name(name)
                                .value(qType.toUpperCase())
                                .status("Active")
                                .description("Auto-synced from question table")
                                .orderIndex(0)
                                .build());
                    }
                }
                
                System.out.println(">>> SettingSyncSeeder completed: synced CEFR, SKILL, QUESTION_TYPE from question table to setting table.");
            } catch (Exception e) {
                System.out.println(">>> SettingSyncSeeder skipped: Question table might not exist yet.");
            }
        };
    }
}
