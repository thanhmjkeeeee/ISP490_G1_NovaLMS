package com.example.DoAn.service.impl;

import com.example.DoAn.dto.request.AIImportRequestDTO;
import com.example.DoAn.dto.request.QuestionRequestDTO;
import com.example.DoAn.dto.response.QuestionResponseDTO;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpertQuestionServiceImpl implements IExpertQuestionService {

    private final QuestionRepository questionRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final ModuleRepository moduleRepository;
    private final UserRepository userRepository;

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
        validateAnswerOptions(request.getOptions());

        Module module = null;
        if (request.getModuleId() != null) {
            module = moduleRepository.findById(request.getModuleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương."));
        }
        User expert = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyên gia."));

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

        for (QuestionRequestDTO.AnswerOptionDTO optDTO : request.getOptions()) {
            AnswerOption opt = AnswerOption.builder()
                    .question(question)
                    .title(optDTO.getTitle())
                    .correctAnswer(Boolean.TRUE.equals(optDTO.getCorrect()))
                    .build();
            answerOptionRepository.save(opt);
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
            validateAnswerOptions(request.getOptions());
        }

        if (request.getContent() != null) question.setContent(request.getContent());
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
            for (QuestionRequestDTO.AnswerOptionDTO optDTO : request.getOptions()) {
                AnswerOption opt = AnswerOption.builder()
                        .question(question)
                        .title(optDTO.getTitle())
                        .correctAnswer(Boolean.TRUE.equals(optDTO.getCorrect()))
                        .build();
                answerOptionRepository.save(opt);
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
                .map(o -> QuestionResponseDTO.AnswerOptionResponseDTO.builder()
                        .answerOptionId(o.getAnswerOptionId())
                        .title(o.getTitle())
                        .correctAnswer(o.getCorrectAnswer())
                        .build())
                .collect(Collectors.toList());

        int correctCount = (int) opts.stream()
                .filter(o -> Boolean.TRUE.equals(o.getCorrectAnswer()))
                .count();

        return QuestionResponseDTO.builder()
                .questionId(question.getQuestionId())
                .moduleId(module != null ? module.getModuleId() : null)
                .moduleName(module != null ? module.getModuleName() : null)
                .content(question.getContent())
                .status(question.getStatus())
                .optionCount(opts.size())
                .correctOptionCount(correctCount)
                .options(optDTOs)
                .build();
    }

    private void validateAnswerOptions(List<QuestionRequestDTO.AnswerOptionDTO> options) {
        if (options.size() < 2) {
            throw new InvalidDataException("Mỗi câu hỏi phải có ít nhất 2 đáp án.");
        }
        long correctCount = options.stream()
                .filter(o -> Boolean.TRUE.equals(o.getCorrect()))
                .count();
        if (correctCount == 0) {
            throw new InvalidDataException("Phải có ít nhất 1 đáp án đúng (correct=true).");
        }
    }

    @Override
    @Transactional
    public int saveAIQuestions(AIImportRequestDTO request, String email) {
        User expert = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyên gia."));

        int saved = 0;
        for (AIImportRequestDTO.AIQuestionDTO qdto : request.getQuestions()) {
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
                    AnswerOption aoLeft = AnswerOption.builder()
                            .question(question)
                            .title(left.get(i))
                            .correctAnswer(false)
                            .orderIndex(i)
                            .build();
                    answerOptionRepository.save(aoLeft);
                    AnswerOption aoRight = AnswerOption.builder()
                            .question(question)
                            .title(right.get(rightIdx))
                            .correctAnswer(false)
                            .orderIndex(left.size() + rightIdx)
                            .matchTarget(left.get(i))
                            .build();
                    answerOptionRepository.save(aoRight);
                }
            }
            saved++;
        }
        return saved;
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
}
