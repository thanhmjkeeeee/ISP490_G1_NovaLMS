package com.example.DoAn.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lesson_quiz_progress",
    uniqueConstraints = @UniqueConstraint(columnNames = {"lesson_id", "user_id", "quiz_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonQuizProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "progress_id")
    private Integer progressId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    // AVAILABLE | LOCKED | COMPLETED
    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "best_score")
    private Double bestScore;

    @Column(name = "best_passed")
    private Boolean bestPassed = false;
}
