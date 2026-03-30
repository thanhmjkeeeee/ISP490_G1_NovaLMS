package com.example.DoAn.repository;

import com.example.DoAn.model.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ModuleRepository extends JpaRepository<Module, Integer> {
    @EntityGraph(attributePaths = {"lessons"})
    List<Module> findByCourse_CourseIdOrderByOrderIndexAsc(Integer courseId);

    long countByCourse_CourseId(Integer courseId);

    long countByCourse_Expert_UserId(Integer userId);
}