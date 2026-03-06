package com.example.DoAn.service;

import com.example.DoAn.model.Clazz;
import java.util.List;

public interface ClassPublicService {
    // Khai báo hàm lấy danh sách lớp có lọc theo Category
    List<Clazz> getOpenClassesByFilter(Integer categoryId);
}