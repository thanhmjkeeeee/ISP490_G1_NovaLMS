package com.example.DoAn.service;

import com.example.DoAn.dto.request.QuestionGradingRequestDTO;
import com.example.DoAn.dto.response.PageResponse;
import com.example.DoAn.dto.response.QuizResultDetailDTO;
import com.example.DoAn.dto.response.QuizResultHistoryDTO;
import com.example.DoAn.dto.response.QuizResultPendingDTO;
import com.example.DoAn.dto.response.QuizTakingDTO;
import java.util.List;
import java.util.Map;

public interface QuizResultService {
    QuizTakingDTO getQuizForTaking(Integer quizId, String email);
    Integer submitQuiz(Integer quizId, String email, Map<Integer, Object> answers);
    QuizResultDetailDTO getQuizResult(Integer resultId, String email);

    PageResponse<QuizResultHistoryDTO> getStudentQuizHistory(String email, int page, int size);

    PageResponse<QuizResultPendingDTO> getPendingGradingList(String email, int page, int size);
    void gradeQuizResult(Integer resultId, List<QuestionGradingRequestDTO> gradingItems, String email);
}
