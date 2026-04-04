package com.example.DoAn.service.impl;

import com.example.DoAn.dto.request.LessonRequestDTO;
import com.example.DoAn.dto.response.ExpertLessonResponseDTO;
import com.example.DoAn.exception.InvalidDataException;
import com.example.DoAn.exception.ResourceNotFoundException;
import com.example.DoAn.model.*;
import com.example.DoAn.model.Module;
import com.example.DoAn.repository.*;
import com.example.DoAn.service.IExpertLessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpertLessonServiceImpl implements IExpertLessonService {

    private final LessonRepository lessonRepository;
    private final ModuleRepository moduleRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final QuizRepository quizRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ExpertLessonResponseDTO> getLessonsByModule(Integer moduleId, String email) {
        validateExpertOwnsModule(email, moduleId);
        return lessonRepository.findByModule_ModuleIdOrderByOrderIndexAsc(moduleId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ExpertLessonResponseDTO createLesson(LessonRequestDTO request, String email) {
        validateExpertOwnsModule(email, request.getModuleId());
        Module module = moduleRepository.findById(request.getModuleId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương."));

        validateLessonType(request);

        List<Lesson> existing = lessonRepository.findByModule_ModuleIdOrderByOrderIndexAsc(request.getModuleId());
        int nextOrder = existing.stream()
                .mapToInt(l -> l.getOrderIndex() != null ? l.getOrderIndex() : 0)
                .max().orElse(0) + 1;

        Lesson lesson = Lesson.builder()
                .module(module)
                .lessonName(request.getLessonName())
                .type(request.getType())
                .videoUrl(request.getVideoUrl())
                .content_text(request.getContentText())
                .duration(request.getDuration())
                .allowDownload(request.getAllowDownload() != null ? request.getAllowDownload() : true)
                .orderIndex(request.getOrderIndex() != null ? request.getOrderIndex() : nextOrder)
                .build();

        lessonRepository.save(lesson);
        return toResponseDTO(lesson, module);
    }

    @Override
    @Transactional
    public ExpertLessonResponseDTO updateLesson(Integer lessonId, LessonRequestDTO request, String email) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học."));
        validateExpertOwnsModule(email, lesson.getModule().getModuleId());

        if (request.getType() != null && !request.getType().equals(lesson.getType())) {
            validateLessonType(request);
        }

        if (request.getLessonName() != null) lesson.setLessonName(request.getLessonName());
        if (request.getType() != null) lesson.setType(request.getType());
        if (request.getVideoUrl() != null) lesson.setVideoUrl(request.getVideoUrl());
        if (request.getContentText() != null) lesson.setContent_text(request.getContentText());
        if (request.getDuration() != null) lesson.setDuration(request.getDuration());
        if (request.getAllowDownload() != null) lesson.setAllowDownload(request.getAllowDownload());
        if (request.getOrderIndex() != null) lesson.setOrderIndex(request.getOrderIndex());

        lessonRepository.save(lesson);
        return toResponseDTO(lesson, lesson.getModule());
    }

    @Override
    @Transactional
    public void deleteLesson(Integer lessonId, String email) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học."));
        validateExpertOwnsModule(email, lesson.getModule().getModuleId());
        lessonRepository.delete(lesson);
    }

    private ExpertLessonResponseDTO toResponseDTO(Lesson lesson) {
        return toResponseDTO(lesson, lesson.getModule());
    }

    private ExpertLessonResponseDTO toResponseDTO(Lesson lesson, Module module) {
        return ExpertLessonResponseDTO.builder()
                .lessonId(lesson.getLessonId())
                .moduleId(module.getModuleId())
                .moduleName(module.getModuleName())
                .lessonName(lesson.getLessonName())
                .type(lesson.getType())
                .videoUrl(lesson.getVideoUrl())
                .videoEmbedUrl(ExpertLessonResponseDTO.toEmbedUrl(lesson.getVideoUrl()))
                .contentText(lesson.getContent_text())
                .duration(lesson.getDuration())
                .allowDownload(lesson.getAllowDownload() != null ? lesson.getAllowDownload() : true)
                .orderIndex(lesson.getOrderIndex())
                .build();
    }

    private void validateLessonType(LessonRequestDTO request) {
        String type = request.getType();
        if ("VIDEO".equals(type)) {
            if (request.getVideoUrl() == null || request.getVideoUrl().isBlank()) {
                throw new InvalidDataException("videoUrl là bắt buộc khi type=VIDEO.");
            }
        } else if ("DOC".equals(type)) {
            if (request.getContentText() == null || request.getContentText().isBlank()) {
                throw new InvalidDataException("contentText là bắt buộc khi type=DOC.");
            }
        } else {
            throw new InvalidDataException("type phải là VIDEO, DOC, hoặc QUIZ.");
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
            throw new ResourceNotFoundException("Bạn không có quyền quản lý chương này.");
        }
    }
}
