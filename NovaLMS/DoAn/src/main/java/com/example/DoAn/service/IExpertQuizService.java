package com.example.DoAn.service;

import com.example.DoAn.dto.request.AssignmentQuestionRequestDTO;
import com.example.DoAn.dto.request.QuizQuestionRequestDTO;
import com.example.DoAn.dto.request.QuizRequestDTO;
import com.example.DoAn.dto.response.AssignmentPreviewDTO;
import com.example.DoAn.dto.response.PageResponse;
import com.example.DoAn.dto.response.QuizResponseDTO;
import com.example.DoAn.dto.response.SkillSectionSummaryDTO;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.List;
import java.util.Map;

public interface IExpertQuizService {

    QuizResponseDTO createQuiz(QuizRequestDTO request, String email) throws JsonProcessingException;

    QuizResponseDTO updateQuiz(Integer quizId, QuizRequestDTO request, String email);

    void deleteQuiz(Integer quizId, String email);

    QuizResponseDTO getQuizById(Integer quizId);

    PageResponse<QuizResponseDTO> getQuizzes(Integer courseId, String category, String status,
        String keyword, int page, int size);

    QuizResponseDTO changeStatus(Integer quizId, String newStatus, String email);

    // Quản lý câu hỏi trong Quiz
    QuizResponseDTO addQuestionToQuiz(Integer quizId, QuizQuestionRequestDTO request, String email);

    QuizResponseDTO removeQuestionFromQuiz(Integer quizId, Integer questionId, String email);

    QuizResponseDTO removeGroupFromQuiz(Integer quizId, Integer groupId, String email);

    QuizResponseDTO reorderQuestions(Integer quizId, List<QuizQuestionRequestDTO> orderedList, String email);

    // Assignment operations (4-skill sequential)
    Map<String, SkillSectionSummaryDTO> getSkillSummaries(Integer quizId);

    void addQuestionsToSection(Integer quizId, AssignmentQuestionRequestDTO dto, String email);

    void removeQuestion(Integer quizId, Integer questionId);

    QuizResponseDTO publishAssignment(Integer quizId);

    AssignmentPreviewDTO getAssignmentPreview(Integer quizId);

    QuizResponseDTO importAIQuestions(Integer quizId, List<com.example.DoAn.dto.response.AIGenerateResponseDTO.QuestionDTO> questions, String passage, String audioUrl, String email);
}
