package com.example.DoAn.service;

import com.example.DoAn.dto.request.AIImportRequestDTO;
import com.example.DoAn.dto.request.QuestionRequestDTO;
import com.example.DoAn.dto.request.QuestionGroupRequestDTO;
import com.example.DoAn.dto.response.QuestionResponseDTO;
import com.example.DoAn.dto.response.QuestionGroupResponseDTO;
import java.util.List;

public interface IExpertQuestionService {
    List<QuestionResponseDTO> getQuestionsByModule(Integer moduleId, String email);
    QuestionResponseDTO createQuestion(QuestionRequestDTO request, String email);
    QuestionResponseDTO updateQuestion(Integer questionId, QuestionRequestDTO request, String email);
    void deleteQuestion(Integer questionId, String email);
    int saveAIQuestions(AIImportRequestDTO request, String email);

    // Question Group Methods
    QuestionGroupResponseDTO createQuestionGroup(QuestionGroupRequestDTO request, String email);
    QuestionGroupResponseDTO updateQuestionGroup(Integer groupId, QuestionGroupRequestDTO request, String email);
    void deleteQuestionGroup(Integer groupId, String email);
    List<QuestionGroupResponseDTO> getMyQuestionGroups(String email);
    QuestionGroupResponseDTO getQuestionGroupById(Integer groupId, String email);
}
