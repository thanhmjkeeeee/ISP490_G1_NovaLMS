package com.example.DoAn.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Entity
@Table(name = "assignment_session",
    uniqueConstraints = @UniqueConstraint(columnNames = {"quiz_id", "user_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // IN_PROGRESS | COMPLETED | EXPIRED
    @Column(nullable = false, length = 20)
    private String status = "IN_PROGRESS";

    // 0=LISTENING, 1=READING, 2=SPEAKING, 3=WRITING
    @Column(name = "current_skill_index", nullable = false)
    private Integer currentSkillIndex = 0;

    // JSON: {"LISTENING": {"1": "a", "2": "cloudinary_url"}, ...}
    @Column(name = "section_answers", columnDefinition = "JSON")
    private String sectionAnswers;

    // JSON: {"LISTENING": "COMPLETED", "READING": "IN_PROGRESS", ...}
    @Column(name = "section_statuses", columnDefinition = "JSON")
    private String sectionStatuses;

    // JSON: {"SPEAKING": "2024-04-01T10:05:00", ...}
    @Column(name = "section_expiry", columnDefinition = "JSON")
    private String sectionExpiry;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "external_submission_link", columnDefinition = "TEXT")
    private String externalSubmissionLink;

    @Column(name = "external_submission_note", columnDefinition = "TEXT")
    private String externalSubmissionNote;

    @PrePersist
    protected void onCreate() {
        if (startedAt == null) startedAt = LocalDateTime.now();
        if (status == null) status = "IN_PROGRESS";
        if (currentSkillIndex == null) currentSkillIndex = 0;
    }
}
