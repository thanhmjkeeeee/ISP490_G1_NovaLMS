package com.example.DoAn.service;

import com.example.DoAn.dto.CourseLearningInfoDTO;

public interface LearningService {
    CourseLearningInfoDTO getCourseLearningInfo(Long courseId, String email);
}