package com.example.DoAn.service;

import com.example.DoAn.dto.request.QuestionGradingRequestDTO;
import com.example.DoAn.dto.request.QuizGradingRequestDTO;
import com.example.DoAn.dto.response.*;

import java.util.List;
import java.util.Map;

public interface QuizResultService {
    QuizTakingDTO getQuizForTaking(Integer quizId, String email);
    Integer submitQuiz(Integer quizId, String email, Map<Integer, Object> answers);
    QuizResultDetailDTO getQuizResult(Integer resultId, String email);

    PageResponse<QuizResultHistoryDTO> getStudentQuizHistory(String email, int page, int size, String category, String keyword);

    PageResponse<QuizResultPendingDTO> getPendingGradingList(String email, Integer classId, int page, int size);

    PageResponse<QuizResultGradedDTO> getGradedResults(String email, Integer classId, int page, int size);

    /** Legacy method — accepts List of grading items only */
    void gradeQuizResult(Integer resultId, List<QuestionGradingRequestDTO> gradingItems, String email);

    /** Extended method — accepts skillScores + overallNote */
    void gradeQuizResult(Integer resultId, QuizGradingRequestDTO request, String email);
    
    /** Handles a violation: increments count, logs time, and locks if >= 3 */
    Map<String, Object> handleViolation(Integer quizId, String email, String reason);
    
    void unlockQuiz(Integer resultId);
    
    void requestUnlock(Integer resultId, String email, String reason);
    
    
    PageResponse<QuizResultPendingDTO> getUnlockRequests(String email, Integer classId, int page, int size);

    /** Recalculates total score, correct rate, skill bands, and overall IELTS band */
    void recalculateQuizResult(Integer resultId);
}
