package com.example.DoAn.service.impl;

import com.example.DoAn.dto.request.QuestionGradingRequestDTO;
import com.example.DoAn.dto.request.QuizGradingRequestDTO;
import com.example.DoAn.dto.request.QuizItemGradingRequestDTO;
import com.example.DoAn.dto.response.*;
import com.example.DoAn.model.*;
import com.example.DoAn.repository.*;
import com.example.DoAn.service.EmailService;
import com.example.DoAn.service.INotificationService;
import com.example.DoAn.service.LearningService;
import com.example.DoAn.service.QuizResultService;
import com.example.DoAn.util.IELTSScoreMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import com.example.DoAn.service.GroqGradingService;
import com.example.DoAn.service.LessonQuizService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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
@Transactional
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
    private final SessionQuizRepository sessionQuizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final EmailService emailService;
    private final INotificationService notificationService;
    private final com.example.DoAn.service.IAIPromptConfigService aiPromptConfigService;

    @Autowired
    @Lazy
    private LessonQuizService lessonQuizService;

    @Autowired
    @Lazy
    private GroqGradingService groqGradingService;

    @Override
    @Transactional(readOnly = true)
    public QuizTakingDTO getQuizForTaking(Integer quizId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài kiểm tra"));

        if (!"PUBLISHED".equals(quiz.getStatus())) {
            throw new RuntimeException("Quiz chưa được xuất bản");
        }

        // Tìm clas sId và sessionId để redirect đúng
        // Ưu tiên lấy từ session_quiz (buổi học N:N) — luôn đúng cho quiz gắn buổi học
        // Fallback sang quiz.getClazz() cho quiz gắn class trực tiếp
        Integer classId = null;
        Integer sessionId = null;

        com.example.DoAn.model.SessionQuiz openSq = null;
        List<com.example.DoAn.model.SessionQuiz> sqList = sessionQuizRepository.findAllByQuizId(quizId);
        if (!sqList.isEmpty()) {
            // Quiz có trong session_quiz
            // Tìm session mà user ĐANG ĐĂNG KÝ và quiz ĐANG MỞ
            openSq = sqList.stream()
                    .filter(sq -> Boolean.TRUE.equals(sq.getIsOpen()))
                    .filter(sq -> {
                        // Chỉ cho phép nếu quiz mở trong session thuộc class mà user đã đăng ký
                        if (sq.getSession() == null || sq.getSession().getClazz() == null)
                            return false;
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

        // Kiểm tra xem giáo viên có đang "mở cưỡng ép" (Manual Open) hay không
        boolean isForceOpen = (openSq != null && Boolean.TRUE.equals(openSq.getIsOpen()));

        // Kiểm tra thời điểm mở/đóng tự động theo lịch (openAt / closeAt)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime effectiveOpenAt = (openSq != null && openSq.getOpenAt() != null) ? openSq.getOpenAt()
                : quiz.getOpenAt();
        LocalDateTime effectiveCloseAt = (openSq != null && openSq.getCloseAt() != null) ? openSq.getCloseAt()
                : quiz.getCloseAt();

        // Nếu giáo viên đã chủ động Mở (isOpen = true), bỏ qua kiểm tra thời gian
        if (!isForceOpen) {
            if (effectiveOpenAt != null && now.isBefore(effectiveOpenAt)) {
                throw new RuntimeException("Bài tập chưa đến giờ mở (Mở lúc: "
                        + effectiveOpenAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + ")");
            }
            if (effectiveCloseAt != null && now.isAfter(effectiveCloseAt)) {
                throw new RuntimeException("Bài tập đã đóng (Đóng lúc: "
                        + effectiveCloseAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                        + "). Vui lòng liên hệ giáo viên.");
            }
        }

        // Kiểm tra deadline cho ASSIGNMENT
        if ("COURSE_ASSIGNMENT".equals(quiz.getQuizCategory())) {
            LocalDateTime effectiveDeadline = (openSq != null && openSq.getDeadline() != null) ? openSq.getDeadline()
                    : quiz.getDeadline();
            if (!isForceOpen && effectiveDeadline != null && now.isAfter(effectiveDeadline)) {
                throw new RuntimeException("Đã hết hạn nộp bài. Deadline: " + effectiveDeadline);
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
            if (!enrolled)
                throw new RuntimeException("Bạn chưa đăng ký khóa học này");
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
            if (!enrolled)
                throw new RuntimeException("Bạn chưa đăng ký khóa học này");
        }

        // Kiểm tra xem có attempt nào đang in progress/locked không
        Optional<QuizResult> lockedOpt = quizResultRepository.findByQuizQuizIdAndUser_EmailAndStatus(quizId, email,
                "LOCKED");
        if (lockedOpt.isPresent()) {
            throw new RuntimeException(
                    "Bài làm của bạn đang bị khóa do hành vi vi phạm nội quy. Vui lòng liên hệ giáo viên để được mở khóa.");
        }

        // Kiểm tra số lần đã làm so với giới hạn cho phép
        // Lưu ý: Loại trừ status 'IN_PROGRESS' để tránh bị tính là lượt làm bài khi
        // đang bị vi phạm/đang làm.
        long attemptCount = quizResultRepository.countByQuizQuizIdAndUserUserIdAndStatusNot(quizId, user.getUserId(),
                "IN_PROGRESS");
        if (quiz.getMaxAttempts() != null && attemptCount >= quiz.getMaxAttempts()) {
            throw new RuntimeException("Bạn đã hết lượt làm bài. Số lần làm tối đa: " + quiz.getMaxAttempts());
        }

        // CHẶN LÀM LẠI KHI ĐANG CHỜ CHẤM ĐIỂM
        Optional<QuizResult> latestResultOpt = quizResultRepository
                .findFirstByQuizQuizIdAndUserUserIdOrderByStartedAtDesc(quizId, user.getUserId());
        if (latestResultOpt.isPresent()) {
            QuizResult latest = latestResultOpt.get();
            if (latest.getPassed() == null && !"IN_PROGRESS".equals(latest.getStatus())
                    && !"LOCKED".equals(latest.getStatus())) {
                throw new RuntimeException(
                        "Bài làm trước đó của bạn đang chờ giáo viên chấm điểm. Vui lòng đợi kết quả trước khi làm lại.");
            }
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
                    Function<AnswerOption, AnswerOptionPayloadDTO> toDto = opt -> AnswerOptionPayloadDTO.builder()
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

            boolean noOptionsType = "WRITING".equals(q.getQuestionType()) || "SPEAKING".equals(q.getQuestionType())
                    || "FILL_IN_BLANK".equals(q.getQuestionType());

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

        int maxAttempts = quiz.getMaxAttempts() != null ? quiz.getMaxAttempts() : 0;
        int attemptsLeft = maxAttempts > 0 ? (int) Math.max(0, maxAttempts - attemptCount) : -1;

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
                .canRetake(maxAttempts == 0 || attemptCount < maxAttempts)
                .attemptsLeft(attemptsLeft)
                .maxAttempts(maxAttempts > 0 ? maxAttempts : null)
                .build();
    }

    @Override
    @Transactional
    public Integer submitQuiz(Integer quizId, String email, Map<Integer, Object> answers) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài kiểm tra"));

        // Kiểm tra số lần đã làm so với giới hạn cho phép
        long attemptCount = quizResultRepository.countByQuizQuizIdAndUserUserIdAndStatusNot(quizId, user.getUserId(),
                "IN_PROGRESS");
        if (quiz.getMaxAttempts() != null && attemptCount >= quiz.getMaxAttempts()) {
            throw new RuntimeException("Bạn đã hết lượt làm bài. Số lần làm tối đa: " + quiz.getMaxAttempts());
        }

        BigDecimal score = BigDecimal.ZERO;
        int totalGradedQuestions = 0;
        BigDecimal maxScoreAvailable = BigDecimal.ZERO;
        boolean hasPendingReview = false;

        QuizResult quizResult = QuizResult.builder()
                .quiz(quiz)
                .user(user)
                .submittedAt(LocalDateTime.now())
                .status("SUBMITTED")
                .build();
        quizResult = quizResultRepository.save(quizResult);

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

            if ("WRITING".equals(qType) || "SPEAKING".equals(qType)) {
                isCorrect = null;
                hasPendingReview = true;
            } else {
                totalGradedQuestions++;
                if (userAnswerObj != null) {
                    if ("MULTIPLE_CHOICE_SINGLE".equals(qType)) {
                        Integer selectedId = Integer.valueOf(userAnswerObj.toString());
                        isCorrect = q.getAnswerOptions().stream()
                                .anyMatch(opt -> opt.getAnswerOptionId().equals(selectedId)
                                        && Boolean.TRUE.equals(opt.getCorrectAnswer()));
                    } else if ("MULTIPLE_CHOICE_MULTI".equals(qType)) {
                        List<Integer> selectedIds;
                        if (userAnswerObj instanceof List) {
                            selectedIds = ((List<?>) userAnswerObj).stream().map(o -> Integer.valueOf(o.toString()))
                                    .collect(Collectors.toList());
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
                                .anyMatch(opt -> Boolean.TRUE.equals(opt.getCorrectAnswer())
                                        && (opt.getTitle() != null && opt.getTitle().trim().equalsIgnoreCase(userTxt)));
                    } else if ("MATCHING".equals(qType)) {
                        try {
                            Map<String, String> userMatch = objectMapper.convertValue(userAnswerObj,
                                    new TypeReference<Map<String, String>>() {
                                    });
                            boolean allCorrect = true;
                            for (AnswerOption opt : q.getAnswerOptions()) {
                                String userTarget = userMatch.get(String.valueOf(opt.getAnswerOptionId()));
                                if (userTarget == null
                                        || !userTarget.trim().equalsIgnoreCase(opt.getMatchTarget().trim())) {
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

            BigDecimal qPoints = qq.getPoints() != null ? qq.getPoints() : BigDecimal.ONE;
            BigDecimal awardedPoints = BigDecimal.ZERO;

            if (Boolean.TRUE.equals(isCorrect)) {
                awardedPoints = qPoints;
                score = score.add(awardedPoints);
            }
            if (!"WRITING".equals(qType) && !"SPEAKING".equals(qType)) {
                maxScoreAvailable = maxScoreAvailable.add(qPoints);
            }

            QuizAnswer qa = QuizAnswer.builder()
                    .quizResult(quizResult)
                    .question(q)
                    .answeredOptions(answeredOptionsJson)
                    .isCorrect(isCorrect)
                    .pointsAwarded(!"WRITING".equals(qType) && !"SPEAKING".equals(qType) ? awardedPoints : null)
                    .pendingAiReview(
                            "WRITING".equals(qType) || "SPEAKING".equals(qType)
                                    ? true
                                    : false)
                    .aiGradingStatus(
                            "WRITING".equals(qType) || "SPEAKING".equals(qType)
                                    ? "PENDING"
                                    : null)
                    .audioUrl(
                            "SPEAKING".equals(qType)
                                    ? extractRawString(answeredOptionsJson)
                                    : null)
                    .build();
            qa = quizAnswerRepository.save(qa);

        }

        // Fire async BATCH AI grading for SPEAKING/WRITING questions
        if (hasPendingReview) {
            final Integer capturedResultId = quizResult.getResultId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        log.info("[SUBMIT] Scheduling BATCH AI grading → resultId={}", capturedResultId);
                        groqGradingService.processBatchAIForQuiz(capturedResultId);
                    } catch (Exception e) {
                        log.warn("Failed to fire batch AI grading for result {}: {}", capturedResultId, e.getMessage());
                    }
                }
            });
        }

        // Apply IELTS logic
        recalculateQuizResult(quizResult.getResultId());

        // Refresh for email data
        quizResult = quizResultRepository.findById(quizResult.getResultId()).orElse(quizResult);
        Boolean passed = quizResult.getPassed();

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
                BigDecimal correctRate = maxScoreAvailable.compareTo(BigDecimal.ZERO) > 0
                        ? score.multiply(new BigDecimal("100")).divide(maxScoreAvailable, 4, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

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
        QuizResult qr = quizResultRepository.findById(resultId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kết quả bài làm"));
        boolean isStudent = qr.getUser().getEmail().equals(email);

        // Teacher có quyền xem nếu: tạo quiz HOẶC được gán lớp quiz HOẶC phụ trách
        // course quiz (qua lớp khác)
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
            throw new RuntimeException("Không được phép thực hiện.");
        }

        List<QuizAnswer> answers = qr.getQuizAnswers();
        boolean showAnswer = isTeacher || Boolean.TRUE.equals(quiz.getShowAnswerAfterSubmit());

        Double totalPoints = 0.0;
        List<QuestionResultDTO> questionsRes = new ArrayList<>();

        for (QuizQuestion qq : quiz.getQuizQuestions()) {
            Question q = qq.getQuestion();
            double points = qq.getPoints() != null ? qq.getPoints().doubleValue() : 1.0;
            totalPoints += points;

            QuizAnswer userAns = answers.stream().filter(a -> a.getQuestion().getQuestionId().equals(q.getQuestionId()))
                    .findFirst().orElse(null);

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
                        List<Integer> selectedIds = objectMapper.readValue(rawJson, new TypeReference<List<Integer>>() {
                        });
                        List<String> titles = q.getAnswerOptions().stream()
                                .filter(o -> selectedIds.contains(o.getAnswerOptionId()))
                                .map(AnswerOption::getTitle)
                                .collect(Collectors.toList());
                        userAnswerDisplay = String.join(", ", titles);
                    } else if ("FILL_IN_BLANK".equals(qType)) {
                        userAnswerDisplay = objectMapper.readValue(rawJson, String.class);
                    } else if ("MATCHING".equals(qType)) {
                        Map<String, String> userMatch = objectMapper.readValue(rawJson,
                                new TypeReference<Map<String, String>>() {
                                });
                        List<String> matchDisplays = new ArrayList<>();
                        for (AnswerOption opt : q.getAnswerOptions()) {
                            String userTarget = userMatch.get(String.valueOf(opt.getAnswerOptionId()));
                            if (userTarget != null) {
                                matchDisplays.add(opt.getTitle() + " -> " + userTarget);
                            }
                        }
                        userAnswerDisplay = String.join(" | ", matchDisplays);
                    } else if ("SPEAKING".equals(qType) || "WRITING".equals(qType)) {
                        userAnswerDisplay = objectMapper.readValue(rawJson, String.class);
                        if (userAnswerDisplay == null)
                            userAnswerDisplay = "";
                    } else {
                        userAnswerDisplay = rawJson;
                    }
                } catch (Exception e) {
                    userAnswerDisplay = rawJson;
                }
            }

            // Parse student selections for option-based questions
            Set<Integer> selectedIds = new HashSet<>();
            if (userAns != null && userAns.getAnsweredOptions() != null) {
                try {
                    String raw = userAns.getAnsweredOptions();
                    if ("MULTIPLE_CHOICE_SINGLE".equals(q.getQuestionType())) {
                        selectedIds.add(objectMapper.readValue(raw, Integer.class));
                    } else if ("MULTIPLE_CHOICE_MULTI".equals(q.getQuestionType())) {
                        List<Integer> ids = objectMapper.readValue(raw, new TypeReference<List<Integer>>() {
                        });
                        if (ids != null)
                            selectedIds.addAll(ids);
                    }
                } catch (Exception e) {
                    // Ignore parsing errors for non-option types
                }
            }

            String correctAnswerDisplay = null;
            if (showAnswer) {
                List<String> corrLogs = new ArrayList<>();
                if ("MULTIPLE_CHOICE_SINGLE".equals(q.getQuestionType())
                        || "MULTIPLE_CHOICE_MULTI".equals(q.getQuestionType())) {
                    for (AnswerOption op : q.getAnswerOptions()) {
                        if (Boolean.TRUE.equals(op.getCorrectAnswer())) {
                            corrLogs.add(op.getTitle());
                        }
                    }
                    correctAnswerDisplay = String.join(", ", corrLogs);
                } else if ("FILL_IN_BLANK".equals(q.getQuestionType())) {
                    for (AnswerOption op : q.getAnswerOptions()) {
                        if (Boolean.TRUE.equals(op.getCorrectAnswer()))
                            corrLogs.add(op.getTitle());
                    }
                    correctAnswerDisplay = String.join(" OR ", corrLogs);
                } else if ("MATCHING".equals(q.getQuestionType())) {
                    for (AnswerOption op : q.getAnswerOptions()) {
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
                    .isSelected(selectedIds.contains(opt.getAnswerOptionId()))
                    .build()).collect(Collectors.toList());

            // Fix: Fallback for auto-graded questions that missed points_awarded in DB
            Double pointsAwarded = null;
            if (userAns != null) {
                if (userAns.getPointsAwarded() != null) {
                    pointsAwarded = userAns.getPointsAwarded().doubleValue();
                } else if (Boolean.TRUE.equals(userAns.getIsCorrect())) {
                    // Fallback to full points if marked correct but pointsAwarded is null
                    pointsAwarded = points;
                } else if (Boolean.FALSE.equals(userAns.getIsCorrect())) {
                    pointsAwarded = 0.0;
                }
            }

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
                    .pointsAwarded(pointsAwarded)
                    .teacherNote(userAns != null ? userAns.getTeacherNote() : null)
                    .aiScore(userAns != null ? userAns.getAiScore() : null)
                    .aiFeedback(userAns != null ? userAns.getAiFeedback() : null)
                    .aiRubricJson(userAns != null ? userAns.getAiRubricJson() : null)
                    .studentAudioUrl(userAns != null ? userAns.getAudioUrl() : null)
                    .aiGradingStatus(userAns != null ? userAns.getAiGradingStatus() : null)
                    .teacherOverrideScore(userAns != null ? userAns.getTeacherOverrideScore() : null)
                    .writingTaskAchievement(userAns != null ? userAns.getWritingTaskAchievement() : null)
                    .writingCoherenceCohesion(userAns != null ? userAns.getWritingCoherenceCohesion() : null)
                    .writingLexicalResource(userAns != null ? userAns.getWritingLexicalResource() : null)
                    .writingGrammarAccuracy(userAns != null ? userAns.getWritingGrammarAccuracy() : null)
                    .build());
        }

        // Compute distinct skills present in this quiz's questions
        List<String> skillsPresent = quiz.getQuizQuestions().stream()
                .map(qq -> qq.getQuestion().getSkill())
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // Count used attempts for this student
        long usedAttempts = quizResultRepository.countByQuizQuizIdAndUserUserIdAndStatusNot(
                quiz.getQuizId(), qr.getUser().getUserId(), "IN_PROGRESS");

        Map<String, Double> sectionScores = null;
        if (qr.getSectionScores() != null && !qr.getSectionScores().isBlank()) {
            try {
                sectionScores = objectMapper.readValue(qr.getSectionScores(), new TypeReference<Map<String, Double>>() {
                });
            } catch (Exception e) {
                log.warn("Failed to parse section scores for result {}: {}", qr.getResultId(), e.getMessage());
            }
        }

        QuizCategory category = QuizCategory.fromValue(quiz.getQuizCategory());
        boolean isAssignment = category != null && category.isAssignment();

        return QuizResultDetailDTO.builder()
                .resultId(qr.getResultId())
                .quizId(quiz.getQuizId())
                .quizTitle(quiz.getTitle())
                .studentName(qr.getUser().getFullName())
                .className(quiz.getClazz() != null ? quiz.getClazz().getClassName()
                        : registrationRepository
                                .findByUser_UserIdAndCourse_CourseIdAndStatus(qr.getUser().getUserId(),
                                        quiz.getCourse().getCourseId(), "Approved")
                                .map(reg -> reg.getClazz() != null ? reg.getClazz().getClassName()
                                        : "Lớp học đã đăng ký")
                                .orElse("Lớp học đã đăng ký"))
                .courseName(quiz.getCourse() != null ? quiz.getCourse().getTitle() : null)
                .submittedAt(qr.getSubmittedAt())
                .score(qr.getScore() != null ? qr.getScore().doubleValue() : 0.0)
                .totalPoints(totalPoints)
                .correctRate(qr.getCorrectRate() != null ? qr.getCorrectRate().doubleValue() : null)
                .overallBand(qr.getOverallBand() != null ? qr.getOverallBand().doubleValue() : null)
                .passed(qr.getPassed())
                .showAnswer(showAnswer)
                .passScoreDescription(
                        quiz.getPassScore() != null ? "Điểm đạt: " + quiz.getPassScore().toString() + "%"
                                : "Không yêu cầu điểm đạt")
                .questions(questionsRes)
                .skillsPresent(skillsPresent)
                .maxAttempts(quiz.getMaxAttempts())
                .usedAttempts(usedAttempts)
                .canRetake(isStudent && (quiz.getMaxAttempts() == null || usedAttempts < quiz.getMaxAttempts()))
                .status(qr.getStatus())
                .sectionScores(sectionScores)
                .quizCategory(quiz.getQuizCategory())
                .isAssignment(isAssignment)
                .criteriaLabels(fetchCriteriaLabels((quiz.getCourse() != null && quiz.getCourse().getLevelTag() != null)
                        ? quiz.getCourse().getLevelTag()
                        : "B2"))
                .build();
    }

    private String mapCefrToBucket(String cefr) {
        if (cefr == null)
            return "advanced";
        String c = cefr.toUpperCase();
        if (c.contains("A1") || c.contains("A2") || c.contains("BEGINNER"))
            return "beginner";
        if (c.contains("B1") || c.contains("B2") || c.contains("INTERMEDIATE"))
            return "intermediate";
        return "advanced";
    }

    private Map<String, String> fetchCriteriaLabels(String cefrLevel) {
        Map<String, String> labels = new LinkedHashMap<>();
        // Defaults
        labels.put("ta", "Task Achievement");
        labels.put("cc", "Coherence and cohesion");
        labels.put("lr", "Lexical resource");
        labels.put("gra", "Grammatical range and accuracy");

        try {
            String bucket = mapCefrToBucket(cefrLevel);
            AIPromptConfig config = aiPromptConfigService.getConfigByBucket(bucket);
            if (config != null && config.getWritingRubricJson() != null) {
                com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(config.getWritingRubricJson());
                if (root.has("task_achievement"))
                    labels.put("ta", root.path("task_achievement").path("label").asText("Task Achievement"));
                if (root.has("coherence_cohesion"))
                    labels.put("cc", root.path("coherence_cohesion").path("label").asText("Coherence & Cohesion"));
                if (root.has("lexical_resource"))
                    labels.put("lr", root.path("lexical_resource").path("label").asText("Lexical Resource"));
                if (root.has("grammatical_range"))
                    labels.put("gra", root.path("grammatical_range").path("label").asText("Grammar Range & Accuracy"));
            }
        } catch (Exception e) {
            // keep defaults
        }
        return labels;
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
                .violationCount(qr.getViolationCount())
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
    public PageResponse<QuizResultPendingDTO> getUnlockRequests(String email, Integer classId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<QuizResult> resultPage = quizResultRepository.findUnlockRequestsForTeacher(email, classId, pageable);

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
                .violationCount(qr.getViolationCount())
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
                            ? qr.getQuiz().getCourse().getTitle()
                            : null)
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
    public PageResponse<QuizResultHistoryDTO> getStudentQuizHistory(String email, int page, int size, String category,
            String keyword) {
        Pageable pageable = PageRequest.of(page, size);
        Page<QuizResult> resultPage = quizResultRepository.findByUserEmailAndCategory(email, category, keyword,
                pageable);

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
                    .courseName(qr.getQuiz() != null && qr.getQuiz().getCourse() != null
                            ? qr.getQuiz().getCourse().getTitle()
                            : null)
                    .quizCategory(qr.getQuiz() != null ? qr.getQuiz().getQuizCategory() : null)
                    .submittedAt(qr.getSubmittedAt())
                    .startedAt(qr.getStartedAt())
                    .score(qr.getScore())
                    .maxScore(maxScore)
                    .overallBand(qr.getOverallBand())
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
    public Double gradeQuizResult(Integer resultId, List<QuestionGradingRequestDTO> gradingItems, String email) {
        QuizResult qr = quizResultRepository.findById(resultId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kết quả bài làm"));
        Quiz quiz = qr.getQuiz();

        // Cho phép teacher đã tạo quiz HOẶC teacher được phân công lớp HOẶC teacher phụ
        // trách course của quiz (qua lớp khác)
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
            throw new RuntimeException("Bạn không có quyền chấm bài kiểm tra này.");
        }

        if (qr.getPassed() != null) {
            throw new RuntimeException("Bài quiz này đã được chấm xong");
        }

        Map<Integer, BigDecimal> gradeMap = gradingItems.stream()
                .collect(Collectors.toMap(QuestionGradingRequestDTO::getQuestionId,
                        QuestionGradingRequestDTO::getPointsAwarded));

        int newScore = 0;

        for (QuizAnswer ans : qr.getQuizAnswers()) {
            Integer qId = ans.getQuestion().getQuestionId();
            String qType = ans.getQuestion().getQuestionType();

            // Apply teacher grading
            if (gradeMap.containsKey(qId)) {
                BigDecimal awarded = gradeMap.get(qId);
                ans.setPointsAwarded(awarded);
                ans.setTeacherOverrideScore(awarded.toString());
                ans.setIsCorrect(awarded.compareTo(BigDecimal.ZERO) > 0);

                gradingItems.stream()
                        .filter(i -> i.getQuestionId().equals(qId))
                        .findFirst()
                        .ifPresent(item -> {
                            if (item.getTeacherNote() != null) {
                                ans.setTeacherNote(item.getTeacherNote());
                            }
                            // Save Writing criteria if present
                            if ("WRITING".equalsIgnoreCase(qType)) {
                                ans.setWritingTaskAchievement(item.getWritingTaskAchievement());
                                ans.setWritingCoherenceCohesion(item.getWritingCoherenceCohesion());
                                ans.setWritingLexicalResource(item.getWritingLexicalResource());
                                ans.setWritingGrammarAccuracy(item.getWritingGrammarAccuracy());

                                // Auto-calculate average if criteria are provided but pointsAwarded is
                                // null/handled elsewhere?
                                // Actually, in this method, pointsAwarded is already in the map.
                                // But if the teacher provided criteria, we might want to ensure pointsAwarded
                                // matches the average.
                                if (item.getWritingTaskAchievement() != null
                                        && item.getWritingCoherenceCohesion() != null
                                        && item.getWritingLexicalResource() != null
                                        && item.getWritingGrammarAccuracy() != null) {
                                    BigDecimal avg = item.getWritingTaskAchievement()
                                            .add(item.getWritingCoherenceCohesion())
                                            .add(item.getWritingLexicalResource())
                                            .add(item.getWritingGrammarAccuracy())
                                            .divide(new BigDecimal("4"), 2, RoundingMode.HALF_UP);
                                    ans.setPointsAwarded(avg);
                                    ans.setTeacherOverrideScore(avg.toString());
                                }
                            }
                        });
                ans.setPendingAiReview(false);
                quizAnswerRepository.save(ans);
            }

            // Sum up
            if (ans.getPointsAwarded() != null) {
                newScore += ans.getPointsAwarded().intValue();
            } else {
                int pts = quizQuestionRepository.findByQuizQuizIdAndQuestionQuestionId(quiz.getQuizId(), qId)
                        .map(qq -> qq.getPoints() != null ? qq.getPoints().intValue() : 1)
                        .orElse(1);
                if (Boolean.TRUE.equals(ans.getIsCorrect())) {
                    newScore += pts;
                }
            }
        }

        int maxScoreAvailable = 0;
        for (QuizQuestion qq : quiz.getQuizQuestions()) {
            maxScoreAvailable += qq.getPoints() != null ? qq.getPoints().intValue() : 1;
        }

        BigDecimal correctRate = maxScoreAvailable > 0 ? BigDecimal.valueOf(100.0 * newScore / maxScoreAvailable)
                : BigDecimal.ZERO;
        Boolean passed = null;
        if (quiz.getPassScore() != null) {
            passed = correctRate.compareTo(quiz.getPassScore()) >= 0;
        } else {
            passed = true;
        }

        // Recalculate everything using the centralized IELTS logic
        recalculateQuizResult(qr);

        qr.setStatus("GRADED"); // Mark as graded by teacher
        quizResultRepository.save(qr);

        // ── Send email + in-app notification to student ──────────────────────
        passed = qr.getPassed();
        // maxScoreAvailable already calculated above
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
        return (qr.getScore() != null) ? qr.getScore().doubleValue() : 0.0;
    }

    /**
     * Extended grading: saves per-question points + teacherNote,
     * skillScores JSON, and overallNote.
     */
    @Override
    @Transactional
    public Double gradeQuizResult(Integer resultId, QuizGradingRequestDTO request, String email) {
        QuizResult qr = quizResultRepository.findById(resultId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kết quả bài làm"));
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
            throw new RuntimeException("Bạn không có quyền chấm bài kiểm tra này.");
        }

        List<QuestionGradingRequestDTO> gradingItems = request.getGradingItems();

        // Build a map of questionId → item
        Map<Integer, QuestionGradingRequestDTO> gradeMap = gradingItems != null
                ? gradingItems.stream().collect(Collectors.toMap(
                        QuestionGradingRequestDTO::getQuestionId, Function.identity()))
                : Collections.emptyMap();

        int newScore = 0;

        for (QuizAnswer ans : qr.getQuizAnswers()) {
            Integer qId = ans.getQuestion().getQuestionId();
            QuestionGradingRequestDTO item = gradeMap.get(qId);
            String qType = ans.getQuestion().getQuestionType();

            // Apply teacher grading
            if (item != null && (item.getPointsAwarded() != null
                    || (item.getWritingTaskAchievement() != null && "WRITING".equalsIgnoreCase(qType)))) {
                BigDecimal awarded = item.getPointsAwarded();

                // For Writing, if criteria are provided, pointsAwarded is the average
                if ("WRITING".equalsIgnoreCase(qType) && item.getWritingTaskAchievement() != null) {
                    BigDecimal ta = item.getWritingTaskAchievement() != null ? item.getWritingTaskAchievement()
                            : BigDecimal.ZERO;
                    BigDecimal cc = item.getWritingCoherenceCohesion() != null ? item.getWritingCoherenceCohesion()
                            : BigDecimal.ZERO;
                    BigDecimal lr = item.getWritingLexicalResource() != null ? item.getWritingLexicalResource()
                            : BigDecimal.ZERO;
                    BigDecimal gra = item.getWritingGrammarAccuracy() != null ? item.getWritingGrammarAccuracy()
                            : BigDecimal.ZERO;

                    awarded = ta.add(cc).add(lr).add(gra).divide(new BigDecimal("4"), 2, RoundingMode.HALF_UP);

                    ans.setWritingTaskAchievement(ta);
                    ans.setWritingCoherenceCohesion(cc);
                    ans.setWritingLexicalResource(lr);
                    ans.setWritingGrammarAccuracy(gra);
                }

                // Fetch max points for this question in this quiz
                Optional<QuizQuestion> qqOpt = quizQuestionRepository
                        .findByQuizQuizIdAndQuestionQuestionId(quiz.getQuizId(), qId);
                BigDecimal maxPts;
                if (qqOpt.isPresent() && qqOpt.get().getPoints() != null) {
                    maxPts = qqOpt.get().getPoints();
                } else {
                    maxPts = quizQuestionRepository.findByQuestion_QuestionId(qId)
                            .map(it -> it.getPoints() != null ? it.getPoints() : BigDecimal.valueOf(9.0))
                            .orElse(BigDecimal.valueOf(9.0));
                }

                // Respect the configured max points (e.g. 5.0) instead of hardcoding 9.0
                BigDecimal maxAllowed = maxPts;

                if (awarded.compareTo(BigDecimal.ZERO) < 0 || awarded.compareTo(maxAllowed) > 0) {
                    throw new RuntimeException("Số điểm chấm (" + awarded + ") cho câu hỏi " + qId
                            + " không hợp lệ. Điểm phải từ 0 đến " + maxAllowed + ".");
                }

                ans.setPointsAwarded(awarded);
                ans.setTeacherOverrideScore(awarded.toString());
                ans.setTeacherNote(item.getTeacherNote());
                ans.setIsCorrect(awarded.compareTo(BigDecimal.ZERO) > 0);
                ans.setPendingAiReview(false);
            } else if (("WRITING".equals(qType) || "SPEAKING".equals(qType)) && item != null
                    && item.getTeacherNote() != null) {
                ans.setTeacherNote(item.getTeacherNote());
                ans.setPendingAiReview(false);
            }

            quizAnswerRepository.save(ans);

            // Sum up points
            if (ans.getPointsAwarded() != null) {
                newScore += ans.getPointsAwarded().intValue();
            } else {
                int pts = quizQuestionRepository.findByQuizQuizIdAndQuestionQuestionId(quiz.getQuizId(), qId)
                        .map(qq -> qq.getPoints() != null ? qq.getPoints().intValue() : 1)
                        .orElse(1);
                if (Boolean.TRUE.equals(ans.getIsCorrect())) {
                    newScore += pts;
                }
            }
        }

        // Save skillScores and overallNote
        if (request.getSkillScores() != null) {
            try {
                qr.setSectionScores(objectMapper.writeValueAsString(request.getSkillScores()));
            } catch (JsonProcessingException e) {
                // Ignore
            }
        }

        // Mark all as reviewed when finalizing
        for (QuizAnswer a : qr.getQuizAnswers()) {
            a.setPendingAiReview(false);
            quizAnswerRepository.save(a);
        }

        // Recalculate everything using the centralized IELTS logic
        recalculateQuizResult(qr);

        qr.setStatus("GRADED");
        quizResultRepository.save(qr);

        // Fetch the updated record for email/notification
        QuizResult updatedQr = qr;
        int maxScoreAvailable = quiz.getQuizQuestions().stream()
                .mapToInt(qq -> qq.getPoints() != null ? qq.getPoints().intValue() : 1)
                .sum();
        newScore = updatedQr.getScore() != null ? updatedQr.getScore() : 0;
        BigDecimal correctRate = updatedQr.getCorrectRate() != null ? updatedQr.getCorrectRate() : BigDecimal.ZERO;
        Boolean passed = updatedQr.getPassed();

        // ── Send email + in-app notification to student ──────────────────────
        if (updatedQr.getUser() != null && passed != null) {
            User student = updatedQr.getUser();
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
        return (qr.getScore() != null) ? qr.getScore().doubleValue() : 0.0;
    }

    /** Strip JSON quotes from a JSON-encoded string like "\"https://...\"" */
    private String extractRawString(String json) {
        if (json == null)
            return null;
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
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài kiểm tra"));

        QuizResult qr = quizResultRepository.findByQuizQuizIdAndUser_EmailAndStatus(quizId, email, "IN_PROGRESS")
                .orElseGet(() -> {
                    Optional<QuizResult> locked = quizResultRepository.findByQuizQuizIdAndUser_EmailAndStatus(quizId,
                            email, "LOCKED");
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
                            if (registrationRepository
                                    .existsByUser_UserIdAndClazz_ClassIdAndStatusApproved(user.getUserId(), classId)) {
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
        QuizResult qr = quizResultRepository.findById(resultId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy"));
        if ("LOCKED".equals(qr.getStatus())) {
            quizResultRepository.delete(qr);
        }
    }

    @Override
    @Transactional
    public void requestUnlock(Integer resultId, String email, String reason) {
        QuizResult qr = quizResultRepository.findById(resultId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy"));
        if (!qr.getUser().getEmail().equals(email))
            throw new RuntimeException("Không được phép thực hiện.");
        if (!"LOCKED".equals(qr.getStatus()))
            throw new RuntimeException("Bài thi không bị khóa");

        qr.setIsUnlockRequested(true);
        qr.setStudentAppealReason(reason);
        quizResultRepository.save(qr);
    }

    @Override
    @Transactional
    public void recalculateQuizResult(Integer resultId) {
        QuizResult result = quizResultRepository.findById(resultId).orElse(null);
        if (result != null) {
            recalculateQuizResult(result);
        }
    }

    private void recalculateQuizResult(QuizResult result) {
        if (result == null)
            return;
        Integer resultId = result.getResultId();

        List<QuizAnswer> answers = quizAnswerRepository.findByQuizResultResultIdWithQuestion(resultId);
        boolean anyPending = answers.stream().anyMatch(a -> Boolean.TRUE.equals(a.getPendingAiReview()));

        Map<String, Double> skillRawScore = new HashMap<>();
        Map<String, Double> skillMaxScore = new HashMap<>();

        for (QuizAnswer a : answers) {
            Question q = a.getQuestion();
            String skill = (q.getSkill() != null ? q.getSkill() : "DEFAULT").toUpperCase();
            String qType = q.getQuestionType();

            Optional<QuizQuestion> qqOpt = quizQuestionRepository
                    .findByQuizQuizIdAndQuestionQuestionId(result.getQuiz().getQuizId(), q.getQuestionId());
            double pts;
            if (qqOpt.isPresent() && qqOpt.get().getPoints() != null) {
                pts = qqOpt.get().getPoints().doubleValue();
            } else {
                pts = quizQuestionRepository.findByQuestion_QuestionId(q.getQuestionId())
                        .map(it -> it.getPoints() != null ? it.getPoints().doubleValue() : 9.0)
                        .orElse(9.0);
            }

            skillMaxScore.put(skill, skillMaxScore.getOrDefault(skill, 0.0) + pts);

            // 1. Ưu tiên Teacher Override (Chuỗi hoặc BigDecimal)
            if (a.getTeacherOverrideScore() != null) {
                try {
                    double inputVal = Double.parseDouble(a.getTeacherOverrideScore());
                    double teacherPts;

                    if ("WRITING".equalsIgnoreCase(qType) || "SPEAKING".equalsIgnoreCase(qType)) {
                        // Band scores: sử dụng trực tiếp (pts thường = 1.0, không cap)
                        teacherPts = inputVal;
                    } else {
                        // MC / điền từ: cap tại điểm tối đa của câu hỏi
                        teacherPts = Math.min(inputVal, pts);
                    }

                    skillRawScore.put(skill, skillRawScore.getOrDefault(skill, 0.0) + teacherPts);

                    // Đồng bộ pointsAwarded
                    a.setPointsAwarded(BigDecimal.valueOf(teacherPts));
                    quizAnswerRepository.save(a);
                    continue; // Đã xử lý xong câu này bằng điểm của giáo viên
                } catch (Exception ignored) {
                }
            }

            if (a.getPointsAwarded() != null) {
                skillRawScore.put(skill, skillRawScore.getOrDefault(skill, 0.0) + a.getPointsAwarded().doubleValue());
                continue;
            }

            // 2. Nếu là Writing/Speaking và có điểm AI (nhưng chưa có điểm giáo viên)
            if (("WRITING".equals(qType) || "SPEAKING".equals(qType)) && a.getAiScore() != null) {
                try {
                    String scoreStr = a.getAiScore();
                    double scoreVal = Double.parseDouble(scoreStr.split("/")[0].trim());
                    int maxVal = Integer.parseInt(scoreStr.split("/")[1].trim());
                    double scaledScore = maxVal > 0 ? (scoreVal / maxVal) * pts : 0;
                    skillRawScore.put(skill, skillRawScore.getOrDefault(skill, 0.0) + scaledScore);

                    a.setPointsAwarded(BigDecimal.valueOf(scaledScore));
                    quizAnswerRepository.save(a);
                    continue;
                } catch (Exception ignored) {
                }
            }

            // 3. Cuối cùng mới dùng logic isCorrect (cho Trắc nghiệm/Điền từ tự động)
            if (Boolean.TRUE.equals(a.getIsCorrect())) {
                skillRawScore.put(skill, skillRawScore.getOrDefault(skill, 0.0) + pts);
                if (a.getPointsAwarded() == null) {
                    a.setPointsAwarded(BigDecimal.valueOf(pts));
                    quizAnswerRepository.save(a);
                }
            } else if (Boolean.FALSE.equals(a.getIsCorrect())) {
                if (a.getPointsAwarded() == null) {
                    a.setPointsAwarded(BigDecimal.ZERO);
                    quizAnswerRepository.save(a);
                }
            }
        }

        // ── Tính band dựa theo cefrLevel của câu hỏi (không dùng bảng IELTS chuẩn)
        // ──────────
        // Logic: achievedBand = avgCefrOfSkill × (rawScore / maxScore)
        // Ví dụ: câu hỏi Band 5.0, đúng 100% → Band 5.0 (không phải 9.0)
        Map<String, Double> skillCefrSum = new HashMap<>();
        Map<String, Integer> skillCefrCount = new HashMap<>();

        for (QuizAnswer a : answers) {
            Question q = a.getQuestion();
            String skill = (q.getSkill() != null ? q.getSkill() : "DEFAULT").toUpperCase();
            String cefrRaw = q.getCefrLevel();
            if (cefrRaw != null && !cefrRaw.isBlank()) {
                try {
                    double cefrVal = Double.parseDouble(cefrRaw.trim());
                    skillCefrSum.put(skill, skillCefrSum.getOrDefault(skill, 0.0) + cefrVal);
                    skillCefrCount.put(skill, skillCefrCount.getOrDefault(skill, 0) + 1);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        Map<String, Double> skillBands = new HashMap<>();
        for (String skill : skillMaxScore.keySet()) {
            double raw = skillRawScore.getOrDefault(skill, 0.0);
            double max = skillMaxScore.get(skill);
            double ratio = max > 0 ? raw / max : 0.0;

            // Source of Truth for IELTS: Use cefrLevel if it's a numeric band (e.g. "4.0")
            // We find the max numeric cefrLevel among questions in this skill.
            double configuredMaxBand = answers.stream()
                .filter(a -> skill.equalsIgnoreCase(a.getQuestion().getSkill()))
                .mapToDouble(a -> {
                    String cefr = a.getQuestion().getCefrLevel();
                    if (cefr != null && !cefr.isBlank()) {
                        try {
                            double val = Double.parseDouble(cefr.trim());
                            if (val > 0) return val;
                        } catch (Exception ignored) {}
                    }
                    // Fallback to dominant points if cefrLevel is not numeric
                    java.math.BigDecimal maxPts = quizQuestionRepository.findMaxPointsByQuestionId(a.getQuestion().getQuestionId());
                    return (maxPts != null && maxPts.doubleValue() > 1.0) ? maxPts.doubleValue() : 9.0;
                })
                .max().orElse(9.0);

            if ("WRITING".equalsIgnoreCase(skill) || "SPEAKING".equalsIgnoreCase(skill)) {
                // Writing/Speaking: Points awarded is already the band, average if multiple questions
                long totalQs = answers.stream()
                    .filter(a -> skill.equalsIgnoreCase(a.getQuestion().getSkill()))
                    .count();
                double avgBand = (totalQs > 0) ? raw / totalQs : 0.0;
                skillBands.put(skill, avgBand);
            } else {
                // Reading/Listening: achieved = (CorrectCount / TotalQuestions) * configuredMaxBand
                long correctCount = answers.stream()
                    .filter(a -> skill.equalsIgnoreCase(a.getQuestion().getSkill()) && Boolean.TRUE.equals(a.getIsCorrect()))
                    .count();
                long totalQs = answers.stream()
                    .filter(a -> skill.equalsIgnoreCase(a.getQuestion().getSkill()))
                    .count();
                
                double achieved = (totalQs > 0) ? ((double)correctCount / totalQs) * configuredMaxBand : 0.0;
                skillBands.put(skill, IELTSScoreMapper.roundToIELTS(achieved));
            }
        }

        double overallBandScore = IELTSScoreMapper.calculateOverallBand(skillBands);
        result.setOverallBand(BigDecimal.valueOf(overallBandScore));

        double totalRaw = skillRawScore.values().stream().mapToDouble(Double::doubleValue).sum();
        double totalMax = skillMaxScore.values().stream().mapToDouble(Double::doubleValue).sum();

        if (totalMax > 0) {
            BigDecimal correctRate = BigDecimal.valueOf((totalRaw / totalMax) * 100).setScale(2, RoundingMode.HALF_UP);
            result.setCorrectRate(correctRate);
            result.setScore((int) Math.round(totalRaw));

            // Finalize status: If no manual questions (WRITING/SPEAKING) exist, mark as
            // GRADED
            Quiz quiz = result.getQuiz();
            // Finalize status: If no manual questions (WRITING/SPEAKING) exist, mark as
            // GRADED
            boolean hasManual = answers.stream()
                    .anyMatch(a -> {
                        String sk = a.getQuestion().getSkill();
                        String qt = a.getQuestion().getQuestionType();
                        return "WRITING".equalsIgnoreCase(sk) || "SPEAKING".equalsIgnoreCase(sk)
                                || "WRITING".equalsIgnoreCase(qt) || "SPEAKING".equalsIgnoreCase(qt);
                    });

            if (!"LOCKED".equals(result.getStatus()) && !"PENDING_GRADING".equals(result.getStatus())) {
                boolean allManualGraded = answers.stream()
                    .filter(a -> {
                        String sk = a.getQuestion().getSkill();
                        String qt = a.getQuestion().getQuestionType();
                        return "WRITING".equalsIgnoreCase(sk) || "SPEAKING".equalsIgnoreCase(sk)
                                || "WRITING".equalsIgnoreCase(qt) || "SPEAKING".equalsIgnoreCase(qt);
                    })
                    .allMatch(a -> a.getTeacherOverrideScore() != null);

                if (!hasManual || allManualGraded) {
                    result.setStatus("GRADED");
                } else {
                    // If has manual and not all are graded, it should be SUBMITTED or GRADING
                    // This allows reverting from incorrectly set GRADED status
                    if ("GRADED".equals(result.getStatus()) || result.getStatus() == null) {
                        result.setStatus("SUBMITTED");
                    }
                }
            }

            // Set passed if:
            // 1. AI is done (!anyPending)
            // 2. AND (No manual content (!hasManual) OR Teacher has finalized (GRADED))
            if (!anyPending && (!hasManual || "GRADED".equals(result.getStatus()))) {
                if (quiz.getPassScore() != null) {
                    // For sequential (IELTS) assignments: compare overallBandScore vs expert-configured pass band
                    // For regular quizzes: compare correctRate (%) vs passScore (%)
                    boolean isSequential = Boolean.TRUE.equals(result.getQuiz().getIsSequential());
                    if (isSequential) {
                        result.setPassed(BigDecimal.valueOf(overallBandScore).compareTo(quiz.getPassScore()) >= 0);
                    } else {
                        result.setPassed(correctRate.compareTo(quiz.getPassScore()) >= 0);
                    }
                } else {
                    result.setPassed(true);
                }

                // If passed, mark lesson completed
                if (Boolean.TRUE.equals(result.getPassed())) {
                    lessonRepository.findByQuizId(quiz.getQuizId()).ifPresent(l -> {
                        learningService.markLessonCompleted(l.getLessonId(), result.getUser().getEmail());
                    });
                }
            } else {
                // Keep as null to trigger "Waiting for Grade" UI for students
                result.setPassed(null);
            }
        }

        try {
            result.setSectionScores(objectMapper.writeValueAsString(skillBands));
        } catch (Exception e) {
            log.error("Failed to serialize section scores", e);
        }

        quizResultRepository.save(result);
    }

    @Override
    public List<Map<String, Object>> getQuizCompletionList(String teacherEmail, Integer classId, Integer quizId) {
        User teacher = userRepository.findByEmail(teacherEmail)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));
        Clazz clazz = clazzRepository.findById(classId).orElseThrow(() -> new RuntimeException("Class not found"));

        if (clazz.getTeacher() == null || !clazz.getTeacher().getUserId().equals(teacher.getUserId())) {
            throw new RuntimeException("Bạn không có quyền xem dữ liệu của lớp này");
        }

        List<Registration> registrations = registrationRepository.findApprovedByClassId(classId);
        List<Map<String, Object>> list = new ArrayList<>();

        for (Registration reg : registrations) {
            User student = reg.getUser();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("studentId", student.getUserId());
            m.put("fullName", student.getFullName());
            m.put("email", student.getEmail());

            // Get latest result for this quiz
            Optional<QuizResult> optResult = quizResultRepository
                    .findFirstByQuizQuizIdAndUserUserIdOrderByStartedAtDesc(quizId, student.getUserId());

            if (optResult.isPresent()) {
                QuizResult r = optResult.get();
                m.put("resultId", r.getResultId());
                m.put("status", r.getStatus());
                m.put("score", r.getScore());
                m.put("overallBand", r.getOverallBand());
                m.put("submittedAt", r.getSubmittedAt());
                m.put("startedAt", r.getStartedAt());
                m.put("violationCount", r.getViolationCount());
                m.put("isLocked", "LOCKED".equals(r.getStatus()));
            } else {
                m.put("status", "NOT_STARTED");
                m.put("score", null);
                m.put("submittedAt", null);
            }
            list.add(m);
        }
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getResultBasicStatus(Integer resultId) {
        QuizResult qr = quizResultRepository.findById(resultId).orElse(null);
        Map<String, Object> map = new HashMap<>();
        if (qr != null) {
            map.put("status", qr.getStatus());
            map.put("passed", qr.getPassed());
        }
        return map;
    }

    @Override
    public Double overrideScore(Integer answerId, String score, String teacherEmail) {
        QuizAnswer answer = quizAnswerRepository.findById(answerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy câu trả lời."));

        Quiz quiz = answer.getQuizResult().getQuiz();
        // Authorization check
        boolean isCreator = quiz.getUser() != null && quiz.getUser().getEmail().equals(teacherEmail);
        boolean isAssignedTeacher = quiz.getClazz() != null
                && quiz.getClazz().getTeacher() != null
                && quiz.getClazz().getTeacher().getEmail().equals(teacherEmail);

        if (!isCreator && !isAssignedTeacher) {
            // Check course-level access
            User teacher = userRepository.findByEmail(teacherEmail).orElse(null);
            boolean isCourseTeacher = false;
            if (teacher != null && quiz.getCourse() != null) {
                List<Clazz> teacherClasses = clazzRepository.findAllByTeacher_UserId(teacher.getUserId());
                isCourseTeacher = teacherClasses.stream()
                        .anyMatch(c -> c.getCourse() != null
                                && c.getCourse().getCourseId().equals(quiz.getCourse().getCourseId()));
            }
            if (!isCourseTeacher) {
                throw new RuntimeException("Bạn không có quyền thay đổi điểm cho bài làm này.");
            }
        }

        BigDecimal decimalScore;
        try {
            decimalScore = new BigDecimal(score);
        } catch (Exception e) {
            throw new RuntimeException("Điểm số không hợp lệ: " + score);
        }

        // Validate range: Respect the configured max points
        Optional<QuizQuestion> qqOpt = quizQuestionRepository.findByQuizQuizIdAndQuestionQuestionId(quiz.getQuizId(),
                answer.getQuestion().getQuestionId());
        BigDecimal maxPts;
        if (qqOpt.isPresent() && qqOpt.get().getPoints() != null) {
            maxPts = qqOpt.get().getPoints();
        } else {
            maxPts = quizQuestionRepository.findByQuestion_QuestionId(answer.getQuestion().getQuestionId())
                    .map(it -> it.getPoints() != null ? it.getPoints() : BigDecimal.valueOf(9.0))
                    .orElse(BigDecimal.valueOf(9.0));
        }

        BigDecimal maxAllowed = maxPts;

        if (decimalScore.compareTo(BigDecimal.ZERO) < 0 || decimalScore.compareTo(maxAllowed) > 0) {
            throw new RuntimeException(
                    "Số điểm chấm (" + decimalScore + ") không hợp lệ. Điểm phải từ 0 đến " + maxAllowed + ".");
        }

        answer.setTeacherOverrideScore(score);
        answer.setPointsAwarded(decimalScore); // Ensure pointsAwarded is also updated for consistency
        answer.setAiGradingStatus("REVIEWED");
        quizAnswerRepository.save(answer);

        // Recalculate
        recalculateQuizResult(answer.getQuizResult().getResultId());

        // Return new total score
        QuizResult updated = quizResultRepository.findById(answer.getQuizResult().getResultId()).orElse(null);
        return (updated != null && updated.getScore() != null) ? updated.getScore().doubleValue() : 0.0;
    }

    @Override
    @Transactional
    public void gradeQuizItem(QuizItemGradingRequestDTO request, String email) {
        QuizResult qr = quizResultRepository.findById(request.getResultId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kết quả bài làm"));

        QuizAnswer ans = quizAnswerRepository.findByQuizResultResultIdAndQuestionQuestionId(
                request.getResultId(), request.getQuestionId());
        if (ans == null) {
            throw new RuntimeException("Không tìm thấy câu trả lời của học sinh");
        }

        BigDecimal awarded = request.getScore();
        String qType = ans.getQuestion().getQuestionType();

        // Writing criteria logic
        if ("WRITING".equalsIgnoreCase(qType) && request.getWritingTaskAchievement() != null) {
            BigDecimal ta = request.getWritingTaskAchievement();
            BigDecimal cc = request.getWritingCoherenceCohesion() != null ? request.getWritingCoherenceCohesion()
                    : BigDecimal.ZERO;
            BigDecimal lr = request.getWritingLexicalResource() != null ? request.getWritingLexicalResource()
                    : BigDecimal.ZERO;
            BigDecimal gra = request.getWritingGrammarAccuracy() != null ? request.getWritingGrammarAccuracy()
                    : BigDecimal.ZERO;

            awarded = ta.add(cc).add(lr).add(gra).divide(new BigDecimal("4"), 2, RoundingMode.HALF_UP);

            ans.setWritingTaskAchievement(ta);
            ans.setWritingCoherenceCohesion(cc);
            ans.setWritingLexicalResource(lr);
            ans.setWritingGrammarAccuracy(gra);
        }

        if (awarded != null) {
            // Validation (limit to 9.0 for Writing/Speaking)
            BigDecimal maxAllowed = new BigDecimal("9.0");
            if (awarded.compareTo(BigDecimal.ZERO) < 0 || awarded.compareTo(maxAllowed) > 0) {
                throw new RuntimeException("Số điểm không hợp lệ (0-9.0)");
            }
            ans.setPointsAwarded(awarded);
            ans.setTeacherOverrideScore(awarded.toString());
            ans.setIsCorrect(awarded.compareTo(BigDecimal.ZERO) > 0);
        }

        if (request.getNote() != null) {
            ans.setTeacherNote(request.getNote());
        }

        ans.setPendingAiReview(false);
        quizAnswerRepository.save(ans);

        // Update status to indicate teacher has started grading
        if (!"GRADED".equals(qr.getStatus())) {
            qr.setStatus("GRADING");
            quizResultRepository.save(qr);
        }
    }
}
