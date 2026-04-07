package com.example.DoAn.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentGradingQueueDTO {
    private Integer resultId;
    private Long assignmentSessionId;
    private String studentName;
    private String studentEmail;
    private Integer quizId;
    private String quizTitle;
    private Long classId;
    private String className;
    private LocalDateTime submittedAt;
    // ALL_AUTO | PENDING_SPEAKING | PENDING_WRITING | PENDING_BOTH | ALL_GRADED
    private String overallStatus;
    private SectionStatus listening;
    private SectionStatus reading;
    private SectionStatus speaking;
    private SectionStatus writing;
    private BigDecimal autoScore;    // LISTENING + READING sum
    private BigDecimal totalScore;   // final (null if not yet graded)
    private Boolean isGraded;        // teacher submitted final grade

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectionStatus {
        private String skill;               // LISTENING/READING/SPEAKING/WRITING
        private String gradingStatus;        // AUTO | AI_PENDING | AI_READY | GRADED
        private BigDecimal score;           // section score (null if not yet graded)
        private BigDecimal maxScore;        // max possible for this section
        private String aiScore;             // e.g. "7/10"
        private String aiFeedback;          // AI feedback text
    }
}
