package com.example.DoAn.service.impl;

import com.example.DoAn.dto.request.HybridSessionCreateDTO;
import com.example.DoAn.dto.response.*;
import com.example.DoAn.model.*;
import com.example.DoAn.repository.*;
import com.example.DoAn.dto.response.QuestionAIResultDTO;
import com.example.DoAn.repository.PlacementTestAnswerRepository;
import com.example.DoAn.service.CourseService;
import com.example.DoAn.service.HomeService;
import com.example.DoAn.service.HybridPlacementService;
import com.example.DoAn.service.PlacementTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HybridPlacementServiceImpl implements HybridPlacementService {

    private final HybridSessionRepository sessionRepository;
    private final HybridSessionQuizRepository sessionQuizRepository;
    private final PlacementTestResultRepository resultRepository;
    private final QuizRepository quizRepository;
    private final PlacementTestService placementTestService;
    private final HomeService homeService;
    private final CourseService courseService;
    private final PlacementTestAnswerRepository answerRepository;

    // 4 kỹ năng cố định (khớp với Question.skill)
    private static final List<String> ALL_SKILLS = List.of(
            "LISTENING", "READING", "WRITING", "SPEAKING"
    );

    @Override
    @Transactional(readOnly = true)
    public List<HybridSkillDTO> getAvailableSkills() {
        List<Quiz> hybridQuizzes = quizRepository.findAll().stream()
                .filter(q -> "ENTRY_TEST".equals(q.getQuizCategory())
                        && "PUBLISHED".equals(q.getStatus())
                        && Boolean.TRUE.equals(q.getIsHybridEnabled()))
                .collect(Collectors.toList());

        Map<String, Long> countBySkill = new HashMap<>();
        for (Quiz q : hybridQuizzes) {
            String skill = q.getTargetSkill();
            if (skill == null) {
                // Fallback: extract từ questions (quiz hybrid cũ chưa có targetSkill)
                Set<String> extracted = extractSkillsFromQuiz(q);
                skill = extracted.isEmpty() ? null : extracted.iterator().next();
            }
            if (skill != null) {
                countBySkill.merge(skill, 1L, Long::sum);
            }
        }

        return ALL_SKILLS.stream()
                .map(skill -> HybridSkillDTO.builder()
                        .skill(skill)
                        .availableQuizzes(countBySkill.getOrDefault(skill, 0L).intValue())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<HybridQuizSummaryDTO>> getQuizzesBySkills(List<String> skills) {
        if (skills == null || skills.isEmpty()) return Collections.emptyMap();

        List<Quiz> hybridQuizzes = quizRepository.findAll().stream()
                .filter(q -> "ENTRY_TEST".equals(q.getQuizCategory())
                        && "PUBLISHED".equals(q.getStatus())
                        && Boolean.TRUE.equals(q.getIsHybridEnabled()))
                .collect(Collectors.toList());

        Map<String, List<HybridQuizSummaryDTO>> result = new LinkedHashMap<>();
        for (String skill : skills) {
            result.put(skill, new ArrayList<>());
        }

        for (Quiz q : hybridQuizzes) {
            String quizSkill = q.getTargetSkill();
            if (quizSkill == null) {
                Set<String> extracted = extractSkillsFromQuiz(q);
                quizSkill = extracted.isEmpty() ? null : extracted.iterator().next();
            }
            if (quizSkill == null) continue;

            for (String requestedSkill : skills) {
                if (quizSkill.equals(requestedSkill)) {
                    int totalQuestions = q.getQuizQuestions() != null ? q.getQuizQuestions().size() : 0;
                    result.get(requestedSkill).add(HybridQuizSummaryDTO.builder()
                            .quizId(q.getQuizId())
                            .title(q.getTitle())
                            .description(q.getDescription())
                            .totalQuestions(totalQuestions)
                            .timeLimitMinutes(q.getTimeLimitMinutes())
                            .skill(quizSkill)
                            .build());
                }
            }
        }
        return result;
    }

    @Override
    @Transactional
    public HybridSessionDTO createSession(HybridSessionCreateDTO request, String guestSessionId) {
        // Validate selections
        if (request.getSelections() == null || request.getSelections().isEmpty()) {
            throw new RuntimeException("Phải chọn ít nhất 1 kỹ năng.");
        }
        if (request.getSelections().size() > 4) {
            throw new RuntimeException("Tối đa 4 kỹ năng.");
        }

        // Validate no duplicate quiz
        Set<Integer> quizIds = request.getSelections().stream()
                .map(HybridSessionCreateDTO.SkillSelection::getQuizId)
                .collect(Collectors.toSet());
        if (quizIds.size() != request.getSelections().size()) {
            throw new RuntimeException("Không được chọn trùng quiz.");
        }

        // Validate each quiz exists and is eligible
        for (HybridSessionCreateDTO.SkillSelection sel : request.getSelections()) {
            Quiz quiz = quizRepository.findById(sel.getQuizId())
                    .orElseThrow(() -> new RuntimeException("Quiz ID " + sel.getQuizId() + " không tồn tại."));
            if (!"ENTRY_TEST".equals(quiz.getQuizCategory())) {
                throw new RuntimeException("Quiz '" + quiz.getTitle() + "' không phải là bài kiểm tra đầu vào.");
            }
            if (!"PUBLISHED".equals(quiz.getStatus())) {
                throw new RuntimeException("Quiz '" + quiz.getTitle() + "' chưa được công bố.");
            }
            if (!Boolean.TRUE.equals(quiz.getIsHybridEnabled())) {
                throw new RuntimeException("Quiz '" + quiz.getTitle() + "' không hỗ trợ chế độ Hybrid.");
            }
        }

        // Build HybridSession
        HybridSession session = HybridSession.builder()
                .guestSessionId(guestSessionId)
                .guestName(request.getGuestName())
                .guestEmail(request.getGuestEmail())
                .totalQuizzes(request.getSelections().size())
                .completedQuizzes(0)
                .status("IN_PROGRESS")
                .build();

        // Build HybridSessionQuiz entries (preserve order of selections)
        List<HybridSessionQuiz> sessionQuizzes = new ArrayList<>();
        for (int i = 0; i < request.getSelections().size(); i++) {
            HybridSessionCreateDTO.SkillSelection sel = request.getSelections().get(i);
            Quiz quiz = quizRepository.findById(sel.getQuizId()).get();
            String skill = sel.getSkill();

            // Derive skill from quiz questions if not explicitly provided
            if (skill == null || skill.isBlank()) {
                skill = deriveSkillFromQuiz(quiz);
            }

            sessionQuizzes.add(HybridSessionQuiz.builder()
                    .hybridSession(session)
                    .quiz(quiz)
                    .skill(skill)
                    .quizOrder(i + 1) // 1-based
                    .status("PENDING")
                    .build());
        }

        session.setSessionQuizzes(sessionQuizzes);
        HybridSession saved = sessionRepository.save(session);

        return HybridSessionDTO.builder()
                .sessionId(saved.getId())
                .totalQuizzes(saved.getTotalQuizzes())
                .completedQuizzes(saved.getCompletedQuizzes())
                .status(saved.getStatus())
                .redirectUrl("/hybrid/" + saved.getId() + "/quiz/1")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public QuizTakingDTO getQuizForSession(Integer sessionId, Integer quizIndex) {
        HybridSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Phiên hybrid không tồn tại."));

        if (!"IN_PROGRESS".equals(session.getStatus())) {
            throw new RuntimeException("Phiên hybrid đã kết thúc hoặc bị hủy.");
        }

        List<HybridSessionQuiz> sqList = session.getSessionQuizzes();
        if (quizIndex < 1 || quizIndex > sqList.size()) {
            throw new RuntimeException("Quiz index không hợp lệ.");
        }

        HybridSessionQuiz sq = sqList.get(quizIndex - 1);

        // Update status to IN_PROGRESS if still PENDING
        if ("PENDING".equals(sq.getStatus())) {
            sq.setStatus("IN_PROGRESS");
            sessionQuizRepository.save(sq);
        }

        // Delegate to existing placement test logic
        return placementTestService.getPlacementTest(sq.getQuiz().getQuizId());
    }

    @Override
    @Transactional(readOnly = true)
    public HybridTransitionDTO getTransitionInfo(Integer sessionId) {
        HybridSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Phiên hybrid không tồn tại."));

        List<HybridSessionQuiz> sqList = session.getSessionQuizzes();

        // Find the most recently completed quiz
        HybridSessionQuiz completed = sqList.stream()
                .filter(sq -> "COMPLETED".equals(sq.getStatus()) && sq.getPlacementTestResultId() != null)
                .reduce((first, second) -> second) // last one
                .orElse(null);

        PlacementTestResult lastResult = null;
        if (completed != null) {
            lastResult = resultRepository.findById(completed.getPlacementTestResultId()).orElse(null);
        }

        HybridTransitionDTO.SectionResult sectionResult = null;
        if (lastResult != null) {
            int totalPoints = sumTotalPoints(lastResult.getQuiz());
            sectionResult = HybridTransitionDTO.SectionResult.builder()
                    .resultId(lastResult.getId())
                    .quizTitle(lastResult.getQuiz().getTitle())
                    .score(lastResult.getScore() != null ? lastResult.getScore() : 0)
                    .totalPoints(totalPoints)
                    .correctRate(lastResult.getCorrectRate())
                    .suggestedLevel(lastResult.getSuggestedLevel())
                    .build();
        }

        int completedCount = (int) sqList.stream().filter(sq -> "COMPLETED".equals(sq.getStatus())).count();
        boolean isLast = completedCount >= sqList.size();

        Integer nextIndex = null;
        if (!isLast) {
            nextIndex = completedCount + 1;
        }

        return HybridTransitionDTO.builder()
                .sessionId(sessionId)
                .completedQuizzes(completedCount)
                .totalQuizzes(sqList.size())
                .currentResult(sectionResult)
                .nextQuizIndex(nextIndex)
                .isLastQuiz(isLast)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public HybridResultDTO getHybridResults(Integer sessionId) {
        HybridSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Phiên hybrid không tồn tại."));

        List<HybridSessionQuiz> sqList = session.getSessionQuizzes();

        List<HybridResultSectionDTO> sections = new ArrayList<>();
        int totalScore = 0;
        int totalPoints = 0;

        for (HybridSessionQuiz sq : sqList) {
            if (sq.getPlacementTestResultId() == null) continue;

            PlacementTestResult r = resultRepository.findById(sq.getPlacementTestResultId()).orElse(null);
            if (r == null) continue;

            // Sum total points
            int pts = sumTotalPoints(r.getQuiz());

            // Per-question AI details for WRITING/SPEAKING
            List<QuestionAIResultDTO> aiQuestions = buildQuestionAIResults(r, sq.getSkill());

            // AI-aware score: prefer totalScoreIncludingAi if available, else sum from answers
            int aiTotal = 0;
            int aiMax = 0;
            boolean hasAnyAIResult = false;
            for (PlacementTestAnswer a : r.getAnswers()) {
                String qt = a.getQuestion().getQuestionType();
                int qpts = getQuestionPoints(r.getQuiz(), a.getQuestion().getQuestionId());
                if ("WRITING".equals(qt) || "SPEAKING".equals(qt)) {
                    if (a.getAiScore() != null) {
                        aiTotal += a.getAiScore();
                        aiMax += qpts;
                        hasAnyAIResult = true;
                    }
                } else {
                    if (Boolean.TRUE.equals(a.getIsCorrect())) {
                        aiTotal += qpts;
                    }
                    aiMax += qpts;
                }
            }
            Integer aiScoreVal = hasAnyAIResult ? aiTotal : null;
            Integer aiTotalPts = aiMax > 0 ? aiMax : null;
            BigDecimal aiRate = aiMax > 0 ? BigDecimal.valueOf(100.0 * aiTotal / aiMax).setScale(2, RoundingMode.HALF_UP) : null;

            sections.add(HybridResultSectionDTO.builder()
                    .resultId(r.getId())
                    .quizTitle(r.getQuiz().getTitle())
                    .skill(capitalize(sq.getSkill()))
                    .score(r.getScore() != null ? r.getScore() : 0)
                    .totalPoints(pts)
                    .correctRate(r.getCorrectRate())
                    .suggestedLevel(r.getSuggestedLevel())
                    .aiScore(aiScoreVal)
                    .aiTotalPoints(aiTotalPts)
                    .aiCorrectRate(aiRate)
                    .aiQuestions(aiQuestions)
                    .build());

            totalScore += r.getScore() != null ? r.getScore() : 0;
            totalPoints += pts;
        }

        // Recalculate overall with AI scores included
        int overallWithAI = 0;
        int overallMaxAI = 0;
        for (HybridResultSectionDTO s : sections) {
            if (s.getAiScore() != null) {
                overallWithAI += s.getAiScore();
                int sectionMax = s.getAiTotalPoints() != null ? s.getAiTotalPoints() : s.getTotalPoints();
                overallMaxAI += sectionMax;
            } else {
                overallWithAI += s.getScore();
                overallMaxAI += s.getTotalPoints();
            }
        }
        BigDecimal overallRate = overallMaxAI > 0
                ? BigDecimal.valueOf(100.0 * overallWithAI / overallMaxAI).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        int finalOverallScore = overallWithAI;
        int finalOverallPoints = overallMaxAI;

        String overallCEFR = calculateCEFRLevel(overallRate.doubleValue());

        // Get suggested courses based on overall CEFR
        List<CoursePublicResponseDTO> suggestedCourses = getSuggestedCoursesForLevel(overallCEFR);

        String overallCEFRIncludingAI = calculateCEFRLevel(overallRate.doubleValue());

        return HybridResultDTO.builder()
                .sessionId(sessionId)
                .guestName(session.getGuestName())
                .guestEmail(session.getGuestEmail())
                .completedAt(session.getUpdatedAt())
                .sections(sections)
                .overallScore(finalOverallScore)
                .overallTotalPoints(finalOverallPoints)
                .overallCorrectRate(overallRate)
                .overallCEFR(overallCEFR)
                .levelDescription(getLevelDescription(overallCEFR))
                .overallScoreIncludingAI(finalOverallScore)
                .overallTotalPointsIncludingAI(finalOverallPoints)
                .overallCorrectRateIncludingAI(overallRate)
                .overallCEFRIncludingAI(overallCEFRIncludingAI)
                .suggestedCourses(suggestedCourses)
                .build();
    }

    private List<QuestionAIResultDTO> buildQuestionAIResults(PlacementTestResult r, String skill) {
        List<QuestionAIResultDTO> aiQuestions = new ArrayList<>();

        // Re-fetch answers directly from DB to get latest AI grading data
        List<PlacementTestAnswer> freshAnswers = answerRepository.findByResultId(r.getId());

        for (PlacementTestAnswer a : freshAnswers) {
            String qt = a.getQuestion().getQuestionType();
            if (!"WRITING".equals(qt) && !"SPEAKING".equals(qt)) continue;

            int qpts = getQuestionPoints(r.getQuiz(), a.getQuestion().getQuestionId());
            aiQuestions.add(QuestionAIResultDTO.builder()
                    .answerId(a.getId())
                    .questionId(a.getQuestion().getQuestionId())
                    .questionType(qt)
                    .questionContent(a.getQuestion().getContent())
                    .pendingAiReview(a.getPendingAiReview())
                    .aiScore(a.getAiScore())
                    .maxPoints(qpts)
                    .aiFeedback(a.getAiFeedback())
                    .aiRubricJson(a.getAiRubricJson())
                    .isCorrect(a.getIsCorrect())
                    .build());
        }
        return aiQuestions;
    }

    private int getQuestionPoints(Quiz quiz, Integer questionId) {
        if (quiz.getQuizQuestions() == null) return 1;
        return quiz.getQuizQuestions().stream()
                .filter(qq -> qq.getQuestion().getQuestionId().equals(questionId))
                .findFirst()
                .map(qq -> qq.getPoints() != null ? qq.getPoints().intValue() : 1)
                .orElse(1);
    }

    // ── Private helpers ──────────────────────────────────────────

    /** Extract distinct skills from a quiz's questions */
    private Set<String> extractSkillsFromQuiz(Quiz quiz) {
        if (quiz.getQuizQuestions() == null) return Collections.emptySet();
        return quiz.getQuizQuestions().stream()
                .map(qq -> qq.getQuestion())
                .filter(Objects::nonNull)
                .map(Question::getSkill)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /** Try to derive the skill from the quiz's questions if not explicitly known */
    private String deriveSkillFromQuiz(Quiz quiz) {
        Set<String> skills = extractSkillsFromQuiz(quiz);
        return skills.isEmpty() ? "LISTENING" : skills.iterator().next();
    }

    /** Sum total points of all questions in a quiz */
    private int sumTotalPoints(Quiz quiz) {
        if (quiz.getQuizQuestions() == null) return 0;
        return quiz.getQuizQuestions().stream()
                .mapToInt(qq -> qq.getPoints() != null ? qq.getPoints().intValue() : 1)
                .sum();
    }

    /** Same CEFR table as PlacementTestServiceImpl */
    private String calculateCEFRLevel(double rateScore) {
        if (rateScore <= 20) return "A1";
        if (rateScore <= 40) return "A2";
        if (rateScore <= 60) return "B1";
        if (rateScore <= 75) return "B2";
        if (rateScore <= 90) return "C1";
        return "C2";
    }

    private String getLevelDescription(String level) {
        switch (level) {
            case "A1": return "Beginner - Có thể hiểu và sử dụng các biểu thức hàng ngày quen thuộc và các cụm từ rất cơ bản.";
            case "A2": return "Elementary - Có thể giao tiếp trong các công việc đơn giản và thường xuyên về các chủ đề quen thuộc.";
            case "B1": return "Intermediate - Có thể xử lý hầu hết các tình huống phát sinh khi đi lại ở khu vực nói ngôn ngữ đó.";
            case "B2": return "Upper Intermediate - Có thể tương tác với mức độ trôi chảy và tự nhiên với người bản xứ trưởng thành.";
            case "C1": return "Advanced - Có thể thể hiện bản thân trôi chảy và tự nhiên mà không cần tìm kiếm biểu thức rõ ràng.";
            case "C2": return "Proficient - Có thể hiểu một cách dễ dàng hầu như tất cả mọi thứ nghe hoặc đọc được.";
            default: return "Chưa xác định";
        }
    }

    private List<CoursePublicResponseDTO> getSuggestedCoursesForLevel(String level) {
        try {
            String keyword = getKeywordForLevel(level);
            var match = courseService.searchAndFilterCourses(keyword, null, "newest", 0, 3);
            if (match != null && match.getItems() != null && !match.getItems().isEmpty()) {
                return match.getItems();
            }
        } catch (Exception ignored) {}
        try {
            var courses = homeService.getFeaturedCourses();
            if (courses.size() > 3) return courses.subList(0, 3);
            return courses;
        } catch (Exception ignored) {}
        return Collections.emptyList();
    }

    private String capitalize(String s) {
        if (s == null || s.isBlank()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    private String getKeywordForLevel(String level) {
        switch (level) {
            case "A1": return "Beginner";
            case "A2": return "Basic";
            case "B1": return "Intermediate";
            case "B2": return "Upper";
            case "C1": return "Advanced";
            case "C2": return "Master";
            default: return "English";
        }
    }
}
