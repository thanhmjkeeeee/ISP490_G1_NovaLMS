package com.example.DoAn.service.impl;

import com.example.DoAn.dto.request.QuestionGradingRequestDTO;
import com.example.DoAn.dto.response.*;
import com.example.DoAn.model.*;
import com.example.DoAn.repository.*;
import com.example.DoAn.service.LearningService;
import com.example.DoAn.service.QuizResultService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizResultServiceImpl implements QuizResultService {

    private final QuizRepository quizRepository;
    private final QuizResultRepository quizResultRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final LearningService learningService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public QuizTakingDTO getQuizForTaking(Integer quizId, String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        Quiz quiz = quizRepository.findById(quizId).orElseThrow(() -> new RuntimeException("Quiz not found"));

        if (!"PUBLISHED".equals(quiz.getStatus())) {
            throw new RuntimeException("Quiz is not published");
        }

        if ("COURSE_QUIZ".equals(quiz.getQuizCategory())) {
            boolean enrolled = false;
            // Ưu tiên kiểm tra enrollment theo CLASS (quiz gắn với class)
            if (quiz.getClazz() != null) {
                enrolled = registrationRepository.existsByUser_UserIdAndClazz_ClassIdAndStatusApproved(
                        user.getUserId(), quiz.getClazz().getClassId());
            }
            // Fallback: kiểm tra enrollment theo COURSE
            if (!enrolled && quiz.getCourse() != null) {
                enrolled = registrationRepository.existsByUser_UserIdAndCourse_CourseIdAndStatus(
                        user.getUserId(), quiz.getCourse().getCourseId(), "Approved");
            }
            if (!enrolled) throw new RuntimeException("User is not enrolled in this course");
        }

        // Kiểm tra số lần đã làm so với giới hạn cho phép
        long attemptCount = quizResultRepository.countByQuizQuizId(quizId);
        if (quiz.getMaxAttempts() != null && attemptCount >= quiz.getMaxAttempts()) {
            throw new RuntimeException("Bạn đã hết lượt làm bài. Số lần làm tối đa: " + quiz.getMaxAttempts());
        }

        List<QuizQuestion> quizQuestions = quiz.getQuizQuestions();
        if ("RANDOM".equals(quiz.getQuestionOrder())) {
            List<QuizQuestion> shuffled = new ArrayList<>(quizQuestions);
            Collections.shuffle(shuffled);
            quizQuestions = shuffled;
        }

        List<QuizQuestionPayloadDTO> questionsDTO = quizQuestions.stream().map(qq -> {
            Question q = qq.getQuestion();
            List<AnswerOption> options = q.getAnswerOptions();
            if ("RANDOM".equals(quiz.getQuestionOrder())) {
                List<AnswerOption> shuffledOptions = new ArrayList<>(options);
                Collections.shuffle(shuffledOptions);
                options = shuffledOptions;
            }

            List<AnswerOptionPayloadDTO> optionsDTO = new ArrayList<>();
            if (options != null && !options.isEmpty()) {
                optionsDTO = options.stream()
                        .map(opt -> AnswerOptionPayloadDTO.builder()
                                .answerOptionId(opt.getAnswerOptionId())
                                .title(opt.getTitle())
                                .matchTarget(opt.getMatchTarget())
                                .build())
                        .collect(Collectors.toList());
            }

            boolean noOptionsType = "WRITING".equals(q.getQuestionType()) || "SPEAKING".equals(q.getQuestionType()) || "FILL_IN_BLANK".equals(q.getQuestionType());

            return QuizQuestionPayloadDTO.builder()
                    .questionId(q.getQuestionId())
                    .content(q.getContent())
                    .questionType(q.getQuestionType())
                    .skill(q.getSkill())
                    .cefrLevel(q.getCefrLevel())
                    .points(qq.getPoints() != null ? qq.getPoints().intValue() : 1)
                    .options(noOptionsType ? new ArrayList<AnswerOptionPayloadDTO>() : optionsDTO)
                    .build();
        }).collect(Collectors.toList());

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
    public Integer submitQuiz(Integer quizId, String email, Map<Integer, Object> answers) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        Quiz quiz = quizRepository.findById(quizId).orElseThrow(() -> new RuntimeException("Quiz not found"));

        // Kiểm tra số lần đã làm so với giới hạn cho phép
        long attemptCount = quizResultRepository.countByQuizQuizId(quizId);
        if (quiz.getMaxAttempts() != null && attemptCount >= quiz.getMaxAttempts()) {
            throw new RuntimeException("Bạn đã hết lượt làm bài. Số lần làm tối đa: " + quiz.getMaxAttempts());
        }

        int score = 0;
        int totalGradedQuestions = 0;
        int maxScoreAvailable = 0;
        boolean hasPendingReview = false;

        QuizResult quizResult = QuizResult.builder()
                .quiz(quiz)
                .user(user)
                .submittedAt(LocalDateTime.now())
                .build();
        quizResult = quizResultRepository.save(quizResult);

        for (QuizQuestion qq : quiz.getQuizQuestions()) {
            Question q = qq.getQuestion();
            Integer qId = q.getQuestionId();
            Object userAnswerObj = answers != null ? answers.get(qId) : null;
            String answeredOptionsJson = "";
            try {
                if(userAnswerObj != null) {
                    answeredOptionsJson = objectMapper.writeValueAsString(userAnswerObj);
                }
            } catch (JsonProcessingException e) {
                // Ignore
            }

            Boolean isCorrect = false;
            String qType = q.getQuestionType();

            if ("WRITING".equals(qType) || "SPEAKING".equals(qType)) {
                isCorrect = null;
                hasPendingReview = true;
            } else {
                totalGradedQuestions++;
                if (userAnswerObj != null) {
                    if ("MULTIPLE_CHOICE_SINGLE".equals(qType)) {
                        Integer selectedId = Integer.valueOf(userAnswerObj.toString());
                        isCorrect = q.getAnswerOptions().stream()
                                .anyMatch(opt -> opt.getAnswerOptionId().equals(selectedId) && Boolean.TRUE.equals(opt.getCorrectAnswer()));
                    } else if ("MULTIPLE_CHOICE_MULTI".equals(qType)) {
                        List<Integer> selectedIds;
                        if (userAnswerObj instanceof List) {
                            selectedIds = ((List<?>) userAnswerObj).stream().map(o -> Integer.valueOf(o.toString())).collect(Collectors.toList());
                        } else {
                            selectedIds = List.of(Integer.valueOf(userAnswerObj.toString()));
                        }
                        List<Integer> correctIds = q.getAnswerOptions().stream()
                                .filter(opt -> Boolean.TRUE.equals(opt.getCorrectAnswer()))
                                .map(AnswerOption::getAnswerOptionId).collect(Collectors.toList());
                        isCorrect = selectedIds.size() == correctIds.size() && selectedIds.containsAll(correctIds);
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
            }

            if (Boolean.TRUE.equals(isCorrect)) {
                score += qq.getPoints() != null ? qq.getPoints().intValue() : 1;
            }
            if (!"WRITING".equals(qType) && !"SPEAKING".equals(qType)) {
                maxScoreAvailable += qq.getPoints() != null ? qq.getPoints().intValue() : 1;
            }

            QuizAnswer qa = QuizAnswer.builder()
                    .quizResult(quizResult)
                    .question(q)
                    .answeredOptions(answeredOptionsJson)
                    .isCorrect(isCorrect)
                    .build();
            quizAnswerRepository.save(qa);
        }

        BigDecimal correctRate = totalGradedQuestions > 0 ? BigDecimal.valueOf(100.0 * score / maxScoreAvailable) : BigDecimal.ZERO;
        Boolean passed = null;
        if (!hasPendingReview && quiz.getPassScore() != null) {
            passed = correctRate.compareTo(quiz.getPassScore()) >= 0;
        }

        quizResult.setScore(score);
        quizResult.setCorrectRate(correctRate.setScale(2, RoundingMode.HALF_UP));
        quizResult.setPassed(passed);
        quizResultRepository.save(quizResult);

        if (Boolean.TRUE.equals(passed)) {
            lessonRepository.findByQuizId(quizId).ifPresent(l -> {
                learningService.markLessonCompleted(l.getLessonId(), email);
            });
        }

        return quizResult.getResultId();
    }

    @Override
    @Transactional(readOnly = true)
    public QuizResultDetailDTO getQuizResult(Integer resultId, String email) {
        QuizResult qr = quizResultRepository.findById(resultId).orElseThrow(() -> new RuntimeException("Result not found"));
        boolean isStudent = qr.getUser().getEmail().equals(email);
        boolean isTeacher = qr.getQuiz().getUser() != null && qr.getQuiz().getUser().getEmail().equals(email);
        
        if (!isStudent && !isTeacher) {
            throw new RuntimeException("Unauthorized");
        }

        Quiz quiz = qr.getQuiz();
        List<QuizAnswer> answers = qr.getQuizAnswers();
        boolean showAnswer = isTeacher || Boolean.TRUE.equals(quiz.getShowAnswerAfterSubmit());

        Integer totalPoints = 0;
        List<QuestionResultDTO> questionsRes = new ArrayList<>();

        for (QuizQuestion qq : quiz.getQuizQuestions()) {
            Question q = qq.getQuestion();
            int points = qq.getPoints() != null ? qq.getPoints().intValue() : 1;
            totalPoints += points;

            QuizAnswer userAns = answers.stream().filter(a -> a.getQuestion().getQuestionId().equals(q.getQuestionId())).findFirst().orElse(null);
            
            String userAnswerDisplay = "";
            if (userAns != null && userAns.getAnsweredOptions() != null) {
                userAnswerDisplay = userAns.getAnsweredOptions();
            }

            String correctAnswerDisplay = null;
            if (showAnswer) {
                List<String> corrLogs = new ArrayList<>();
                if ("MULTIPLE_CHOICE_SINGLE".equals(q.getQuestionType()) || "MULTIPLE_CHOICE_MULTI".equals(q.getQuestionType())) {
                    for (AnswerOption op : q.getAnswerOptions()) {
                        if (Boolean.TRUE.equals(op.getCorrectAnswer())) {
                            corrLogs.add(op.getTitle());
                        }
                    }
                    correctAnswerDisplay = String.join(", ", corrLogs);
                } else if ("FILL_IN_BLANK".equals(q.getQuestionType())) {
                    for(AnswerOption op: q.getAnswerOptions()) {
                         if(Boolean.TRUE.equals(op.getCorrectAnswer())) corrLogs.add(op.getTitle());
                    }
                    correctAnswerDisplay = String.join(" OR ", corrLogs);
                } else if("MATCHING".equals(q.getQuestionType())) {
                    for(AnswerOption op: q.getAnswerOptions()) {
                        corrLogs.add(op.getTitle() + " -> " + op.getMatchTarget());
                    }
                    correctAnswerDisplay = String.join(" | ", corrLogs);
                }
            }

            List<AnswerOptionDTO> optDTOs = q.getAnswerOptions().stream().map(opt -> AnswerOptionDTO.builder()
                    .answerOptionId(opt.getAnswerOptionId())
                    .title(opt.getTitle())
                    .matchTarget(opt.getMatchTarget())
                    .isCorrect(showAnswer ? opt.getCorrectAnswer() : null)
                    .build()).collect(Collectors.toList());

            questionsRes.add(QuestionResultDTO.builder()
                    .questionId(q.getQuestionId())
                    .content(q.getContent())
                    .questionType(q.getQuestionType())
                    .skill(q.getSkill())
                    .points(points)
                    .isCorrect(userAns != null ? userAns.getIsCorrect() : null)
                    .userAnswerDisplay(userAnswerDisplay)
                    .correctAnswerDisplay(correctAnswerDisplay)
                    .explanation(showAnswer ? q.getExplanation() : null)
                    .options(optDTOs)
                    .build());
        }

        return QuizResultDetailDTO.builder()
                .quizId(quiz.getQuizId())
                .quizTitle(quiz.getTitle())
                .courseName(quiz.getCourse() != null ? quiz.getCourse().getTitle() : null)
                .submittedAt(qr.getSubmittedAt())
                .score(qr.getScore())
                .totalPoints(totalPoints)
                .correctRate(qr.getCorrectRate() != null ? qr.getCorrectRate().doubleValue() : null)
                .passed(qr.getPassed())
                .showAnswer(showAnswer)
                .passScoreDescription(quiz.getPassScore() != null ? "Passing score: " + quiz.getPassScore().toString() + "%" : "No passing score")
                .questions(questionsRes)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<QuizResultPendingDTO> getPendingGradingList(String email, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<QuizResult> resultPage = quizResultRepository.findByPassedIsNullAndQuiz_User_EmailOrderBySubmittedAtAsc(email, pageable);

        List<QuizResultPendingDTO> dtoList = resultPage.getContent().stream().map(qr -> QuizResultPendingDTO.builder()
                .resultId(qr.getResultId())
                .quizId(qr.getQuiz().getQuizId())
                .quizTitle(qr.getQuiz().getTitle())
                .studentName(qr.getUser().getFullName())
                .studentEmail(qr.getUser().getEmail())
                .submittedAt(qr.getSubmittedAt())
                .courseName(qr.getQuiz().getCourse() != null ? qr.getQuiz().getCourse().getTitle() : null)
                .build()).collect(Collectors.toList());

        return PageResponse.<QuizResultPendingDTO>builder()
                .items(dtoList)
                .pageNo(resultPage.getNumber())
                .pageSize(resultPage.getSize())
                .totalPages(resultPage.getTotalPages())
                .totalElements(resultPage.getTotalElements())
                .last(resultPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<QuizResultHistoryDTO> getStudentQuizHistory(String email, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<QuizResult> resultPage = quizResultRepository.findByUserEmailOrderBySubmittedAtDesc(email, pageable);

        List<QuizResultHistoryDTO> list = resultPage.getContent().stream().map(qr -> {
            int maxScore = 0;
            if (qr.getQuiz() != null && qr.getQuiz().getQuizQuestions() != null) {
                for (QuizQuestion qq : qr.getQuiz().getQuizQuestions()) {
                    maxScore += qq.getPoints() != null ? qq.getPoints().intValue() : 1;
                }
            }
            return QuizResultHistoryDTO.builder()
                    .resultId(qr.getResultId())
                    .quizId(qr.getQuiz() != null ? qr.getQuiz().getQuizId() : null)
                    .quizTitle(qr.getQuiz() != null ? qr.getQuiz().getTitle() : "Unknown")
                    .courseName(qr.getQuiz() != null && qr.getQuiz().getCourse() != null ? qr.getQuiz().getCourse().getTitle() : null)
                    .submittedAt(qr.getSubmittedAt())
                    .score(qr.getScore())
                    .maxScore(maxScore)
                    .passed(qr.getPassed())
                    .build();
        }).collect(Collectors.toList());

        return PageResponse.<QuizResultHistoryDTO>builder()
                .items(list)
                .pageNo(resultPage.getNumber())
                .pageSize(resultPage.getSize())
                .totalPages(resultPage.getTotalPages())
                .totalElements(resultPage.getTotalElements())
                .last(resultPage.isLast())
                .build();
    }

    @Override
    @Transactional
    public void gradeQuizResult(Integer resultId, List<QuestionGradingRequestDTO> gradingItems, String email) {
        QuizResult qr = quizResultRepository.findById(resultId).orElseThrow(() -> new RuntimeException("Result not found"));
        Quiz quiz = qr.getQuiz();

        if (quiz.getUser() == null || !quiz.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Unauthorized: Bạn không phải người tạo bài Quiz này");
        }

        if (qr.getPassed() != null) {
            throw new RuntimeException("Bài quiz này đã được chấm xong");
        }

        Map<Integer, BigDecimal> gradeMap = gradingItems.stream()
                .collect(Collectors.toMap(QuestionGradingRequestDTO::getQuestionId, QuestionGradingRequestDTO::getPointsAwarded));

        int newScore = qr.getScore() != null ? qr.getScore() : 0;
        
        for (QuizAnswer ans : qr.getQuizAnswers()) {
            if ("WRITING".equals(ans.getQuestion().getQuestionType()) || "SPEAKING".equals(ans.getQuestion().getQuestionType())) {
                Integer qId = ans.getQuestion().getQuestionId();
                if (gradeMap.containsKey(qId)) {
                    BigDecimal awarded = gradeMap.get(qId);
                    ans.setIsCorrect(awarded.compareTo(BigDecimal.ZERO) > 0);
                    newScore += awarded.intValue();
                    quizAnswerRepository.save(ans);
                }
            }
        }

        int maxScoreAvailable = 0;
        for (QuizQuestion qq : quiz.getQuizQuestions()) {
            maxScoreAvailable += qq.getPoints() != null ? qq.getPoints().intValue() : 1;
        }

        BigDecimal correctRate = maxScoreAvailable > 0 ? BigDecimal.valueOf(100.0 * newScore / maxScoreAvailable) : BigDecimal.ZERO;
        Boolean passed = null;
        if (quiz.getPassScore() != null) {
            passed = correctRate.compareTo(quiz.getPassScore()) >= 0;
        } else {
            passed = true;
        }

        qr.setScore(newScore);
        qr.setCorrectRate(correctRate.setScale(2, RoundingMode.HALF_UP));
        qr.setPassed(passed);
        quizResultRepository.save(qr);

        if (Boolean.TRUE.equals(passed)) {
            lessonRepository.findByQuizId(quiz.getQuizId()).ifPresent(l -> {
                learningService.markLessonCompleted(l.getLessonId(), qr.getUser().getEmail());
            });
        }
    }
}
