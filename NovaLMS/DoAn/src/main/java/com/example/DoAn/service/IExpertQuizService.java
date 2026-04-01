package com.example.DoAn.service;

import com.example.DoAn.dto.request.QuizQuestionRequestDTO;
import com.example.DoAn.dto.request.QuizRequestDTO;
import com.example.DoAn.dto.response.PageResponse;
import com.example.DoAn.dto.response.QuizResponseDTO;
import java.util.List;

public interface IExpertQuizService {

    QuizResponseDTO createQuiz(QuizRequestDTO request, String email);

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
}
