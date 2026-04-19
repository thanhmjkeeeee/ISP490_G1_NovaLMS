package com.example.DoAn.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentGradingDetailDTO {
    private Integer resultId;
    private String status;
    private Long assignmentSessionId;
    private Integer classId;
    private String studentName;
    private String quizTitle;
    private String className;
    private LocalDateTime submittedAt;
    private BigDecimal autoScore;        // LISTENING + READING sum
    private BigDecimal totalScore;       // final total (null if not yet graded)
    private Map<String, BigDecimal> sectionScores;
    private List<SkillSectionDetail> sections;
    
    // External Submission fields
    private Boolean allowExternalSubmission;
    private String externalSubmissionLink;
    private String externalSubmissionNote;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillSectionDetail {
        private String skill;
        private String gradingStatus;  // AUTO | AI_PENDING | AI_READY | GRADED
        private BigDecimal maxScore;
        private List<QuestionGradeItem> questions;
        private String aiScore;
        private String aiFeedback;
        private String aiRubricJson;
        private BigDecimal teacherScore;   // submitted score for this section
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionGradeItem {
        private Integer questionId;
        private String questionType;
        private String content;
        private BigDecimal maxPoints;
        private Object studentAnswer;     // varies by type (JSON string or audio URL)
        private String correctAnswer;     // for MC types
        private Boolean isCorrect;
        private String aiScore;
        private String aiFeedback;
        private BigDecimal teacherScore;  // submitted score for this question
        private String teacherNote;
        private String audioUrl;          // for SPEAKING questions
    }
}
