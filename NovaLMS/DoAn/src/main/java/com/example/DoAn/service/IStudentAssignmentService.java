package com.example.DoAn.service;

import com.example.DoAn.dto.response.AssignmentGradingDetailDTO;
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

    /** Save external link (Google Drive, etc.) */
    void saveExternalSubmission(Long sessionId, String link, String note, String userEmail);

    /** Complete assignment — returns QuizResult.id */
    Integer completeAssignment(Long sessionId, String userEmail);

    /** Auto-submit on timer expiry */
    @org.springframework.transaction.annotation.Transactional
    void autoSubmit(Long sessionId, String userEmail);

    /** Process all expired sessions in background */
    @org.springframework.transaction.annotation.Transactional
    void autoSubmitAllExpired();

    /** Get detailed result for student */
    AssignmentGradingDetailDTO getAssignmentResultDetail(Integer resultId, String studentEmail);

    /** Get preview info for teacher (bypasses enrollment/status checks) */
    AssignmentInfoDTO getAssignmentPreviewInfo(Integer quizId, String teacherEmail);
}
