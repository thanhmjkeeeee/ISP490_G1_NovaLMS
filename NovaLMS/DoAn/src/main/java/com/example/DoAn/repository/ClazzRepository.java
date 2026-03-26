package com.example.DoAn.repository;

import com.example.DoAn.model.Clazz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClazzRepository extends JpaRepository<Clazz, Integer> {

    // Lấy tất cả lớp mà giáo viên (theo email) đang phụ trách
    @Query("SELECT c FROM Clazz c JOIN FETCH c.course WHERE c.teacher.userId = :teacherId")
    List<Clazz> findAllByTeacher_UserId(@Param("teacherId") Integer teacherId);
}
