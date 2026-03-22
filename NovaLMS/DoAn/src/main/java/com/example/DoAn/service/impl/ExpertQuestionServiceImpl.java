package com.example.DoAn.service.impl;

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
        validateExpertOwnsModule(email, request.getModuleId());
        validateAnswerOptions(request.getOptions());

        Module module = moduleRepository.findById(request.getModuleId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương."));
        User expert = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyên gia."));

        Question question = Question.builder()
                .module(module)
                .user(expert)
                .status(request.getContent() != null ? request.getContent() : "")
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
        validateExpertOwnsModule(email, question.getModule().getModuleId());

        if (request.getOptions() != null && !request.getOptions().isEmpty()) {
            validateAnswerOptions(request.getOptions());
        }

        if (request.getContent() != null) {
            question.setStatus(request.getContent());
        }
        if (request.getStatus() != null) {
            question.setStatus(request.getStatus());
        }

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
        validateExpertOwnsModule(email, question.getModule().getModuleId());
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
                .moduleId(module.getModuleId())
                .moduleName(module.getModuleName())
                .content(question.getStatus())
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
