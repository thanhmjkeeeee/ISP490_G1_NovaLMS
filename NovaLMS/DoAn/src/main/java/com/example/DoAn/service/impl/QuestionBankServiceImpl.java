package com.example.DoAn.service.impl;

import com.example.DoAn.dto.request.QuestionBankRequestDTO;
import com.example.DoAn.dto.response.PageResponse;
import com.example.DoAn.dto.response.QuestionBankResponseDTO;
import com.example.DoAn.dto.response.QuestionBankItemDTO;
import com.example.DoAn.exception.InvalidDataException;
import com.example.DoAn.exception.ResourceNotFoundException;
import com.example.DoAn.model.AnswerOption;
import com.example.DoAn.model.Question;
import com.example.DoAn.model.QuestionGroup;
import com.example.DoAn.model.User;
import com.example.DoAn.repository.AnswerOptionRepository;
import com.example.DoAn.repository.QuestionRepository;
import com.example.DoAn.repository.QuestionGroupRepository;
import com.example.DoAn.repository.QuizQuestionRepository;
import com.example.DoAn.repository.UserRepository;
import com.example.DoAn.service.IQuestionBankService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class QuestionBankServiceImpl implements IQuestionBankService {

    private final QuestionRepository questionRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final UserRepository userRepository;
    private final QuestionGroupRepository questionGroupRepository;
    private final QuizQuestionRepository quizQuestionRepository;

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
        "DRAFT", "PUBLISHED", "ARCHIVED", "PENDING_REVIEW"
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
        checkDuplicate(request.getContent(), request.getSkill(), request.getCefrLevel());

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
                .source("EXPERT_BANK")
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
            
            if ("MATCHING".equals(request.getQuestionType())) {
                var allOptions = request.getOptions();
                var lefts = allOptions.stream()
                        .filter(o -> o.getMatchTarget() != null && !o.getMatchTarget().isBlank())
                        .toList();
                
                var manualRights = allOptions.stream()
                        .filter(o -> o.getMatchTarget() == null || o.getMatchTarget().isBlank())
                        .map(QuestionBankRequestDTO.AnswerOptionDTO::getTitle)
                        .collect(java.util.stream.Collectors.toSet());
                
                var implicitRights = lefts.stream()
                        .map(QuestionBankRequestDTO.AnswerOptionDTO::getMatchTarget)
                        .collect(java.util.stream.Collectors.toSet());
                
                manualRights.addAll(implicitRights);
                var uniqueRights = manualRights.stream().toList();

                for (int i = 0; i < lefts.size(); i++) {
                    var left = lefts.get(i);
                    question.getAnswerOptions().add(AnswerOption.builder()
                            .question(question)
                            .title(left.getTitle())
                            .correctAnswer(false)
                            .orderIndex(i)
                            .matchTarget(left.getMatchTarget())
                            .build());
                }
                for (int i = 0; i < uniqueRights.size(); i++) {
                    question.getAnswerOptions().add(AnswerOption.builder()
                            .question(question)
                            .title(uniqueRights.get(i))
                            .correctAnswer(false)
                            .orderIndex(lefts.size() + i)
                            .build());
                }
            } else {
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

        // Block duplicate on content change (exclude self)
        if (request.getContent() != null && !request.getContent().equals(question.getContent())) {
            checkDuplicateOnUpdate(question, request.getContent(), request.getSkill(), request.getCefrLevel());
        }

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
            
            if ("MATCHING".equals(request.getQuestionType())) {
                var allOptions = request.getOptions();
                var lefts = allOptions.stream()
                        .filter(o -> o.getMatchTarget() != null && !o.getMatchTarget().isBlank())
                        .toList();
                
                var manualRights = allOptions.stream()
                        .filter(o -> o.getMatchTarget() == null || o.getMatchTarget().isBlank())
                        .map(QuestionBankRequestDTO.AnswerOptionDTO::getTitle)
                        .collect(java.util.stream.Collectors.toSet());
                
                var implicitRights = lefts.stream()
                        .map(QuestionBankRequestDTO.AnswerOptionDTO::getMatchTarget)
                        .collect(java.util.stream.Collectors.toSet());
                
                manualRights.addAll(implicitRights);
                var uniqueRights = manualRights.stream().toList();

                for (int i = 0; i < lefts.size(); i++) {
                    var left = lefts.get(i);
                    question.getAnswerOptions().add(AnswerOption.builder()
                            .question(question)
                            .title(left.getTitle())
                            .correctAnswer(false)
                            .orderIndex(i)
                            .matchTarget(left.getMatchTarget())
                            .build());
                }
                for (int i = 0; i < uniqueRights.size(); i++) {
                    question.getAnswerOptions().add(AnswerOption.builder()
                            .question(question)
                            .title(uniqueRights.get(i))
                            .correctAnswer(false)
                            .orderIndex(lefts.size() + i)
                            .build());
                }
            } else {
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
    public PageResponse<QuestionBankItemDTO> getQuestions(
            String skill, String cefrLevel, String questionType,
            String topic, String status, String keyword,
            int page, int size) {

        // Chuẩn hóa tham số: "" -> null để @Query IS NULL hoạt động đúng
        skill = (skill != null && !skill.trim().isEmpty()) ? skill : null;
        cefrLevel = (cefrLevel != null && !cefrLevel.trim().isEmpty()) ? cefrLevel : null;
        questionType = (questionType != null && !questionType.trim().isEmpty()) ? questionType : null;
        topic = (topic != null && !topic.trim().isEmpty()) ? topic : null;
        status = (status != null && !status.trim().isEmpty()) ? status : null;
        keyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword : null;

        // 1. Lấy danh sách câu hỏi lẻ
        List<Question> loneQuestions = new ArrayList<>();
        if (questionType == null || (!questionType.equals("PASSAGE"))) {
            loneQuestions = questionRepository.findAllLoneQuestions(
                skill, cefrLevel, questionType, topic, status, keyword
            );
        }

        // 2. Lấy danh sách bộ câu hỏi (Passages)
        List<com.example.DoAn.model.QuestionGroup> groups = new ArrayList<>();
        if (questionType == null || questionType.equals("PASSAGE")) {
            groups = questionGroupRepository.findByFilters(
                skill, cefrLevel, topic, status, keyword
            );
        }

        // 3. Chuyển đổi và Gộp
        List<QuestionBankItemDTO> allItems = new ArrayList<>();
        loneQuestions.forEach(q -> allItems.add(toUnifiedItemDTO(q)));
        groups.forEach(g -> allItems.add(toUnifiedItemDTO(g)));

        // 4. Sắp xếp theo thời gian mới nhất (Nếu có trường createdAt), null đẩy xuống cuối
        allItems.sort((a, b) -> {
            if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
            if (a.getCreatedAt() == null) return 1;
            if (b.getCreatedAt() == null) return -1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });

        // 5. Phân trang thủ công
        int total = allItems.size();
        int start = Math.min(page * size, total);
        int end = Math.min(start + size, total);
        List<QuestionBankItemDTO> pagedItems = allItems.subList(start, end);

        return PageResponse.<QuestionBankItemDTO>builder()
                .items(pagedItems)
                .pageNo(page)
                .pageSize(size)
                .totalPages((int) Math.ceil((double) total / size))
                .totalElements((long) total)
                .last(end >= total)
                .build();
    }

    private QuestionBankItemDTO toUnifiedItemDTO(Question q) {
        return QuestionBankItemDTO.builder()
                .id(q.getQuestionId())
                .type("SINGLE")
                .content(q.getContent())
                .skill(q.getSkill())
                .cefrLevel(q.getCefrLevel())
                .topic(q.getTopic())
                .status(q.getStatus())
                .createdAt(q.getCreatedAt())
                .questionType(q.getQuestionType() != null ? q.getQuestionType() : "MULTIPLE_CHOICE_SINGLE")
                .subQuestionCount(1)
                .usedInQuizCount(questionRepository.countQuizUsage(q.getQuestionId()))
                .build();
    }

    private QuestionBankItemDTO toUnifiedItemDTO(QuestionGroup g) {
        return QuestionBankItemDTO.builder()
                .id(g.getGroupId())
                .type("GROUP")
                .content(g.getGroupContent())
                .skill(g.getSkill())
                .cefrLevel(g.getCefrLevel())
                .topic(g.getTopic())
                .status(g.getStatus()) // Không mặc định PUBLISHED nữa
                .createdAt(g.getCreatedAt())
                .questionType("PASSAGE")
                .subQuestionCount(g.getQuestions() != null ? g.getQuestions().size() : 0)
                .usedInQuizCount(quizQuestionRepository.countByQuestionGroup_GroupId(g.getGroupId()))
                .questions(g.getQuestions() != null ? g.getQuestions().stream()
                        .map(this::toResponseDTO)
                        .collect(java.util.stream.Collectors.toList()) : null)
                .build();
    }

    // ─── CHANGE STATUS ──────────────────────────────────────────────────────

    @Override
    @Transactional
    public QuestionBankResponseDTO changeStatus(Integer questionId, java.util.Map<String, String> body, String email) {
        User expert = findExpert(email);
        String newStatus = body.get("status");
        String type = body.get("type");
        String note = body.get("note");

        if (!VALID_STATUSES.contains(newStatus)) {
            throw new InvalidDataException("Trạng thái không hợp lệ: " + newStatus);
        }

        if ("GROUP".equals(type)) {
            QuestionGroup group = questionGroupRepository.findById(questionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ câu hỏi ID: " + questionId));
            
            String currentStatus = group.getStatus();
            if (newStatus.equals(currentStatus)) return toResponseDTO(group);

            validateTransition(currentStatus, newStatus);
            group.setStatus(newStatus);
            
            // Record review metadata
            group.setReviewerId(Long.valueOf(expert.getUserId()));
            group.setReviewedAt(java.time.LocalDateTime.now());
            group.setReviewNote(note);

            questionGroupRepository.save(group);
            return toResponseDTO(group);
        } else {
            Question question = findQuestion(questionId);
            String currentStatus = question.getStatus();
            
            if (newStatus.equals(currentStatus)) return toResponseDTO(question);

            validateTransition(currentStatus, newStatus);
            question.setStatus(newStatus);
            
            // Record review metadata
            question.setReviewerId(Long.valueOf(expert.getUserId()));
            question.setReviewedAt(java.time.LocalDateTime.now());
            question.setReviewNote(note);

            questionRepository.save(question);
            return toResponseDTO(question);
        }
    }

    private void validateTransition(String currentStatus, String newStatus) {
        // Validate transitions: 
        // DRAFT -> PENDING_REVIEW (Teacher resubmitting)
        // PENDING_REVIEW -> PUBLISHED (Expert approving)
        // PENDING_REVIEW -> DRAFT (Expert rejecting)
        // DRAFT -> PUBLISHED (Expert manual publish)
        // PUBLISHED -> ARCHIVED
        // PUBLISHED -> DRAFT (Expert un-publish)
        boolean valid =
            ("DRAFT".equals(currentStatus) && "PENDING_REVIEW".equals(newStatus)) ||
            ("PENDING_REVIEW".equals(currentStatus) && "PUBLISHED".equals(newStatus)) ||
            ("PENDING_REVIEW".equals(currentStatus) && "DRAFT".equals(newStatus)) ||
            ("DRAFT".equals(currentStatus) && "PUBLISHED".equals(newStatus)) ||
            ("PUBLISHED".equals(currentStatus) && "ARCHIVED".equals(newStatus)) ||
            ("PUBLISHED".equals(currentStatus) && "DRAFT".equals(newStatus));

        if (!valid) {
            throw new InvalidDataException(
                "Không thể chuyển trạng thái từ " + (currentStatus == null ? "NULL" : currentStatus) + " sang " + newStatus
            );
        }
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

    /** Block duplicate: same content + skill + CEFR level. */
    private void checkDuplicate(String content, String skill, String cefrLevel) {
        if (questionRepository.existsByContentIgnoreCaseAndSkillAndCefrLevel(content, skill, cefrLevel)) {
            throw new InvalidDataException("Câu hỏi đã tồn tại: [" + skill + "/" + cefrLevel + "] " + truncate(content, 80));
        }
    }

    /** Same check but excludes the current question being updated. */
    private void checkDuplicateOnUpdate(Question current, String content, String skill, String cefrLevel) {
        boolean exists = questionRepository.existsByContentIgnoreCaseAndSkillAndCefrLevel(content, skill, cefrLevel);
        if (exists) {
            List<Question> found = questionRepository.findAll().stream()
                    .filter(q -> q.getQuestionId().equals(current.getQuestionId())
                            && q.getContent().equalsIgnoreCase(content)
                            && Objects.equals(q.getSkill(), skill)
                            && Objects.equals(q.getCefrLevel(), cefrLevel))
                    .toList();
            if (found.isEmpty()) {
                throw new InvalidDataException("Câu hỏi đã tồn tại: [" + skill + "/" + cefrLevel + "] " + truncate(content, 80));
            }
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "..." : s;
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
            long withTarget = request.getOptions().stream()
                    .filter(o -> o.getMatchTarget() != null && !o.getMatchTarget().isBlank()).count();
            if (request.getOptions().size() < 2) {
                throw new InvalidDataException("Câu hỏi Matching phải có ít nhất 2 hàng dữ liệu.");
            }
            if (withTarget == 0) {
                throw new InvalidDataException(
                    "Câu hỏi Matching cần ít nhất 1 vế trái (nội dung có vế phải ghép nối).");
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

        Function<AnswerOption, QuestionBankResponseDTO.AnswerOptionResponseDTO> toDto = o ->
                QuestionBankResponseDTO.AnswerOptionResponseDTO.builder()
                        .answerOptionId(o.getAnswerOptionId())
                        .title(o.getTitle())
                        .correctAnswer(o.getCorrectAnswer())
                        .orderIndex(o.getOrderIndex())
                        .matchTarget(o.getMatchTarget())
                        .build();

        List<QuestionBankResponseDTO.AnswerOptionResponseDTO> optionDTOs;
        List<QuestionBankResponseDTO.AnswerOptionResponseDTO> rightDTOs = null;

        if ("MATCHING".equals(question.getQuestionType())) {
            List<AnswerOption> lefts = opts.stream()
                    .filter(o -> o.getMatchTarget() != null && !o.getMatchTarget().isBlank())
                    .sorted((a, b) -> Integer.compare(
                            a.getOrderIndex() != null ? a.getOrderIndex() : 0,
                            b.getOrderIndex() != null ? b.getOrderIndex() : 0))
                    .toList();
            List<AnswerOption> rights = opts.stream()
                    .filter(o -> o.getMatchTarget() == null || o.getMatchTarget().isBlank())
                    .sorted((a, b) -> Integer.compare(
                            a.getOrderIndex() != null ? a.getOrderIndex() : 0,
                            b.getOrderIndex() != null ? b.getOrderIndex() : 0))
                    .toList();
            optionDTOs = lefts.stream().map(toDto).toList();
            rightDTOs = rights.stream().map(toDto).toList();
        } else {
            optionDTOs = opts.stream()
                    .sorted((a, b) -> Integer.compare(
                            a.getOrderIndex() != null ? a.getOrderIndex() : 0,
                            b.getOrderIndex() != null ? b.getOrderIndex() : 0))
                    .map(toDto)
                    .toList();
        }

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
                .source(question.getSource())
                .createdByName(question.getUser() != null ? question.getUser().getFullName() : null)
                .createdAt(question.getCreatedAt())
                .updatedAt(question.getUpdatedAt())
                .usedInQuizCount((int) quizUsage)
                .options(optionDTOs)
                .matchRightOptions(rightDTOs)
                .build();
    }

    private QuestionBankResponseDTO toResponseDTO(QuestionGroup group) {
        int quizUsage = (int) quizQuestionRepository.countByQuestionGroup_GroupId(group.getGroupId());
        return QuestionBankResponseDTO.builder()
                .questionId(group.getGroupId())
                .content(group.getGroupContent())
                .questionType("PASSAGE")
                .skill(group.getSkill())
                .cefrLevel(group.getCefrLevel())
                .topic(group.getTopic())
                .explanation(group.getExplanation())
                .audioUrl(group.getAudioUrl())
                .imageUrl(group.getImageUrl())
                .status(group.getStatus())
                .createdByName(group.getUser() != null ? group.getUser().getFullName() : null)
                .createdAt(group.getCreatedAt())
                .usedInQuizCount(quizUsage)
                .build();
    }
}
