package com.example.DoAn.repository;

import com.example.DoAn.model.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ModuleRepository extends JpaRepository<Module, Integer> {
    // Lấy các chương của khóa học và sắp xếp theo thứ tự
    List<Module> findByCourse_CourseIdOrderByOrderIndexAsc(Integer courseId);
}