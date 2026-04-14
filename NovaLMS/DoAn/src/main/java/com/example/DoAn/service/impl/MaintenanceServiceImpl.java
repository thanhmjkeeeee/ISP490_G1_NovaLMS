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
    private final EntityManager entityManager;

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
            return ResponseData.success("System Cleanup Successful", summary);

        } catch (Exception e) {
            log.error("Error during system cleanup: ", e);
            return ResponseData.error(500, "Clean up failed: " + e.getMessage());
        }
    }
}
