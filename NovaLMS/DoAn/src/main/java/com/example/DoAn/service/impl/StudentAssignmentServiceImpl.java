package com.example.DoAn.service.impl;

import com.example.DoAn.dto.response.*;
import com.example.DoAn.exception.InvalidDataException;
import com.example.DoAn.exception.ResourceNotFoundException;
import com.example.DoAn.model.*;
import com.example.DoAn.repository.*;
import com.example.DoAn.service.IStudentAssignmentService;
import com.example.DoAn.service.GroqGradingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StudentAssignmentServiceImpl implements IStudentAssignmentService {

    private final QuizRepository quizRepository;
    private final AssignmentSessionRepository sessionRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizResultRepository quizResultRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final UserRepository userRepository;
    private final RegistrationRepository registrationRepository;
    private final GroqGradingService groqGradingService;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;
    private final SessionQuizRepository sessionQuizRepository;

    private static final List<String> SEQUENTIAL_SKILLS = Arrays.asList(
        "LISTENING", "READING", "SPEAKING", "WRITING"
    );

    // ─── getAssignmentInfo ───────────────────────────────────────────────

    @Override
    @Transactional
    public AssignmentInfoDTO getAssignmentInfo(Integer quizId, String userEmail) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        // Validate sequential assignment
        if (quiz.getIsSequential() == null || !quiz.getIsSequential()) {
            throw new InvalidDataException("Đây không phải bài tập theo thứ tự từng phần");
        }

        // Validate published + open
        if (!"PUBLISHED".equals(quiz.getStatus()) || quiz.getIsOpen() == null || !quiz.getIsOpen()) {
            throw new InvalidDataException("Assignment hiện đang đóng. Vui lòng liên hệ giáo viên.");
        }

        // Time-based validation (openAt, closeAt, deadline)
        LocalDateTime now = LocalDateTime.now();

        // 1. Check if linked to a session (for session-specific timing)
        SessionQuiz sessionQuiz = null;
        List<SessionQuiz> sqList = sessionQuizRepository.findAllByQuizId(quiz.getQuizId());
        if (!sqList.isEmpty()) {
            // Find session matching student's enrollment
            sessionQuiz = sqList.stream()
                    .filter(sq -> {
                        if (sq.getSession() == null || sq.getSession().getClazz() == null) return false;
                        return registrationRepository.existsByUser_UserIdAndClazz_ClassIdAndStatusApproved(
                                user.getUserId(), sq.getSession().getClazz().getClassId());
                    })
                    .findFirst()
                    .orElse(null);
        }

        // 2. Determine effective times
        LocalDateTime effectiveOpenAt = (sessionQuiz != null && sessionQuiz.getOpenAt() != null)
                ? sessionQuiz.getOpenAt() : quiz.getOpenAt();
        LocalDateTime effectiveCloseAt = (sessionQuiz != null && sessionQuiz.getCloseAt() != null)
                ? sessionQuiz.getCloseAt() : quiz.getCloseAt();
        LocalDateTime effectiveDeadline = (sessionQuiz != null && sessionQuiz.getDeadline() != null)
                ? sessionQuiz.getDeadline() : quiz.getDeadline();

        // 3. Apply checks
        if (effectiveOpenAt != null && now.isBefore(effectiveOpenAt)) {
            throw new InvalidDataException("Bài tập này chưa đến thời gian mở (Thời gian mở: "
                    + effectiveOpenAt.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + ")");
        }
        if (effectiveCloseAt != null && now.isAfter(effectiveCloseAt)) {
            throw new InvalidDataException("Bài tập này đã kết thúc thời gian làm bài (Kết thúc lúc: "
                    + effectiveCloseAt.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + ")");
        }
        if (effectiveDeadline != null && now.isAfter(effectiveDeadline)) {
            throw new InvalidDataException("Đã hết hạn nộp bài (Deadline: "
                    + effectiveDeadline.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + ")");
        }

        // Validate enrollment
        boolean enrolled = false;
        if (quiz.getClazz() != null) {
            enrolled = registrationRepository.existsByUser_UserIdAndClazz_ClassIdAndStatusApproved(
                    user.getUserId(), quiz.getClazz().getClassId());
            if (!enrolled) throw new InvalidDataException("Bạn chưa đăng ký lớp học này");
        } else if (quiz.getCourse() != null) {
            enrolled = registrationRepository.existsByUser_UserIdAndCourse_CourseIdAndStatus(
                    user.getUserId(), quiz.getCourse().getCourseId(), "APPROVED");
            if (!enrolled) throw new InvalidDataException("Bạn chưa đăng ký khóa học này");
        } else if (quiz.getModule() != null && quiz.getModule().getCourse() != null) {
            enrolled = registrationRepository.existsByUser_UserIdAndCourse_CourseIdAndStatus(
                    user.getUserId(), quiz.getModule().getCourse().getCourseId(), "APPROVED");
            if (!enrolled) throw new InvalidDataException("Bạn chưa đăng ký khóa học này");
        } else {
            throw new InvalidDataException("Bài tập chưa được gắn với lớp học hoặc khóa học");
        }

        // Check attempts using QuizResult (submitted ones)
        long attemptsUsed = quizResultRepository.countByQuizQuizIdAndUserUserIdAndStatusNot(quizId, user.getUserId(), "IN_PROGRESS");
        Long maxAttempts = quiz.getMaxAttempts() != null ? quiz.getMaxAttempts().longValue() : null;

        // Find or create session
        Optional<AssignmentSession> existing = sessionRepository
                .findByQuizQuizIdAndUserUserId(quizId, user.getUserId().longValue());

        AssignmentSession session = null;
        if (existing.isPresent()) {
            session = existing.get();
            // If already completed, check if they can retake
            if ("COMPLETED".equals(session.getStatus())) {
                if (maxAttempts != null && attemptsUsed >= maxAttempts) {
                    AssignmentInfoDTO dto = buildInfoDTO(quiz, session, attemptsUsed, maxAttempts, false);
                    dto.setAttemptsExceeded(true);
                    return dto;
                }
                // They can retake! Clear the old session state for a fresh start
                session.setStatus("IN_PROGRESS");
                session.setCurrentSkillIndex(0);
                session.setSectionAnswers("{}");
                session.setSectionStatuses("{}");
                session.setStartedAt(LocalDateTime.now());
                session = sessionRepository.save(session);
            }
        } else {
            if (maxAttempts != null && attemptsUsed >= maxAttempts) {
                AssignmentInfoDTO dto = new AssignmentInfoDTO();
                dto.setAttemptsExceeded(true);
                dto.setAttemptsUsed(attemptsUsed);
                dto.setMaxAttempts(maxAttempts);
                return dto;
            }
            session = createNewSession(quiz, user);
            session = sessionRepository.save(session);
        }

        return buildInfoDTO(quiz, session, attemptsUsed, maxAttempts, existing.isEmpty() || !"COMPLETED".equals(session.getStatus()));
    }

    @Override
    @Transactional
    public AssignmentInfoDTO getAssignmentPreviewInfo(Integer quizId, String teacherEmail) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found"));

        User user = userRepository.findByEmail(teacherEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Bypass enrollment and status checks for preview
        
        // Find or create session (reuse or reset)
        Optional<AssignmentSession> existing = sessionRepository
                .findByQuizQuizIdAndUserUserId(quizId, user.getUserId().longValue());

        AssignmentSession session;
        if (existing.isPresent()) {
            session = existing.get();
            // Reset for a fresh preview
            session.setStatus("IN_PROGRESS");
            session.setCurrentSkillIndex(0);
            session.setSectionAnswers("{}");
            session.setSectionStatuses("{}");
            session.setStartedAt(LocalDateTime.now());
            if (quiz.getTimeLimitMinutes() != null) {
                session.setExpiresAt(LocalDateTime.now().plusMinutes(quiz.getTimeLimitMinutes()));
            } else {
                session.setExpiresAt(null);
            }
            session = sessionRepository.save(session);
        } else {
            session = createNewSession(quiz, user);
            session = sessionRepository.save(session);
        }

        AssignmentInfoDTO dto = buildInfoDTO(quiz, session, 0, null, true);
        dto.setIsPreview(true);
        return dto;
    }

    private AssignmentSession createNewSession(Quiz quiz, User user) {
        AssignmentSession session = new AssignmentSession();
        session.setQuiz(quiz);
        session.setUser(user);
        session.setStatus("IN_PROGRESS");
        session.setCurrentSkillIndex(0);
        Map<String, String> statuses = new LinkedHashMap<>();
        for (int i = 0; i < SEQUENTIAL_SKILLS.size(); i++) {
            statuses.put(SEQUENTIAL_SKILLS.get(i),
                    i == 0 ? "IN_PROGRESS" : "LOCKED");
        }
        try {
            session.setSectionStatuses(objectMapper.writeValueAsString(statuses));
            session.setSectionAnswers("{}");
        } catch (Exception ignored) {}
        session.setStartedAt(LocalDateTime.now());
        if (quiz.getTimeLimitMinutes() != null) {
            session.setExpiresAt(LocalDateTime.now().plusMinutes(quiz.getTimeLimitMinutes()));
        }
        return session;
    }

    private AssignmentInfoDTO buildInfoDTO(Quiz quiz, AssignmentSession session,
            long attemptsUsed, Long maxAttempts, boolean isNewSession) {
        AssignmentInfoDTO dto = new AssignmentInfoDTO();
        dto.setQuizId(quiz.getQuizId());
        dto.setTitle(quiz.getTitle());
        dto.setDescription(quiz.getDescription());
        dto.setQuizCategory(quiz.getQuizCategory());
        dto.setSkillOrder(SEQUENTIAL_SKILLS);
        if (quiz.getTimeLimitPerSkill() != null) {
            try {
                dto.setTimeLimitPerSkill(objectMapper.readValue(
                        quiz.getTimeLimitPerSkill(), new TypeReference<Map<String, Integer>>() {}));
            } catch (Exception ignored) {}
        }
        dto.setQuizLevelTimeLimit(quiz.getTimeLimitMinutes());
        dto.setSessionId(session.getId());
        dto.setSessionStatus(session.getStatus());
        dto.setCurrentSkillIndex(session.getCurrentSkillIndex());
        if (session.getSectionStatuses() != null) {
            try {
                dto.setSectionStatuses(objectMapper.readValue(
                        session.getSectionStatuses(), new TypeReference<Map<String, String>>() {}));
            } catch (Exception ignored) {}
        }
        dto.setCanStart(isNewSession);
        dto.setCanResume("IN_PROGRESS".equals(session.getStatus()));
        dto.setIsCompleted("COMPLETED".equals(session.getStatus()));
        dto.setAttemptsUsed(attemptsUsed);
        dto.setMaxAttempts(maxAttempts);
        dto.setAttemptsExceeded(maxAttempts != null && attemptsUsed >= maxAttempts);

        // New flags for frontend logic
        dto.setAttemptsLeft(maxAttempts != null ? (int) Math.max(0, maxAttempts - attemptsUsed) : -1);
        dto.setCanRetake(maxAttempts == null || attemptsUsed < maxAttempts);

        dto.setAllowExternalSubmission(quiz.getAllowExternalSubmission());
        dto.setExternalSubmissionInstruction(quiz.getExternalSubmissionInstruction());
        dto.setExternalSubmissionLink(session.getExternalSubmissionLink());
        dto.setExternalSubmissionNote(session.getExternalSubmissionNote());

        return dto;
    }

    // ─── getSection ────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AssignmentSectionDTO getSection(Long sessionId, String skill, String userEmail) {
        AssignmentSession session = getSessionForUser(sessionId, userEmail);
        int skillIdx = SEQUENTIAL_SKILLS.indexOf(skill);
        if (skillIdx < 0) {
            throw new InvalidDataException("Kỹ năng không hợp lệ: " + skill);
        }

        // Cannot access future sections
        if (skillIdx > session.getCurrentSkillIndex()) {
            throw new InvalidDataException("Phần này chưa được mở");
        }

        // Load questions for this skill
        List<QuizQuestion> qqList = quizQuestionRepository
                .findByQuizQuizIdAndSkill(session.getQuiz().getQuizId(), skill);

        // Load saved answers
        Map<Integer, Object> savedAnswers = new HashMap<>();
        if (session.getSectionAnswers() != null) {
            try {
                Map<String, Map<String, Object>> allAnswers = objectMapper.readValue(
                        session.getSectionAnswers(),
                        new TypeReference<Map<String, Map<String, Object>>>() {});
                Map<String, Object> skillAnswers = allAnswers.get(skill);
                if (skillAnswers != null) {
                    for (Map.Entry<String, Object> e : skillAnswers.entrySet()) {
                        savedAnswers.put(Integer.parseInt(e.getKey()), e.getValue());
                    }
                }
            } catch (Exception ignored) {}
        }

        // Build section DTO
        AssignmentSectionDTO dto = new AssignmentSectionDTO();
        dto.setSessionId(sessionId);
        dto.setSkill(skill);
        dto.setSectionIndex(skillIdx);
        dto.setCurrentSkillIndex(session.getCurrentSkillIndex());
        dto.setQuestions(mapToPayload(qqList));
        dto.setSavedAnswers(savedAnswers);
        dto.setIsSpeaking("SPEAKING".equals(skill));
        dto.setIsWriting("WRITING".equals(skill));
        dto.setIsLastSection(skillIdx == SEQUENTIAL_SKILLS.size() - 1);
        dto.setIsLocked(skillIdx > session.getCurrentSkillIndex());

        dto.setAllowExternalSubmission(session.getQuiz().getAllowExternalSubmission());
        dto.setExternalSubmissionInstruction(session.getQuiz().getExternalSubmissionInstruction());
        dto.setExternalSubmissionLink(session.getExternalSubmissionLink());
        dto.setExternalSubmissionNote(session.getExternalSubmissionNote());

        if (skillIdx > 0) {
            dto.setPreviousSkill(SEQUENTIAL_SKILLS.get(skillIdx - 1));
        }
        if (skillIdx < SEQUENTIAL_SKILLS.size() - 1) {
            dto.setNextSkill(SEQUENTIAL_SKILLS.get(skillIdx + 1));
            dto.setNextSkillIndex(skillIdx + 1);
        }

        if (session.getSectionStatuses() != null) {
            try {
                dto.setSectionStatuses(objectMapper.readValue(
                        session.getSectionStatuses(),
                        new TypeReference<Map<String, String>>() {}));
            } catch (Exception ignored) {}
        }

        // Quiz-level timer (remaining seconds)
        if (session.getExpiresAt() != null) {
            long remaining = java.time.Duration.between(
                    LocalDateTime.now(), session.getExpiresAt()).getSeconds();
            dto.setTimerSeconds(Math.max(0, remaining));
        }

        // Per-skill timer for ALL skills
        if (session.getQuiz().getTimeLimitPerSkill() != null) {
            try {
                Map<String, Integer> limits = objectMapper.readValue(
                        session.getQuiz().getTimeLimitPerSkill(),
                        new TypeReference<Map<String, Integer>>() {});
                Integer sectionMins = limits.get(skill);
                if (sectionMins != null) {
                    // Check if we already have an expiry for this section
                    Map<String, String> expiries = new HashMap<>();
                    if (session.getSectionExpiry() != null) {
                        expiries = objectMapper.readValue(session.getSectionExpiry(), new TypeReference<Map<String, String>>() {});
                    }
                    
                    String skillExpiryStr = expiries.get(skill);
                    LocalDateTime skillExpiry;
                    if (skillExpiryStr == null) {
                        // First time entering this section, set expiry
                        skillExpiry = LocalDateTime.now().plusMinutes(sectionMins);
                        expiries.put(skill, skillExpiry.toString());
                        session.setSectionExpiry(objectMapper.writeValueAsString(expiries));
                        sessionRepository.save(session);
                    } else {
                        skillExpiry = LocalDateTime.parse(skillExpiryStr);
                    }
                    
                    long remainingSecs = java.time.Duration.between(LocalDateTime.now(), skillExpiry).getSeconds();
                    dto.setSectionTimerSeconds(Math.max(0, remainingSecs));
                    dto.setSectionExpiry(skillExpiry.toString());

                    // Legacy fields for backward compatibility
                    if ("SPEAKING".equals(skill)) {
                        dto.setSpeakingTimerSeconds(dto.getSectionTimerSeconds());
                        dto.setSpeakingExpiry(dto.getSectionExpiry());
                    } else if ("WRITING".equals(skill)) {
                        dto.setWritingTimerSeconds(dto.getSectionTimerSeconds());
                        dto.setWritingExpiry(dto.getSectionExpiry());
                    }
                }
            } catch (Exception e) {
                log.error("Error calculating section timer", e);
            }
        }

        return dto;
    }

    // ─── saveAnswers ─────────────────────────────────────────────────────

    @Override
    public void saveAnswers(Long sessionId, String skill,
            Map<Integer, Object> answers, String userEmail) {
        AssignmentSession session = getSessionForUser(sessionId, userEmail);
        Map<String, Map<String, Object>> allAnswers = new HashMap<>();
        if (session.getSectionAnswers() != null) {
            try {
                allAnswers = objectMapper.readValue(session.getSectionAnswers(),
                        new TypeReference<Map<String, Map<String, Object>>>() {});
            } catch (Exception ignored) {}
        }
        Map<String, Object> stringKeyAnswers = new HashMap<>();
        answers.forEach((qId, val) -> stringKeyAnswers.put(String.valueOf(qId), val));
        allAnswers.put(skill, stringKeyAnswers);
        try {
            session.setSectionAnswers(objectMapper.writeValueAsString(allAnswers));
        } catch (Exception ignored) {}
        sessionRepository.save(session);
    }

    // ─── submitSection ──────────────────────────────────────────────────

    @Override
    public Map<String, Object> submitSection(Long sessionId, String skill,
            Map<Integer, Object> answers, String userEmail) {
        AssignmentSession session = getSessionForUser(sessionId, userEmail);
        int skillIdx = SEQUENTIAL_SKILLS.indexOf(skill);

        // Guard: already completed
        if ("COMPLETED".equals(session.getStatus())) {
            throw new InvalidDataException("Bài tập đã hoàn thành");
        }

        // Guard: section already submitted
        if (session.getSectionStatuses() != null) {
            try {
                Map<String, String> statuses = objectMapper.readValue(
                        session.getSectionStatuses(), new TypeReference<Map<String, String>>() {});
                if ("COMPLETED".equals(statuses.get(skill))) {
                    throw new InvalidDataException("Phần này đã được nộp");
                }
            } catch (Exception ignored) {}
        }

        // Save answers
        saveAnswers(sessionId, skill, answers, userEmail);

        // Grade MC/FILL/MATCH answers
        gradeMultipleChoiceSection(session, skill, answers);

        // Update statuses
        Map<String, String> statuses;
        try {
            statuses = session.getSectionStatuses() != null
                    ? objectMapper.readValue(session.getSectionStatuses(),
                            new TypeReference<Map<String, String>>() {})
                    : new LinkedHashMap<>();
        } catch (Exception e) {
            statuses = new LinkedHashMap<>();
        }
        statuses.put(skill, "COMPLETED");

        if (skillIdx < SEQUENTIAL_SKILLS.size() - 1) {
            // Advance to next section
            String nextSkill = SEQUENTIAL_SKILLS.get(skillIdx + 1);
            session.setCurrentSkillIndex(skillIdx + 1);
            statuses.put(nextSkill, "IN_PROGRESS");
            session.setSectionStatuses(objectMapper.valueToTree(statuses).asText());
            sessionRepository.save(session);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("nextSkill", nextSkill);
            result.put("nextSkillIndex", skillIdx + 1);
            result.put("sectionCompleted", true);
            return result;
        } else {
            // Last section — complete
            Map<String, Object> result = completeAndReturn(session);
            result.put("sectionCompleted", true);
            return result;
        }
    }

    @Override
    public Map<String, Object> submitSpeakingSection(Long sessionId,
            Map<Integer, String> audioUrls, String userEmail) {
        Map<Integer, Object> answers = new HashMap<>(audioUrls);
        return submitSection(sessionId, "SPEAKING", answers, userEmail);
    }

    @Override
    public void saveExternalSubmission(Long sessionId, String link, String note, String userEmail) {
        AssignmentSession session = getSessionForUser(sessionId, userEmail);
        session.setExternalSubmissionLink(link);
        session.setExternalSubmissionNote(note);
        sessionRepository.save(session);
    }

    // ─── completeAssignment ─────────────────────────────────────────────

    @Override
    public Integer completeAssignment(Long sessionId, String userEmail) {
        AssignmentSession session = getSessionForUser(sessionId, userEmail);
        Map<String, Object> result = completeAndReturn(session);
        return (Integer) result.get("resultId");
    }

    // ─── autoSubmit ─────────────────────────────────────────────────────

    @Override
    @Transactional
    public void autoSubmit(Long sessionId, String userEmail) {
        AssignmentSession session = getSessionForUser(sessionId, userEmail);
        if (!"IN_PROGRESS".equals(session.getStatus())) return;

        Map<String, String> statuses;
        try {
            statuses = session.getSectionStatuses() != null
                    ? objectMapper.readValue(session.getSectionStatuses(),
                            new TypeReference<Map<String, String>>() {})
                    : new LinkedHashMap<>();
        } catch (Exception e) {
            statuses = new LinkedHashMap<>();
        }

        for (String skill : SEQUENTIAL_SKILLS) {
            if (!"COMPLETED".equals(statuses.get(skill))) {
                statuses.put(skill, "EXPIRED");
            }
        }
        try {
            session.setSectionStatuses(objectMapper.writeValueAsString(statuses));
        } catch (Exception ignored) {}
        session.setStatus("COMPLETED");
        session.setCompletedAt(LocalDateTime.now());
        sessionRepository.save(session);

        createQuizResult(session);
    }

    @Override
    @Transactional
    public void autoSubmitAllExpired() {
        log.info("[AutoSubmit] Checking for expired assignment sessions...");
        List<AssignmentSession> expired = sessionRepository.findExpiredSessions(LocalDateTime.now());
        if (expired.isEmpty()) return;

        log.info("[AutoSubmit] Found {} expired sessions. Processing...", expired.size());
        for (AssignmentSession session : expired) {
            try {
                // We use the student's email to satisfy getSessionForUser/autoSubmit internal checks
                String studentEmail = session.getUser().getEmail();
                autoSubmit(session.getId(), studentEmail);
                log.info("[AutoSubmit] Successfully auto-submitted sessionId={} for user={}",
                        session.getId(), studentEmail);
            } catch (Exception e) {
                log.error("[AutoSubmit] Failed to auto-submit sessionId={}: {}",
                        session.getId(), e.getMessage());
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentGradingDetailDTO getAssignmentResultDetail(Integer resultId, String studentEmail) {
        QuizResult result = quizResultRepository.findById(resultId)
                .orElseThrow(() -> new ResourceNotFoundException("Kết quả không tìm thấy: " + resultId));

        // Security check: result must belong to the student
        if (!result.getUser().getEmail().equals(studentEmail)) {
            throw new InvalidDataException("Bạn không có quyền xem kết quả này");
        }

        Quiz quiz = result.getQuiz();
        if (quiz == null) throw new ResourceNotFoundException("Quiz không tìm thấy");

        // Fetch all answers eagerly
        List<QuizAnswer> answers = quizAnswerRepository.findByQuizResultResultIdWithQuestion(resultId);

        // Group answers by skill
        Map<String, List<QuizAnswer>> bySkill = new LinkedHashMap<>();
        for (QuizAnswer a : answers) {
            Question q = a.getQuestion();
            String skill = q.getSkill() != null ? q.getSkill() : "OTHER";
            bySkill.computeIfAbsent(skill, k -> new ArrayList<>()).add(a);
        }

        AssignmentGradingDetailDTO dto = new AssignmentGradingDetailDTO();
        dto.setResultId(resultId);
        dto.setAssignmentSessionId(result.getAssignmentSessionId());
        dto.setStudentName(result.getUser() != null ? result.getUser().getFullName() : null);
        dto.setQuizTitle(quiz.getTitle());
        if (quiz.getClazz() != null) {
            dto.setClassName(quiz.getClazz().getClassName());
            dto.setClassId(quiz.getClazz().getClassId());
        }
        dto.setSubmittedAt(result.getSubmittedAt());

        List<AssignmentGradingDetailDTO.SkillSectionDetail> sections = new ArrayList<>();
        BigDecimal autoScore = BigDecimal.ZERO;

        for (String skill : Arrays.asList("LISTENING", "READING", "SPEAKING", "WRITING")) {
            List<QuizAnswer> skillAnswers = bySkill.getOrDefault(skill, Collections.emptyList());
            if (skillAnswers.isEmpty()) continue;

            AssignmentGradingDetailDTO.SkillSectionDetail section =
                    new AssignmentGradingDetailDTO.SkillSectionDetail();
            section.setSkill(skill);

            BigDecimal sectionMax = BigDecimal.ZERO;
            BigDecimal sectionTeacherScore = BigDecimal.ZERO;
            List<AssignmentGradingDetailDTO.QuestionGradeItem> items = new ArrayList<>();

            for (QuizAnswer a : skillAnswers) {
                Question q = a.getQuestion();
                BigDecimal maxPts = getQuestionMaxPoints(q);
                sectionMax = sectionMax.add(maxPts);

                AssignmentGradingDetailDTO.QuestionGradeItem item =
                        new AssignmentGradingDetailDTO.QuestionGradeItem();
                item.setQuestionId(q.getQuestionId());
                item.setQuestionType(q.getQuestionType());
                item.setContent(q.getContent());
                item.setMaxPoints(maxPts);
                item.setStudentAnswer(a.getAnsweredOptions());
                item.setIsCorrect(a.getIsCorrect());
                item.setAiScore(a.getAiScore());
                item.setAiFeedback(a.getAiFeedback());
                item.setAudioUrl(q.getAudioUrl());
                item.setTeacherScore(a.getPointsAwarded());
                item.setTeacherNote(a.getTeacherNote());

                // Accumulate teacher score for this section
                if (a.getPointsAwarded() != null) {
                    sectionTeacherScore = sectionTeacherScore.add(a.getPointsAwarded());
                } else if ("LISTENING".equals(skill) || "READING".equals(skill)) {
                    // For auto-graded sections, pointsAwarded might be null but isCorrect is set
                    if (Boolean.TRUE.equals(a.getIsCorrect())) {
                        sectionTeacherScore = sectionTeacherScore.add(maxPts);
                    }
                }

                items.add(item);
            }

            section.setQuestions(items);
            section.setMaxScore(sectionMax);
            section.setTeacherScore(sectionTeacherScore.compareTo(BigDecimal.ZERO) > 0
                    ? sectionTeacherScore : null);

            // AI summary (first answer with AI score)
            QuizAnswer aiAnswer = skillAnswers.stream()
                    .filter(a -> a.getAiScore() != null)
                    .findFirst().orElse(null);
            if (aiAnswer != null) {
                section.setAiScore(aiAnswer.getAiScore());
                section.setAiFeedback(aiAnswer.getAiFeedback());
                section.setAiRubricJson(aiAnswer.getAiRubricJson());
            }

            if ("LISTENING".equals(skill) || "READING".equals(skill)) {
                section.setGradingStatus("AUTO");
                autoScore = autoScore.add(sectionTeacherScore);
            } else {
                boolean hasAi = skillAnswers.stream().anyMatch(a -> a.getAiScore() != null);
                boolean hasTeacher = skillAnswers.stream().anyMatch(a -> a.getPointsAwarded() != null);
                section.setGradingStatus(hasTeacher ? "GRADED" : (hasAi ? "AI_READY" : "AI_PENDING"));
            }

            sections.add(section);
        }

        dto.setSections(sections);
        dto.setAutoScore(autoScore);
        dto.setAllowExternalSubmission(quiz.getAllowExternalSubmission());
        dto.setExternalSubmissionLink(result.getExternalSubmissionLink());
        dto.setExternalSubmissionNote(result.getExternalSubmissionNote());

        if (result.getSectionScores() != null && !result.getSectionScores().isBlank()) {
            try {
                dto.setSectionScores(objectMapper.readValue(
                        result.getSectionScores(),
                        new TypeReference<Map<String, BigDecimal>>() {}));
            } catch (Exception ignored) { /* leave null */ }
        }

        if (result.getScore() != null) {
            dto.setTotalScore(BigDecimal.valueOf(result.getScore()));
        }

        return dto;
    }

    // ─── Private helpers ────────────────────────────────────────────────

    private AssignmentSession getSessionForUser(Long sessionId, String userEmail) {
        AssignmentSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy buổi học"));
        if (!session.getUser().getEmail().equals(userEmail)) {
            throw new InvalidDataException("Không có quyền truy cập");
        }
        return session;
    }

    private void gradeMultipleChoiceSection(AssignmentSession session,
            String skill, Map<Integer, Object> answers) {
        if ("SPEAKING".equals(skill) || "WRITING".equals(skill)) {
            return; // AI-graded, skip
        }
        List<QuizQuestion> qqList = quizQuestionRepository
                .findByQuizQuizIdAndSkill(session.getQuiz().getQuizId(), skill);
        for (QuizQuestion qq : qqList) {
            Question q = qq.getQuestion();
            Object rawAnswer = answers.get(q.getQuestionId());
            if (rawAnswer == null) continue;
            boolean correct = checkAnswer(q, rawAnswer);
            // Store grade result in the answers map for createQuizResult
            answers.put(q.getQuestionId(), Map.of("value", rawAnswer, "correct", correct));
        }
    }

    private boolean checkAnswer(Question q, Object answer) {
        String type = q.getQuestionType();
        if ("MULTIPLE_CHOICE_SINGLE".equals(type) || "MULTIPLE_CHOICE_MULTI".equals(type)) {
            List<AnswerOption> options = q.getAnswerOptions();
            if (options == null) return false;
            if ("MULTIPLE_CHOICE_SINGLE".equals(type)) {
                // answer is Integer (answerOptionId)
                return options.stream()
                        .filter(AnswerOption::getCorrectAnswer)
                        .anyMatch(c -> c.getAnswerOptionId().equals(answer));
            } else {
                // MULTI: answer is List<Integer>
                if (!(answer instanceof List)) return false;
                @SuppressWarnings("unchecked")
                List<Integer> selected = (List<Integer>) answer;
                List<Integer> correctIds = options.stream()
                        .filter(AnswerOption::getCorrectAnswer)
                        .map(AnswerOption::getAnswerOptionId)
                        .toList();
                return new HashSet<>(selected).equals(new HashSet<>(correctIds));
            }
        } else if ("FILL_IN_BLANK".equals(type)) {
            List<AnswerOption> options = q.getAnswerOptions();
            if (options == null || options.isEmpty()) return false;
            return options.get(0).getTitle().trim()
                    .equalsIgnoreCase(String.valueOf(answer).trim());
        }
        return false;
    }

    private List<QuizQuestionPayloadDTO> mapToPayload(List<QuizQuestion> qqList) {
        return qqList.stream().map(qq -> {
            Question q = qq.getQuestion();
            List<AnswerOption> opts = q.getAnswerOptions();
            List<AnswerOptionPayloadDTO> optionPayloads = (opts == null) ? List.of()
                    : opts.stream().map(o -> AnswerOptionPayloadDTO.builder()
                            .answerOptionId(o.getAnswerOptionId())
                            .title(o.getTitle())
                            .matchTarget(o.getMatchTarget())
                            .build())
                    .toList();
            return QuizQuestionPayloadDTO.builder()
                    .questionId(q.getQuestionId())
                    .content(q.getContent())
                    .questionType(q.getQuestionType())
                    .skill(q.getSkill())
                    .cefrLevel(q.getCefrLevel())
                    .points(qq.getPoints() != null ? qq.getPoints().intValue() : 1)
                    .audioUrl(q.getAudioUrl())
                    .imageUrl(q.getImageUrl())
                    .options(optionPayloads)
                    .build();
        }).toList();
    }

    private Map<String, Object> completeAndReturn(AssignmentSession session) {
        session.setStatus("COMPLETED");
        session.setCompletedAt(LocalDateTime.now());
        sessionRepository.save(session);

        Integer resultId = createQuizResult(session);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("resultId", resultId);
        result.put("completed", true);
        return result;
    }

    private Integer createQuizResult(AssignmentSession session) {
        QuizResult result = new QuizResult();
        result.setQuiz(session.getQuiz());
        result.setUser(session.getUser());
        result.setSubmittedAt(LocalDateTime.now());
        result.setAssignmentSessionId(session.getId());
        result.setExternalSubmissionLink(session.getExternalSubmissionLink());
        result.setExternalSubmissionNote(session.getExternalSubmissionNote());
        result.setSectionScores("{}");

        // Load all answers
        Map<String, Map<String, Object>> allAnswers = new HashMap<>();
        if (session.getSectionAnswers() != null) {
            try {
                allAnswers = objectMapper.readValue(session.getSectionAnswers(),
                        new TypeReference<Map<String, Map<String, Object>>>() {});
            } catch (Exception ignored) {}
        }

        // Auto-grade LISTENING/READING
        int totalScore = 0;
        int maxScore = 0;
        Map<String, Double> sectionScores = new LinkedHashMap<>();

        for (String skill : Arrays.asList("LISTENING", "READING")) {
            Map<String, Object> answers = allAnswers.get(skill);
            List<QuizQuestion> qqList = quizQuestionRepository
                    .findByQuizQuizIdAndSkill(session.getQuiz().getQuizId(), skill);
            int sectionScore = 0;
            for (QuizQuestion qq : qqList) {
                int pts = qq.getPoints() != null ? qq.getPoints().intValue() : 1;
                maxScore += pts;
                Object rawAnswer = answers != null ? answers.get(String.valueOf(qq.getQuestion().getQuestionId())) : null;
                boolean correct = false;
                if (rawAnswer instanceof Map) {
                    correct = Boolean.TRUE.equals(((Map<?, ?>) rawAnswer).get("correct"));
                }
                if (correct) totalScore += pts;
                sectionScore += correct ? pts : 0;
            }
            sectionScores.put(skill, (double) sectionScore);
        }

        BigDecimal rate = maxScore > 0
                ? BigDecimal.valueOf(totalScore).divide(BigDecimal.valueOf(maxScore), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        result.setScore(totalScore);
        result.setCorrectRate(rate);
        result.setPassed(rate.compareTo(BigDecimal.valueOf(50)) >= 0);
        result.setSectionScores(toJson(sectionScores));

        QuizResult saved = quizResultRepository.save(result);

        // Save MC answers into QuizAnswer table
        for (String skill : Arrays.asList("LISTENING", "READING")) {
            Map<String, Object> answers = allAnswers.get(skill);
            if (answers == null) continue;
            List<QuizQuestion> qqList = quizQuestionRepository
                    .findByQuizQuizIdAndSkill(session.getQuiz().getQuizId(), skill);
            for (QuizQuestion qq : qqList) {
                Object rawAnswer = answers.get(String.valueOf(qq.getQuestion().getQuestionId()));
                if (rawAnswer == null) continue;
                boolean correct = false;
                Object value = rawAnswer;
                if (rawAnswer instanceof Map) {
                    value = ((Map<?, ?>) rawAnswer).get("value");
                    correct = Boolean.TRUE.equals(((Map<?, ?>) rawAnswer).get("correct"));
                }
                saveQuizAnswer(saved, qq.getQuestion(),
                        String.valueOf(value), correct, null);
            }
        }

        // Fire AI grading for SPEAKING/WRITING
        for (String skill : Arrays.asList("SPEAKING", "WRITING")) {
            Map<String, Object> answers = allAnswers.get(skill);
            if (answers == null) continue;
            List<QuizQuestion> qqList = quizQuestionRepository
                    .findByQuizQuizIdAndSkill(session.getQuiz().getQuizId(), skill);
            for (QuizQuestion qq : qqList) {
                Object rawAnswer = answers.get(String.valueOf(qq.getQuestion().getQuestionId()));
                if (rawAnswer == null) continue;
                String answeredValue = rawAnswer instanceof Map
                        ? String.valueOf(((Map<?, ?>) rawAnswer).get("value"))
                        : String.valueOf(rawAnswer);
                QuizAnswer qa = saveQuizAnswer(saved, qq.getQuestion(),
                        answeredValue, null, true);
                // Fire async AI grading via GroqGradingServiceImpl directly
                fireAiGrading(saved.getResultId(), qq.getQuestion().getQuestionId(),
                        qq.getQuestion().getQuestionType());
            }
        }

        return saved.getResultId();
    }

    private void fireAiGrading(Integer resultId, Integer questionId, String questionType) {
        // Caller wraps in transactionTemplate — gradeSync runs JPA logic inside that tx
        transactionTemplate.executeWithoutResult(status -> {
            try {
                groqGradingService.gradeSync(resultId, questionId, questionType);
            } catch (Exception e) {
                log.error("AI grading failed for resultId={}, questionId={}: {}",
                        resultId, questionId, e.getMessage());
            }
        });
    }

    private QuizAnswer saveQuizAnswer(QuizResult result, Question question,
            String answeredOptions, Boolean isCorrect, Boolean hasPendingReview) {
        QuizAnswer qa = new QuizAnswer();
        qa.setQuizResult(result);
        qa.setQuestion(question);
        qa.setAnsweredOptions(answeredOptions);
        qa.setIsCorrect(isCorrect);
        if (hasPendingReview != null) qa.setPendingAiReview(hasPendingReview);
        return quizAnswerRepository.save(qa);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    private BigDecimal getQuestionMaxPoints(Question q) {
        // Try QuizQuestion for explicit points, else default to 1
        return quizQuestionRepository.findByQuestion_QuestionId(
                        q.getQuestionId())
                .filter(qq -> qq.getPoints() != null)
                .map(QuizQuestion::getPoints)
                .orElse(BigDecimal.ONE);
    }
}
