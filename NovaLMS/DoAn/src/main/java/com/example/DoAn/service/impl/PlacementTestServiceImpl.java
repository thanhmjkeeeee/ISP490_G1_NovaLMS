package com.example.DoAn.service.impl;

import com.example.DoAn.dto.request.PlacementTestSubmissionDTO;
import com.example.DoAn.dto.response.*;
import com.example.DoAn.model.*;
import com.example.DoAn.repository.*;
import com.example.DoAn.service.GroqGradingService;
import com.example.DoAn.service.PlacementTestService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlacementTestServiceImpl implements PlacementTestService {

    private final QuizRepository quizRepository;
    private final PlacementTestResultRepository resultRepository;
    private final HybridSessionRepository hybridSessionRepository;
    private final HybridSessionQuizRepository hybridSessionQuizRepository;
    private final GroqGradingService groqGradingService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public QuizTakingDTO getQuizForTaking(Integer quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài kiểm tra đầu vào."));
        if (!"PUBLISHED".equals(quiz.getStatus()) || !"ENTRY_TEST".equals(quiz.getQuizCategory())) {
            throw new RuntimeException("Bài kiểm tra không hợp lệ hoặc chưa được công bố.");
        }

        List<QuizQuestion> quizQuestions = quiz.getQuizQuestions();
        if ("RANDOM".equals(quiz.getQuestionOrder())) {
            List<QuizQuestion> shuffled = new ArrayList<>(quizQuestions);
            java.util.Collections.shuffle(shuffled);
            quizQuestions = shuffled;
        }

        List<QuizQuestionPayloadDTO> questionsDTO = quizQuestions.stream().map(qq -> {
            Question q = qq.getQuestion();
            List<AnswerOption> options = q.getAnswerOptions();
            if ("RANDOM".equals(quiz.getQuestionOrder())) {
                List<AnswerOption> shuffledOptions = new ArrayList<>(options);
                java.util.Collections.shuffle(shuffledOptions);
                options = shuffledOptions;
            }

            List<AnswerOptionPayloadDTO> optionsDTO = new ArrayList<>();
            List<AnswerOptionPayloadDTO> matchRightOptionsDTO = new ArrayList<>();
            if (options != null && !options.isEmpty()) {
                if ("MATCHING".equals(q.getQuestionType())) {
                    List<AnswerOption> lefts = options.stream()
                            .filter(o -> o.getMatchTarget() != null && !o.getMatchTarget().isBlank())
                            .sorted((a, b) -> Integer.compare(
                                    a.getOrderIndex() != null ? a.getOrderIndex() : 0,
                                    b.getOrderIndex() != null ? b.getOrderIndex() : 0))
                            .toList();
                    List<AnswerOption> rights = options.stream()
                            .filter(o -> o.getMatchTarget() == null || o.getMatchTarget().isBlank())
                            .sorted((a, b) -> Integer.compare(
                                    a.getOrderIndex() != null ? a.getOrderIndex() : 0,
                                    b.getOrderIndex() != null ? b.getOrderIndex() : 0))
                            .toList();
                    java.util.function.Function<AnswerOption, AnswerOptionPayloadDTO> toDto = opt ->
                            AnswerOptionPayloadDTO.builder()
                                    .answerOptionId(opt.getAnswerOptionId())
                                    .title(opt.getTitle())
                                    .matchTarget(opt.getMatchTarget())
                                    .build();
                    optionsDTO = lefts.stream().map(toDto).toList();
                    matchRightOptionsDTO = rights.stream().map(toDto).toList();
                } else {
                    optionsDTO = options.stream()
                            .map(opt -> AnswerOptionPayloadDTO.builder()
                                    .answerOptionId(opt.getAnswerOptionId())
                                    .title(opt.getTitle())
                                    .matchTarget(opt.getMatchTarget())
                                    .build())
                            .collect(java.util.stream.Collectors.toList());
                }
            }

            boolean noOptionsType = "WRITING".equals(q.getQuestionType()) || "SPEAKING".equals(q.getQuestionType()) || "FILL_IN_BLANK".equals(q.getQuestionType());

            return QuizQuestionPayloadDTO.builder()
                    .questionId(q.getQuestionId())
                    .content(q.getContent())
                    .questionType(q.getQuestionType())
                    .skill(q.getSkill())
                    .cefrLevel(q.getCefrLevel())
                    .points(qq.getPoints() != null ? qq.getPoints().intValue() : 1)
                    .imageUrl(q.getImageUrl())
                    .audioUrl(q.getAudioUrl())
                    .options(noOptionsType ? new ArrayList<>() : optionsDTO)
                    .matchRightOptions(matchRightOptionsDTO)
                    .build();
        }).collect(java.util.stream.Collectors.toList());

        return QuizTakingDTO.builder()
                .quizId(quiz.getQuizId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .timeLimitMinutes(quiz.getTimeLimitMinutes())
                .totalQuestions(quizQuestions.size())
                .questionOrder(quiz.getQuestionOrder())
                .questions(questionsDTO)
                .build();
    }

    @Override
    @Transactional
    public Integer submitPlacementTest(PlacementTestSubmissionDTO submission, String sessionId) {
        Quiz quiz = quizRepository.findById(submission.getQuizId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài kiểm tra."));

        // ── Validate WRITING/SPEAKING questions must have answers ──
        Map<Integer, Object> answers = submission.getAnswers();
        for (QuizQuestion qq : quiz.getQuizQuestions()) {
            Question q = qq.getQuestion();
            String qType = q.getQuestionType();
            if (!"WRITING".equals(qType) && !"SPEAKING".equals(qType)) continue;

            Object userAnswer = answers != null ? answers.get(q.getQuestionId()) : null;
            boolean isEmpty = userAnswer == null
                    || userAnswer.toString().trim().isEmpty()
                    || (userAnswer instanceof String && ((String) userAnswer).trim().isEmpty());

            if (isEmpty) {
                throw new RuntimeException(
                    ("WRITING".equals(qType) ? "Câu hỏi Viết" : "Câu hỏi Nói") +
                    " chưa có nội dung. Vui lòng nhập câu trả lời trước khi nộp bài.");
            }
        }

        int score = 0;
        int maxScoreAvailable = 0;
        int totalGradedQuestions = 0;

        PlacementTestResult result = PlacementTestResult.builder()
                .quiz(quiz)
                .guestSessionId(sessionId)
                .guestName(submission.getGuestName())
                .guestEmail(submission.getGuestEmail())
                .answers(new ArrayList<>())
                .hybridSessionId(submission.getHybridSessionId())
                .build();

        // answers already declared and validated above — reuse it
        for (QuizQuestion qq : quiz.getQuizQuestions()) {
            Question q = qq.getQuestion();
            Integer qId = q.getQuestionId();
            Object userAnswerObj = answers != null ? answers.get(qId) : null;
            String answeredOptionsJson = "";

            try {
                if (userAnswerObj != null) {
                    answeredOptionsJson = objectMapper.writeValueAsString(userAnswerObj);
                }
            } catch (JsonProcessingException e) {
                // Ignore
            }

            Boolean isCorrect = false;
            String qType = q.getQuestionType();

            // MC/FILL/MATCH: auto-grade immediately
            if (!"WRITING".equals(qType) && !"SPEAKING".equals(qType)) {
                totalGradedQuestions++;
                if (userAnswerObj != null && !userAnswerObj.toString().trim().isEmpty()) {
                    if ("MULTIPLE_CHOICE_SINGLE".equals(qType)) {
                        try {
                            Integer selectedId = Integer.valueOf(userAnswerObj.toString());
                            isCorrect = q.getAnswerOptions().stream()
                                    .anyMatch(opt -> opt.getAnswerOptionId().equals(selectedId) && Boolean.TRUE.equals(opt.getCorrectAnswer()));
                        } catch (NumberFormatException e) {
                             isCorrect = false;
                        }
                    } else if ("MULTIPLE_CHOICE_MULTI".equals(qType)) {
                        List<Integer> selectedIds;
                        if (userAnswerObj instanceof List) {
                            selectedIds = ((List<?>) userAnswerObj).stream().map(o -> Integer.valueOf(o.toString())).collect(Collectors.toList());
                        } else {
                            try {
                                selectedIds = List.of(Integer.valueOf(userAnswerObj.toString()));
                            } catch(NumberFormatException e) {
                                selectedIds = new ArrayList<>();
                            }
                        }
                        List<Integer> correctIds = q.getAnswerOptions().stream()
                                .filter(opt -> Boolean.TRUE.equals(opt.getCorrectAnswer()))
                                .map(AnswerOption::getAnswerOptionId).collect(Collectors.toList());
                        isCorrect = !selectedIds.isEmpty() && selectedIds.size() == correctIds.size() && selectedIds.containsAll(correctIds);
                    } else if ("FILL_IN_BLANK".equals(qType)) {
                        String userTxt = userAnswerObj.toString().trim();
                        isCorrect = q.getAnswerOptions().stream()
                                .anyMatch(opt -> Boolean.TRUE.equals(opt.getCorrectAnswer()) && (opt.getTitle() != null && opt.getTitle().trim().equalsIgnoreCase(userTxt)));
                    } else if ("MATCHING".equals(qType)) {
                        try {
                            Map<String, String> userMatch = objectMapper.convertValue(userAnswerObj, new TypeReference<Map<String, String>>(){});
                            boolean allCorrect = true;
                            for (AnswerOption opt : q.getAnswerOptions()) {
                                String userTarget = userMatch.get(String.valueOf(opt.getAnswerOptionId()));
                                if (userTarget == null || !userTarget.trim().equalsIgnoreCase(opt.getMatchTarget().trim())) {
                                    allCorrect = false;
                                    break;
                                }
                            }
                            isCorrect = allCorrect;
                        } catch (Exception e) {
                            isCorrect = false;
                        }
                    }
                }

                if (Boolean.TRUE.equals(isCorrect)) {
                    score += qq.getPoints() != null ? qq.getPoints().intValue() : 1;
                }
                maxScoreAvailable += qq.getPoints() != null ? qq.getPoints().intValue() : 1;
            }

            boolean isAiQuestion = "WRITING".equals(qType) || "SPEAKING".equals(qType);
            PlacementTestAnswer qa = PlacementTestAnswer.builder()
                    .placementTestResult(result)
                    .question(q)
                    .answeredOptions(answeredOptionsJson)
                    .isCorrect(isAiQuestion ? null : isCorrect)
                    .pendingAiReview(isAiQuestion)
                    .build();
            result.getAnswers().add(qa);
        }

        BigDecimal correctRate = maxScoreAvailable > 0 ? BigDecimal.valueOf(100.0 * score / maxScoreAvailable) : BigDecimal.ZERO;
        double rateScore = correctRate.doubleValue();

        String suggestedLevel = calculateCEFRLevel(rateScore);
        Boolean passed = quiz.getPassScore() == null || correctRate.compareTo(quiz.getPassScore()) >= 0;

        result.setScore(score);
        result.setCorrectRate(correctRate.setScale(2, RoundingMode.HALF_UP));
        result.setSuggestedLevel(suggestedLevel);
        result.setPassed(passed);

        PlacementTestResult savedResult = resultRepository.save(result);

        // ── Hybrid mode: link result back to session ──
        if (submission.getHybridSessionId() != null && submission.getQuizIndex() != null) {
            try {
                var hybridSession = hybridSessionRepository.findById(submission.getHybridSessionId()).orElse(null);
                if (hybridSession != null) {
                    hybridSession.setCompletedQuizzes(hybridSession.getCompletedQuizzes() + 1);
                    hybridSessionRepository.save(hybridSession);

                    List<HybridSessionQuiz> sqList = hybridSession.getSessionQuizzes();
                    if (sqList != null && submission.getQuizIndex() >= 1 && submission.getQuizIndex() <= sqList.size()) {
                        HybridSessionQuiz sq = sqList.get(submission.getQuizIndex() - 1);
                        sq.setStatus("COMPLETED");
                        sq.setPlacementTestResultId(savedResult.getId());
                        hybridSessionQuizRepository.save(sq);
                    }
                }
            } catch (Exception e) {
                // Non-critical: log but don't fail the submission
            }
        }

        // ── Fire async AI grading for WRITING/SPEAKING AFTER transaction commits ──
        final Integer resultId = savedResult.getId();
        for (PlacementTestAnswer answer : savedResult.getAnswers()) {
            String qType = answer.getQuestion().getQuestionType();
            if ("WRITING".equals(qType) || "SPEAKING".equals(qType)) {
                final Integer questionId = answer.getQuestion().getQuestionId();
                final String type = qType;
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        groqGradingService.fireAndForget(resultId, questionId, type);
                    }
                });
            }
        }

        return savedResult.getId();
    }

    private String calculateCEFRLevel(double rateScore) {
        if (rateScore <= 20) return "A1";
        if (rateScore <= 40) return "A2";
        if (rateScore <= 60) return "B1";
        if (rateScore <= 75) return "B2";
        if (rateScore <= 90) return "C1";
        return "C2";
    }
}
