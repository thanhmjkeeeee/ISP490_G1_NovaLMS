package com.example.DoAn.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentInfoDTO {
    private Integer quizId;
    private String title;
    private String description;
    private String quizCategory;
    private List<String> skillOrder;
    private Map<String, Integer> timeLimitPerSkill;
    private Integer quizLevelTimeLimit;
    private Long sessionId;
    private String sessionStatus; // IN_PROGRESS / COMPLETED / EXPIRED
    private Integer currentSkillIndex;
    private Map<String, String> sectionStatuses;
    private Boolean canStart;
    private Boolean canResume;
    private Boolean isCompleted;
    private Long attemptsUsed;
    private Long maxAttempts;
    private Boolean attemptsExceeded;
    private Integer attemptsLeft;
    private Boolean canRetake;
    
    // External Submission fields
    private Boolean allowExternalSubmission;
    private String externalSubmissionInstruction;
    private String externalSubmissionLink;
    private String externalSubmissionNote;
    private Boolean isPreview;
}
