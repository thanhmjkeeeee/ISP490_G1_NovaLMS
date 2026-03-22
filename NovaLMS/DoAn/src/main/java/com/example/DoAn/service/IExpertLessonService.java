package com.example.DoAn.service;

import com.example.DoAn.dto.request.LessonRequestDTO;
import com.example.DoAn.dto.response.ExpertLessonResponseDTO;

import java.util.List;

public interface IExpertLessonService {
    List<ExpertLessonResponseDTO> getLessonsByModule(Integer moduleId, String email);
    ExpertLessonResponseDTO createLesson(LessonRequestDTO request, String email);
    ExpertLessonResponseDTO updateLesson(Integer lessonId, LessonRequestDTO request, String email);
    void deleteLesson(Integer lessonId, String email);
}
