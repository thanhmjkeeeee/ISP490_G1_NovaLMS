package com.example.DoAn.service;

import com.example.DoAn.dto.CourseLearningInfoDTO;
import com.example.DoAn.dto.ResponseData;

import java.util.Map;

public interface LearningService {
    ResponseData<CourseLearningInfoDTO> getCourseLearningInfo(Long courseId, String email);
    ResponseData<Map<String, Object>> getLessonViewData(Integer lessonId, String email);
    ResponseData<Void> markLessonCompleted(Integer lessonId, String email);
    Integer getLessonIdToContinue(Integer courseId, String email);
}