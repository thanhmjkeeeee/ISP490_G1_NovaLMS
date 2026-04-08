package com.example.DoAn.service.impl;

import com.example.DoAn.dto.request.AssignmentGradingRequestDTO;
import com.example.DoAn.dto.response.AssignmentGradingDetailDTO;
import com.example.DoAn.dto.response.AssignmentGradingQueueDTO;
import com.example.DoAn.exception.ResourceNotFoundException;
import com.example.DoAn.model.*;
import com.example.DoAn.repository.*;
import com.example.DoAn.service.EmailService;
import com.example.DoAn.service.ITeacherAssignmentGradingService;
import com.example.DoAn.service.INotificationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class TeacherAssignmentGradingServiceImpl implements ITeacherAssignmentGradingService {

    private final QuizResultRepository quizResultRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final ObjectMapper objectMapper;
    private final EmailService emailService;
    private final INotificationService notificationService;

    // ─── Grading Queue ────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<AssignmentGradingQueueDTO> getGradingQueue(
            String teacherEmail, Integer quizId, Integer classId, String status, Pageable pageable) {

        Page<QuizResult> results = quizResultRepository.findAssignmentResultsForTeacher(
                teacherEmail, quizId, classId, pageable);

        // Filter by status if needed (done in-memory since status is derived)
        List<QuizResult> all = results.getContent();
        List<QuizResult> filtered = switch (status != null ? status : "ALL") {
            case "PENDING_SPEAKING" -> all.stream()
                    .filter(r -> hasPendingSpeaking(r) && !hasPendingWriting(r)).toList();
            case "PENDING_WRITING" -> all.stream()
                    .filter(r -> !hasPendingSpeaking(r) && hasPendingWriting(r)).toList();
            case "PENDING_BOTH" -> all.stream()
                    .filter(r -> hasPendingSpeaking(r) && hasPendingWriting(r)).toList();
            case "ALL_GRADED" -> all.stream()
                    .filter(r -> Boolean.TRUE.equals(r.getPassed())).toList();
            default -> all;
        };

        List<AssignmentGradingQueueDTO> dtos = filtered.stream()
                .map(this::toQueueDTO)
                .toList();

        return new org.springframework.data.domain.PageImpl<>(dtos, pageable, results.getTotalElements());
    }

    private boolean hasPendingSpeaking(QuizResult r) {
        List<QuizAnswer> answers = quizAnswerRepository.findByQuizResultResultId(r.getResultId());
        return answers.stream().anyMatch(a -> {
            Question q = a.getQuestion();
            return "SPEAKING".equalsIgnoreCase(q.getSkill())
                    && a.getPointsAwarded() == null;
        });
    }

    private boolean hasPendingWriting(QuizResult r) {
        List<QuizAnswer> answers = quizAnswerRepository.findByQuizResultResultId(r.getResultId());
        return answers.stream().anyMatch(a -> {
            Question q = a.getQuestion();
            return "WRITING".equalsIgnoreCase(q.getSkill())
                    && a.getPointsAwarded() == null;
        });
    }

    private AssignmentGradingQueueDTO toQueueDTO(QuizResult r) {
        AssignmentGradingQueueDTO dto = new AssignmentGradingQueueDTO();
        dto.setResultId(r.getResultId());
        dto.setAssignmentSessionId(r.getAssignmentSessionId());
        dto.setStudentName(r.getUser() != null ? r.getUser().getFullName() : null);
        dto.setStudentEmail(r.getUser() != null ? r.getUser().getEmail() : null);
        dto.setQuizId(r.getQuiz() != null ? r.getQuiz().getQuizId() : null);
        dto.setQuizTitle(r.getQuiz() != null ? r.getQuiz().getTitle() : null);

        Quiz quiz = r.getQuiz();
        if (quiz != null && quiz.getClazz() != null) {
            dto.setClassId(Long.valueOf(quiz.getClazz().getClassId()));
            dto.setClassName(quiz.getClazz().getClassName());
        }

        dto.setSubmittedAt(r.getSubmittedAt());

        List<QuizAnswer> answers = quizAnswerRepository.findByQuizResultResultId(r.getResultId());
        dto.setListening(buildSectionStatus(answers, "LISTENING"));
        dto.setReading(buildSectionStatus(answers, "READING"));
        dto.setSpeaking(buildSectionStatus(answers, "SPEAKING"));
        dto.setWriting(buildSectionStatus(answers, "WRITING"));

        // Overall auto score (LISTENING + READING)
        BigDecimal autoScore = BigDecimal.ZERO;
        if (dto.getListening() != null && dto.getListening().getScore() != null) {
            autoScore = autoScore.add(dto.getListening().getScore());
        }
        if (dto.getReading() != null && dto.getReading().getScore() != null) {
            autoScore = autoScore.add(dto.getReading().getScore());
        }
        dto.setAutoScore(autoScore);

        // Total score
        if (r.getScore() != null) {
            dto.setTotalScore(BigDecimal.valueOf(r.getScore()));
        }

        // Overall status
        dto.setOverallStatus(deriveOverallStatus(dto));

        // isGraded: if passed is set (meaning teacher submitted final grading)
        dto.setIsGraded(r.getPassed() != null);

        return dto;
    }

    private AssignmentGradingQueueDTO.SectionStatus buildSectionStatus(
            List<QuizAnswer> answers, String skill) {

        List<QuizAnswer> sectionAnswers = answers.stream()
                .filter(a -> skill.equalsIgnoreCase(a.getQuestion().getSkill()))
                .toList();

        if (sectionAnswers.isEmpty()) return null;

        BigDecimal totalScore = BigDecimal.ZERO;
        BigDecimal maxScore = BigDecimal.ZERO;
        String aiScore = null;
        String aiFeedback = null;
        String gradingStatus = "AUTO"; // default, overridden below for SPEAKING/WRITING

        for (QuizAnswer a : sectionAnswers) {
            BigDecimal pts = a.getPointsAwarded();
            BigDecimal max = a.getQuestion() != null
                    ? getQuestionMaxPoints(a.getQuestion())
                    : BigDecimal.ONE;

            maxScore = maxScore.add(max);

            if ("LISTENING".equals(skill) || "READING".equals(skill)) {
                // Auto-graded
                if (Boolean.TRUE.equals(a.getIsCorrect())) {
                    totalScore = totalScore.add(pts != null ? pts : BigDecimal.ZERO);
                }
                gradingStatus = "AUTO";
            } else {
                // SPEAKING / WRITING: teacher-graded
                if (pts != null) {
                    totalScore = totalScore.add(pts);
                    gradingStatus = "GRADED";
                } else if (a.getAiScore() != null) {
                    gradingStatus = "AI_READY";
                    if (aiScore == null) aiScore = a.getAiScore();
                    if (aiFeedback == null) aiFeedback = a.getAiFeedback();
                } else {
                    gradingStatus = "AI_PENDING";
                }
            }
        }

        // Re-derive gradingStatus for SPEAKING/WRITING based on all answers
        if ("SPEAKING".equals(skill) || "WRITING".equals(skill)) {
            boolean allGraded = sectionAnswers.stream()
                    .allMatch(a -> a.getPointsAwarded() != null);
            gradingStatus = allGraded ? "GRADED"
                    : sectionAnswers.stream().anyMatch(a -> a.getAiScore() != null) ? "AI_READY"
                    : "AI_PENDING";
        }

        AssignmentGradingQueueDTO.SectionStatus s = new AssignmentGradingQueueDTO.SectionStatus();
        s.setSkill(skill);
        s.setGradingStatus(gradingStatus);
        s.setScore(totalScore.compareTo(BigDecimal.ZERO) > 0 ? totalScore : null);
        s.setMaxScore(maxScore.compareTo(BigDecimal.ZERO) > 0 ? maxScore : null);
        s.setAiScore(aiScore);
        s.setAiFeedback(aiFeedback);
        return s;
    }

    private BigDecimal getQuestionMaxPoints(Question q) {
        // Try QuizQuestion for explicit points, else default to 1
        return quizQuestionRepository.findByQuizQuizIdAndQuestionQuestionId(
                        q.getQuestionId(), q.getQuestionId())
                .filter(qq -> qq.getPoints() != null)
                .map(QuizQuestion::getPoints)
                .orElse(BigDecimal.ONE);
    }

    private String deriveOverallStatus(AssignmentGradingQueueDTO dto) {
        boolean spk = "GRADED".equals(dto.getSpeaking() != null ? dto.getSpeaking().getGradingStatus() : null);
        boolean wrt = "GRADED".equals(dto.getWriting() != null ? dto.getWriting().getGradingStatus() : null);
        if (spk && wrt) return "ALL_GRADED";
        if (!spk && !wrt) return "PENDING_BOTH";
        return spk ? "PENDING_WRITING" : "PENDING_SPEAKING";
    }

    // ─── Grading Detail ───────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AssignmentGradingDetailDTO getGradingDetail(Integer resultId, String teacherEmail) {
        QuizResult result = quizResultRepository.findById(resultId)
                .orElseThrow(() -> new ResourceNotFoundException("Kết quả không tìm thấy: " + resultId));

        Quiz quiz = result.getQuiz();
        if (quiz == null) throw new ResourceNotFoundException("Quiz không tìm thấy");

        // Fetch all answers eagerly
        List<QuizAnswer> answers = quizAnswerRepository.findByQuizResultResultId(resultId);

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

    // ─── Submit Grading ───────────────────────────────────────────────────────

    @Override
    public void gradeAssignment(Integer resultId, AssignmentGradingRequestDTO request, String teacherEmail) {
        QuizResult result = quizResultRepository.findById(resultId)
                .orElseThrow(() -> new ResourceNotFoundException("Kết quả không tìm thấy: " + resultId));

        // Save per-question scores
        if (request.getGradingItems() != null) {
            for (AssignmentGradingRequestDTO.QuestionGradingItem item : request.getGradingItems()) {
                Optional<QuizAnswer> optAnswer = Optional.ofNullable(
                        quizAnswerRepository.findByQuizResultResultIdAndQuestionQuestionId(resultId, item.getQuestionId()));
                if (optAnswer.isPresent()) {
                    QuizAnswer answer = optAnswer.get();
                    answer.setPointsAwarded(item.getPointsAwarded());
                    answer.setTeacherNote(item.getTeacherNote());
                    answer.setIsCorrect(
                            item.getPointsAwarded() != null
                                    && item.getPointsAwarded().compareTo(BigDecimal.ZERO) > 0);
                    quizAnswerRepository.save(answer);
                }
            }
        }

        // Update section scores JSON
        if (request.getSectionScores() != null) {
            try {
                result.setSectionScores(objectMapper.writeValueAsString(request.getSectionScores()));
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize section scores", e);
            }
        }

        // Recalculate total score
        BigDecimal total = request.getSectionScores() != null
                ? request.getSectionScores().values().stream()
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                : BigDecimal.ZERO;
        result.setScore(total.intValue());

        // Calculate correctRate
        Quiz quiz = result.getQuiz();
        BigDecimal totalMax = BigDecimal.ZERO;
        if (quiz != null) {
            totalMax = quizQuestionRepository
                    .findByQuizQuizId(quiz.getQuizId()).stream()
                    .map(qq -> qq.getPoints() != null ? qq.getPoints() : BigDecimal.ONE)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            result.setCorrectRate(
                    totalMax.compareTo(BigDecimal.ZERO) > 0
                            ? total.divide(totalMax, 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100))
                            : BigDecimal.ZERO);

            // Determine pass/fail
            if (quiz.getPassScore() != null) {
                BigDecimal pct = result.getCorrectRate();
                result.setPassed(pct.compareTo(quiz.getPassScore()) >= 0);
            }
        }

        quizResultRepository.save(result);

        // ── Send email + in-app notification to student ──────────────────────
        if (result.getUser() != null) {
            User student = result.getUser();
            String studentName = student.getFullName() != null ? student.getFullName() : "";
            String email = student.getEmail();

            String assignmentTitle = quiz != null ? quiz.getTitle() : "";
            String className = quiz != null && quiz.getClazz() != null ? quiz.getClazz().getClassName() : "";
            String scoreStr = total != null ? total.intValue() + "/" + totalMax.intValue() : "";
            String passedStatus = Boolean.TRUE.equals(result.getPassed()) ? "Dat" : "Khong dat";

            if (email != null && !email.isBlank()) {
                emailService.sendAssignmentGradedEmail(email, studentName,
                        assignmentTitle, className, scoreStr, passedStatus);
            }

            if (student.getUserId() != null) {
                notificationService.sendAssignmentGraded(
                        Long.valueOf(student.getUserId()),
                        assignmentTitle, className, scoreStr, passedStatus);
            }
        }
    }
}
