package com.example.DoAn.service.impl;

import com.example.DoAn.dto.request.QuestionGradingRequestDTO;
import com.example.DoAn.dto.request.QuizGradingRequestDTO;
import com.example.DoAn.dto.response.*;
import com.example.DoAn.model.*;
import com.example.DoAn.repository.*;
import com.example.DoAn.service.EmailService;
import com.example.DoAn.service.INotificationService;
import com.example.DoAn.service.LearningService;
import com.example.DoAn.service.QuizResultService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
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
    private final ClazzRepository clazzRepository;
    private final ObjectMapper objectMapper;
    private final com.example.DoAn.repository.SessionQuizRepository sessionQuizRepository;
    private final com.example.DoAn.service.LessonQuizService lessonQuizService;
    private final com.example.DoAn.service.GroqGradingService groqGradingService;
    private final QuizQuestionRepository quizQuestionRepository;
    private final EmailService emailService;
    private final INotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public QuizTakingDTO getQuizForTaking(Integer quizId, String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        Quiz quiz = quizRepository.findById(quizId).orElseThrow(() -> new RuntimeException("Quiz not found"));

        if (!"PUBLISHED".equals(quiz.getStatus())) {
            throw new RuntimeException("Quiz chưa được xuất bản");
        }

        // Tìm classId và sessionId để redirect đúng
        // Ưu tiên lấy từ session_quiz (buổi học N:N) — luôn đúng cho quiz gắn buổi học
        // Fallback sang quiz.getClazz() cho quiz gắn class trực tiếp
        Integer classId = null;
        Integer sessionId = null;

        List<com.example.DoAn.model.SessionQuiz> sqList = sessionQuizRepository.findAllByQuizId(quizId);
        if (!sqList.isEmpty()) {
            // Quiz có trong session_quiz
            // Tìm session mà user ĐANG ĐĂNG KÝ và quiz ĐANG MỞ
            com.example.DoAn.model.SessionQuiz openSq = sqList.stream()
                    .filter(sq -> Boolean.TRUE.equals(sq.getIsOpen()))
                    .filter(sq -> {
                        // Chỉ cho phép nếu quiz mở trong session thuộc class mà user đã đăng ký
                        if (sq.getSession() == null || sq.getSession().getClazz() == null) return false;
                        Integer clazzId = sq.getSession().getClazz().getClassId();
                        return registrationRepository.existsByUser_UserIdAndClazz_ClassIdAndStatusApproved(
                                user.getUserId(), clazzId);
                    })
                    .findFirst()
                    .orElse(null);

            if (openSq != null) {
                // Quiz đang mở trong session thuộc lớp user đã đăng ký
                classId = openSq.getSession().getClazz().getClassId();
                sessionId = openSq.getSession().getSessionId();
            } else {
                // Quiz không mở ở session nào mà user đã đăng ký
                throw new RuntimeException("Quiz hiện đang đóng. Vui lòng liên hệ giáo viên để mở quiz.");
            }
        } else if (quiz.getClazz() != null) {
            // Không có session_quiz → quiz gắn class trực tiếp → dùng quiz.getClazz()
            classId = quiz.getClazz().getClassId();
            // Kiểm tra quiz.isOpen
            boolean quizOpen = Boolean.TRUE.equals(quiz.getIsOpen());
            if (!quizOpen) {
                throw new RuntimeException("Quiz hiện đang đóng. Vui lòng liên hệ giáo viên để mở quiz.");
            }
        }

        // Kiểm tra thời điểm mở/đóng tự động theo lịch (openAt / closeAt)
        LocalDateTime now = LocalDateTime.now();
        if (quiz.getOpenAt() != null && now.isBefore(quiz.getOpenAt())) {
            throw new RuntimeException("Quiz chưa đến thời điểm mở bài. Vui lòng quay lại sau.");
        }
        if (quiz.getCloseAt() != null && now.isAfter(quiz.getCloseAt())) {
            throw new RuntimeException("Quiz đã đóng. Vui lòng liên hệ giáo viên.");
        }

        // Kiểm tra deadline cho ASSIGNMENT
        if ("COURSE_ASSIGNMENT".equals(quiz.getQuizCategory())) {
            if (quiz.getDeadline() != null && now.isAfter(quiz.getDeadline())) {
                throw new RuntimeException("Đã hết hạn nộp bài. Deadline: " + quiz.getDeadline());
            }
            // Kiểm tra enrollment
            boolean enrolled = false;
            if (quiz.getClazz() != null) {
                enrolled = registrationRepository.existsByUser_UserIdAndClazz_ClassIdAndStatusApproved(
                        user.getUserId(), quiz.getClazz().getClassId());
            }
            if (!enrolled && quiz.getCourse() != null) {
                enrolled = registrationRepository.existsByUser_UserIdAndCourse_CourseIdAndStatus(
                        user.getUserId(), quiz.getCourse().getCourseId(), "Approved");
            }
            if (!enrolled) throw new RuntimeException("User is not enrolled in this course");
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

        // Kiểm tra xem có attempt nào đang in progress/locked không
        Optional<QuizResult> lockedOpt = quizResultRepository.findByQuizQuizIdAndUser_EmailAndStatus(quizId, email, "LOCKED");
        if (lockedOpt.isPresent()) {
            throw new RuntimeException("Bài làm của bạn đang bị khóa do hành vi vi phạm nội quy. Vui lòng liên hệ giáo viên để được mở khóa.");
        }

        // Kiểm tra số lần đã làm so với giới hạn cho phép
        // Lưu ý: Loại trừ status 'IN_PROGRESS' để tránh bị tính là lượt làm bài khi đang bị vi phạm/đang làm.
        long attemptCount = quizResultRepository.countByQuizQuizIdAndUserUserIdAndStatusNot(quizId, user.getUserId(), "IN_PROGRESS");
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
            List<AnswerOptionPayloadDTO> matchRightOptionsDTO = new ArrayList<>();
            if (options != null && !options.isEmpty()) {
                if ("MATCHING".equals(q.getQuestionType())) {
                    List<AnswerOption> lefts = options.stream()
                            .filter(o -> o.getMatchTarget() != null && !o.getMatchTarget().isBlank())
                            .sorted(Comparator.comparingInt(a -> a.getOrderIndex() != null ? a.getOrderIndex() : 0))
                            .toList();
                    List<AnswerOption> rights = options.stream()
                            .filter(o -> o.getMatchTarget() == null || o.getMatchTarget().isBlank())
                            .sorted(Comparator.comparingInt(a -> a.getOrderIndex() != null ? a.getOrderIndex() : 0))
                            .toList();
                    Function<AnswerOption, AnswerOptionPayloadDTO> toDto = opt ->
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
                            .collect(Collectors.toList());
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
                    .options(noOptionsType ? new ArrayList<AnswerOptionPayloadDTO>() : optionsDTO)
                    .matchRightOptions(matchRightOptionsDTO)
                    .groupId(q.getQuestionGroup() != null ? q.getQuestionGroup().getGroupId() : null)
                    .passage(q.getQuestionGroup() != null ? q.getQuestionGroup().getGroupContent() : null)
                    .groupAudioUrl(q.getQuestionGroup() != null ? q.getQuestionGroup().getAudioUrl() : null)
                    .groupImageUrl(q.getQuestionGroup() != null ? q.getQuestionGroup().getImageUrl() : null)
                    .build();
        }).collect(Collectors.toList());

        return QuizTakingDTO.builder()
                .quizId(quiz.getQuizId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .timeLimitMinutes(quiz.getTimeLimitMinutes())
                .speakingTimeLimitSeconds(quiz.getSpeakingTimeLimitSeconds())
                .totalQuestions(quizQuestions.size())
                .questionOrder(quiz.getQuestionOrder())
                .questions(questionsDTO)
                .classId(classId)
                .sessionId(sessionId)
                .build();
    }

    @Override
    @Transactional
    public Integer submitQuiz(Integer quizId, String email, Map<Integer, Object> answers) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        Quiz quiz = quizRepository.findById(quizId).orElseThrow(() -> new RuntimeException("Quiz not found"));

        // Kiểm tra số lần đã làm so với giới hạn cho phép
        long attemptCount = quizResultRepository.countByQuizQuizIdAndUserUserIdAndStatusNot(quizId, user.getUserId(), "IN_PROGRESS");
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
                    .pendingAiReview(
                            "WRITING".equals(qType) || "SPEAKING".equals(qType)
                                    ? true : false)
                    .aiGradingStatus(
                            "WRITING".equals(qType) || "SPEAKING".equals(qType)
                                    ? "PENDING" : null)
                    .audioUrl(
                            "SPEAKING".equals(qType)
                                    ? extractRawString(answeredOptionsJson)
                                    : null)
                    .build();
            qa = quizAnswerRepository.save(qa);

            // Fire async AI grading for SPEAKING/WRITING questions
            // Use TransactionSynchronization to ensure the QuizAnswer is committed to DB
            // before the async task tries to find it (prevents race condition where
            // @Async thread starts before HTTP transaction commits).
            if ("WRITING".equals(qType) || "SPEAKING".equals(qType)) {
                final Integer capturedResultId = quizResult.getResultId();
                final Integer capturedQuestionId = q.getQuestionId();
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            log.info("[SUBMIT] Scheduling AI grading → resultId={}, questionId={}, type={}",
                                    capturedResultId, capturedQuestionId, qType);
                            groqGradingService.fireAndForgetForQuizAnswer(capturedResultId, capturedQuestionId);
                            log.info("[SUBMIT] AI grading scheduled OK → resultId={}, questionId={}",
                                    capturedResultId, capturedQuestionId);
                        } catch (Exception e) {
                            // Non-critical: AI grading failure should not block submission
                            log.warn("Failed to fire AI grading for question {} in result {}: {}",
                                    capturedQuestionId, capturedResultId, e.getMessage());
                        }
                    }
                });
            }
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

        // ── Send email + in-app notification to student (quiz auto-graded) ────
        if (passed != null && quiz.getPassScore() != null) {
            String studentName = user.getFullName() != null ? user.getFullName() : "";
            String quizTitle = quiz.getTitle() != null ? quiz.getTitle() : "";
            String className = quiz.getClazz() != null ? quiz.getClazz().getClassName() : "";
            String scoreStr = score + "/" + maxScoreAvailable;
            String passedStatus = Boolean.TRUE.equals(passed) ? "Dat" : "Khong dat";

            if (user.getEmail() != null && !user.getEmail().isBlank()) {
                emailService.sendQuizResultEmail(user.getEmail(), studentName,
                        quizTitle, className, scoreStr, passedStatus);
            }
            if (user.getUserId() != null) {
                notificationService.sendQuizResult(
                        Long.valueOf(user.getUserId()),
                        quizTitle, className, scoreStr, passedStatus);
            }
        }

        if (Boolean.TRUE.equals(passed)) {
            lessonRepository.findByQuizId(quizId).ifPresent(l -> {
                learningService.markLessonCompleted(l.getLessonId(), email);
            });
        }

        // Update lesson quiz progress + unlock next quiz
        if (quiz.getLesson() != null) {
            try {
                double scorePercent = correctRate.setScale(2, RoundingMode.HALF_UP).doubleValue();
                lessonQuizService.updateProgressAfterSubmit(
                        quiz.getLesson().getLessonId(), quizId, user.getUserId(),
                        scorePercent, Boolean.TRUE.equals(passed));
            } catch (Exception e) {
                // Non-critical: log but don't fail the submission
            }
        }

        return quizResult.getResultId();
    }

    @Override
    @Transactional(readOnly = true)
    public QuizResultDetailDTO getQuizResult(Integer resultId, String email) {
        QuizResult qr = quizResultRepository.findById(resultId).orElseThrow(() -> new RuntimeException("Result not found"));
        boolean isStudent = qr.getUser().getEmail().equals(email);

        // Teacher có quyền xem nếu: tạo quiz HOẶC được gán lớp quiz HOẶC phụ trách course quiz (qua lớp khác)
        Quiz quiz = qr.getQuiz();
        boolean isCreator = quiz.getUser() != null && quiz.getUser().getEmail().equals(email);
        boolean isAssignedTeacher = quiz.getClazz() != null
                && quiz.getClazz().getTeacher() != null
                && quiz.getClazz().getTeacher().getEmail().equals(email);
        boolean isCourseTeacher = false;
        if (quiz.getCourse() != null && !isAssignedTeacher) {
            User teacher = userRepository.findByEmail(email).orElse(null);
            if (teacher != null) {
                List<Clazz> teacherClasses = clazzRepository.findAllByTeacher_UserId(teacher.getUserId());
                isCourseTeacher = teacherClasses.stream()
                        .anyMatch(c -> c.getCourse() != null
                                && c.getCourse().getCourseId().equals(quiz.getCourse().getCourseId()));
            }
        }
        boolean isTeacher = isCreator || isAssignedTeacher || isCourseTeacher;

        if (!isStudent && !isTeacher) {
            throw new RuntimeException("Unauthorized");
        }

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
                String rawJson = userAns.getAnsweredOptions();
                try {
                    String qType = q.getQuestionType();
                    if ("MULTIPLE_CHOICE_SINGLE".equals(qType)) {
                        Integer selectedId = objectMapper.readValue(rawJson, Integer.class);
                        userAnswerDisplay = q.getAnswerOptions().stream()
                                .filter(o -> o.getAnswerOptionId().equals(selectedId))
                                .findFirst().map(AnswerOption::getTitle).orElse(rawJson);
                    } else if ("MULTIPLE_CHOICE_MULTI".equals(qType)) {
                        List<Integer> selectedIds = objectMapper.readValue(rawJson, new TypeReference<List<Integer>>(){});
                        List<String> titles = q.getAnswerOptions().stream()
                                .filter(o -> selectedIds.contains(o.getAnswerOptionId()))
                                .map(AnswerOption::getTitle)
                                .collect(Collectors.toList());
                        userAnswerDisplay = String.join(", ", titles);
                    } else if ("FILL_IN_BLANK".equals(qType)) {
                        userAnswerDisplay = objectMapper.readValue(rawJson, String.class);
                    } else if ("MATCHING".equals(qType)) {
                        Map<String, String> userMatch = objectMapper.readValue(rawJson, new TypeReference<Map<String, String>>(){});
                        List<String> matchDisplays = new ArrayList<>();
                        for (AnswerOption opt : q.getAnswerOptions()) {
                            String userTarget = userMatch.get(String.valueOf(opt.getAnswerOptionId()));
                            if (userTarget != null) {
                                matchDisplays.add(opt.getTitle() + " -> " + userTarget);
                            }
                        }
                        userAnswerDisplay = String.join(" | ", matchDisplays);
                    } else {
                        userAnswerDisplay = rawJson;
                    }
                } catch (Exception e) {
                    userAnswerDisplay = rawJson;
                }
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
                    .imageUrl(q.getImageUrl())
                    .audioUrl(q.getAudioUrl())
                    .options(optDTOs)
                    .answerId(userAns != null ? userAns.getAnswerId() : null)
                    .pointsAwarded(userAns != null ? userAns.getPointsAwarded() != null
                            ? userAns.getPointsAwarded().intValue() : null : null)
                    .teacherNote(userAns != null ? userAns.getTeacherNote() : null)
                    .aiScore(userAns != null ? userAns.getAiScore() : null)
                    .aiFeedback(userAns != null ? userAns.getAiFeedback() : null)
                    .aiRubricJson(userAns != null ? userAns.getAiRubricJson() : null)
                    .studentAudioUrl(userAns != null ? userAns.getAudioUrl() : null)
                    .aiGradingStatus(userAns != null ? userAns.getAiGradingStatus() : null)
                    .teacherOverrideScore(userAns != null ? userAns.getTeacherOverrideScore() : null)
                    .build());
        }

        // Compute distinct skills present in this quiz's questions
        List<String> skillsPresent = quiz.getQuizQuestions().stream()
                .map(qq -> qq.getQuestion().getSkill())
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return QuizResultDetailDTO.builder()
                .resultId(qr.getResultId())
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
                .skillsPresent(skillsPresent)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<QuizResultPendingDTO> getPendingGradingList(String email, Integer classId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<QuizResult> resultPage = quizResultRepository.findPendingGradingForTeacher(email, classId, pageable);

        List<QuizResultPendingDTO> dtoList = resultPage.getContent().stream().map(qr -> QuizResultPendingDTO.builder()
                .resultId(qr.getResultId())
                .quizId(qr.getQuiz().getQuizId())
                .quizTitle(qr.getQuiz().getTitle())
                .studentName(qr.getUser().getFullName())
                .studentEmail(qr.getUser().getEmail())
                .submittedAt(qr.getSubmittedAt())
                .startedAt(qr.getStartedAt())
                .status(qr.getStatus())
                .violationLog(qr.getViolationLog())
                .isUnlockRequested(qr.getIsUnlockRequested())
                .studentAppealReason(qr.getStudentAppealReason())
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
    public PageResponse<QuizResultGradedDTO> getGradedResults(String email, Integer classId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<QuizResult> resultPage = quizResultRepository.findGradedForTeacher(email, classId, pageable);

        List<QuizResultGradedDTO> dtoList = resultPage.getContent().stream().map(qr -> {
            int maxScore = 0;
            if (qr.getQuiz() != null && qr.getQuiz().getQuizQuestions() != null) {
                for (QuizQuestion qq : qr.getQuiz().getQuizQuestions()) {
                    maxScore += qq.getPoints() != null ? qq.getPoints().intValue() : 1;
                }
            }
            int score = qr.getScore() != null ? qr.getScore() : 0;
            double percentage = maxScore > 0 ? (double) score / maxScore * 100 : 0;

            String quizType = "LESSON_QUIZ";
            if (qr.getQuiz() != null) {
                String cat = qr.getQuiz().getQuizCategory();
                if ("COURSE_ASSIGNMENT".equals(cat) || "MODULE_ASSIGNMENT".equals(cat)) {
                    quizType = "ASSIGNMENT";
                }
            }

            return QuizResultGradedDTO.builder()
                    .resultId(qr.getResultId())
                    .quizId(qr.getQuiz() != null ? qr.getQuiz().getQuizId() : null)
                    .quizTitle(qr.getQuiz() != null ? qr.getQuiz().getTitle() : "—")
                    .studentName(qr.getUser() != null ? qr.getUser().getFullName() : "—")
                    .studentEmail(qr.getUser() != null ? qr.getUser().getEmail() : "—")
                    .submittedAt(qr.getSubmittedAt())
                    .courseName(qr.getQuiz() != null && qr.getQuiz().getCourse() != null
                            ? qr.getQuiz().getCourse().getTitle() : null)
                    .quizType(quizType)
                    .score(score)
                    .maxScore(maxScore)
                    .percentage(Math.round(percentage * 10.0) / 10.0)
                    .passed(qr.getPassed())
                    .build();
        }).collect(Collectors.toList());

        return PageResponse.<QuizResultGradedDTO>builder()
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
    public PageResponse<QuizResultHistoryDTO> getStudentQuizHistory(String email, int page, int size, String category) {
        Pageable pageable = PageRequest.of(page, size);
        Page<QuizResult> resultPage = quizResultRepository.findByUserEmailAndCategory(email, category, null, pageable);

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
                    .quizCategory(qr.getQuiz() != null ? qr.getQuiz().getQuizCategory() : null)
                    .submittedAt(qr.getSubmittedAt())
                    .score(qr.getScore())
                    .maxScore(maxScore)
                    .passed(qr.getPassed())
                    .status(qr.getStatus())
                    .violationLog(qr.getViolationLog())
                    .isUnlockRequested(qr.getIsUnlockRequested())
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

        // Cho phép teacher đã tạo quiz HOẶC teacher được phân công lớp HOẶC teacher phụ trách course của quiz (qua lớp khác)
        boolean isCreator = quiz.getUser() != null && quiz.getUser().getEmail().equals(email);
        boolean isAssignedTeacher = quiz.getClazz() != null
                && quiz.getClazz().getTeacher() != null
                && quiz.getClazz().getTeacher().getEmail().equals(email);
        boolean isCourseTeacher = false;
        if (quiz.getCourse() != null && !isAssignedTeacher) {
            User teacher = userRepository.findByEmail(email).orElse(null);
            if (teacher != null) {
                List<Clazz> teacherClasses = clazzRepository.findAllByTeacher_UserId(teacher.getUserId());
                isCourseTeacher = teacherClasses.stream()
                        .anyMatch(c -> c.getCourse() != null
                                && c.getCourse().getCourseId().equals(quiz.getCourse().getCourseId()));
            }
        }
        if (!isCreator && !isAssignedTeacher && !isCourseTeacher) {
            throw new RuntimeException("Unauthorized: Bạn không có quyền chấm bài Quiz này");
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
                    // Also save teacherNote if available in gradingItems
                    gradingItems.stream()
                            .filter(i -> i.getQuestionId().equals(qId))
                            .findFirst()
                            .ifPresent(item -> {
                                if (item.getTeacherNote() != null) {
                                    ans.setTeacherNote(item.getTeacherNote());
                                }
                            });
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

        // ── Send email + in-app notification to student ──────────────────────
        if (qr.getUser() != null && passed != null) {
            User student = qr.getUser();
            String studentName = student.getFullName() != null ? student.getFullName() : "";
            String quizTitle = quiz.getTitle() != null ? quiz.getTitle() : "";
            String className = quiz.getClazz() != null ? quiz.getClazz().getClassName() : "";
            String finalScore = newScore + "/" + maxScoreAvailable;
            String passedStatus = Boolean.TRUE.equals(passed) ? "Dat" : "Khong dat";

            if (student.getEmail() != null && !student.getEmail().isBlank()) {
                emailService.sendManualGradingResultEmail(student.getEmail(), studentName,
                        quizTitle, className, finalScore, passedStatus);
            }
            if (student.getUserId() != null) {
                notificationService.sendManualGradingResult(
                        Long.valueOf(student.getUserId()),
                        quizTitle, className, finalScore, passedStatus);
            }
        }

        if (Boolean.TRUE.equals(passed)) {
            lessonRepository.findByQuizId(quiz.getQuizId()).ifPresent(l -> {
                learningService.markLessonCompleted(l.getLessonId(), qr.getUser().getEmail());
            });
        }

        // After grading, update lesson quiz progress + unlock next
        if (quiz.getLesson() != null) {
            try {
                double scorePercent = correctRate.setScale(2, RoundingMode.HALF_UP).doubleValue();
                lessonQuizService.updateProgressAfterSubmit(
                        quiz.getLesson().getLessonId(), quiz.getQuizId(), qr.getUser().getUserId(),
                        scorePercent, Boolean.TRUE.equals(passed));
            } catch (Exception e) {
                // Non-critical
            }
        }
    }

    /**
     * Extended grading: saves per-question points + teacherNote,
     * skillScores JSON, and overallNote.
     */
    @Override
    @Transactional
    public void gradeQuizResult(Integer resultId, QuizGradingRequestDTO request, String email) {
        QuizResult qr = quizResultRepository.findById(resultId)
                .orElseThrow(() -> new RuntimeException("Result not found"));
        Quiz quiz = qr.getQuiz();

        // Authorization check (same as legacy method)
        boolean isCreator = quiz.getUser() != null && quiz.getUser().getEmail().equals(email);
        boolean isAssignedTeacher = quiz.getClazz() != null
                && quiz.getClazz().getTeacher() != null
                && quiz.getClazz().getTeacher().getEmail().equals(email);
        boolean isCourseTeacher = false;
        if (quiz.getCourse() != null && !isAssignedTeacher) {
            User teacher = userRepository.findByEmail(email).orElse(null);
            if (teacher != null) {
                List<Clazz> teacherClasses = clazzRepository.findAllByTeacher_UserId(teacher.getUserId());
                isCourseTeacher = teacherClasses.stream()
                        .anyMatch(c -> c.getCourse() != null
                                && c.getCourse().getCourseId().equals(quiz.getCourse().getCourseId()));
            }
        }
        if (!isCreator && !isAssignedTeacher && !isCourseTeacher) {
            throw new RuntimeException("Unauthorized: Bạn không có quyền chấm bài Quiz này");
        }

        List<QuestionGradingRequestDTO> gradingItems = request.getGradingItems();

        // Build a map of questionId → item
        Map<Integer, QuestionGradingRequestDTO> gradeMap = gradingItems != null
                ? gradingItems.stream().collect(Collectors.toMap(
                        QuestionGradingRequestDTO::getQuestionId, Function.identity()))
                : Collections.emptyMap();

        int newScore = qr.getScore() != null ? qr.getScore() : 0;

        for (QuizAnswer ans : qr.getQuizAnswers()) {
            Integer qId = ans.getQuestion().getQuestionId();
            QuestionGradingRequestDTO item = gradeMap.get(qId);
            String qType = ans.getQuestion().getQuestionType();

            if ("WRITING".equals(qType) || "SPEAKING".equals(qType)) {
                if (item != null) {
                    ans.setPointsAwarded(item.getPointsAwarded());
                    ans.setTeacherNote(item.getTeacherNote());
                    ans.setIsCorrect(item.getPointsAwarded() != null
                            && item.getPointsAwarded().compareTo(BigDecimal.ZERO) > 0);
                    newScore += item.getPointsAwarded().intValue();
                }
            } else {
                // MC/FILL/MATCH — teacher can override
                if (item != null && item.getPointsAwarded() != null) {
                    ans.setPointsAwarded(item.getPointsAwarded());
                    ans.setTeacherNote(item.getTeacherNote());
                    ans.setIsCorrect(item.getPointsAwarded().compareTo(BigDecimal.ZERO) > 0);
                    // Recalculate: subtract old score contribution then add new
                    int oldPts = ans.getPointsAwarded() != null
                            ? ans.getPointsAwarded().intValue()
                            : (Boolean.TRUE.equals(ans.getIsCorrect())
                                    ? (quizQuestionRepository.findByQuizQuizIdAndQuestionQuestionId(
                                            quiz.getQuizId(), qId)
                                            .map(qq -> qq.getPoints() != null ? qq.getPoints().intValue() : 1)
                                            .orElse(1)) : 0);
                    newScore = newScore - oldPts + item.getPointsAwarded().intValue();
                }
            }
            ans.setPendingAiReview(false);
            quizAnswerRepository.save(ans);
        }

        // Save skillScores and overallNote
        if (request.getSkillScores() != null) {
            try {
                qr.setSectionScores(objectMapper.writeValueAsString(request.getSkillScores()));
            } catch (JsonProcessingException e) {
                // Ignore
            }
        }

        // Recalculate total
        int maxScoreAvailable = quiz.getQuizQuestions().stream()
                .mapToInt(qq -> qq.getPoints() != null ? qq.getPoints().intValue() : 1)
                .sum();

        BigDecimal correctRate = maxScoreAvailable > 0
                ? BigDecimal.valueOf(100.0 * newScore / maxScoreAvailable)
                : BigDecimal.ZERO;

        Boolean passed = quiz.getPassScore() != null
                ? correctRate.compareTo(quiz.getPassScore()) >= 0
                : true;

        qr.setScore(newScore);
        qr.setCorrectRate(correctRate.setScale(2, RoundingMode.HALF_UP));
        qr.setPassed(passed);
        quizResultRepository.save(qr);

        // ── Send email + in-app notification to student ──────────────────────
        if (qr.getUser() != null && passed != null) {
            User student = qr.getUser();
            String studentName = student.getFullName() != null ? student.getFullName() : "";
            String quizTitle = quiz.getTitle() != null ? quiz.getTitle() : "";
            String className = quiz.getClazz() != null ? quiz.getClazz().getClassName() : "";
            String finalScore = newScore + "/" + maxScoreAvailable;
            String passedStatus = Boolean.TRUE.equals(passed) ? "Dat" : "Khong dat";

            if (student.getEmail() != null && !student.getEmail().isBlank()) {
                emailService.sendManualGradingResultEmail(student.getEmail(), studentName,
                        quizTitle, className, finalScore, passedStatus);
            }
            if (student.getUserId() != null) {
                notificationService.sendManualGradingResult(
                        Long.valueOf(student.getUserId()),
                        quizTitle, className, finalScore, passedStatus);
            }
        }

        if (Boolean.TRUE.equals(passed)) {
            lessonRepository.findByQuizId(quiz.getQuizId())
                    .ifPresent(l -> learningService.markLessonCompleted(l.getLessonId(), qr.getUser().getEmail()));
        }

        if (quiz.getLesson() != null) {
            try {
                double scorePercent = correctRate.setScale(2, RoundingMode.HALF_UP).doubleValue();
                lessonQuizService.updateProgressAfterSubmit(
                        quiz.getLesson().getLessonId(), quiz.getQuizId(), qr.getUser().getUserId(),
                        scorePercent, Boolean.TRUE.equals(passed));
            } catch (Exception e) {
                // Non-critical
            }
        }
    }

    /** Strip JSON quotes from a JSON-encoded string like "\"https://...\"" */
    private String extractRawString(String json) {
        if (json == null) return null;
        json = json.trim();
        if (json.startsWith("\"") && json.endsWith("\"")) {
            return json.substring(1, json.length() - 1)
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
        }
        return json;
    }

    @Override
    @Transactional
    public Map<String, Object> handleViolation(Integer quizId, String email, String reason) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        Quiz quiz = quizRepository.findById(quizId).orElseThrow(() -> new RuntimeException("Quiz not found"));

        QuizResult qr = quizResultRepository.findByQuizQuizIdAndUser_EmailAndStatus(quizId, email, "IN_PROGRESS")
                .orElseGet(() -> {
                    Optional<QuizResult> locked = quizResultRepository.findByQuizQuizIdAndUser_EmailAndStatus(quizId, email, "LOCKED");
                    return locked.orElseGet(() -> QuizResult.builder()
                            .quiz(quiz)
                            .user(user)
                            .status("IN_PROGRESS")
                            .startedAt(LocalDateTime.now())
                            .violationCount(0)
                            .build());
                });

        if ("LOCKED".equals(qr.getStatus())) {
            return Map.of("status", "LOCKED", "violationCount", qr.getViolationCount());
        }

        int count = (qr.getViolationCount() != null ? qr.getViolationCount() : 0) + 1;
        qr.setViolationCount(count);

        String timeNow = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String newLogEntry = String.format("[%s] Vi phạm lần %d: %s", timeNow, count, reason);
        String existingLog = qr.getViolationLog();
        qr.setViolationLog(existingLog == null ? newLogEntry : existingLog + "\n" + newLogEntry);

        if (count >= 3) {
            qr.setStatus("LOCKED");
            qr.setSubmittedAt(LocalDateTime.now());
            quizResultRepository.save(qr);

            // ── Notify Teacher via Email ────────
            try {
                User teacher = null;
                if (quiz.getClazz() != null && quiz.getClazz().getTeacher() != null) {
                    teacher = quiz.getClazz().getTeacher();
                } else {
                    List<com.example.DoAn.model.SessionQuiz> sqList = sessionQuizRepository.findAllByQuizId(quizId);
                    for (com.example.DoAn.model.SessionQuiz sq : sqList) {
                        if (sq.getSession() != null && sq.getSession().getClazz() != null) {
                            Integer classId = sq.getSession().getClazz().getClassId();
                            if (registrationRepository.existsByUser_UserIdAndClazz_ClassIdAndStatusApproved(user.getUserId(), classId)) {
                                teacher = sq.getSession().getClazz().getTeacher();
                                break;
                            }
                        }
                    }
                }
                if (teacher != null) {
                    emailService.sendQuizLockedEmail(teacher.getEmail(), teacher.getFullName(),
                            user.getFullName(), quiz.getTitle(), reason, count, qr.getViolationLog());
                }
            } catch (Exception e) {
                log.warn("Lỗi khi gửi email vi phạm: {}", e.getMessage());
            }

            return Map.of("status", "LOCKED", "violationCount", count);
        } else {
            quizResultRepository.save(qr);
            return Map.of("status", "WARNING", "violationCount", count);
        }
    }

    @Override
    @Transactional
    public void unlockQuiz(Integer resultId) {
        QuizResult qr = quizResultRepository.findById(resultId).orElseThrow(() -> new RuntimeException("Not found"));
        if ("LOCKED".equals(qr.getStatus())) {
            quizResultRepository.delete(qr);
        }
    }

    @Override
    @Transactional
    public void requestUnlock(Integer resultId, String email, String reason) {
        QuizResult qr = quizResultRepository.findById(resultId).orElseThrow(() -> new RuntimeException("Not found"));
        if (!qr.getUser().getEmail().equals(email)) throw new RuntimeException("Unauthorized");
        if (!"LOCKED".equals(qr.getStatus())) throw new RuntimeException("Bài thi không bị khóa");
        
        qr.setIsUnlockRequested(true);
        qr.setStudentAppealReason(reason);
        quizResultRepository.save(qr);
    }
}
