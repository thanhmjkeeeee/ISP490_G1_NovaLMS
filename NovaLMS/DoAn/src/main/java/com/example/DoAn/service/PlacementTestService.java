package com.example.DoAn.service;

import com.example.DoAn.dto.request.PlacementTestSubmissionDTO;
import com.example.DoAn.dto.response.PlacementTestResultDTO;
import com.example.DoAn.dto.response.PlacementTestSummaryDTO;
import com.example.DoAn.dto.response.QuizTakingDTO;

import java.util.List;

public interface PlacementTestService {
    // Load danh sách tất cả bài ENTRY_TEST đang PUBLISHED để guest chọn (có tìm kiếm + lọc)
    List<PlacementTestSummaryDTO> getPlacementTests(String keyword, String skill);

    // Load danh sách tất cả (không filter - dùng cho backward compat)
    List<PlacementTestSummaryDTO> getAllPlacementTests();

    // Load quiz ENTRY_TEST để guest làm (theo quizId cụ thể)
    QuizTakingDTO getPlacementTest(Integer quizId);

    // Guest submit bài test, trả về resultId
    // Khi submission.hybridSessionId != null → gắn kết quả vào hybrid session
    Integer submitPlacementTest(PlacementTestSubmissionDTO submission, String sessionId);

    // Lấy kết quả chi tiết
    PlacementTestResultDTO getPlacementTestResult(Integer resultId);
}
