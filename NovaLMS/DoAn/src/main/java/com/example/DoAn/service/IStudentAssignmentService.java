package com.example.DoAn.service;

import com.example.DoAn.dto.response.AssignmentInfoDTO;
import com.example.DoAn.dto.response.AssignmentSectionDTO;

import java.util.Map;

public interface IStudentAssignmentService {

    /** Entry point — get assignment info, create or resume session */
    AssignmentInfoDTO getAssignmentInfo(Integer quizId, String userEmail);

    /** Get section data + questions */
    AssignmentSectionDTO getSection(Long sessionId, String skill, String userEmail);

    /** Auto-save answers (every 30s) */
    void saveAnswers(Long sessionId, String skill, Map<Integer, Object> answers, String userEmail);

    /** Submit a section (LISTENING/READING/WRITING) */
    Map<String, Object> submitSection(Long sessionId, String skill,
            Map<Integer, Object> answers, String userEmail);

    /** Submit SPEAKING with audio URLs */
    Map<String, Object> submitSpeakingSection(Long sessionId,
            Map<Integer, String> audioUrls, String userEmail);

    /** Complete assignment — returns QuizResult.id */
    Integer completeAssignment(Long sessionId, String userEmail);

    /** Auto-submit on timer expiry */
    void autoSubmit(Long sessionId, String userEmail);
}
