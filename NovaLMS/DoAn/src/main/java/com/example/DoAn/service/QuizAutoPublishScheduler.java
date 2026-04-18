package com.example.DoAn.service;

import com.example.DoAn.model.*;
import com.example.DoAn.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Scheduler tự động publish quiz khi buổi học bắt đầu.
 *
 * Luồng:
 * 1. Teacher tạo Quiz → Map vào Session → Save as DRAFT
 * 2. Scheduler chạy mỗi 2 phút → Kiểm tra session nào đã bắt đầu mà quiz vẫn DRAFT
 * 3. Tự động set Quiz status = PUBLISHED, isOpen = true, SessionQuiz.isOpen = true
 * 4. Gửi notification + email cho học sinh đã đăng ký lớp
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class QuizAutoPublishScheduler {

    private final SessionQuizRepository sessionQuizRepository;
    private final QuizRepository quizRepository;
    private final TeacherQuizService teacherQuizService;

    /**
     * Chạy mỗi 2 phút để kiểm tra và auto-publish quiz.
     * Cron: giây 0, mỗi 2 phút, mọi giờ, mọi ngày.
     */
    @Scheduled(fixedRate = 120_000) // 2 phút = 120,000ms
    @Transactional
    public void autoPublishQuizzesOnSessionStart() {
        try {
            List<SessionQuiz> draftSessionQuizzes = sessionQuizRepository.findDraftQuizzesWithSessions();

            if (draftSessionQuizzes.isEmpty()) return;

            LocalDateTime now = LocalDateTime.now();
            // Grace window: chỉ auto-publish nếu session bắt đầu trong vòng 1 giờ qua
            // (tránh publish hàng loạt quiz cũ nếu server bị restart)
            LocalDateTime graceCutoff = now.minusHours(1);

            int publishedCount = 0;

            for (SessionQuiz sq : draftSessionQuizzes) {
                ClassSession session = sq.getSession();
                Quiz quiz = sq.getQuiz();

                if (session == null || quiz == null) continue;

                // Tính thời điểm chính xác session bắt đầu
                LocalDateTime sessionStartDateTime = resolveSessionStartTime(session);
                if (sessionStartDateTime == null) continue;

                // Chỉ publish nếu session đã bắt đầu (hoặc đang diễn ra)
                // VÀ không quá xa trong quá khứ (grace window 1 giờ)
                if (sessionStartDateTime.isAfter(now)) {
                    // Session chưa bắt đầu → bỏ qua
                    continue;
                }

                if (sessionStartDateTime.isBefore(graceCutoff)) {
                    // Session đã quá 1 giờ trước → bỏ qua (tránh publish quiz cũ)
                    log.debug("[AutoPublish] Skipping quiz '{}' (id={}) — session started too long ago: {}",
                            quiz.getTitle(), quiz.getQuizId(), sessionStartDateTime);
                    continue;
                }

                // ═══ Auto-Publish ═══
                // 1. Set quiz status → PUBLISHED
                quiz.setStatus("PUBLISHED");
                quiz.setIsOpen(true);
                quizRepository.save(quiz);

                // 2. Set SessionQuiz.isOpen → true
                sq.setIsOpen(true);
                sessionQuizRepository.save(sq);

                // 3. Notify enrolled students
                try {
                    teacherQuizService.notifyStudentsQuizPublished(quiz, true, "PUBLISHED");
                } catch (Exception notifyEx) {
                    log.warn("[AutoPublish] Failed to notify students for quiz '{}': {}",
                            quiz.getTitle(), notifyEx.getMessage());
                }

                publishedCount++;
                log.info("[AutoPublish] ✅ Published quiz '{}' (id={}) for session #{} of class '{}' — session start: {}",
                        quiz.getTitle(),
                        quiz.getQuizId(),
                        session.getSessionNumber(),
                        session.getClazz() != null ? session.getClazz().getClassName() : "N/A",
                        sessionStartDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            }

            if (publishedCount > 0) {
                log.info("[AutoPublish] 🎯 Auto-published {} quiz(zes) this cycle.", publishedCount);
            }

        } catch (Exception e) {
            log.error("[AutoPublish] ❌ Error in auto-publish scheduler: {}", e.getMessage(), e);
        }
    }

    /**
     * Kết hợp sessionDate (LocalDateTime chứa ngày) và startTime (String "HH:mm")
     * thành một LocalDateTime chính xác.
     *
     * Ví dụ: sessionDate = 2026-04-18T00:00:00, startTime = "09:00"
     * → Kết quả: 2026-04-18T09:00:00
     */
    private LocalDateTime resolveSessionStartTime(ClassSession session) {
        if (session.getSessionDate() == null) return null;

        LocalDateTime dateBase = session.getSessionDate();
        String startTimeStr = session.getStartTime();

        if (startTimeStr == null || startTimeStr.isBlank()) {
            // Nếu không có startTime, sử dụng ngày session gốc (00:00)
            return dateBase;
        }

        try {
            // Parse "HH:mm" hoặc "H:mm"
            startTimeStr = startTimeStr.trim();
            LocalTime time;
            if (startTimeStr.contains(":")) {
                String[] parts = startTimeStr.split(":");
                int hour = Integer.parseInt(parts[0]);
                int minute = Integer.parseInt(parts[1]);
                time = LocalTime.of(hour, minute);
            } else {
                time = LocalTime.parse(startTimeStr, DateTimeFormatter.ofPattern("HH:mm"));
            }

            // Kết hợp ngày từ sessionDate với giờ từ startTime
            return dateBase.toLocalDate().atTime(time);
        } catch (Exception e) {
            log.warn("[AutoPublish] Cannot parse startTime '{}' for session {}: {}",
                    startTimeStr, session.getSessionId(), e.getMessage());
            return dateBase;
        }
    }
}
