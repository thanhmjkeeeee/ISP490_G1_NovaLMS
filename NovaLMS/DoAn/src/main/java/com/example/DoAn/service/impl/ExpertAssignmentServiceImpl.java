package com.example.DoAn.service.impl;

import com.example.DoAn.dto.request.AssignmentQuestionRequestDTO;
import com.example.DoAn.dto.request.QuizRequestDTO;
import com.example.DoAn.model.QuizCategory;
import com.example.DoAn.dto.response.AssignmentPreviewDTO;
import com.example.DoAn.dto.response.SkillSectionSummaryDTO;
import com.example.DoAn.exception.InvalidDataException;
import com.example.DoAn.exception.ResourceNotFoundException;
import com.example.DoAn.model.Question;
import com.example.DoAn.model.Quiz;
import com.example.DoAn.model.QuizQuestion;
import com.example.DoAn.model.User;
import com.example.DoAn.repository.QuestionRepository;
import com.example.DoAn.repository.QuizQuestionRepository;
import com.example.DoAn.repository.QuizRepository;
import com.example.DoAn.repository.UserRepository;
import com.example.DoAn.service.IExpertAssignmentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpertAssignmentServiceImpl implements IExpertAssignmentService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private static final List<String> SEQUENTIAL_SKILLS = Arrays.asList(
        "LISTENING", "READING", "SPEAKING", "WRITING"
    );

    @Override
    public Quiz createAssignment(QuizRequestDTO dto, String expertEmail) throws JsonProcessingException {
        User expert = userRepository.findByEmail(expertEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Expert not found"));

        if (!"ROLE_EXPERT".equals(expert.getRole().getName())) {
            throw new InvalidDataException("Only experts can create assignments");
        }

        QuizCategory category = QuizCategory.fromValue(dto.getQuizCategory());
        if (category == null || !category.isAssignment()) {
            throw new InvalidDataException("Invalid category for assignment");
        }

        Quiz quiz = new Quiz();
        quiz.setTitle(dto.getTitle());
        quiz.setDescription(dto.getDescription());
        quiz.setQuizCategory(String.valueOf(category));
        quiz.setStatus("DRAFT");
        quiz.setUser(expert);
        quiz.setIsSequential(true);
        quiz.setSkillOrder(objectMapper.writeValueAsString(category.getSkillOrder()));

        if (dto.getTimeLimitPerSkill() != null) {
            quiz.setTimeLimitPerSkill(objectMapper.writeValueAsString(dto.getTimeLimitPerSkill()));
        }
        if (dto.getPassScore() != null) quiz.setPassScore(dto.getPassScore());
        if (dto.getMaxAttempts() != null) quiz.setMaxAttempts(dto.getMaxAttempts());
        if (dto.getShowAnswerAfterSubmit() != null) quiz.setShowAnswerAfterSubmit(dto.getShowAnswerAfterSubmit());

        return quizRepository.save(quiz);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, SkillSectionSummaryDTO> getSkillSummaries(Integer quizId) {
        if (!quizRepository.existsById(quizId)) {
            throw new ResourceNotFoundException("Quiz not found");
        }

        Map<String, SkillSectionSummaryDTO> result = new LinkedHashMap<>();
        for (String skill : SEQUENTIAL_SKILLS) {
            long count = quizQuestionRepository.countByQuizIdAndSkill(quizId, skill);
            SkillSectionSummaryDTO dto = new SkillSectionSummaryDTO(
                skill, count, 0L,
                count > 0 ? "READY" : "DRAFT"
            );
            result.put(skill, dto);
        }
        return result;
    }

    @Override
    public void addQuestionsToSection(Integer quizId, AssignmentQuestionRequestDTO dto, String expertEmail) {
        Quiz quiz = quizRepository.findById(quizId)
            .orElseThrow(() -> new ResourceNotFoundException("Quiz not found"));

        if (!Boolean.TRUE.equals(quiz.getIsSequential())) {
            throw new InvalidDataException("This quiz does not support section-based question addition");
        }

        String skill = dto.getSkill();
        if (!SEQUENTIAL_SKILLS.contains(skill)) {
            throw new InvalidDataException("Invalid skill: " + skill);
        }

        List<QuizQuestion> existing = quizQuestionRepository.findByQuizQuizIdAndSkill(quizId, skill);
        Set<Integer> existingIds = new HashSet<>();
        for (QuizQuestion qq : existing) {
            existingIds.add(qq.getQuestion().getQuestionId());
        }

        int nextOrder = existing.size() + 1;
        for (Integer questionId : dto.getQuestionIds()) {
            if (existingIds.contains(questionId)) continue;

            Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));

            QuizQuestion qq = new QuizQuestion();
            qq.setQuiz(quiz);
            qq.setQuestion(question);
            qq.setSkill(skill);
            qq.setOrderIndex(nextOrder++);
            qq.setPoints(BigDecimal.ONE);
            quizQuestionRepository.save(qq);
        }
    }

    @Override
    public void removeQuestion(Integer quizId, Integer questionId) {
        quizQuestionRepository.findByQuizQuizIdAndQuestionQuestionId(quizId, questionId)
            .ifPresent(quizQuestionRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentPreviewDTO getPreview(Integer quizId) {
        Quiz quiz = quizRepository.findById(quizId)
            .orElseThrow(() -> new ResourceNotFoundException("Quiz not found"));

        Map<String, SkillSectionSummaryDTO> summaries = getSkillSummaries(quizId);
        List<String> missingSkills = new ArrayList<>();
        for (SkillSectionSummaryDTO s : summaries.values()) {
            if (s.getQuestionCount() == 0) {
                missingSkills.add(s.getSkill());
            }
        }

        long totalQuestions = summaries.values().stream()
            .mapToLong(SkillSectionSummaryDTO::getQuestionCount).sum();

        Map<String, Integer> timeLimits = null;
        if (quiz.getTimeLimitPerSkill() != null) {
            try {
                timeLimits = objectMapper.readValue(
                    quiz.getTimeLimitPerSkill(),
                    new TypeReference<Map<String, Integer>>() {}
                );
            } catch (Exception ignored) {}
        }

        return new AssignmentPreviewDTO(
            quiz.getQuizId(),
            quiz.getTitle(),
            quiz.getDescription(),
            quiz.getQuizCategory(),
            new ArrayList<>(summaries.values()),
            totalQuestions,
            quiz.getPassScore() != null ? quiz.getPassScore() : BigDecimal.ZERO,
            timeLimits,
            quiz.getPassScore(),
            quiz.getMaxAttempts(),
            quiz.getShowAnswerAfterSubmit(),
            missingSkills,
            missingSkills.isEmpty()
        );
    }

    @Override
    public void publishAssignment(Integer quizId) {
        Quiz quiz = quizRepository.findById(quizId)
            .orElseThrow(() -> new ResourceNotFoundException("Quiz not found"));

        if (!"DRAFT".equals(quiz.getStatus())) {
            throw new InvalidDataException("Only DRAFT quizzes can be published");
        }

        Map<String, SkillSectionSummaryDTO> summaries = getSkillSummaries(quizId);
        List<String> missing = new ArrayList<>();
        for (SkillSectionSummaryDTO s : summaries.values()) {
            if (s.getQuestionCount() == 0) {
                missing.add(s.getSkill());
            }
        }

        if (!missing.isEmpty()) {
            throw new InvalidDataException("Missing questions for skills: " + String.join(", ", missing));
        }

        quiz.setStatus("PUBLISHED");
        quiz.setIsOpen(false);
        quizRepository.save(quiz);
    }

    @Override
    public void changeStatus(Integer quizId, String status) {
        Quiz quiz = quizRepository.findById(quizId)
            .orElseThrow(() -> new ResourceNotFoundException("Quiz not found"));
        quiz.setStatus(status);
        quizRepository.save(quiz);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Quiz> getAssignments(String expertEmail) {
        return quizRepository.findAll().stream()
            .filter(q -> ("COURSE_ASSIGNMENT".equals(q.getQuizCategory())
                       || "MODULE_ASSIGNMENT".equals(q.getQuizCategory()))
                  && expertEmail.equals(q.getUser().getEmail()))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Quiz getAssignment(Integer quizId, String expertEmail) {
        Quiz quiz = quizRepository.findById(quizId)
            .orElseThrow(() -> new ResourceNotFoundException("Quiz not found"));
        if (!expertEmail.equals(quiz.getUser().getEmail())) {
            throw new InvalidDataException("Access denied");
        }
        return quiz;
    }
}
