package com.example.DoAn.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class QuestionResultDTO {
    private Integer questionId;
    private String content;
    private String questionType;
    private String skill;
    private String cefrLevel;
    private Double points;
    private Boolean isCorrect;
    private String userAnswerDisplay;
    private String correctAnswerDisplay;
    private String explanation;
    private String imageUrl;
    private String audioUrl;
    private List<AnswerOptionDTO> options;
    // ── Grading fields ────────────────────────────────────────────────────
    private Integer answerId;         // QuizAnswer answerId — used for override API
    private Double pointsAwarded;  // teacher-assigned score for this question
    private String teacherNote;       // teacher's per-question note
    private String aiScore;           // e.g. "8/10" — from AI grading
    private String aiFeedback;        // AI verbal feedback
    private String aiRubricJson;      // AI rubric breakdown as JSON string
    private String studentAudioUrl;   // student's SPEAKING answer audio URL (Cloudinary)
    private String aiGradingStatus;  // PENDING | COMPLETED | REVIEWED
    private String teacherOverrideScore; // override AI score

    // Writing criteria breakdown
    private java.math.BigDecimal writingTaskAchievement;
    private java.math.BigDecimal writingCoherenceCohesion;
    private java.math.BigDecimal writingLexicalResource;
    private java.math.BigDecimal writingGrammarAccuracy;
}
