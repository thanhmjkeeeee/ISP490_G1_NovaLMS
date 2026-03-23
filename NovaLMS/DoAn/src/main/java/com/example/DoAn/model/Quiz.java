package com.example.DoAn.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "quiz")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quiz_id")
    private Integer quizId;

    // Nullable — Entry Test quiz không gắn course
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = true)
    private Course course;

    // Expert tạo quiz
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private Clazz clazz;

    // ENTRY_TEST | COURSE_QUIZ
    @Column(name = "quiz_category", length = 50)
    private String quizCategory;

    @Column(name = "title")
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // DRAFT | PUBLISHED | ARCHIVED
    @Column(name = "status", length = 20)
    private String status;

    // null = không giới hạn thời gian
    @Column(name = "time_limit_minutes")
    private Integer timeLimitMinutes;

    // Điểm đạt (%), ví dụ 70.00
    @Column(name = "pass_score", precision = 5, scale = 2)
    private BigDecimal passScore;

    // null = không giới hạn số lần làm
    @Column(name = "max_attempts")
    private Integer maxAttempts;

    // FIXED | RANDOM
    @Column(name = "question_order", length = 10)
    private String questionOrder;

    // true = hiển thị đáp án ngay sau nộp
    @Column(name = "show_answer_after_submit")
    private Boolean showAnswerAfterSubmit;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    private List<QuizQuestion> quizQuestions;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
        if (status == null) status = "DRAFT";
        if (questionOrder == null) questionOrder = "FIXED";
        if (showAnswerAfterSubmit == null) showAnswerAfterSubmit = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
