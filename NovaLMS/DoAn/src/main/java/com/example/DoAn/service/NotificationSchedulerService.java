package com.example.DoAn.service;

import com.example.DoAn.model.*;
import com.example.DoAn.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationSchedulerService {

    private final QuizRepository quizRepository;
    private final RegistrationRepository registrationRepository;
    private final ClassSessionRepository classSessionRepository;
    private final EmailService emailService;
    private final INotificationService notificationService;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Chạy mỗi ngày lúc 08:00 — nhắc deadline quiz/assignment và buổi học sắp tới.
     */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional(readOnly = true)
    public void sendDailyReminders() {
        log.info("[Scheduler] Running daily reminder job");
        try {
            sendQuizDeadlineReminders();
            sendSessionReminders();
        } catch (Exception e) {
            log.error("[Scheduler] Error in daily reminder job: {}", e.getMessage(), e);
        }
    }

    // ─── Quiz / Assignment Deadline Reminders ────────────────────────────────

    private void sendQuizDeadlineReminders() {
        // Lấy quiz đang mở, có deadline trong 24h tới
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow = now.plusDays(1);

        List<Quiz> quizzes = quizRepository.findAll().stream()
                .filter(q -> "PUBLISHED".equals(q.getStatus()))
                .filter(q -> Boolean.TRUE.equals(q.getIsOpen()))
                .filter(q -> q.getDeadline() != null)
                .filter(q -> {
                    LocalDateTime dl = q.getDeadline();
                    return dl.isAfter(now) && dl.isBefore(tomorrow);
                })
                .toList();

        for (Quiz quiz : quizzes) {
            if (quiz.getClazz() == null) continue;
            String quizTitle = quiz.getTitle() != null ? quiz.getTitle() : "";
            String className = quiz.getClazz().getClassName() != null ? quiz.getClazz().getClassName() : "";
            String deadline = quiz.getDeadline().format(DT_FMT);
            boolean isAssignment = "COURSE_ASSIGNMENT".equals(quiz.getQuizCategory())
                    || "MODULE_ASSIGNMENT".equals(quiz.getQuizCategory());

            List<Registration> regs = registrationRepository
                    .findApprovedByClassId(quiz.getClazz().getClassId());

            for (Registration reg : regs) {
                User student = reg.getUser();
                if (student == null) continue;
                String studentName = student.getFullName() != null ? student.getFullName() : "";

                if (student.getEmail() != null && !student.getEmail().isBlank()) {
                    if (isAssignment) {
                        emailService.sendAssignmentDeadlineReminderEmail(
                                student.getEmail(), studentName, quizTitle, className, deadline);
                    } else {
                        emailService.sendQuizDeadlineReminderEmail(
                                student.getEmail(), studentName, quizTitle, className, deadline);
                    }
                }
                if (student.getUserId() != null) {
                    if (isAssignment) {
                        notificationService.sendAssignmentDeadlineReminder(
                                Long.valueOf(student.getUserId()), quizTitle, className, deadline);
                    } else {
                        notificationService.sendQuizDeadlineReminder(
                                Long.valueOf(student.getUserId()), quizTitle, className, deadline);
                    }
                }
            }
        }
    }

    // ─── Session Reminders ──────────────────────────────────────────────────

    private void sendSessionReminders() {
        // Nhắc buổi học sắp tới trong 24h
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow = now.plusDays(1);

        List<ClassSession> upcomingSessions = classSessionRepository.findAll().stream()
                .filter(s -> s.getSessionDate() != null)
                .filter(s -> {
                    LocalDateTime sd = s.getSessionDate();
                    return sd.isAfter(now) && sd.isBefore(tomorrow);
                })
                .toList();

        for (ClassSession session : upcomingSessions) {
            if (session.getClazz() == null) continue;
            Clazz clazz = session.getClazz();

            String className = clazz.getClassName() != null ? clazz.getClassName() : "";
            String sessionTopic = session.getTopic() != null ? session.getTopic() : "";
            String sessionDate = session.getSessionDate() != null
                    ? session.getSessionDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "";
            String sessionTime = session.getStartTime() != null ? session.getStartTime() : "";
            String meetLink = session.getMeetLink() != null && !session.getMeetLink().isBlank()
                    ? session.getMeetLink()
                    : (clazz.getMeetLink() != null ? clazz.getMeetLink() : "");

            List<Registration> regs = registrationRepository
                    .findApprovedByClassId(clazz.getClassId());

            for (Registration reg : regs) {
                User student = reg.getUser();
                if (student == null) continue;
                String studentName = student.getFullName() != null ? student.getFullName() : "";

                if (student.getEmail() != null && !student.getEmail().isBlank()) {
                    emailService.sendSessionReminderEmail(
                            student.getEmail(), studentName, className,
                            sessionTopic, sessionDate, sessionTime, meetLink);
                }
                if (student.getUserId() != null) {
                    notificationService.sendSessionReminder(
                            Long.valueOf(student.getUserId()), className,
                            sessionTopic, sessionDate, meetLink);
                }
            }
        }
    }
}
