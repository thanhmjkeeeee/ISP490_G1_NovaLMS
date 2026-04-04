package com.example.DoAn.service;

import com.example.DoAn.dto.response.ClassPublicResponseDTO;
import com.example.DoAn.dto.response.PageResponse;
import java.util.List;

public interface ClassPublicService {
    PageResponse<ClassPublicResponseDTO> getOpenClassesWithFilter(int pageNo, int pageSize, Integer categoryId, String keyword);
}