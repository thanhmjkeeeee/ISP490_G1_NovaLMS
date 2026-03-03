package com.example.DoAn.service;

import com.example.DoAn.model.Clazz;
import com.example.DoAn.repository.ClassRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ClassService {
    @Autowired
    private ClassRepository classRepository;

    public List<Clazz> findAll() {
        return classRepository.findAll();
    }

    public Clazz findById(Integer id) {
        return classRepository.findById(id).orElse(null);
    }

    public void save(Clazz clazz) {
        classRepository.save(clazz);
    }
}