package com.example.DoAn.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HybridResultDTO {
    private Integer sessionId;
    private String guestName;
    private String guestEmail;
    private LocalDateTime completedAt;

    /** True if any WRITING/SPEAKING question is still pending AI grading — triggers polling. */
    private boolean hasPendingAI;

    private List<HybridResultSectionDTO> sections;

    private int overallScore;
    private int overallTotalPoints;
    private BigDecimal overallCorrectRate;
    private String overallCEFR;
    private String levelDescription;

    // AI-aware overall (includes WRITING/SPEAKING after AI grading)
    private Integer overallScoreIncludingAI;
    private Integer overallTotalPointsIncludingAI;
    private BigDecimal overallCorrectRateIncludingAI;
    private String overallCEFRIncludingAI;

    private List<CoursePublicResponseDTO> suggestedCourses;
}
