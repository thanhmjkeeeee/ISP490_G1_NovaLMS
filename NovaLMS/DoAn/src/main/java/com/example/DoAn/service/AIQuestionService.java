package com.example.DoAn.service;

import com.example.DoAn.dto.request.AIGenerateRequestDTO;
import com.example.DoAn.dto.response.AIGenerateResponseDTO;

public interface AIQuestionService {

    AIGenerateResponseDTO generate(AIGenerateRequestDTO request, String userEmail);

    class RateLimitExceededException extends RuntimeException {
        public RateLimitExceededException(String message) { super(message); }
    }

    class AIException extends RuntimeException {
        public AIException(String message, Throwable cause) { super(message, cause); }
    }
}
