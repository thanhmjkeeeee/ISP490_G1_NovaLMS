package com.example.DoAn.repository;

import com.example.DoAn.model.Clazz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassRepository extends JpaRepository<Clazz, Integer> {
    // Tìm các lớp theo Course ID và trạng thái (ví dụ: tìm lớp đang mở 'Open')
    List<Clazz> findByCourse_CourseIdAndStatus(Integer courseId, String status);
}