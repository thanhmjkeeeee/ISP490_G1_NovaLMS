package com.example.DoAn.service.impl;

import com.example.DoAn.dto.GradingResponse;
import com.example.DoAn.model.*;
import com.example.DoAn.repository.*;
import com.example.DoAn.service.EmailService;
import com.example.DoAn.service.GroqClient;
import com.example.DoAn.service.GroqGradingService;
import com.example.DoAn.service.INotificationService;
import com.example.DoAn.util.IELTSScoreMapper;
import com.example.DoAn.service.QuizResultService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroqGradingServiceImpl implements GroqGradingService {

    private final GroqClient groqClient;
    private final QuizQuestionRepository quizQuestionRepository;
    private final TransactionTemplate transactionTemplate;
    private final QuizAnswerRepository quizAnswerRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final QuizResultRepository quizResultRepository;
    private final INotificationService notificationService;
    private final EmailService emailService;

    @Autowired
    @Lazy
    private QuizResultService quizResultService;

    @Override
    @Async
    public void processBatchAIForQuiz(Integer quizResultId) {
        log.info("[AI-BATCH] Started batch grading for resultId={}", quizResultId);
        try {
            transactionTemplate.executeWithoutResult(status -> {
                List<QuizAnswer> pendingAnswers = quizAnswerRepository.findByQuizResultResultId(quizResultId).stream()
                        .filter(a -> Boolean.TRUE.equals(a.getPendingAiReview()))
                        .toList();

                if (pendingAnswers.isEmpty()) {
                    log.info("[AI-BATCH] No pending answers for resultId={}", quizResultId);
                    return;
                }

                for (QuizAnswer answer : pendingAnswers) {
                    try {
                        log.info("[AI-BATCH] Grading Q{} (Type={})", answer.getQuestion().getQuestionId(), answer.getQuestion().getQuestionType());
                        doGradeQuizAnswer(quizResultId, answer.getQuestion().getQuestionId(), null, false);
                    } catch (Exception e) {
                        log.error("[AI-BATCH] Failed grading Q{} for result {}: {}", 
                                answer.getQuestion().getQuestionId(), quizResultId, e.getMessage());
                    }
                }
                
                // Recalculate ONCE after batch
                quizResultService.recalculateQuizResult(quizResultId);
            });
            log.info("[AI-BATCH] Completed batch grading and recalculated for resultId={}", quizResultId);
        } catch (Exception e) {
            log.error("[AI-BATCH] Fatal error in batch grading for resultId={}: {}", quizResultId, e.getMessage(), e);
        }
    }

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

    @Override
    public void gradeSync(Integer quizResultId, Integer questionId, String questionTypeOverride) {
        log.info("[AI-GRADE-SYNC] Starting sync grade → resultId={}, questionId={}, type={}",
                quizResultId, questionId, questionTypeOverride);
        try {
            doGradeQuizAnswer(quizResultId, questionId, questionTypeOverride, true);
            log.info("[AI-GRADE-SYNC] Done → resultId={}, questionId={}", quizResultId, questionId);
        } catch (Exception e) {
            log.error("[AI-GRADE-SYNC] FAILED → resultId={}, questionId={}: {}",
                    quizResultId, questionId, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private void gradeQuizAnswerInNewTx(Integer quizResultId, Integer questionId, String questionTypeOverride) {
        transactionTemplate.executeWithoutResult(status -> {
            try {
                doGradeQuizAnswer(quizResultId, questionId, questionTypeOverride, true);
            } catch (Exception e) {
                log.error("[AI-GRADE] gradeQuizAnswerInNewTx error → resultId={}, questionId={}: {}",
                        quizResultId, questionId, e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }

    private void doGradeQuizAnswer(Integer quizResultId, Integer questionId, String questionTypeOverride, boolean shouldRecalculate) {
        QuizAnswer answer = quizAnswerRepository
                .findByQuizResultResultIdAndQuestionQuestionId(quizResultId, questionId);

        if (answer == null) return;

        Question q = answer.getQuestion();
        String questionType = questionTypeOverride != null ? questionTypeOverride : q.getQuestionType();
        String userAnswer = answer.getAnsweredOptions();

        try {
            if ("SPEAKING".equals(questionType)) {
                userAnswer = groqClient.transcribe(userAnswer);
            }

            GradingResponse grading = groqClient.gradeWritingOrSpeaking(
                    q.getContent(),
                    q.getSkill(),
                    q.getCefrLevel() != null ? q.getCefrLevel() : "B1",
                    userAnswer != null ? userAnswer : "",
                    questionType,
                    9 // IELTS Scale
            );

            // aiScore format: "8.50/9"
            String displayScoreStr = String.format("%.2f", grading.getDisplayScore());
            answer.setAiScore(displayScoreStr + "/" + (int) grading.getMaxScore());
            answer.setAiFeedback(grading.getFeedback());
            answer.setAiRubricJson(objectMapper.writeValueAsString(grading.getRubric()));
            answer.setPendingAiReview(false);
            answer.setAiGradingStatus("COMPLETED");
            answer.setIsCorrect(grading.getOverallBand() >= 5.0);
            quizAnswerRepository.save(answer);

            if (shouldRecalculate) {
                quizResultService.recalculateQuizResult(quizResultId);
            }

        } catch (Exception e) {
            log.error("AI grading failed: {}", e.getMessage());
            answer.setPendingAiReview(false);
            answer.setAiGradingStatus("COMPLETED");
            answer.setIsCorrect(false);
            quizAnswerRepository.save(answer);
        }
    }

}
