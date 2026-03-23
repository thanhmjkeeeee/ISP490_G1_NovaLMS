package com.example.DoAn.service.impl;

import com.example.DoAn.dto.request.QuestionBankRequestDTO;
import com.example.DoAn.dto.response.PageResponse;
import com.example.DoAn.dto.response.QuestionBankResponseDTO;
import com.example.DoAn.exception.InvalidDataException;
import com.example.DoAn.exception.ResourceNotFoundException;
import com.example.DoAn.model.AnswerOption;
import com.example.DoAn.model.Question;
import com.example.DoAn.model.User;
import com.example.DoAn.repository.AnswerOptionRepository;
import com.example.DoAn.repository.QuestionRepository;
import com.example.DoAn.repository.UserRepository;
import com.example.DoAn.service.IQuestionBankService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionBankServiceImpl implements IQuestionBankService {

    private final QuestionRepository questionRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final UserRepository userRepository;

    private static final Set<String> VALID_QUESTION_TYPES = Set.of(
        "MULTIPLE_CHOICE_SINGLE", "MULTIPLE_CHOICE_MULTI", "FILL_IN_BLANK",
        "MATCHING", "WRITING", "SPEAKING"
    );

    private static final Set<String> VALID_SKILLS = Set.of(
        "LISTENING", "READING", "WRITING", "SPEAKING"
    );

    private static final Set<String> VALID_CEFR_LEVELS = Set.of(
        "A1", "A2", "B1", "B2", "C1", "C2"
    );

    private static final Set<String> VALID_STATUSES = Set.of(
        "DRAFT", "PUBLISHED", "ARCHIVED"
    );

    // --- Loại câu hỏi KHÔNG cần answer options ---
    private static final Set<String> NO_OPTIONS_TYPES = Set.of("WRITING", "SPEAKING");

    // ─── CREATE ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public QuestionBankResponseDTO createQuestion(QuestionBankRequestDTO request, String email) {
        User expert = findExpert(email);
        validateMetadata(request);
        validateAnswerOptions(request);

        Question question = Question.builder()
                .content(request.getContent())
                .questionType(request.getQuestionType())
                .skill(request.getSkill())
                .cefrLevel(request.getCefrLevel())
                .topic(request.getTopic())
                .tags(request.getTags())
                .explanation(request.getExplanation())
                .audioUrl(request.getAudioUrl())
                .imageUrl(request.getImageUrl())
                .status(request.getStatus() != null ? request.getStatus() : "DRAFT")
                .user(expert)
                .build();

        questionRepository.save(question);

        // Tạo answer options (nếu có)
        if (request.getOptions() != null && !request.getOptions().isEmpty()) {
            if (question.getAnswerOptions() == null) {
                question.setAnswerOptions(new ArrayList<>());
            } else {
                question.getAnswerOptions().clear();
            }
            
            for (int i = 0; i < request.getOptions().size(); i++) {
                QuestionBankRequestDTO.AnswerOptionDTO optDTO = request.getOptions().get(i);
                AnswerOption opt = AnswerOption.builder()
                        .question(question)
                        .title(optDTO.getTitle())
                        .correctAnswer(Boolean.TRUE.equals(optDTO.getCorrect()))
                        .orderIndex(optDTO.getOrderIndex() != null ? optDTO.getOrderIndex() : i)
                        .matchTarget(optDTO.getMatchTarget())
                        .build();
                question.getAnswerOptions().add(opt);
            }
            // Sử dụng repository để đảm bảo dữ liệu được lưu tức thì nếu cần
            answerOptionRepository.saveAll(question.getAnswerOptions());
        }

        return toResponseDTO(question);
    }

    // ─── UPDATE ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public QuestionBankResponseDTO updateQuestion(Integer questionId, QuestionBankRequestDTO request, String email) {
        findExpert(email);
        Question question = findQuestion(questionId);

        if ("ARCHIVED".equals(question.getStatus())) {
            throw new InvalidDataException("Không thể cập nhật câu hỏi đã Archived.");
        }

        validateMetadata(request);
        validateAnswerOptions(request);

        question.setContent(request.getContent());
        question.setQuestionType(request.getQuestionType());
        question.setSkill(request.getSkill());
        question.setCefrLevel(request.getCefrLevel());
        question.setTopic(request.getTopic());
        question.setTags(request.getTags());
        question.setExplanation(request.getExplanation());
        question.setAudioUrl(request.getAudioUrl());
        question.setImageUrl(request.getImageUrl());

        if (request.getStatus() != null) {
            question.setStatus(request.getStatus());
        }

        // Cập nhật lại answer options
        if (request.getOptions() != null) {
            if (question.getAnswerOptions() == null) {
                question.setAnswerOptions(new ArrayList<>());
            } else {
                question.getAnswerOptions().clear();
            }
            
            for (int i = 0; i < request.getOptions().size(); i++) {
                QuestionBankRequestDTO.AnswerOptionDTO optDTO = request.getOptions().get(i);
                AnswerOption opt = AnswerOption.builder()
                        .question(question)
                        .title(optDTO.getTitle())
                        .correctAnswer(Boolean.TRUE.equals(optDTO.getCorrect()))
                        .orderIndex(optDTO.getOrderIndex() != null ? optDTO.getOrderIndex() : i)
                        .matchTarget(optDTO.getMatchTarget())
                        .build();
                question.getAnswerOptions().add(opt);
            }
        }

        questionRepository.save(question);
        return toResponseDTO(question);
    }

    // ─── DELETE ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteQuestion(Integer questionId, String email) {
        findExpert(email);
        Question question = findQuestion(questionId);

        long quizUsage = questionRepository.countQuizUsage(questionId);
        if (quizUsage > 0) {
            throw new InvalidDataException(
                "Không thể xóa câu hỏi. Câu hỏi đang được sử dụng trong " + quizUsage + " quiz."
            );
        }

        questionRepository.delete(question);
    }

    // ─── GET BY ID ──────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public QuestionBankResponseDTO getQuestionById(Integer questionId) {
        Question question = findQuestion(questionId);
        return toResponseDTO(question);
    }

    // ─── LIST + FILTER ──────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<QuestionBankResponseDTO> getQuestions(
            String skill, String cefrLevel, String questionType,
            String topic, String status, String keyword,
            int page, int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Question> pageResult = questionRepository.findByFilters(
            skill, cefrLevel, questionType, topic, status, keyword, pageable
        );

        List<QuestionBankResponseDTO> items = pageResult.getContent().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());

        return PageResponse.<QuestionBankResponseDTO>builder()
                .items(items)
                .pageNo(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalPages(pageResult.getTotalPages())
                .totalElements(pageResult.getTotalElements())
                .last(pageResult.isLast())
                .build();
    }

    // ─── CHANGE STATUS ──────────────────────────────────────────────────────

    @Override
    @Transactional
    public QuestionBankResponseDTO changeStatus(Integer questionId, String newStatus, String email) {
        findExpert(email);
        Question question = findQuestion(questionId);

        if (!VALID_STATUSES.contains(newStatus)) {
            throw new InvalidDataException("Trạng thái không hợp lệ: " + newStatus);
        }

        String currentStatus = question.getStatus();
        // Validate transitions: DRAFT→PUBLISHED, PUBLISHED→ARCHIVED, PUBLISHED→DRAFT
        boolean validTransition =
            ("DRAFT".equals(currentStatus) && "PUBLISHED".equals(newStatus)) ||
            ("PUBLISHED".equals(currentStatus) && "ARCHIVED".equals(newStatus)) ||
            ("PUBLISHED".equals(currentStatus) && "DRAFT".equals(newStatus));

        if (!validTransition) {
            throw new InvalidDataException(
                "Không thể chuyển trạng thái từ " + currentStatus + " sang " + newStatus
            );
        }

        question.setStatus(newStatus);
        questionRepository.save(question);
        return toResponseDTO(question);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private User findExpert(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyên gia với email: " + email));
    }

    private Question findQuestion(Integer questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi với ID: " + questionId));
    }

    private void validateMetadata(QuestionBankRequestDTO request) {
        if (!VALID_QUESTION_TYPES.contains(request.getQuestionType())) {
            throw new InvalidDataException("Loại câu hỏi không hợp lệ: " + request.getQuestionType());
        }
        if (!VALID_SKILLS.contains(request.getSkill())) {
            throw new InvalidDataException("Kỹ năng không hợp lệ: " + request.getSkill());
        }
        if (!VALID_CEFR_LEVELS.contains(request.getCefrLevel())) {
            throw new InvalidDataException("Cấp độ CEFR không hợp lệ: " + request.getCefrLevel());
        }
        if (request.getStatus() != null && !VALID_STATUSES.contains(request.getStatus())) {
            throw new InvalidDataException("Trạng thái không hợp lệ: " + request.getStatus());
        }
    }

    private void validateAnswerOptions(QuestionBankRequestDTO request) {
        String type = request.getQuestionType();

        // Writing, Speaking không bắt buộc answer options
        if (NO_OPTIONS_TYPES.contains(type)) {
            return;
        }

        if (request.getOptions() == null || request.getOptions().isEmpty()) {
            throw new InvalidDataException("Câu hỏi loại " + type + " phải có ít nhất 2 đáp án.");
        }

        if ("MULTIPLE_CHOICE_SINGLE".equals(type) || "MULTIPLE_CHOICE_MULTI".equals(type)) {
            if (request.getOptions().size() < 2) {
                throw new InvalidDataException("Câu hỏi trắc nghiệm phải có ít nhất 2 đáp án.");
            }
            long correctCount = request.getOptions().stream()
                    .filter(o -> Boolean.TRUE.equals(o.getCorrect()))
                    .count();
            if (correctCount == 0) {
                throw new InvalidDataException("Phải có ít nhất 1 đáp án đúng.");
            }
            if ("MULTIPLE_CHOICE_SINGLE".equals(type) && correctCount > 1) {
                throw new InvalidDataException("Câu hỏi Single Choice chỉ được có 1 đáp án đúng.");
            }
        }

        if ("MATCHING".equals(type)) {
            boolean allHaveTarget = request.getOptions().stream()
                    .allMatch(o -> o.getMatchTarget() != null && !o.getMatchTarget().isBlank());
            if (!allHaveTarget) {
                throw new InvalidDataException("Câu hỏi Matching: mỗi đáp án phải có match_target.");
            }
        }

        if ("FILL_IN_BLANK".equals(type)) {
            long correctCount = request.getOptions().stream()
                    .filter(o -> Boolean.TRUE.equals(o.getCorrect()))
                    .count();
            if (correctCount == 0) {
                throw new InvalidDataException("Câu hỏi Fill in the Blank phải có ít nhất 1 đáp án đúng.");
            }
        }
    }

    private QuestionBankResponseDTO toResponseDTO(Question question) {
        List<AnswerOption> opts = answerOptionRepository.findByQuestionQuestionId(question.getQuestionId());
        long quizUsage = questionRepository.countQuizUsage(question.getQuestionId());

        List<QuestionBankResponseDTO.AnswerOptionResponseDTO> optDTOs = opts.stream()
                .map(o -> QuestionBankResponseDTO.AnswerOptionResponseDTO.builder()
                        .answerOptionId(o.getAnswerOptionId())
                        .title(o.getTitle())
                        .correctAnswer(o.getCorrectAnswer())
                        .orderIndex(o.getOrderIndex())
                        .matchTarget(o.getMatchTarget())
                        .build())
                .collect(Collectors.toList());

        return QuestionBankResponseDTO.builder()
                .questionId(question.getQuestionId())
                .content(question.getContent())
                .questionType(question.getQuestionType())
                .skill(question.getSkill())
                .cefrLevel(question.getCefrLevel())
                .topic(question.getTopic())
                .tags(question.getTags())
                .explanation(question.getExplanation())
                .audioUrl(question.getAudioUrl())
                .imageUrl(question.getImageUrl())
                .status(question.getStatus())
                .createdByName(question.getUser() != null ? question.getUser().getFullName() : null)
                .createdAt(question.getCreatedAt())
                .updatedAt(question.getUpdatedAt())
                .usedInQuizCount((int) quizUsage)
                .options(optDTOs)
                .build();
    }
}
