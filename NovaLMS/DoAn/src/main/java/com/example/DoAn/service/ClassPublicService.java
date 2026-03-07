package com.example.DoAn.service;

import com.example.DoAn.dto.response.ClassPublicResponseDTO;
import com.example.DoAn.dto.response.PageResponse;
import java.util.List;

public interface ClassPublicService {
    // Kiểu trả về là PageResponse của một danh sách
    PageResponse<List<ClassPublicResponseDTO>> getOpenClassesWithFilter(int pageNo, int pageSize, Integer categoryId);
}