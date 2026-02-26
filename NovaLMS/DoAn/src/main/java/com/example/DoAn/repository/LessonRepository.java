package com.example.DoAn.repository;

import com.example.DoAn.model.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Integer> {
    // Lấy danh sách bài học của một Module
    List<Lesson> findByModule_ModuleIdOrderByOrderIndexAsc(Integer moduleId);
}