package com.example.DoAn.service.impl;

import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.service.MaintenanceService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaintenanceServiceImpl implements MaintenanceService {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public ResponseData<Map<String, Long>> performCleanup() {
        Map<String, Long> summary = new HashMap<>();

        try {
            // 1. Cleanup Stale Quiz Results (IN_PROGRESS > 7 days)
            // Note: We delete QuizAnswers first via Cascade or manual if not cascading
            // Our model has CascadeType.ALL on QuizAnswers in QuizResult
            int deletedResults = entityManager.createNativeQuery(
                "DELETE FROM quiz_result WHERE status = 'IN_PROGRESS' AND started_at < DATE_SUB(NOW(), INTERVAL 7 DAY)"
            ).executeUpdate();
            summary.put("staleQuizResultsDeleted", (long) deletedResults);

            // 2. Cleanup Old Notifications (> 30 days)
            int deletedNotifications = entityManager.createNativeQuery(
                "DELETE FROM notification WHERE created_at < DATE_SUB(NOW(), INTERVAL 30 DAY)"
            ).executeUpdate();
            summary.put("oldNotificationsDeleted", (long) deletedNotifications);

            // 3. De-duplicate Questions (Keep newest, delete others with same content and skill)
            // This is a bit more complex. 
            // We use a temporary table strategy for MySQL safety
            entityManager.createNativeQuery("CREATE TEMPORARY TABLE temp_questions_to_keep AS " +
                "SELECT MAX(question_id) as keep_id " +
                "FROM question " +
                "GROUP BY content, skill, cefr_level").executeUpdate();

            int deletedDuplicates = entityManager.createNativeQuery(
                "DELETE q FROM question q " +
                "LEFT JOIN temp_questions_to_keep t ON q.question_id = t.keep_id " +
                "WHERE t.keep_id IS NULL AND q.source = 'EXPERT_BANK'"
            ).executeUpdate();
            
            entityManager.createNativeQuery("DROP TEMPORARY TABLE temp_questions_to_keep").executeUpdate();
            summary.put("duplicateQuestionsDeleted", (long) deletedDuplicates);

            log.info("System maintenance cleanup completed: {}", summary);
            return ResponseData.success("Dọn dẹp hệ thống thành công", summary);
        } catch (Exception e) {
            log.error("Maintenance cleanup failed: ", e);
            return ResponseData.error(500, "Lỗi khi dọn dẹp hệ thống: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ResponseData<Void> migrateToIELTSBands() {
        try {
            // 1. Update Settings table
            entityManager.createNativeQuery("DELETE FROM setting WHERE setting_type = 'CEFR_LEVEL'").executeUpdate();
            String[] bands = {"3.0", "3.5", "4.0", "4.5", "5.0", "5.5", "6.0", "6.5", "7.0", "7.5", "8.0", "8.5", "9.0"};
            for (int i = 0; i < bands.length; i++) {
                entityManager.createNativeQuery(
                    "INSERT INTO setting (name, value, setting_type, order_index, status) VALUES (?, ?, 'CEFR_LEVEL', ?, 'ACTIVE')"
                )
                .setParameter(1, bands[i])
                .setParameter(2, bands[i])
                .setParameter(3, i + 1)
                .executeUpdate();
            }

            // 2. Map existing A1-C2 to Bands
            Map<String, String> mapping = Map.of(
                "A1", "3.0",
                "A2", "4.0",
                "B1", "5.0",
                "B2", "6.0",
                "C1", "7.0",
                "C2", "8.0"
            );

            for (Map.Entry<String, String> entry : mapping.entrySet()) {
                entityManager.createNativeQuery("UPDATE question SET cefr_level = ? WHERE cefr_level = ?")
                        .setParameter(1, entry.getValue())
                        .setParameter(2, entry.getKey())
                        .executeUpdate();
                entityManager.createNativeQuery("UPDATE question_group SET cefr_level = ? WHERE cefr_level = ?")
                        .setParameter(1, entry.getValue())
                        .setParameter(2, entry.getKey())
                        .executeUpdate();
            }

            log.info("Migration to IELTS Bands completed successfully.");
            return ResponseData.success("Di chuyển sang IELTS Bands thành công");
        } catch (Exception e) {
            log.error("Migration failed: ", e);
            return ResponseData.error(500, "Lỗi khi di chuyển dữ liệu: " + e.getMessage());
        }
    }
}
