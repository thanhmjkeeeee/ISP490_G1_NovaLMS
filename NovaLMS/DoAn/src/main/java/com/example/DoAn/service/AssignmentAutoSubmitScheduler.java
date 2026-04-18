package com.example.DoAn.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduler to automatically submit assignments when the deadline is reached.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AssignmentAutoSubmitScheduler {

    private final IStudentAssignmentService assignmentService;

    /**
     * Runs every 5 minutes to sweep and auto-submit expired sessions.
     */
    @Scheduled(fixedRate = 300_000) // 5 minutes = 300,000ms
    public void sweepExpiredAssignments() {
        try {
            assignmentService.autoSubmitAllExpired();
        } catch (Exception e) {
            log.error("[AssignmentScheduler] Error during sweep: {}", e.getMessage(), e);
        }
    }
}
