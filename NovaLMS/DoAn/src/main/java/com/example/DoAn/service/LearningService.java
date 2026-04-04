package com.example.DoAn.service;

import com.example.DoAn.dto.response.ChartDataDTO;
import com.example.DoAn.dto.response.CourseLearningInfoDTO;
import com.example.DoAn.dto.response.ResponseData;

import java.util.List;
import java.util.Map;

public interface LearningService {
    ResponseData<CourseLearningInfoDTO> getCourseLearningInfo(Long courseId, String email);
    ResponseData<Map<String, Object>> getLessonViewData(Integer lessonId, String email);
    ResponseData<Void> markLessonCompleted(Integer lessonId, String email);
    Integer getLessonIdToContinue(Integer courseId, String email);

    // Study Time Tracking
    ResponseData<Void> trackTime(String email, int seconds);
    ResponseData<ChartDataDTO> getDashboardChartData(String email, int days);
}