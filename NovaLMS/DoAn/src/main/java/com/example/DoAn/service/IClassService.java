package com.example.DoAn.service;

import com.example.DoAn.dto.request.ClassRequestDTO;
import com.example.DoAn.dto.response.ClassDetailResponse;
import com.example.DoAn.dto.response.PageResponse;

public interface IClassService {
    Integer saveClass(ClassRequestDTO request);
    void updateClass(Integer id, ClassRequestDTO request);
    ClassDetailResponse getClassById(Integer id);
    PageResponse<?> getAllClasses(int pageNo, int pageSize, String search, String status);
    void deleteClass(Integer id);
}