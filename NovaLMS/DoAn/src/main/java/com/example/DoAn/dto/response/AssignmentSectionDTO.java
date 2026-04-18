package com.example.DoAn.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentSectionDTO {
    private Long sessionId;
    private String skill;
    private Integer sectionIndex;      // 0-3
    private Integer currentSkillIndex;
    private Long timerSeconds;         // quiz-level remaining seconds
    private Long speakingTimerSeconds;  // per-skill timer (only for SPEAKING)
    private String speakingExpiry;      // ISO datetime string
    private Long writingTimerSeconds;   // per-skill timer (only for WRITING)
    private String writingExpiry;
    private List<QuizQuestionPayloadDTO> questions;
    private Map<Integer, Object> savedAnswers;
    private Map<String, String> sectionStatuses;
    private String nextSkill;
    private Integer nextSkillIndex;
    private String previousSkill;
    private Boolean isSpeaking;
    private Boolean isWriting;
    private Boolean isLastSection;
    private Boolean isLocked;
    
    // External Submission fields
    private Boolean allowExternalSubmission;
    private String externalSubmissionInstruction;
    private String externalSubmissionLink;
    private String externalSubmissionNote;
}
