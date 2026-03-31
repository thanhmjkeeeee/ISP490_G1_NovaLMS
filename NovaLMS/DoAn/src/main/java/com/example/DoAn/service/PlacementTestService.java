package com.example.DoAn.service;

import com.example.DoAn.dto.request.PlacementTestSubmissionDTO;
import com.example.DoAn.dto.response.QuizTakingDTO;

public interface PlacementTestService {

    /**
     * Submit a placement test.
     * When submission.hybridSessionId != null → attaches result to hybrid session.
     * WRITING/SPEAKING questions are graded asynchronously via Groq AI.
     */
    Integer submitPlacementTest(PlacementTestSubmissionDTO submission, String sessionId);

    /**
     * Load a published ENTRY_TEST quiz for taking (used by hybrid flow).
     */
    QuizTakingDTO getQuizForTaking(Integer quizId);
}
