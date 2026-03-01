package com.example.DoAn.service;

import com.example.DoAn.dto.CourseLearningInfoDTO;
import com.example.DoAn.model.Lesson;

public interface LearningService {
    CourseLearningInfoDTO getCourseLearningInfo(Long courseId, String email);

    Integer getLessonIdToContinue(Integer courseId, String email);
    Lesson getLessonEntity(Integer lessonId);
}