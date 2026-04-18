package com.example.DoAn.service;

import com.example.DoAn.dto.request.ClassRequestDTO;
import com.example.DoAn.dto.response.ClassDetailResponse;
import com.example.DoAn.dto.response.PageResponse;

import java.util.List;

public interface IClassService {
    Integer saveClass(ClassRequestDTO request);
    void updateClass(Integer id, ClassRequestDTO request);
    ClassDetailResponse getClassById(Integer id);
    PageResponse<ClassDetailResponse> getAllClasses(int pageNo, int pageSize, String className, String courseName, String teacherName, String status);
    List<String> getAvailableSlotTimes(Integer teacherId, String schedule, Integer excludeClassId);
    List<com.example.DoAn.model.User> getAvailableTeachers(String startDate, String endDate, String schedule, String slotTime, Integer excludeClassId);
    void deleteClass(Integer id);
}