package com.example.DoAn.dto.request;

import lombok.Data;
import java.util.Map;

@Data
public class PlacementTestSubmissionDTO {
    private Integer quizId;
    private String guestName;          // optional
    private String guestEmail;         // optional
    private Map<Integer, Object> answers;

    // --- Hybrid mode ---
    // null = submit bình thường (legacy 1-quiz)
    private Integer hybridSessionId;
    // 1-based, chỉ dùng khi hybridSessionId != null
    private Integer quizIndex;
}
