package com.example.DoAn.service.impl;

import com.example.DoAn.model.Clazz;
import com.example.DoAn.repository.ClassRepository;
import com.example.DoAn.service.ClassPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClassPublicServiceImpl implements ClassPublicService {

    private final ClassRepository classRepository;

    @Override
    public List<Clazz> getOpenClassesByFilter(Integer categoryId) {
        return Optional.ofNullable(categoryId)
                .map(id -> classRepository.findByStatusAndCourse_Category_SettingId("Open", id))
                .orElseGet(() -> classRepository.findByStatus("Open"));
    }
}