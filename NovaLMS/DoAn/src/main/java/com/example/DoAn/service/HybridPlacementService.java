package com.example.DoAn.service;

import com.example.DoAn.dto.request.HybridSessionCreateDTO;
import com.example.DoAn.dto.response.*;
import java.util.List;
import java.util.Map;

public interface HybridPlacementService {

    /** Danh sách kỹ năng có quiz ENTRY_TEST khả dụng cho Hybrid */
    List<HybridSkillDTO> getAvailableSkills();

    /** Map skill -> danh sách quiz hybrid-enabled phù hợp */
    Map<String, List<HybridQuizSummaryDTO>> getQuizzesBySkills(List<String> skills);

    /** Tạo phiên hybrid — validate và lưu HybridSession + HybridSessionQuiz */
    HybridSessionDTO createSession(HybridSessionCreateDTO request, String guestSessionId);

    /** Lấy quiz tiếp theo (1-based index) trong phiên */
    QuizTakingDTO getQuizForSession(Integer sessionId, Integer quizIndex);

    /** Thông tin chuyển phần sau khi nộp 1 quiz */
    HybridTransitionDTO getTransitionInfo(Integer sessionId);

    /** Kết quả tổng hợp toàn bộ phiên */
    HybridResultDTO getHybridResults(Integer sessionId);
}
