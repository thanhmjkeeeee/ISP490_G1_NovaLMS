package com.example.DoAn.service;

import com.example.DoAn.dto.request.QuestionBankRequestDTO;
import com.example.DoAn.dto.response.PageResponse;
import com.example.DoAn.dto.response.QuestionBankResponseDTO;

public interface IQuestionBankService {

    QuestionBankResponseDTO createQuestion(QuestionBankRequestDTO request, String email);

    QuestionBankResponseDTO updateQuestion(Integer questionId, QuestionBankRequestDTO request, String email);

    void deleteQuestion(Integer questionId, String email);

    QuestionBankResponseDTO getQuestionById(Integer questionId);

    PageResponse<QuestionBankResponseDTO> getQuestions(String skill, String cefrLevel,
        String questionType, String topic, String status, String keyword,
        int page, int size);

    QuestionBankResponseDTO changeStatus(Integer questionId, String newStatus, String email);
}
