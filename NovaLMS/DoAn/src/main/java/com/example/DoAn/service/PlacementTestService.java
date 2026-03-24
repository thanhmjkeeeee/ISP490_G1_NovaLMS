package com.example.DoAn.service;

import com.example.DoAn.dto.request.PlacementTestSubmissionDTO;
import com.example.DoAn.dto.response.PlacementTestResultDTO;
import com.example.DoAn.dto.response.QuizTakingDTO;

public interface PlacementTestService {
    // Load quiz ENTRY_TEST để guest làm
    QuizTakingDTO getPlacementTest();
    
    // Guest submit bài test, trả về resultId  
    Integer submitPlacementTest(PlacementTestSubmissionDTO submission, String sessionId);
    
    // Lấy kết quả chi tiết
    PlacementTestResultDTO getPlacementTestResult(Integer resultId);
}
