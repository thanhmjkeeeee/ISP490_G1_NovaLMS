package com.example.DoAn.service;

public interface ITeacherQuestionService {
    void cancelSubmitQuestion(Integer questionId, String teacherEmail);
    void submitQuestion(Integer questionId, String teacherEmail);
    void updateQuestion(Integer questionId, java.util.Map<String, Object> data, String teacherEmail);
}
