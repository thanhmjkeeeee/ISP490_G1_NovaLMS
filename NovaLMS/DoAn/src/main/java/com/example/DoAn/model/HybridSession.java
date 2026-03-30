package com.example.DoAn.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hybrid_session")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HybridSession {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hybrid_session_id")
    private Integer id;

    @Column(name = "guest_session_id", nullable = false)
    private String guestSessionId;

    @Column(name = "guest_name")
    private String guestName;

    @Column(name = "guest_email")
    private String guestEmail;

    @Column(name = "total_quizzes", nullable = false)
    private Integer totalQuizzes;

    @Column(name = "completed_quizzes")
    @Builder.Default
    private Integer completedQuizzes = 0;

    // IN_PROGRESS | COMPLETED | ABANDONED
    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "hybridSession", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("quizOrder ASC")
    @Builder.Default
    private List<HybridSessionQuiz> sessionQuizzes = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
        if (status == null) status = "IN_PROGRESS";
        if (completedQuizzes == null) completedQuizzes = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
