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

    // Sequential assignment: always true for COURSE_ASSIGNMENT / MODULE_ASSIGNMENT
    @Column(name = "is_sequential")
    private Boolean isSequential = false;

    @Column(name = "allow_external_submission")
    private Boolean allowExternalSubmission = false;

    @Column(name = "external_submission_instruction", columnDefinition = "TEXT")
    private String externalSubmissionInstruction;

    // JSON array, e.g. ["LISTENING","READING","SPEAKING","WRITING"]
    @Column(name = "skill_order", columnDefinition = "JSON")
    private String skillOrder;

    // JSON object, e.g. {"SPEAKING": 2, "WRITING": 30}
    @Column(name = "time_limit_per_skill", columnDefinition = "JSON")
    private String timeLimitPerSkill;

    // Thời gian giới hạn ghi âm cho MỖI câu speaking (giây), ví dụ: 120 = 2 phút/câu. null = không giới hạn.
    @Column(name = "speaking_time_limit_seconds")
    private Integer speakingTimeLimitSeconds;

    // Teacher/Giáo viên mở/đóng quiz cho học sinh làm — độc lập với status DRAFT/PUBLISHED/ARCHIVED
    @Column(name = "is_open")
    private Boolean isOpen = false;

    // Thời điểm bắt đầu mở quiz — null = mở ngay khi publish + isOpen=true
    @Column(name = "open_at")
    private LocalDateTime openAt;

    // Thời điểm đóng quiz — null = không đóng tự động (chỉ teacher toggle isOpen)
    @Column(name = "close_at")
    private LocalDateTime closeAt;

    // Deadline cho assignment — sau thời điểm này không thể nộp bài
    @Column(name = "deadline")
    private LocalDateTime deadline;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    private List<QuizQuestion> quizQuestions;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<QuizAssignment> assignments;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<QuizConfig> configs;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<LessonQuizProgress> progresses;

    @OneToMany(mappedBy = "quiz", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private List<ClassSession> sessions;

    @PreRemove
    private void preRemove() {
        if (sessions != null) {
            for (ClassSession s : sessions) {
                s.setQuiz(null);
            }
        }
    }

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

    public QuizCategory getQuizCategoryEnum() {
        return QuizCategory.fromValue(this.quizCategory);
    }

    public Integer getSpeakingTimeLimitSeconds() {
        return speakingTimeLimitSeconds;
    }

    public void setSpeakingTimeLimitSeconds(Integer speakingTimeLimitSeconds) {
        this.speakingTimeLimitSeconds = speakingTimeLimitSeconds;
    }
}
