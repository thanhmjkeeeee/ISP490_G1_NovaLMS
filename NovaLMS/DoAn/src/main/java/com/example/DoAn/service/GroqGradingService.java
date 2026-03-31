package com.example.DoAn.service;

/**
 * Fire-and-forget AI grading service for WRITING/SPEAKING placement test questions.
 * Uses Groq AI (Whisper for transcription + LLaMA 4 Scout for grading).
 * Does NOT block the HTTP response — runs asynchronously.
 */
public interface GroqGradingService {

    /**
     * Fire-and-forget: grade WRITING/SPEAKING answer asynchronously.
     *
     * @param placementResultId the PlacementTestResult ID
     * @param questionId        the Question ID
     * @param questionType      "WRITING" or "SPEAKING"
     */
    void fireAndForget(Integer placementResultId, Integer questionId, String questionType);
}
