package com.example.DoAn.service.impl;

import com.example.DoAn.dto.GradingResponse;
import com.example.DoAn.model.*;
import com.example.DoAn.repository.*;
import com.example.DoAn.service.EmailService;
import com.example.DoAn.service.GroqClient;
import com.example.DoAn.service.GroqGradingService;
import com.example.DoAn.service.INotificationService;
import com.example.DoAn.util.IELTSScoreMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
                recalculateQuizResult(quizResultId);
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
                recalculateQuizResult(quizResultId);
            }

        } catch (Exception e) {
            log.error("AI grading failed: {}", e.getMessage());
            answer.setPendingAiReview(false);
            answer.setAiGradingStatus("COMPLETED");
            answer.setIsCorrect(false);
            quizAnswerRepository.save(answer);
        }
    }

    private void recalculateQuizResult(Integer resultId) {
        QuizResult result = quizResultRepository.findById(resultId).orElse(null);
        if (result == null) return;

        List<QuizAnswer> answers = quizAnswerRepository.findByQuizResultResultId(resultId);

        Map<String, Double> skillRawScore = new HashMap<>();
        Map<String, Double> skillMaxScore = new HashMap<>();

        for (QuizAnswer a : answers) {
            Question q = a.getQuestion();
            String skill = (q.getSkill() != null ? q.getSkill() : "DEFAULT").toUpperCase();
            String qType = q.getQuestionType();

            double pts = quizQuestionRepository
                    .findByQuizQuizIdAndQuestionQuestionId(result.getQuiz().getQuizId(), q.getQuestionId())
                    .map(qq -> qq.getPoints() != null ? qq.getPoints().doubleValue() : 1.0)
                    .orElse(1.0);

            skillMaxScore.put(skill, skillMaxScore.getOrDefault(skill, 0.0) + pts);

            if ("WRITING".equals(qType) || "SPEAKING".equals(qType)) {
                if (a.getAiScore() != null) {
                    try {
                        String scoreStr = a.getAiScore();
                        double scoreVal = Double.parseDouble(scoreStr.split("/")[0].trim());
                        int maxVal = Integer.parseInt(scoreStr.split("/")[1].trim());
                        double scaledScore = maxVal > 0 ? (scoreVal / maxVal) * pts : 0;
                        skillRawScore.put(skill, skillRawScore.getOrDefault(skill, 0.0) + scaledScore);

                        if (a.getTeacherOverrideScore() == null) {
                            a.setPointsAwarded(BigDecimal.valueOf(scaledScore));
                            quizAnswerRepository.save(a);
                        }
                    } catch (Exception ignored) {}
                }
            } else {
                if (Boolean.TRUE.equals(a.getIsCorrect())) {
                    skillRawScore.put(skill, skillRawScore.getOrDefault(skill, 0.0) + pts);
                }
            }
        }

        Map<String, Double> skillBands = new HashMap<>();
        for (String skill : skillMaxScore.keySet()) {
            double raw = skillRawScore.getOrDefault(skill, 0.0);
            double max = skillMaxScore.get(skill);
            
            if ("WRITING".equalsIgnoreCase(skill) || "SPEAKING".equalsIgnoreCase(skill)) {
                skillBands.put(skill, max > 0 ? (raw / max) * 9.0 : 0.0);
            } else {
                skillBands.put(skill, IELTSScoreMapper.mapRawToBand(raw, max, skill));
            }
        }

        double overallBandScore = IELTSScoreMapper.calculateOverallBand(skillBands);
        result.setOverallBand(BigDecimal.valueOf(overallBandScore));

        double totalRaw = skillRawScore.values().stream().mapToDouble(Double::doubleValue).sum();
        double totalMax = skillMaxScore.values().stream().mapToDouble(Double::doubleValue).sum();
        
        if (totalMax > 0) {
            result.setCorrectRate(BigDecimal.valueOf((totalRaw / totalMax) * 100).setScale(2, RoundingMode.HALF_UP));
            result.setScore((int) Math.round(totalRaw));
        }

        try {
            result.setSectionScores(objectMapper.writeValueAsString(skillBands));
        } catch (Exception e) {
            log.error("Failed to serialize section scores", e);
        }

        quizResultRepository.save(result);
    }
}
