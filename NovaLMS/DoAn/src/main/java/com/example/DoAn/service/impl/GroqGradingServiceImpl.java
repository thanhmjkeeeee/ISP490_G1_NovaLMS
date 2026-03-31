package com.example.DoAn.service.impl;

import com.example.DoAn.dto.GradingResponse;
import com.example.DoAn.model.*;
import com.example.DoAn.repository.*;
import com.example.DoAn.service.GroqClient;
import com.example.DoAn.service.GroqGradingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Async AI grading for WRITING/SPEAKING placement test questions.
 *
 * Flow:
 *   1. submitPlacementTest (HTTP tx) → fires fireAndForget
 *   2. fireAndForget → spawns background thread
 *   3. grade() → uses TransactionTemplate for its own tx → calls Groq → saves result
 *
 * JPA entities NEVER cross thread boundaries — all required data is fetched
 * inside the background transaction via the answerId key.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroqGradingServiceImpl implements GroqGradingService {

    private final GroqClient groqClient;
    private final PlacementTestAnswerRepository answerRepository;
    private final PlacementTestResultRepository resultRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Async
    public void fireAndForget(Integer placementResultId, Integer questionId, String questionType) {
        CompletableFuture.runAsync(() -> grade(placementResultId, questionId, questionType));
    }

    /**
     * Grading logic — runs in its own transaction via TransactionTemplate.
     * Safe: all JPA access is inside this tx-boundary.
     */
    public void grade(Integer placementResultId, Integer questionId, String questionType) {
        transactionTemplate.executeWithoutResult(status -> {
            doGrade(placementResultId, questionId, questionType);
        });
    }

    @Transactional
    public void doGrade(Integer placementResultId, Integer questionId, String questionType) {
        PlacementTestAnswer answer = answerRepository
                .findByResultIdAndQuestionId(placementResultId, questionId)
                .orElse(null);
        if (answer == null) {
            log.warn("Answer not found: result={}, question={}", placementResultId, questionId);
            return;
        }

        // Force-load all lazy proxies while in tx
        Question question = answer.getQuestion();
        PlacementTestResult result = answer.getPlacementTestResult();
        Quiz quiz = result.getQuiz();

        Integer maxPoints = quizQuestionRepository
                .findByQuizQuizIdAndQuestionQuestionId(quiz.getQuizId(), question.getQuestionId())
                .map(qq -> qq.getPoints() != null ? qq.getPoints().intValue() : 10)
                .orElse(10);

        String studentAnswer = answer.getAnsweredOptions();

        try {
            // SPEAKING: transcribe audio first
            if ("SPEAKING".equals(questionType)) {
                log.info("Transcribing SPEAKING audio for answer={}", answer.getId());
                String transcript = groqClient.transcribe(studentAnswer);
                studentAnswer = transcript;
            }

            // Grade with LLaMA
            log.info("Grading {} for answer={}", questionType, answer.getId());
            GradingResponse grading = groqClient.gradeWritingOrSpeaking(
                question.getContent(),
                question.getSkill(),
                question.getCefrLevel(),
                studentAnswer,
                questionType,
                maxPoints
            );

            // Update answer
            answer.setPendingAiReview(false);
            answer.setAiScore(grading.getTotalScore());
            answer.setAiFeedback(grading.getFeedback());
            answer.setAiRubricJson(objectMapper.writeValueAsString(grading.getRubric()));
            answer.setIsCorrect(grading.getTotalScore() >= grading.getMaxScore() * 0.5);
            answerRepository.save(answer);

            // Recalculate result
            recalculateResult(result, quiz.getQuizId());

            log.info("AI grading done for answer={}, score={}/{}",
                answer.getId(), grading.getTotalScore(), grading.getMaxScore());

        } catch (Exception e) {
            log.error("AI grading failed for answer={}: {}", answer.getId(), e.getMessage());
            answer.setPendingAiReview(false);
            answer.setAiFeedback("AI grading unavailable. Please contact support.");
            answer.setIsCorrect(false);
            answerRepository.save(answer);
        }
    }

    private void recalculateResult(PlacementTestResult result, Integer quizId) {
        List<PlacementTestAnswer> answers = answerRepository.findByResultId(result.getId());

        int totalScore = 0;
        int maxScore = 0;

        for (PlacementTestAnswer a : answers) {
            String qType = a.getQuestion().getQuestionType();
            Integer qId = a.getQuestion().getQuestionId();

            Integer pts = quizQuestionRepository
                    .findByQuizQuizIdAndQuestionQuestionId(quizId, qId)
                    .map(qq -> qq.getPoints() != null ? qq.getPoints().intValue() : 1)
                    .orElse(1);

            if ("WRITING".equals(qType) || "SPEAKING".equals(qType)) {
                if (a.getAiScore() != null) {
                    totalScore += a.getAiScore();
                    maxScore += pts;
                }
            } else {
                if (Boolean.TRUE.equals(a.getIsCorrect())) {
                    totalScore += pts;
                }
                maxScore += pts;
            }
        }

        BigDecimal rate = maxScore > 0
            ? BigDecimal.valueOf(totalScore).divide(BigDecimal.valueOf(maxScore), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        String suggestedLevel = calculateCEFR(rate.doubleValue() * 100);

        result.setTotalScoreIncludingAi(totalScore);
        result.setCorrectRateIncludingAi(rate);
        result.setSuggestedLevel(suggestedLevel);
        resultRepository.save(result);
    }

    private String calculateCEFR(double rate) {
        if (rate <= 20) return "A1";
        if (rate <= 40) return "A2";
        if (rate <= 60) return "B1";
        if (rate <= 75) return "B2";
        if (rate <= 90) return "C1";
        return "C2";
    }
}
