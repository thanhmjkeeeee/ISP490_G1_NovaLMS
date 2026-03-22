package com.example.DoAn.service;

import com.example.DoAn.dto.request.QuestionRequestDTO;
import com.example.DoAn.dto.response.QuestionResponseDTO;
import java.util.List;

public interface IExpertQuestionService {
    List<QuestionResponseDTO> getQuestionsByModule(Integer moduleId, String email);
    QuestionResponseDTO createQuestion(QuestionRequestDTO request, String email);
    QuestionResponseDTO updateQuestion(Integer questionId, QuestionRequestDTO request, String email);
    void deleteQuestion(Integer questionId, String email);
}
