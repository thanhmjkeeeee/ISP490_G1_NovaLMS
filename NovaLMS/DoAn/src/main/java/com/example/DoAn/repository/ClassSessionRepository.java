package com.example.DoAn.repository;

import com.example.DoAn.model.ClassSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassSessionRepository extends JpaRepository<ClassSession, Integer> {

    List<ClassSession> findByClazzClassIdOrderBySessionNumberAsc(Integer classId);

    @Query("SELECT COUNT(s) FROM ClassSession s WHERE s.clazz.classId = :classId")
    int countByClassId(@Param("classId") Integer classId);
}
