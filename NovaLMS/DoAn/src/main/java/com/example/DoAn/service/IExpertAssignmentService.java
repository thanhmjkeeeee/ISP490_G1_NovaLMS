package com.example.DoAn.service;

import com.example.DoAn.dto.request.AssignmentQuestionRequestDTO;
import com.example.DoAn.dto.request.QuizRequestDTO;
import com.example.DoAn.dto.response.AssignmentPreviewDTO;
import com.example.DoAn.dto.response.SkillSectionSummaryDTO;
import com.example.DoAn.model.Quiz;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.List;
import java.util.Map;

public interface IExpertAssignmentService {

    // Create new assignment (Step 1)
    Quiz createAssignment(QuizRequestDTO dto, String expertEmail) throws JsonProcessingException;

    // Get skill summary with question counts (for wizard steps)
    Map<String, SkillSectionSummaryDTO> getSkillSummaries(Integer quizId);

    // Add questions to a specific skill section (Steps 2-5)
    void addQuestionsToSection(Integer quizId, AssignmentQuestionRequestDTO dto, String expertEmail);

    // Remove a question from assignment
    void removeQuestion(Integer quizId, Integer questionId);

    // Get full preview (Step 6)
    AssignmentPreviewDTO getPreview(Integer quizId);

    // Publish assignment
    void publishAssignment(Integer quizId);

    // Change status (ARCHIVED, etc.)
    void changeStatus(Integer quizId, String status);

    // List assignments (COURSE_ASSIGNMENT + MODULE_ASSIGNMENT)
    List<Quiz> getAssignments(String expertEmail);

    // Get assignment detail
    Quiz getAssignment(Integer quizId, String expertEmail);
}
