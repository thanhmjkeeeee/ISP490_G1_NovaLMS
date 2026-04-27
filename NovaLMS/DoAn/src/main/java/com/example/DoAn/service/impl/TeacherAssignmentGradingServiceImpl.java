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
    private final RegistrationRepository registrationRepository;
    private final com.example.DoAn.service.QuizResultService quizResultService;
    private final com.example.DoAn.service.IAIPromptConfigService aiPromptConfigService;

    // ─── Grading Queue ────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<AssignmentGradingQueueDTO> getGradingQueue(
            String teacherEmail, Integer quizId, Integer classId, List<String> status, Pageable pageable) {

        // Use the new repository method that handles ALL filtering (including status)
        // at the DB level
        // to ensure correct pagination.
        Page<QuizResult> results = quizResultRepository.findAssignmentResultsForTeacherV2(
                classId, status, pageable);

        List<AssignmentGradingQueueDTO> dtos = results.getContent().stream()
                .map(this::toQueueDTO)
                .toList();

        return new org.springframework.data.domain.PageImpl<>(dtos, pageable, results.getTotalElements());
    }

    private boolean hasPendingSpeaking(QuizResult r) {
        List<QuizAnswer> answers = quizAnswerRepository.findByQuizResultResultIdWithQuestion(r.getResultId());
        return answers.stream().anyMatch(a -> {
            Question q = a.getQuestion();
            return ("SPEAKING".equalsIgnoreCase(q.getSkill()) || "SPEAKING".equalsIgnoreCase(q.getQuestionType()))
                    && a.getPointsAwarded() == null;
        });
    }

    private boolean hasPendingWriting(QuizResult r) {
        List<QuizAnswer> answers = quizAnswerRepository.findByQuizResultResultIdWithQuestion(r.getResultId());
        return answers.stream().anyMatch(a -> {
            Question q = a.getQuestion();
            return ("WRITING".equalsIgnoreCase(q.getSkill()) || "WRITING".equalsIgnoreCase(q.getQuestionType()))
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
        if (quiz != null) {
            if (quiz.getClazz() != null) {
                dto.setClassId(Long.valueOf(quiz.getClazz().getClassId()));
                dto.setClassName(quiz.getClazz().getClassName());
            } else if (quiz.getCourse() != null && r.getUser() != null) {
                // Fallback: find class from registration
                registrationRepository
                        .findByUser_UserIdAndCourse_CourseIdAndStatus(r.getUser().getUserId(),
                                quiz.getCourse().getCourseId(), "Approved")
                        .ifPresent(reg -> {
                            if (reg.getClazz() != null) {
                                dto.setClassId(Long.valueOf(reg.getClazz().getClassId()));
                                dto.setClassName(reg.getClazz().getClassName());
                            }
                        });
            }
        }

        dto.setSubmittedAt(r.getSubmittedAt());

        List<QuizAnswer> answers = quizAnswerRepository.findByQuizResultResultIdWithQuestion(r.getResultId());
        dto.setListening(buildSectionStatus(r.getQuiz().getQuizId(), answers, "LISTENING"));
        dto.setReading(buildSectionStatus(r.getQuiz().getQuizId(), answers, "READING"));
        dto.setSpeaking(buildSectionStatus(r.getQuiz().getQuizId(), answers, "SPEAKING"));
        dto.setWriting(buildSectionStatus(r.getQuiz().getQuizId(), answers, "WRITING"));

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
            Integer quizId, List<QuizAnswer> answers, String skill) {

        List<QuizAnswer> sectionAnswers = answers.stream()
                .filter(a -> {
                    String s = a.getQuestion().getSkill();
                    String t = a.getQuestion().getQuestionType();
                    return skill.equalsIgnoreCase(s) || skill.equalsIgnoreCase(t);
                })
                .toList();

        if (sectionAnswers.isEmpty())
            return null;

        BigDecimal totalScore = BigDecimal.ZERO;
        BigDecimal maxScore = BigDecimal.ZERO;
        String aiScore = null;
        String aiFeedback = null;
        String gradingStatus = "AUTO"; // default, overridden below for SPEAKING/WRITING

        for (QuizAnswer a : sectionAnswers) {
            BigDecimal pts = a.getPointsAwarded();
            BigDecimal max = a.getQuestion() != null
                    ? getQuestionMaxPoints(quizId, a.getQuestion())
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
                if (a.getTeacherOverrideScore() != null) {
                    totalScore = totalScore.add(pts != null ? pts : BigDecimal.ZERO);
                    gradingStatus = "GRADED";
                } else if (a.getAiScore() != null) {
                    gradingStatus = "AI_READY";
                    totalScore = totalScore.add(pts != null ? pts : BigDecimal.ZERO);
                    if (aiScore == null)
                        aiScore = a.getAiScore();
                    if (aiFeedback == null)
                        aiFeedback = a.getAiFeedback();
                } else {
                    gradingStatus = "AI_PENDING";
                }
            }
        }

        // Re-derive gradingStatus for SPEAKING/WRITING based on all answers
        if ("SPEAKING".equals(skill) || "WRITING".equals(skill)) {
            boolean allGraded = sectionAnswers.stream()
                    .allMatch(a -> a.getPointsAwarded() != null);
            if (allGraded) {
                gradingStatus = "GRADED";
            } else if (sectionAnswers.stream().anyMatch(a -> "FAILED".equals(a.getAiGradingStatus()))) {
                gradingStatus = "AI_FAILED";
            } else if (sectionAnswers.stream().anyMatch(a -> a.getAiScore() != null)) {
                gradingStatus = "AI_READY";
            } else {
                gradingStatus = "AI_PENDING";
            }
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

    private BigDecimal getQuestionMaxPoints(Integer quizId, Question q) {
        // Priority 1: Use cefrLevel if it's a numeric band (e.g. "4.0", "5.5")
        String cefr = q.getCefrLevel();
        if (cefr != null && !cefr.isBlank()) {
            try {
                double val = Double.parseDouble(cefr.trim());
                if (val > 0) return BigDecimal.valueOf(val);
            } catch (Exception ignored) {}
        }

        // Priority 2: Use the highest configured points across all quizzes (findMaxPointsByQuestionId)
        BigDecimal maxPts = quizQuestionRepository.findMaxPointsByQuestionId(q.getQuestionId());
        if (maxPts != null && maxPts.compareTo(BigDecimal.valueOf(1.0)) > 0) {
            return maxPts;
        }

        // Priority 3: Specific quiz-question link
        Optional<QuizQuestion> qq = quizQuestionRepository.findByQuizQuizIdAndQuestionQuestionId(quizId, q.getQuestionId());
        if (qq.isPresent() && qq.get().getPoints() != null) {
            return qq.get().getPoints();
        }

        return BigDecimal.valueOf(9.0); // Default IELTS max band
    }

    private String deriveOverallStatus(AssignmentGradingQueueDTO dto) {
        boolean spkNeeded = (dto.getSpeaking() != null);
        boolean wrtNeeded = (dto.getWriting() != null);

        if (spkNeeded && "AI_FAILED".equals(dto.getSpeaking().getGradingStatus()))
            return "AI_FAILED";
        if (wrtNeeded && "AI_FAILED".equals(dto.getWriting().getGradingStatus()))
            return "AI_FAILED";

        boolean spkGraded = !spkNeeded || "GRADED".equals(dto.getSpeaking().getGradingStatus());
        boolean wrtGraded = !wrtNeeded || "GRADED".equals(dto.getWriting().getGradingStatus());

        if (spkGraded && wrtGraded)
            return "ALL_GRADED";
        if (!spkGraded && !wrtGraded)
            return "PENDING_BOTH";
        return spkGraded ? "PENDING_WRITING" : "PENDING_SPEAKING";
    }

    // ─── Grading Detail ───────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AssignmentGradingDetailDTO getGradingDetail(Integer resultId, String teacherEmail) {
        QuizResult result = quizResultRepository.findById(resultId)
                .orElseThrow(() -> new ResourceNotFoundException("Kết quả không tìm thấy: " + resultId));

        Quiz quiz = result.getQuiz();
        if (quiz == null)
            throw new ResourceNotFoundException("Quiz không tìm thấy");

        // Fetch all answers eagerly
        List<QuizAnswer> answers = quizAnswerRepository.findByQuizResultResultIdWithQuestion(resultId);

        // Group answers by skill
        Map<String, List<QuizAnswer>> bySkill = new LinkedHashMap<>();
        for (QuizAnswer a : answers) {
            Question q = a.getQuestion();
            String skill = q.getSkill();
            if (skill == null || skill.isBlank() || "GENERAL".equalsIgnoreCase(skill)) {
                if ("WRITING".equalsIgnoreCase(q.getQuestionType()) || "SPEAKING".equalsIgnoreCase(q.getQuestionType())) {
                    skill = q.getQuestionType().toUpperCase();
                }
            }
            if (skill == null) skill = "OTHER";
            bySkill.computeIfAbsent(skill.toUpperCase(), k -> new ArrayList<>()).add(a);
        }

        AssignmentGradingDetailDTO dto = new AssignmentGradingDetailDTO();
        dto.setResultId(resultId);
        dto.setStatus(result.getStatus());
        dto.setAssignmentSessionId(result.getAssignmentSessionId());
        dto.setStudentName(result.getUser() != null ? result.getUser().getFullName() : null);
        dto.setQuizTitle(quiz.getTitle());
        if (quiz.getClazz() != null) {
            dto.setClassName(quiz.getClazz().getClassName());
        } else if (quiz.getCourse() != null && result.getUser() != null) {
            registrationRepository
                    .findByUser_UserIdAndCourse_CourseIdAndStatus(result.getUser().getUserId(),
                            quiz.getCourse().getCourseId(), "Approved")
                    .ifPresent(reg -> {
                        if (reg.getClazz() != null) {
                            dto.setClassName(reg.getClazz().getClassName());
                        }
                    });
        }
        if (dto.getClassName() == null)
            dto.setClassName("Lớp học đã đăng ký");
        dto.setSubmittedAt(result.getSubmittedAt());

        List<AssignmentGradingDetailDTO.SkillSectionDetail> sections = new ArrayList<>();
        BigDecimal autoScore = BigDecimal.ZERO;
        BigDecimal runningTotal = BigDecimal.ZERO;
        BigDecimal totalMaxPoints = BigDecimal.ZERO;

        for (String skill : Arrays.asList("LISTENING", "READING", "SPEAKING", "WRITING")) {
            List<QuizAnswer> skillAnswers = bySkill.getOrDefault(skill, Collections.emptyList());
            if (skillAnswers.isEmpty())
                continue;

            AssignmentGradingDetailDTO.SkillSectionDetail section = new AssignmentGradingDetailDTO.SkillSectionDetail();
            section.setSkill(skill);

            BigDecimal sectionMax = BigDecimal.ZERO;
            BigDecimal sectionTeacherScore = BigDecimal.ZERO;
            List<AssignmentGradingDetailDTO.QuestionGradeItem> items = new ArrayList<>();

            for (QuizAnswer a : skillAnswers) {
                Question q = a.getQuestion();
                BigDecimal maxPts = getQuestionMaxPoints(quiz.getQuizId(), q);
                sectionMax = sectionMax.add(maxPts);

                AssignmentGradingDetailDTO.QuestionGradeItem item = new AssignmentGradingDetailDTO.QuestionGradeItem();
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
                
                // Writing criteria
                item.setWritingTaskAchievement(a.getWritingTaskAchievement());
                item.setWritingCoherenceCohesion(a.getWritingCoherenceCohesion());
                item.setWritingLexicalResource(a.getWritingLexicalResource());
                item.setWritingGrammarAccuracy(a.getWritingGrammarAccuracy());
                
                // Speaking criteria
                item.setSpeakingFluencyCoherence(a.getSpeakingFluencyCoherence());
                item.setSpeakingLexicalResource(a.getSpeakingLexicalResource());
                item.setSpeakingPronunciation(a.getSpeakingPronunciation());
                item.setSpeakingGrammarAccuracy(a.getSpeakingGrammarAccuracy());

                // Resolve student answer ID to text for Multiple Choice
                String studentAns = a.getAnsweredOptions();
                if (studentAns != null && q.getQuestionType() != null &&
                        (q.getQuestionType().equals("MULTIPLE_CHOICE_SINGLE")
                                || q.getQuestionType().equals("MULTIPLE_CHOICE_MULTI"))) {
                    try {
                        if (q.getQuestionType().equals("MULTIPLE_CHOICE_SINGLE")) {
                            Integer selectedId = objectMapper.readValue(studentAns, Integer.class);
                            studentAns = q.getAnswerOptions().stream()
                                    .filter(o -> o.getAnswerOptionId().equals(selectedId))
                                    .findFirst().map(AnswerOption::getTitle).orElse(studentAns);
                        } else {
                            List<Integer> selectedIds = objectMapper.readValue(studentAns,
                                    new TypeReference<List<Integer>>() {
                                    });
                            List<String> titles = q.getAnswerOptions().stream()
                                    .filter(o -> selectedIds.contains(o.getAnswerOptionId()))
                                    .map(AnswerOption::getTitle)
                                    .toList();
                            studentAns = String.join(", ", titles);
                        }
                    } catch (Exception ignored) {
                    }
                }
                // Also clean up simple string JSON quotes
                if (studentAns != null && studentAns.startsWith("\"") && studentAns.endsWith("\"")) {
                    try {
                        studentAns = objectMapper.readValue(studentAns, String.class);
                    } catch (Exception ignored) {
                    }
                }
                item.setStudentAnswer(studentAns);

                // Set options to resolve IDs on frontend
                if (q.getAnswerOptions() != null) {
                    item.setOptions(q.getAnswerOptions().stream()
                            .map(opt -> new AssignmentGradingDetailDTO.QuestionGradeItem.OptionDTO(
                                    opt.getAnswerOptionId(), opt.getTitle()))
                            .toList());
                }

                // Accumulate teacher score for this section
                if (a.getPointsAwarded() != null) {
                    sectionTeacherScore = sectionTeacherScore.add(a.getPointsAwarded());
                }

                items.add(item);
            }

            section.setQuestions(items);
            
            // For IELTS: section max should be the highest configured band (e.g. 4.0 or 5.0)
            if (!items.isEmpty()) {
                // Find the highest maxPoints among questions in this section
                BigDecimal highestMax = items.stream()
                    .<BigDecimal>map(it -> it.getMaxPoints() != null ? it.getMaxPoints() : BigDecimal.ZERO)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.valueOf(9.0));
                
                sectionMax = highestMax;
                
                // sectionTeacherScore is the average band achieved
                BigDecimal sumAwarded = items.stream()
                    .<BigDecimal>map(it -> it.getTeacherScore() != null ? it.getTeacherScore() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                sectionTeacherScore = sumAwarded.divide(BigDecimal.valueOf(items.size()), 1, RoundingMode.HALF_UP);
            }
            
            section.setMaxScore(sectionMax);
            section.setTeacherScore(sectionTeacherScore.compareTo(BigDecimal.ZERO) > 0
                    ? sectionTeacherScore
                    : null);

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

        // For IELTS: totalMaxScore is the average of section maxes
        if (!sections.isEmpty()) {
            BigDecimal sumMax = sections.stream()
                .map(s -> s.getMaxScore() != null ? s.getMaxScore() : BigDecimal.valueOf(9.0))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal avgMax = sumMax.divide(BigDecimal.valueOf(sections.size()), 1, RoundingMode.HALF_UP);
            dto.setTotalMaxScore(avgMax);
        } else {
            dto.setTotalMaxScore(BigDecimal.valueOf(9.0));
        }

        if (result.getSectionScores() != null && !result.getSectionScores().isBlank()) {
            try {
                dto.setSectionScores(objectMapper.readValue(
                        result.getSectionScores(),
                        new TypeReference<Map<String, BigDecimal>>() {
                        }));
            } catch (Exception ignored) {
                /* leave null */ }
        }

        // Set Total Max Score (average of section max scores for IELTS overall band context)
        List<BigDecimal> activeMaxScores = sections.stream()
                .map(s -> s.getMaxScore())
                .filter(m -> m != null && m.compareTo(BigDecimal.ZERO) > 0)
                .toList();

        if (!activeMaxScores.isEmpty()) {
            BigDecimal sum = activeMaxScores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            dto.setTotalMaxScore(sum.divide(BigDecimal.valueOf(activeMaxScores.size()), 1, RoundingMode.HALF_UP));
        } else {
            dto.setTotalMaxScore(BigDecimal.valueOf(9.0));
        }

        if (result.getScore() != null) {
            dto.setTotalScore(BigDecimal.valueOf(result.getScore()));
        } else if (runningTotal.compareTo(BigDecimal.ZERO) > 0) {
            dto.setTotalScore(runningTotal);
        }

        // Fetch dynamic criteria labels
        String cefr = (quiz.getCourse() != null && quiz.getCourse().getLevelTag() != null) ? quiz.getCourse().getLevelTag() : "B2";
        dto.setCriteriaLabels(fetchCriteriaLabels(cefr));

        return dto;
    }

    private String mapCefrToBucket(String cefr) {
        if (cefr == null) return "advanced";
        String c = cefr.toUpperCase();
        if (c.contains("A1") || c.contains("A2") || c.contains("BEGINNER")) return "beginner";
        if (c.contains("B1") || c.contains("B2") || c.contains("INTERMEDIATE")) return "intermediate";
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
                if (root.has("task_achievement")) labels.put("ta", root.path("task_achievement").path("label").asText("Task Achievement"));
                if (root.has("coherence_cohesion")) labels.put("cc", root.path("coherence_cohesion").path("label").asText("Coherence & Cohesion"));
                if (root.has("lexical_resource")) labels.put("lr", root.path("lexical_resource").path("label").asText("Lexical Resource"));
                if (root.has("grammatical_range")) labels.put("gra", root.path("grammatical_range").path("label").asText("Grammar Range & Accuracy"));
            }
        } catch (Exception e) {
            // keep defaults
        }
        return labels;
    }

    // ─── Submit Grading ───────────────────────────────────────────────────────

    @Override
    public Double gradeAssignment(Integer resultId, AssignmentGradingRequestDTO request, String teacherEmail) {
        QuizResult result = quizResultRepository.findById(resultId)
                .orElseThrow(() -> new ResourceNotFoundException("Kết quả không tìm thấy: " + resultId));

        Quiz quiz = result.getQuiz();

        // 1. Save per-question scores
        if (request.getGradingItems() != null) {
            for (AssignmentGradingRequestDTO.QuestionGradingItem item : request.getGradingItems()) {
                QuizAnswer answer = quizAnswerRepository.findByQuizResultResultIdAndQuestionQuestionId(resultId,
                        item.getQuestionId());
                if (answer != null) {
                    BigDecimal awarded = item.getPointsAwarded();
                    
                    // For Writing, if criteria are provided, pointsAwarded is the average
                    String qType = answer.getQuestion().getQuestionType();
                    if ("WRITING".equalsIgnoreCase(qType) && item.getWritingTaskAchievement() != null) {
                        BigDecimal ta = item.getWritingTaskAchievement() != null ? item.getWritingTaskAchievement() : BigDecimal.ZERO;
                        BigDecimal cc = item.getWritingCoherenceCohesion() != null ? item.getWritingCoherenceCohesion() : BigDecimal.ZERO;
                        BigDecimal lr = item.getWritingLexicalResource() != null ? item.getWritingLexicalResource() : BigDecimal.ZERO;
                        BigDecimal gra = item.getWritingGrammarAccuracy() != null ? item.getWritingGrammarAccuracy() : BigDecimal.ZERO;
                        
                        BigDecimal avg = ta.add(cc).add(lr).add(gra).divide(new BigDecimal("4"), 4, RoundingMode.HALF_UP);
                        // IELTS Rounding: round to nearest 0.5
                        awarded = avg.multiply(new BigDecimal("2")).setScale(0, RoundingMode.HALF_UP).divide(new BigDecimal("2"), 1, RoundingMode.HALF_UP);
                        
                        answer.setWritingTaskAchievement(ta);
                        answer.setWritingCoherenceCohesion(cc);
                        answer.setWritingLexicalResource(lr);
                        answer.setWritingGrammarAccuracy(gra);
                    }

                    // For Speaking
                    if ("SPEAKING".equalsIgnoreCase(qType) && item.getSpeakingFluencyCoherence() != null) {
                        BigDecimal fc = item.getSpeakingFluencyCoherence() != null ? item.getSpeakingFluencyCoherence() : BigDecimal.ZERO;
                        BigDecimal lr = item.getSpeakingLexicalResource() != null ? item.getSpeakingLexicalResource() : BigDecimal.ZERO;
                        BigDecimal pr = item.getSpeakingPronunciation() != null ? item.getSpeakingPronunciation() : BigDecimal.ZERO;
                        BigDecimal gra = item.getSpeakingGrammarAccuracy() != null ? item.getSpeakingGrammarAccuracy() : BigDecimal.ZERO;
                        
                        BigDecimal avg = fc.add(lr).add(pr).add(gra).divide(new BigDecimal("4"), 4, RoundingMode.HALF_UP);
                        // IELTS Rounding: round to nearest 0.5
                        awarded = avg.multiply(new BigDecimal("2")).setScale(0, RoundingMode.HALF_UP).divide(new BigDecimal("2"), 1, RoundingMode.HALF_UP);
                        
                        answer.setSpeakingFluencyCoherence(fc);
                        answer.setSpeakingLexicalResource(lr);
                        answer.setSpeakingPronunciation(pr);
                        answer.setSpeakingGrammarAccuracy(gra);
                    }

                    if (awarded != null) {
                        // Validate points
                        BigDecimal maxPts = getQuestionMaxPoints(quiz.getQuizId(), answer.getQuestion());
                        // For WRITING and SPEAKING, allow up to 9.0 (Standard IELTS Band)
                        BigDecimal maxAllowed = maxPts;
                        if ("WRITING".equalsIgnoreCase(qType) || "SPEAKING".equalsIgnoreCase(qType)) {
                            maxAllowed = new BigDecimal("9.0");
                        }
                        
                        if (awarded.compareTo(BigDecimal.ZERO) < 0 || awarded.compareTo(maxAllowed) > 0) {
                            throw new RuntimeException("Số điểm chấm (" + awarded + ") cho câu hỏi " + item.getQuestionId() 
                                    + " không hợp lệ. Điểm phải từ 0 đến " + maxAllowed + ".");
                        }
                        
                        answer.setPointsAwarded(awarded);
                        answer.setTeacherOverrideScore(awarded.toString());
                        answer.setPendingAiReview(false);
                        answer.setAiGradingStatus("COMPLETED");
                    } else {
                        answer.setPointsAwarded(null);
                        answer.setTeacherOverrideScore(null);
                    }
                    
                    answer.setTeacherNote(item.getTeacherNote());
                    answer.setIsCorrect(awarded != null && awarded.compareTo(BigDecimal.ZERO) > 0);
                    quizAnswerRepository.save(answer);
                }
            }
        }

        quizResultRepository.save(result);

        // 3. Status and Passed check
        boolean isFinal = Boolean.TRUE.equals(request.getIsFinal());
        if (isFinal) {
            result.setStatus("GRADED");
        } else {
            result.setStatus("GRADING");
        }
        quizResultRepository.save(result);

        // 4. Recalculate everything using the shared logic (updates overallBand,
        // correctRate, etc.)
        quizResultService.recalculateQuizResult(resultId);

        // Refresh result after recalculation
        result = quizResultRepository.findById(resultId).orElse(result);

        // ── Send email + in-app notification to student ──────────────────────
        if (isFinal && result.getUser() != null) {
            User student = result.getUser();
            String studentName = student.getFullName() != null ? student.getFullName() : "";
            String email = student.getEmail();

            String assignmentTitle = quiz != null ? quiz.getTitle() : "";
            String className = quiz != null && quiz.getClazz() != null ? quiz.getClazz().getClassName() : "";

            // Re-fetch totals for notification
            BigDecimal totalScoreSum = result.getScore() != null ? BigDecimal.valueOf(result.getScore())
                    : BigDecimal.ZERO;
            BigDecimal totalMax = BigDecimal.ZERO;
            if (quiz != null) {
                totalMax = quizQuestionRepository.findByQuizQuizId(quiz.getQuizId()).stream()
                        .map(qq -> qq.getPoints() != null ? qq.getPoints() : BigDecimal.ONE)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }

            String scoreStr = totalScoreSum.intValue() + "/" + totalMax.intValue();
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
        return (result.getScore() != null) ? result.getScore().doubleValue() : 0.0;
    }
}
