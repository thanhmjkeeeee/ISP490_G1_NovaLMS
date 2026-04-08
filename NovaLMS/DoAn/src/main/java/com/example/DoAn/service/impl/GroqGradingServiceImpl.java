package com.example.DoAn.service.impl;

import com.example.DoAn.dto.GradingResponse;
import com.example.DoAn.model.*;
import com.example.DoAn.repository.*;
import com.example.DoAn.service.EmailService;
import com.example.DoAn.service.GroqClient;
import com.example.DoAn.service.GroqGradingService;
import com.example.DoAn.service.INotificationService;
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
    private final QuizAnswerRepository quizAnswerRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final QuizResultRepository quizResultRepository;
    private final INotificationService notificationService;
    private final EmailService emailService;

    @Override
    @Async
    public void fireAndForget(Integer placementResultId, Integer questionId, String questionType) {
        log.info("[AI-GRADE] fireAndForget (placement) → resultId={}, questionId={}, type={}",
                placementResultId, questionId, questionType);
        transactionTemplate.executeWithoutResult(status -> {
            try {
                doGrade(placementResultId, questionId, questionType);
                log.info("[AI-GRADE] fireAndForget (placement) done → resultId={}, questionId={}",
                        placementResultId, questionId);
            } catch (Exception e) {
                log.error("[AI-GRADE] fireAndForget (placement) FAILED → resultId={}, questionId={}: {}",
                        placementResultId, questionId, e.getMessage(), e);
            }
        });
    }

    /**
     * Fire-and-forget AI grading for a QuizAnswer (SPEAKING/WRITING).
     * Spawns a background thread and calls gradeQuizAnswerInNewTx() with a fresh transaction.
     */
    @Override
    @Async
    public void fireAndForgetForQuizAnswer(Integer quizResultId, Integer questionId) {
        log.info("[AI-GRADE] fireAndForget started → resultId={}, questionId={}", quizResultId, questionId);
        try {
            gradeQuizAnswerInNewTx(quizResultId, questionId, null);
            log.info("[AI-GRADE] fireAndForget completed → resultId={}, questionId={}", quizResultId, questionId);
        } catch (Exception e) {
            log.error("[AI-GRADE] fireAndForget FAILED → resultId={}, questionId={}: {}",
                    quizResultId, questionId, e.getMessage(), e);
        }
    }

    /**
     * Synchronous version — called from StudentAssignmentServiceImpl which wraps
     * this call in its own TransactionTemplate tx (no @Transactional here needed).
     */
    @Override
    public void gradeSync(Integer quizResultId, Integer questionId, String questionTypeOverride) {
        log.info("[AI-GRADE-SYNC] Starting sync grade → resultId={}, questionId={}, type={}",
                quizResultId, questionId, questionTypeOverride);
        try {
            doGradeQuizAnswer(quizResultId, questionId, questionTypeOverride);
            log.info("[AI-GRADE-SYNC] Done → resultId={}, questionId={}", quizResultId, questionId);
        } catch (Exception e) {
            log.error("[AI-GRADE-SYNC] FAILED → resultId={}, questionId={}: {}",
                    quizResultId, questionId, e.getMessage(), e);
            throw new RuntimeException(e);
        }
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

            // Compute int score for placement recalculateResult (expects Integer)
            int intScore = (int) Math.round(grading.getOverallBand() * 2.5);
            int intMax = maxPoints;

            // Update answer
            answer.setPendingAiReview(false);
            answer.setAiScore(intScore);  // Integer field — used by recalculateResult
            answer.setAiFeedback(grading.getFeedback());
            answer.setAiRubricJson(objectMapper.writeValueAsString(grading.getRubric()));
            answer.setIsCorrect(grading.getOverallBand() >= 5.0);
            answerRepository.save(answer);

            // Recalculate result
            recalculateResult(result, quiz.getQuizId());

            log.info("AI grading done for answer={}, band={}, score={}/{}",
                answer.getId(), grading.getOverallBand(), intScore, intMax);

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
                    totalScore += a.getAiScore();  // Integer field — intScore from IELTS band
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

    /**
     * Wrapped inside @Async task — spawns a fresh transaction for JPA access safety.
     */
    private void gradeQuizAnswerInNewTx(Integer quizResultId, Integer questionId, String questionTypeOverride) {
        log.info("[AI-GRADE] Starting new tx → resultId={}, questionId={}", quizResultId, questionId);
        transactionTemplate.executeWithoutResult(status -> {
            try {
                doGradeQuizAnswer(quizResultId, questionId, questionTypeOverride);
            } catch (Exception e) {
                log.error("[AI-GRADE] doGradeQuizAnswer error → resultId={}, questionId={}: {}",
                        quizResultId, questionId, e.getMessage(), e);
                throw new RuntimeException(e); // re-throw to mark tx as rollback
            }
        });
        log.info("[AI-GRADE] Tx committed → resultId={}, questionId={}", quizResultId, questionId);
    }

    /**
     * Actual grading logic — runs INSIDE a TransactionTemplate tx (caller's responsibility).
     * NOTE: called from same class, so @Transactional has no effect — transaction is
     * managed by TransactionTemplate in gradeQuizAnswerInNewTx().
     */
    private void doGradeQuizAnswer(Integer quizResultId, Integer questionId, String questionTypeOverride) {
        QuizAnswer answer = quizAnswerRepository
                .findByQuizResultResultIdAndQuestionQuestionId(quizResultId, questionId);

        if (answer == null) {
            log.warn("QuizAnswer not found: resultId={}, questionId={}", quizResultId, questionId);
            return;
        }

        Question q = answer.getQuestion();
        String questionType = questionTypeOverride != null ? questionTypeOverride : q.getQuestionType();
        String userAnswer = answer.getAnsweredOptions();

        try {
            if ("SPEAKING".equals(questionType)) {
                log.info("Transcribing SPEAKING audio for answerId={}", answer.getAnswerId());
                userAnswer = groqClient.transcribe(userAnswer);
            }

            GradingResponse grading = groqClient.gradeWritingOrSpeaking(
                    q.getContent(),
                    q.getSkill(),
                    q.getCefrLevel() != null ? q.getCefrLevel() : "B1",
                    userAnswer != null ? userAnswer : "",
                    questionType,
                    10 // default maxPoints for SPEAKING/WRITING
            );

            // aiScore format: "8.13/10" = displayScore/maxScore
            String displayScoreStr = String.format("%.2f", grading.getDisplayScore());
            answer.setAiScore(displayScoreStr + "/" + (int) grading.getMaxScore());
            answer.setAiFeedback(grading.getFeedback());
            answer.setAiRubricJson(objectMapper.writeValueAsString(grading.getRubric()));
            answer.setPendingAiReview(false);
            answer.setAiGradingStatus("COMPLETED");
            answer.setIsCorrect(grading.getOverallBand() >= 5.0);
            quizAnswerRepository.save(answer);

            // Update QuizResult section_scores
            recalculateQuizResult(quizResultId);

            log.info("AI grading done for answerId={}, band={}, displayScore={}/{}",
                    answer.getAnswerId(), grading.getOverallBand(),
                    displayScoreStr, (int) grading.getMaxScore());

        } catch (Exception e) {
            log.error("AI grading failed for answerId={}: {}", answer.getAnswerId(), e.getMessage());
            answer.setPendingAiReview(false);
            answer.setAiGradingStatus("COMPLETED");
            answer.setAiFeedback("AI grading unavailable. Please contact support.");
            answer.setIsCorrect(false);
            quizAnswerRepository.save(answer);
        }
    }

    private void recalculateQuizResult(Integer resultId) {
        QuizResult result = quizResultRepository.findById(resultId).orElse(null);
        if (result == null) return;

        List<QuizAnswer> answers = quizAnswerRepository.findByQuizResultResultId(resultId);

        double totalScore = 0;
        double maxScore = 0;

        for (QuizAnswer a : answers) {
            Question q = a.getQuestion();
            String qType = q.getQuestionType();
            Integer qId = q.getQuestionId();

            Integer pts = quizQuestionRepository
                    .findByQuizQuizIdAndQuestionQuestionId(result.getQuiz().getQuizId(), qId)
                    .map(qq -> qq.getPoints() != null ? qq.getPoints().intValue() : 1)
                    .orElse(1);

            if ("WRITING".equals(qType) || "SPEAKING".equals(qType)) {
                if (a.getAiScore() != null && a.getAiRubricJson() != null) {
                    try {
                        // Parse new format: "8.13/10" or fallback "8/10"
                        String scoreStr = a.getAiScore();
                        if (scoreStr.contains("/")) {
                            double scoreVal = Double.parseDouble(scoreStr.split("/")[0].trim());
                            int maxVal = Integer.parseInt(scoreStr.split("/")[1].trim());
                            totalScore += scoreVal;
                            maxScore += maxVal;
                        }
                    } catch (Exception ignored) {}
                }
            } else {
                if (Boolean.TRUE.equals(a.getIsCorrect())) {
                    totalScore += pts;
                }
                maxScore += pts;
            }
        }

        if (maxScore > 0) {
            BigDecimal rate = BigDecimal.valueOf(totalScore)
                    .divide(BigDecimal.valueOf(maxScore), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            result.setCorrectRate(rate);
            result.setScore((int) Math.round(totalScore));
        }
        quizResultRepository.save(result);
    }
}
