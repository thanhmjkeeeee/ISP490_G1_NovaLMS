package com.example.DoAn.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "question")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    private Integer questionId;

    // Nullable — câu hỏi trong Question Bank không bắt buộc gắn module
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = true)
    private Module module;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    // MULTIPLE_CHOICE_SINGLE | MULTIPLE_CHOICE_MULTI | FILL_IN_BLANK | MATCHING | WRITING | SPEAKING
    @Column(name = "question_type", length = 30, nullable = false)
    private String questionType;

    // LISTENING | READING | WRITING | SPEAKING
    @Column(name = "skill", length = 20, nullable = false)
    private String skill;

    // A1 | A2 | B1 | B2 | C1 | C2
    @Column(name = "cefr_level", length = 5, nullable = false)
    private String cefrLevel;

    @Column(name = "topic")
    private String topic;

    // Comma-separated tags
    @Column(name = "tags", length = 500)
    private String tags;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "audio_url", length = 500)
    private String audioUrl;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    // DRAFT | PUBLISHED | ARCHIVED
    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AnswerOption> answerOptions;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
        if (status == null) status = "DRAFT";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
