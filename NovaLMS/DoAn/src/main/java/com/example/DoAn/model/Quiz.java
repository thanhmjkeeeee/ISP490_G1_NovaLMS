package com.example.DoAn.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.example.DoAn.model.Module;
import com.example.DoAn.model.Lesson;

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

    // Module-level quiz (Expert creates)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = true)
    private Module module;

    // Lesson-level quiz (Teacher creates)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = true)
    private Lesson lesson;

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

    // Target number of questions for the quiz
    @Column(name = "number_of_questions")
    private Integer numberOfQuestions;

    // true = hiển thị đáp án ngay sau nộp
    @Column(name = "show_answer_after_submit")
    private Boolean showAnswerAfterSubmit;

    // true = quiz có thể dùng trong Hybrid Placement Test
    @Column(name = "is_hybrid_enabled")
    @Builder.Default
    private Boolean isHybridEnabled = false;

    /**
     * Kỹ năng đích cho quiz ENTRY_TEST hybrid.
     * Nullable — chỉ dùng khi isHybridEnabled = true.
     * Giá trị: Grammar | Vocabulary | Listening | Reading | Writing | Speaking
     */
    @Column(name = "target_skill", length = 20)
    private String targetSkill;

    // Teacher/Giáo viên mở/đóng quiz cho học sinh làm — độc lập với status DRAFT/PUBLISHED/ARCHIVED
    @Column(name = "is_open")
    private Boolean isOpen = false;

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
        if (showAnswerAfterSubmit == null) showAnswerAfterSubmit = true;
        if (isOpen == null) isOpen = false;
        if (isHybridEnabled == null) isHybridEnabled = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
