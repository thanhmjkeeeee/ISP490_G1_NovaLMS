package com.example.DoAn.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "quiz_answer")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "answer_id")
    private Integer answerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "result_id", referencedColumnName = "result_id")
    private QuizResult quizResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "answered_options", columnDefinition = "TEXT")
    private String answeredOptions;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @lombok.Builder.Default
    @Column(name = "pending_ai_review")
    private Boolean pendingAiReview = false;

    @Column(name = "ai_score")
    private String aiScore; // e.g. "8/10"

    @Column(name = "ai_feedback", columnDefinition = "TEXT")
    private String aiFeedback;

    @Column(name = "ai_rubric_json", columnDefinition = "TEXT")
    private String aiRubricJson;

    // Teacher-assigned score for manual/SPEAKING/WRITING grading
    @Column(name = "points_awarded", precision = 5, scale = 2)
    private java.math.BigDecimal pointsAwarded;

    // Teacher's per-question note
    @Column(name = "teacher_note", columnDefinition = "TEXT")
    private String teacherNote;

    // Cloudinary URL for SPEAKING audio answer
    @Column(name = "audio_url")
    private String audioUrl;

    // AI grading status: PENDING | COMPLETED | REVIEWED
    @Column(name = "ai_grading_status", length = 20)
    private String aiGradingStatus;

    // Teacher override score — if non-null, used instead of AI score
    @Column(name = "teacher_override_score", length = 20)
    private String teacherOverrideScore;

    // IELTS Writing criteria
    @Column(name = "writing_task_achievement", precision = 5, scale = 2)
    private java.math.BigDecimal writingTaskAchievement;

    @Column(name = "writing_coherence_cohesion", precision = 5, scale = 2)
    private java.math.BigDecimal writingCoherenceCohesion;

    @Column(name = "writing_lexical_resource", precision = 5, scale = 2)
    private java.math.BigDecimal writingLexicalResource;

    @Column(name = "writing_grammar_accuracy", precision = 5, scale = 2)
    private java.math.BigDecimal writingGrammarAccuracy;
}
