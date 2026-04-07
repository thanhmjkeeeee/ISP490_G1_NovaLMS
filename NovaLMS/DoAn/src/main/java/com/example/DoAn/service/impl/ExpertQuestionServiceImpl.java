package com.example.DoAn.service.impl;

import com.example.DoAn.dto.request.AIImportGroupRequestDTO;
import com.example.DoAn.dto.request.AIImportRequestDTO;
import com.example.DoAn.dto.request.QuestionRequestDTO;
import com.example.DoAn.dto.response.AIGenerateResponseDTO;
import com.example.DoAn.dto.request.QuestionGroupRequestDTO;
import com.example.DoAn.dto.response.QuestionResponseDTO;
import com.example.DoAn.dto.response.QuestionGroupResponseDTO;
import com.example.DoAn.exception.InvalidDataException;
import com.example.DoAn.exception.ResourceNotFoundException;
import com.example.DoAn.model.*;
import com.example.DoAn.model.Module;
import com.example.DoAn.repository.*;
import com.example.DoAn.service.IExpertQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpertQuestionServiceImpl implements IExpertQuestionService {

    private final QuestionRepository questionRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final ModuleRepository moduleRepository;
    private final UserRepository userRepository;
    private final QuestionGroupRepository questionGroupRepository;
    private final QuizQuestionRepository quizQuestionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<QuestionResponseDTO> getQuestionsByModule(Integer moduleId, String email) {
        validateExpertOwnsModule(email, moduleId);
        return questionRepository.findByModuleModuleId(moduleId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public QuestionResponseDTO createQuestion(QuestionRequestDTO request, String email) {
        if (request.getModuleId() != null) {
            validateExpertOwnsModule(email, request.getModuleId());
        }
        validateAnswerOptions(request.getQuestionType(), request.getOptions());

        Module module = null;
        if (request.getModuleId() != null) {
            module = moduleRepository.findById(request.getModuleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương."));
        }
        User expert = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyên gia."));

        checkDuplicate(request.getContent(), request.getSkill(), request.getCefrLevel());

        Question question = Question.builder()
                .module(module)
                .user(expert)
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
                .build();
        questionRepository.save(question);

        if ("MATCHING".equals(request.getQuestionType())) {
            var allOptions = request.getOptions();
            var lefts = allOptions.stream()
                    .filter(o -> o.getMatchTarget() != null && !o.getMatchTarget().isBlank())
                    .toList();
            
            // Lấy danh sách các vế phải (targets): bao gồm các hàng chỉ có target HOẶC được trích xuất từ cặp ghép
            var manualRights = allOptions.stream()
                    .filter(o -> o.getMatchTarget() == null || o.getMatchTarget().isBlank())
                    .map(QuestionRequestDTO.AnswerOptionDTO::getTitle)
                    .collect(java.util.stream.Collectors.toSet());
            
            var implicitRights = lefts.stream()
                    .map(QuestionRequestDTO.AnswerOptionDTO::getMatchTarget)
                    .collect(java.util.stream.Collectors.toSet());
            
            manualRights.addAll(implicitRights);
            var uniqueRights = manualRights.stream().toList();

            for (int i = 0; i < lefts.size(); i++) {
                var left = lefts.get(i);
                answerOptionRepository.save(AnswerOption.builder()
                        .question(question)
                        .title(left.getTitle())
                        .correctAnswer(false)
                        .orderIndex(i)
                        .matchTarget(left.getMatchTarget())
                        .build());
            }
            for (int i = 0; i < uniqueRights.size(); i++) {
                answerOptionRepository.save(AnswerOption.builder()
                        .question(question)
                        .title(uniqueRights.get(i))
                        .correctAnswer(false)
                        .orderIndex(lefts.size() + i)
                        .build());
            }
        } else {
            for (QuestionRequestDTO.AnswerOptionDTO optDTO : request.getOptions()) {
                AnswerOption opt = AnswerOption.builder()
                        .question(question)
                        .title(optDTO.getTitle())
                        .correctAnswer(Boolean.TRUE.equals(optDTO.getCorrect()))
                        .build();
                answerOptionRepository.save(opt);
            }
        }

        return toResponseDTO(question);
    }

    @Override
    @Transactional
    public QuestionResponseDTO updateQuestion(Integer questionId, QuestionRequestDTO request, String email) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi."));
        if (question.getModule() != null) {
            validateExpertOwnsModule(email, question.getModule().getModuleId());
        }

        if (request.getModuleId() != null && (question.getModule() == null
                || !question.getModule().getModuleId().equals(request.getModuleId()))) {
            Module newModule = moduleRepository.findById(request.getModuleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương."));
            question.setModule(newModule);
        }

        if (request.getOptions() != null && !request.getOptions().isEmpty()) {
            validateAnswerOptions(request.getQuestionType() != null ? request.getQuestionType() : question.getQuestionType(), request.getOptions());
        }

        if (request.getContent() != null) question.setContent(request.getContent());
        if (request.getContent() != null) {
            checkDuplicateOnUpdate(question, request.getContent(), request.getSkill(), request.getCefrLevel());
        }
        if (request.getQuestionType() != null) question.setQuestionType(request.getQuestionType());
        if (request.getSkill() != null) question.setSkill(request.getSkill());
        if (request.getCefrLevel() != null) question.setCefrLevel(request.getCefrLevel());
        if (request.getTopic() != null) question.setTopic(request.getTopic());
        if (request.getTags() != null) question.setTags(request.getTags());
        if (request.getExplanation() != null) question.setExplanation(request.getExplanation());
        if (request.getAudioUrl() != null) question.setAudioUrl(request.getAudioUrl());
        if (request.getImageUrl() != null) question.setImageUrl(request.getImageUrl());
        if (request.getStatus() != null) question.setStatus(request.getStatus());

        if (request.getOptions() != null && !request.getOptions().isEmpty()) {
            answerOptionRepository.deleteByQuestionQuestionId(questionId);
            if ("MATCHING".equals(request.getQuestionType())) {
                var allOptions = request.getOptions();
                var lefts = allOptions.stream()
                        .filter(o -> o.getMatchTarget() != null && !o.getMatchTarget().isBlank())
                        .toList();
                
                var manualRights = allOptions.stream()
                        .filter(o -> o.getMatchTarget() == null || o.getMatchTarget().isBlank())
                        .map(QuestionRequestDTO.AnswerOptionDTO::getTitle)
                        .collect(java.util.stream.Collectors.toSet());
                
                var implicitRights = lefts.stream()
                        .map(QuestionRequestDTO.AnswerOptionDTO::getMatchTarget)
                        .collect(java.util.stream.Collectors.toSet());
                
                manualRights.addAll(implicitRights);
                var uniqueRights = manualRights.stream().toList();

                for (int i = 0; i < lefts.size(); i++) {
                    var left = lefts.get(i);
                    answerOptionRepository.save(AnswerOption.builder()
                            .question(question)
                            .title(left.getTitle())
                            .correctAnswer(false)
                            .orderIndex(i)
                            .matchTarget(left.getMatchTarget())
                            .build());
                }
                for (int i = 0; i < uniqueRights.size(); i++) {
                    answerOptionRepository.save(AnswerOption.builder()
                            .question(question)
                            .title(uniqueRights.get(i))
                            .correctAnswer(false)
                            .orderIndex(lefts.size() + i)
                            .build());
                }
            } else {
                for (QuestionRequestDTO.AnswerOptionDTO optDTO : request.getOptions()) {
                    AnswerOption opt = AnswerOption.builder()
                            .question(question)
                            .title(optDTO.getTitle())
                            .correctAnswer(Boolean.TRUE.equals(optDTO.getCorrect()))
                            .build();
                    answerOptionRepository.save(opt);
                }
            }
        }

        questionRepository.save(question);
        return toResponseDTO(question);
    }

    @Override
    @Transactional
    public void deleteQuestion(Integer questionId, String email) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi."));
        if (question.getModule() != null) {
            validateExpertOwnsModule(email, question.getModule().getModuleId());
        }
        questionRepository.delete(question);
    }

    private QuestionResponseDTO toResponseDTO(Question question) {
        List<AnswerOption> opts = answerOptionRepository.findByQuestionQuestionId(question.getQuestionId());
        Module module = question.getModule();

        List<QuestionResponseDTO.AnswerOptionResponseDTO> optDTOs = opts.stream()
                .sorted((a, b) -> Integer.compare(
                        a.getOrderIndex() != null ? a.getOrderIndex() : 0,
                        b.getOrderIndex() != null ? b.getOrderIndex() : 0))
                .map(o -> QuestionResponseDTO.AnswerOptionResponseDTO.builder()
                        .answerOptionId(o.getAnswerOptionId())
                        .title(o.getTitle())
                        .correctAnswer(o.getCorrectAnswer())
                        .matchTarget(o.getMatchTarget())
                        .orderIndex(o.getOrderIndex())
                        .build())
                .collect(Collectors.toList());

        int correctCount = (int) opts.stream()
                .filter(o -> Boolean.TRUE.equals(o.getCorrectAnswer()))
                .count();

        // For MATCHING questions: split into left (with matchTarget) and right (without matchTarget) options
        List<QuestionResponseDTO.AnswerOptionResponseDTO> matchRightOpts = null;
        List<QuestionResponseDTO.AnswerOptionResponseDTO> leftOpts = null;
        if ("MATCHING".equals(question.getQuestionType())) {
            leftOpts = new java.util.ArrayList<>();
            matchRightOpts = new java.util.ArrayList<>();
            for (QuestionResponseDTO.AnswerOptionResponseDTO o : optDTOs) {
                if (o.getMatchTarget() != null) {
                    leftOpts.add(o);
                } else {
                    matchRightOpts.add(o);
                }
            }
        }

        return QuestionResponseDTO.builder()
                .questionId(question.getQuestionId())
                .moduleId(module != null ? module.getModuleId() : null)
                .moduleName(module != null ? module.getModuleName() : null)
                .content(question.getContent())
                .questionType(question.getQuestionType())
                .skill(question.getSkill())
                .cefrLevel(question.getCefrLevel())
                .status(question.getStatus())
                .optionCount(opts.size())
                .correctOptionCount(correctCount)
                .options(leftOpts != null ? leftOpts : optDTOs)
                .matchRightOptions(matchRightOpts)
                .build();
    }

    private void validateAnswerOptions(String type, List<QuestionRequestDTO.AnswerOptionDTO> options) {
        if ("WRITING".equals(type) || "SPEAKING".equals(type)) {
            return;
        }

        if (options == null || options.isEmpty()) {
            throw new InvalidDataException("Câu hỏi loại " + type + " phải có nội dung đáp án.");
        }

        if ("MATCHING".equals(type)) {
            long withTarget = options.stream()
                    .filter(o -> o.getMatchTarget() != null && !o.getMatchTarget().isBlank())
                    .count();
            if (options.size() < 2) {
                throw new InvalidDataException("Câu hỏi Matching cần ít nhất 2 hàng dữ liệu.");
            }
            if (withTarget == 0) {
                throw new InvalidDataException("Câu hỏi Matching cần ít nhất 1 vế trái (nội dung có vế phải ghép nối).");
            }
        } else {
            if (options.size() < 2) {
                throw new InvalidDataException("Câu hỏi trắc nghiệm phải có ít nhất 2 đáp án.");
            }
            long correctCount = options.stream()
                    .filter(o -> Boolean.TRUE.equals(o.getCorrect()))
                    .count();
            if (correctCount == 0) {
                throw new InvalidDataException("Phải có ít nhất 1 đáp án đúng (correct=true).");
            }
            if ("MULTIPLE_CHOICE_SINGLE".equals(type) && correctCount > 1) {
                throw new InvalidDataException("Câu hỏi chỉ được có duy nhất 1 đáp án đúng.");
            }
        }
    }

    @Override
    @Transactional
    public int saveAIQuestions(AIImportRequestDTO request, String email) {
        User expert = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyên gia."));

        int saved = 0;
        for (AIImportRequestDTO.AIQuestionDTO qdto : request.getQuestions()) {
            String skill = qdto.getSkill() != null ? qdto.getSkill().toUpperCase() : "READING";
            String cefr = qdto.getCefrLevel() != null ? qdto.getCefrLevel().toUpperCase() : "B1";
            checkDuplicate(qdto.getContent(), skill, cefr);

            Question question = Question.builder()
                    .content(qdto.getContent())
                    .questionType(qdto.getQuestionType() != null ? qdto.getQuestionType() : "MULTIPLE_CHOICE_SINGLE")
                    .skill(qdto.getSkill() != null ? qdto.getSkill().toUpperCase() : "READING")
                    .cefrLevel(qdto.getCefrLevel() != null ? qdto.getCefrLevel().toUpperCase() : "B1")
                    .topic(qdto.getTopic())
                    .explanation(qdto.getExplanation())
                    .audioUrl(qdto.getAudioUrl())
                    .imageUrl(qdto.getImageUrl())
                    .status("DRAFT")
                    .source("EXPERT_BANK")
                    .createdMethod("AI_GENERATED")
                    .user(expert)
                    .build();
            questionRepository.save(question);

            if (qdto.getOptions() != null && !qdto.getOptions().isEmpty()) {
                int idx = 0;
                for (AIImportRequestDTO.AIOptionDTO opt : qdto.getOptions()) {
                    AnswerOption ao = AnswerOption.builder()
                            .question(question)
                            .title(opt.getTitle())
                            .correctAnswer(Boolean.TRUE.equals(opt.getCorrect()))
                            .orderIndex(idx++)
                            .build();
                    answerOptionRepository.save(ao);
                }
            } else if (qdto.getMatchLeft() != null && qdto.getMatchRight() != null
                    && qdto.getCorrectPairs() != null) {
                List<String> left = qdto.getMatchLeft();
                List<String> right = qdto.getMatchRight();
                List<Integer> pairs = qdto.getCorrectPairs();
                for (int i = 0; i < left.size(); i++) {
                    int rightIdx = pairs.get(i) - 1;
                    answerOptionRepository.save(AnswerOption.builder()
                            .question(question)
                            .title(left.get(i))
                            .correctAnswer(false)
                            .orderIndex(i)
                            .matchTarget(right.get(rightIdx))
                            .build());
                    answerOptionRepository.save(AnswerOption.builder()
                            .question(question)
                            .title(right.get(rightIdx))
                            .correctAnswer(false)
                            .orderIndex(left.size() + i)
                            .build());
                }
            }
            saved++;
        }
        return saved;
    }

    @Override
    @Transactional
    public int saveAIQuestionGroup(AIImportGroupRequestDTO request, String email) {
        User expert = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyên gia."));

        if (request.getQuestions() == null || request.getQuestions().isEmpty()) {
            throw new InvalidDataException("Bộ câu hỏi phải có ít nhất 1 câu hỏi con.");
        }

        QuestionGroup group = QuestionGroup.builder()
                .groupContent(request.getPassage())
                .audioUrl(request.getAudioUrl())
                .imageUrl(request.getImageUrl())
                .skill(request.getSkill() != null ? request.getSkill().toUpperCase() : "READING")
                .cefrLevel(request.getCefrLevel() != null ? request.getCefrLevel().toUpperCase() : "B1")
                .topic(request.getTopic())
                .explanation(request.getExplanation())
                .status(request.getStatus() != null ? request.getStatus() : "DRAFT")
                .user(expert)
                .build();
        questionGroupRepository.save(group);

        int saved = 0;
        for (AIGenerateResponseDTO.QuestionDTO qdto : request.getQuestions()) {
            String skill = qdto.getSkill() != null ? qdto.getSkill().toUpperCase()
                    : (group.getSkill() != null ? group.getSkill() : "READING");
            String cefr = qdto.getCefrLevel() != null ? qdto.getCefrLevel().toUpperCase()
                    : (group.getCefrLevel() != null ? group.getCefrLevel() : "B1");
            checkDuplicate(qdto.getContent(), skill, cefr);

            Question question = Question.builder()
                    .questionGroup(group)
                    .user(expert)
                    .content(qdto.getContent())
                    .questionType(qdto.getQuestionType() != null ? qdto.getQuestionType() : "MULTIPLE_CHOICE_SINGLE")
                    .skill(qdto.getSkill() != null ? qdto.getSkill().toUpperCase()
                            : (group.getSkill() != null ? group.getSkill() : "READING"))
                    .cefrLevel(qdto.getCefrLevel() != null ? qdto.getCefrLevel().toUpperCase()
                            : (group.getCefrLevel() != null ? group.getCefrLevel() : "B1"))
                    .topic(qdto.getTopic() != null ? qdto.getTopic() : group.getTopic())
                    .explanation(qdto.getExplanation())
                    .status("PUBLISHED")
                    .source("EXPERT_BANK")
                    .createdMethod("AI_GENERATED")
                    .build();
            questionRepository.save(question);

            if (qdto.getOptions() != null && !qdto.getOptions().isEmpty()) {
                int idx = 0;
                for (AIGenerateResponseDTO.OptionDTO opt : qdto.getOptions()) {
                    answerOptionRepository.save(AnswerOption.builder()
                            .question(question)
                            .title(opt.getTitle())
                            .correctAnswer(Boolean.TRUE.equals(opt.getCorrect()))
                            .orderIndex(idx++)
                            .build());
                }
            } else if (qdto.getMatchLeft() != null && qdto.getMatchRight() != null
                    && qdto.getCorrectPairs() != null) {
                List<String> left = qdto.getMatchLeft();
                List<String> right = qdto.getMatchRight();
                List<Integer> pairs = qdto.getCorrectPairs();
                for (int i = 0; i < left.size(); i++) {
                    int rightIdx = pairs.get(i) - 1;
                    answerOptionRepository.save(AnswerOption.builder()
                            .question(question)
                            .title(left.get(i))
                            .correctAnswer(false)
                            .orderIndex(i)
                            .matchTarget(right.get(rightIdx))
                            .build());
                    answerOptionRepository.save(AnswerOption.builder()
                            .question(question)
                            .title(right.get(rightIdx))
                            .correctAnswer(false)
                            .orderIndex(left.size() + i)
                            .build());
                }
            }
            saved++;
        }
        return saved;
    }

    @Override
    @Transactional
    public QuestionGroupResponseDTO createQuestionGroup(QuestionGroupRequestDTO request, String email) {
        User expert = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyên gia."));

        QuestionGroup group = QuestionGroup.builder()
                .groupContent(request.getGroupContent())
                .audioUrl(request.getAudioUrl())
                .imageUrl(request.getImageUrl())
                .skill(request.getSkill())
                .cefrLevel(request.getCefrLevel())
                .topic(request.getTopic())
                .explanation(request.getExplanation())
                .status(request.getStatus() != null ? request.getStatus() : "PUBLISHED")
                .user(expert)
                .build();
        
        questionGroupRepository.save(group);

        if (request.getQuestions() != null) {
            if (group.getQuestions() == null) {
                group.setQuestions(new java.util.ArrayList<>());
            }
            for (QuestionRequestDTO qReq : request.getQuestions()) {
                group.getQuestions().add(saveChildQuestion(group, qReq, expert));
            }
        }

        return getQuestionGroupById(group.getGroupId(), email);
    }

    @Override
    @Transactional
    public QuestionGroupResponseDTO updateQuestionGroup(Integer groupId, QuestionGroupRequestDTO request, String email) {
        QuestionGroup group = questionGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ câu hỏi."));
        
        User expert = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyên gia."));

        if (!group.getUser().getUserId().equals(expert.getUserId())) {
            throw new InvalidDataException("Bạn không có quyền sửa bộ câu hỏi này.");
        }

        if (request.getGroupContent() != null) group.setGroupContent(request.getGroupContent());
        if (request.getAudioUrl() != null) group.setAudioUrl(request.getAudioUrl());
        if (request.getImageUrl() != null) group.setImageUrl(request.getImageUrl());
        if (request.getSkill() != null) group.setSkill(request.getSkill());
        if (request.getCefrLevel() != null) group.setCefrLevel(request.getCefrLevel());
        if (request.getTopic() != null) group.setTopic(request.getTopic());
        if (request.getExplanation() != null) group.setExplanation(request.getExplanation());
        if (request.getStatus() != null) group.setStatus(request.getStatus());

        // Cập nhật câu hỏi con: update in-place, tránh xóa câu đang dùng trong quiz
        if (request.getQuestions() != null) {
            List<Question> existingChildren = group.getQuestions() != null
                    ? new java.util.ArrayList<>(group.getQuestions())
                    : new java.util.ArrayList<>();

            // Lấy danh sách ID câu hỏi con mà request muốn giữ lại (có questionId)
            List<Integer> keepIds = request.getQuestions().stream()
                    .filter(q -> q.getQuestionId() != null)
                    .map(QuestionRequestDTO::getQuestionId)
                    .collect(java.util.stream.Collectors.toList());

            // Xóa những câu không còn trong request VÀ không bị dùng trong quiz
            for (Question old : existingChildren) {
                if (!keepIds.contains(old.getQuestionId())) {
                    long quizUsage = quizQuestionRepository.countByQuestion_QuestionId(old.getQuestionId());
                    if (quizUsage == 0) {
                        answerOptionRepository.deleteByQuestionQuestionId(old.getQuestionId());
                        questionRepository.delete(old);
                    }
                    // Nếu đang dùng trong quiz → giữ lại, không làm gì
                }
            }

            // Update hoặc tạo mới từng câu trong request
            List<Question> newChildren = new java.util.ArrayList<>();
            for (QuestionRequestDTO qReq : request.getQuestions()) {
                if (qReq.getQuestionId() != null) {
                    // Update in-place
                    questionRepository.findById(qReq.getQuestionId()).ifPresent(q -> {
                        if (qReq.getContent() != null) q.setContent(qReq.getContent());
                        if (qReq.getQuestionType() != null) q.setQuestionType(qReq.getQuestionType());
                        q.setSkill(group.getSkill());
                        q.setCefrLevel(group.getCefrLevel());
                        q.setTopic(group.getTopic());
                        if (qReq.getExplanation() != null) q.setExplanation(qReq.getExplanation());
                        questionRepository.save(q);
                        // Update answer options
                        if (qReq.getOptions() != null && !qReq.getOptions().isEmpty()) {
                            answerOptionRepository.deleteByQuestionQuestionId(q.getQuestionId());
                            for (QuestionRequestDTO.AnswerOptionDTO optDTO : qReq.getOptions()) {
                                answerOptionRepository.save(AnswerOption.builder()
                                        .question(q)
                                        .title(optDTO.getTitle())
                                        .correctAnswer(Boolean.TRUE.equals(optDTO.getCorrect()))
                                        .build());
                            }
                        }
                        newChildren.add(q);
                    });
                } else {
                    // Tạo mới
                    newChildren.add(saveChildQuestion(group, qReq, expert));
                }
            }
            // QUAN TRỌNG: Không được thay reference của collection (setQuestions) vì Hibernate quản lý nó.
            // Phải clear() rồi addAll() để giữ nguyên reference gốc.
            if (group.getQuestions() == null) {
                group.setQuestions(new java.util.ArrayList<>());
            }
            // Chỉ remove khỏi collection những câu đã bị xóa thực sự
            group.getQuestions().removeIf(q -> !keepIds.contains(q.getQuestionId()) &&
                    quizQuestionRepository.countByQuestion_QuestionId(q.getQuestionId()) == 0);
            // Thêm các câu mới tạo vào collection
            for (Question child : newChildren) {
                if (child.getQuestionId() != null && group.getQuestions().stream()
                        .noneMatch(q -> q.getQuestionId().equals(child.getQuestionId()))) {
                    group.getQuestions().add(child);
                }
            }
        }

        questionGroupRepository.save(group);
        return getQuestionGroupById(groupId, email);
    }

    @Override
    @Transactional
    public void deleteQuestionGroup(Integer groupId, String email) {
        QuestionGroup group = questionGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ câu hỏi."));
        User expert = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyên gia."));

        if (!group.getUser().getUserId().equals(expert.getUserId())) {
            throw new InvalidDataException("Bạn không có quyền xóa bộ câu hỏi này.");
        }

        questionGroupRepository.delete(group);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionGroupResponseDTO> getMyQuestionGroups(String email) {
        return questionGroupRepository.findByUserEmail(email).stream()
                .map(this::toGroupResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionGroupResponseDTO getQuestionGroupById(Integer groupId, String email) {
        QuestionGroup group = questionGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ câu hỏi."));
        return toGroupResponseDTO(group);
    }

    private Question saveChildQuestion(QuestionGroup group, QuestionRequestDTO qReq, User expert) {
        String skill = group.getSkill() != null ? group.getSkill() : "READING";
        String cefr = group.getCefrLevel() != null ? group.getCefrLevel() : "B1";
        checkDuplicate(qReq.getContent(), skill, cefr);

        Question q = Question.builder()
                .questionGroup(group)
                .user(expert)
                .content(qReq.getContent())
                .questionType(qReq.getQuestionType())
                .skill(group.getSkill())
                .cefrLevel(group.getCefrLevel())
                .topic(group.getTopic())
                .explanation(qReq.getExplanation())
                .status("PUBLISHED")
                .build();
        
        validateAnswerOptions(qReq.getQuestionType(), qReq.getOptions());

        q = questionRepository.save(q);

        if (qReq.getOptions() != null) {
            if ("MATCHING".equals(qReq.getQuestionType())) {
                var allOptions = qReq.getOptions();
                var lefts = allOptions.stream()
                        .filter(o -> o.getMatchTarget() != null && !o.getMatchTarget().isBlank())
                        .toList();
                
                var manualRights = allOptions.stream()
                        .filter(o -> o.getMatchTarget() == null || o.getMatchTarget().isBlank())
                        .map(QuestionRequestDTO.AnswerOptionDTO::getTitle)
                        .collect(java.util.stream.Collectors.toSet());
                
                var implicitRights = lefts.stream()
                        .map(QuestionRequestDTO.AnswerOptionDTO::getMatchTarget)
                        .collect(java.util.stream.Collectors.toSet());
                
                manualRights.addAll(implicitRights);
                var uniqueRights = manualRights.stream().toList();

                for (int i = 0; i < lefts.size(); i++) {
                    var left = lefts.get(i);
                    answerOptionRepository.save(AnswerOption.builder()
                            .question(q)
                            .title(left.getTitle())
                            .correctAnswer(false)
                            .orderIndex(i)
                            .matchTarget(left.getMatchTarget())
                            .build());
                }
                for (int i = 0; i < uniqueRights.size(); i++) {
                    answerOptionRepository.save(AnswerOption.builder()
                            .question(q)
                            .title(uniqueRights.get(i))
                            .correctAnswer(false)
                            .orderIndex(lefts.size() + i)
                            .build());
                }
            } else {
                for (QuestionRequestDTO.AnswerOptionDTO optDTO : qReq.getOptions()) {
                    answerOptionRepository.save(AnswerOption.builder()
                            .question(q)
                            .title(optDTO.getTitle())
                            .correctAnswer(Boolean.TRUE.equals(optDTO.getCorrect()))
                            .build());
                }
            }
        }
        return q;
    }

    private QuestionGroupResponseDTO toGroupResponseDTO(QuestionGroup group) {
        List<QuestionResponseDTO> childQ = new java.util.ArrayList<>();
        if (group.getQuestions() != null) {
            childQ = group.getQuestions().stream()
                    .map(this::toResponseDTO)
                    .collect(Collectors.toList());
        }
        return QuestionGroupResponseDTO.builder()
                .groupId(group.getGroupId())
                .groupContent(group.getGroupContent())
                .audioUrl(group.getAudioUrl())
                .imageUrl(group.getImageUrl())
                .skill(group.getSkill())
                .cefrLevel(group.getCefrLevel())
                .topic(group.getTopic())
                .explanation(group.getExplanation())
                .status(group.getStatus())
                .createdAt(group.getCreatedAt())
                .questions(childQ)
                .build();
    }

    private void validateExpertOwnsModule(String email, Integer moduleId) {
        User expert = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyên gia."));
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương."));
        Course course = module.getCourse();
        if (course == null || course.getExpert() == null
                || !course.getExpert().getUserId().equals(expert.getUserId())) {
            throw new ResourceNotFoundException("Bạn không có quyền quản lý câu hỏi trong chương này.");
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
            // Exclude self — same question edited in place
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

    /** Truncate string for error message readability. */
    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }
}
