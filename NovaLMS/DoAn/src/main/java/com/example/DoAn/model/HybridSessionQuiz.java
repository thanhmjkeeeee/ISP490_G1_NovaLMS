package com.example.DoAn.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hybrid_session_quiz")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HybridSessionQuiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hybrid_session_quiz_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hybrid_session_id", nullable = false)
    private HybridSession hybridSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    // Grammar | Vocabulary | Listening | Reading | Writing | Speaking
    @Column(name = "skill", length = 20, nullable = false)
    private String skill;

    // 1-based
    @Column(name = "quiz_order", nullable = false)
    private Integer quizOrder;

    // PENDING | IN_PROGRESS | COMPLETED
    @Column(name = "status", length = 20, nullable = false)
    private String status;

    // Sau khi nộp bài, gắn PlacementTestResult id vào đây
    @Column(name = "placement_test_result_id")
    private Integer placementTestResultId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = "PENDING";
    }
}
