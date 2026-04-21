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
                List<QuizAnswer> pendingAnswers = quizAnswerRepository.findByQuizResultResultIdWithQuestion(quizResultId).stream()
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
        QuizAnswer answer = quizAnswerRepository.findByQuizResultResultIdAndQuestionQuestionId(quizResultId, questionId);
        if (answer == null) return;

        Question q = answer.getQuestion();
        String qType = questionTypeOverride != null ? questionTypeOverride : q.getQuestionType();
        String userAnswer = answer.getAnsweredOptions();

        try {
            // Bước 1: Chuyển giọng nói (Nếu là Speaking)
            if ("SPEAKING".equals(qType)) {
                try {
                    userAnswer = groqClient.transcribe(userAnswer);
                } catch (Exception e) {
                    log.warn("STT Failed, continuing with empty text for grading.");
                    userAnswer = "[Âm thanh không thể xử lý]";
                    throw e; // Ném ra để catch bên ngoài xử lý fallback rubric
                }
            }

            // Bước 2: Gọi AI chấm điểm
            GradingResponse grading = groqClient.gradeWritingOrSpeaking(
                    q.getContent(), q.getSkill(),
                    q.getCefrLevel() != null ? q.getCefrLevel() : "B1",
                    userAnswer, qType, 9
            );

            // Lưu kết quả thành công
            answer.setAiScore(String.format("%.2f", grading.getDisplayScore()) + "/9");
            answer.setAiFeedback(grading.getFeedback());
            answer.setAiRubricJson(objectMapper.writeValueAsString(grading.getRubric()));
            answer.setAiGradingStatus("COMPLETED");
            answer.setPendingAiReview(false);
            quizAnswerRepository.save(answer);

        } catch (Exception e) {
            log.error("AI Grading Error for Q{}: {}", questionId, e.getMessage());

            // BƯỚC 3: FALLBACK - TẠO RUBRIC 0 ĐIỂM KHI CÓ LỖI
            String errorMsg = e.getMessage().contains("401") ? "Lỗi xác thực dịch vụ chấm điểm." : "Lỗi hệ thống xử lý bài làm.";
            String dummyRubric = createZeroRubric(qType, errorMsg);

            answer.setAiScore("0/9");
            answer.setAiFeedback("Hệ thống gặp sự cố khi chấm bài tự động. Giáo viên sẽ kiểm tra lại phần này. Chi tiết: " + errorMsg);
            answer.setAiRubricJson(dummyRubric);
            answer.setAiGradingStatus("COMPLETED"); // Vẫn để COMPLETED để hiện lên UI
            answer.setPendingAiReview(false);
            answer.setIsCorrect(false);
            quizAnswerRepository.save(answer);
        }

        if (shouldRecalculate) quizResultService.recalculateQuizResult(quizResultId);
    }

    // Hàm hỗ trợ tạo JSON Rubric trắng
    private String createZeroRubric(String type, String reason) {
        if ("WRITING".equals(type)) {
            return "{\"task_achievement\":{\"score\":0,\"max\":9,\"bandLabel\":\"0.0\",\"bandDescription\":\"" + reason + "\"},\"lexical_resource\":{\"score\":0,\"max\":9,\"bandLabel\":\"0.0\"},\"grammatical_range\":{\"score\":0,\"max\":9,\"bandLabel\":\"0.0\"},\"coherence_cohesion\":{\"score\":0,\"max\":9,\"bandLabel\":\"0.0\"}}";
        } else {
            return "{\"fluency_cohesion\":{\"score\":0,\"max\":9,\"bandLabel\":\"0.0\",\"bandDescription\":\"" + reason + "\"},\"lexical_resource\":{\"score\":0,\"max\":9,\"bandLabel\":\"0.0\"},\"grammatical_range\":{\"score\":0,\"max\":9,\"bandLabel\":\"0.0\"},\"pronunciation\":{\"score\":0,\"max\":9,\"bandLabel\":\"0.0\"}}";
        }
    }

}
