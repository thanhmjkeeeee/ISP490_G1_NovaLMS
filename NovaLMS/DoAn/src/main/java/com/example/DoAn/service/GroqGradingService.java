package com.example.DoAn.service;

/**
 * Fire-and-forget AI grading service for WRITING/SPEAKING placement test questions.
 * Uses Groq AI (Whisper for transcription + LLaMA 4 Scout for grading).
 * Does NOT block the HTTP response — runs asynchronously.
 */
public interface GroqGradingService {



    /**
     * Fire-and-forget: grade a QuizAnswer (Lesson Quiz or Assignment)
     * for SPEAKING/WRITING asynchronously.
     *
     * @param quizResultId the QuizResult ID
     * @param questionId   the Question ID
     */
    void fireAndForgetForQuizAnswer(Integer quizResultId, Integer questionId);

    /**
     * Synchronous grading — caller manages the transaction via TransactionTemplate.
     * Used by StudentAssignmentServiceImpl which already wraps in its own tx.
     *
     * @param quizResultId         the QuizResult ID
     * @param questionId           the Question ID
     * @param questionTypeOverride null = auto-detect from question
     */
    void gradeSync(Integer quizResultId, Integer questionId, String questionTypeOverride);
}
