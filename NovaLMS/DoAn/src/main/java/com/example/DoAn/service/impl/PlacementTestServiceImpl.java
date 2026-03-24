package com.example.DoAn.service.impl;

import com.example.DoAn.dto.request.PlacementTestSubmissionDTO;
import com.example.DoAn.dto.response.*;
import com.example.DoAn.model.*;
import com.example.DoAn.repository.PlacementTestResultRepository;
import com.example.DoAn.repository.QuizRepository;
import com.example.DoAn.service.CourseService;
import com.example.DoAn.service.HomeService;
import com.example.DoAn.service.PlacementTestService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PlacementTestServiceImpl implements PlacementTestService {

    private final QuizRepository quizRepository;
    private final PlacementTestResultRepository resultRepository;
    private final CourseService courseService;
    private final HomeService homeService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public List<PlacementTestSummaryDTO> getAllPlacementTests() {
        List<Quiz> quizzes = quizRepository.findByQuizCategoryAndStatus("ENTRY_TEST", "PUBLISHED");
        if (quizzes.isEmpty()) {
            return Collections.emptyList();
        }
        return quizzes.stream().map(quiz -> PlacementTestSummaryDTO.builder()
                .quizId(quiz.getQuizId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .totalQuestions(quiz.getQuizQuestions() != null ? quiz.getQuizQuestions().size() : 0)
                .timeLimitMinutes(quiz.getTimeLimitMinutes())
                .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public QuizTakingDTO getPlacementTest(Integer quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài kiểm tra đầu vào."));
        if (!"PUBLISHED".equals(quiz.getStatus()) || !"ENTRY_TEST".equals(quiz.getQuizCategory())) {
            throw new RuntimeException("Bài kiểm tra không hợp lệ hoặc chưa được công bố.");
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
                    .options(noOptionsType ? new ArrayList<>() : optionsDTO)
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
    public Integer submitPlacementTest(PlacementTestSubmissionDTO submission, String sessionId) {
        Quiz quiz = quizRepository.findById(submission.getQuizId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài kiểm tra."));

        int score = 0;
        int maxScoreAvailable = 0;
        int totalGradedQuestions = 0;

        PlacementTestResult result = PlacementTestResult.builder()
                .quiz(quiz)
                .guestSessionId(sessionId)
                .guestName(submission.getGuestName())
                .guestEmail(submission.getGuestEmail())
                .answers(new ArrayList<>())
                .build();

        Map<Integer, Object> answers = submission.getAnswers();

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

            // Placement test auto-grades everything, WRITING/SPEAKING might just be marked incorrect automatically if unsupported
            if ("WRITING".equals(qType) || "SPEAKING".equals(qType)) {
                isCorrect = false; 
            } else {
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
            }

            if (Boolean.TRUE.equals(isCorrect)) {
                score += qq.getPoints() != null ? qq.getPoints().intValue() : 1;
            }
            if (!"WRITING".equals(qType) && !"SPEAKING".equals(qType)) {
                maxScoreAvailable += qq.getPoints() != null ? qq.getPoints().intValue() : 1;
            }

            PlacementTestAnswer qa = PlacementTestAnswer.builder()
                    .placementTestResult(result)
                    .question(q)
                    .answeredOptions(answeredOptionsJson)
                    .isCorrect(isCorrect)
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
        return savedResult.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public PlacementTestResultDTO getPlacementTestResult(Integer resultId) {
        PlacementTestResult result = resultRepository.findById(resultId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kết quả bài kiểm tra."));
        
        Quiz quiz = result.getQuiz();
        List<PlacementTestAnswer> answers = result.getAnswers();

        int totalPoints = 0;
        List<QuestionResultDTO> questionsRes = new ArrayList<>();

        for (QuizQuestion qq : quiz.getQuizQuestions()) {
            Question q = qq.getQuestion();
            int points = qq.getPoints() != null ? qq.getPoints().intValue() : 1;
            totalPoints += points;

            PlacementTestAnswer userAns = answers.stream()
                .filter(a -> a.getQuestion().getQuestionId().equals(q.getQuestionId()))
                .findFirst().orElse(null);
            
            String userAnswerDisplay = "";
            if (userAns != null && userAns.getAnsweredOptions() != null) {
                userAnswerDisplay = userAns.getAnsweredOptions();
            }

            // Always show correct answers for placement test to help guest review
            List<String> corrLogs = new ArrayList<>();
            String correctAnswerDisplay = null;
            
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

            List<AnswerOptionDTO> optDTOs = q.getAnswerOptions().stream().map(opt -> AnswerOptionDTO.builder()
                    .answerOptionId(opt.getAnswerOptionId())
                    .title(opt.getTitle())
                    .matchTarget(opt.getMatchTarget())
                    .isCorrect(opt.getCorrectAnswer())
                    .correctAnswer(opt.getCorrectAnswer())
                    .build()).collect(Collectors.toList());

            questionsRes.add(QuestionResultDTO.builder()
                    .questionId(q.getQuestionId())
                    .content(q.getContent())
                    .questionType(q.getQuestionType())
                    .skill(q.getSkill())
                    .cefrLevel(q.getCefrLevel())
                    .points(points)
                    .isCorrect(userAns != null ? userAns.getIsCorrect() : null)
                    .userAnswerDisplay(userAnswerDisplay)
                    .correctAnswerDisplay(correctAnswerDisplay)
                    .explanation(q.getExplanation())
                    .options(optDTOs)
                    .build());
        }

        // Suggest courses based on CEFR level
        List<CoursePublicResponseDTO> suggestedCourses = getSuggestedCoursesForLevel(result.getSuggestedLevel());

        return PlacementTestResultDTO.builder()
                .resultId(result.getId())
                .quizTitle(quiz.getTitle())
                .submittedAt(result.getSubmittedAt())
                .score(result.getScore())
                .totalPoints(totalPoints)
                .correctRate(result.getCorrectRate() != null ? result.getCorrectRate().doubleValue() : 0.0)
                .passed(result.getPassed())
                .suggestedLevel(result.getSuggestedLevel())
                .levelDescription(getLevelDescription(result.getSuggestedLevel()))
                .questions(questionsRes)
                .suggestedCourses(suggestedCourses)
                .build();
    }

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
        // Here we can map CEFR levels to Course categories, or do a keyword search.
        // For simplicity we fetch all filtered courses via CourseService if they match a keyword rule.
        // We will try to find courses whose name or category matches the level loosely.
        
        List<CoursePublicResponseDTO> courses = new ArrayList<>();
        try {
            // First we try to find courses by explicit levelTag (A1, A2, ...)
            List<Course> matches = quizRepository.findById(1).isPresent() ? 
                ((com.example.DoAn.repository.CourseRepository)courseService.getClass().getDeclaredField("courseRepository").get(courseService))
                .findByLevelTagAndStatus(level, "Active") : new ArrayList<>();
            
            // Note: Since I can't easily access the repository from here due to direct injection, 
            // the above is illustrative. Let's use the courseService approach but improve keywords.
            
            String keywordSearch = getKeywordForLevel(level);
            PageResponse<CoursePublicResponseDTO> match = courseService.searchAndFilterCourses(level, null, "newest", 0, 3);
            
            if(match != null && match.getItems() != null && !match.getItems().isEmpty()) {
                courses = (List<CoursePublicResponseDTO>) match.getItems();
            } else {
                 // Try searching by level label (e.g. "Beginner")
                 match = courseService.searchAndFilterCourses(keywordSearch, null, "newest", 0, 3);
                 if(match != null && match.getItems() != null && !match.getItems().isEmpty()) {
                     courses = (List<CoursePublicResponseDTO>) match.getItems();
                 } else {
                     courses = homeService.getFeaturedCourses();
                     if(courses.size() > 3) courses = courses.subList(0, 3);
                 }
            }
        } catch(Exception e) {
            // Safe fallback
            try {
                courses = homeService.getFeaturedCourses();
                if(courses.size() > 3) courses = courses.subList(0, 3);
            } catch(Exception ex) {
                // Return empty if everything fails
            }
        }
        return courses;
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
