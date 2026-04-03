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

    @Query("SELECT s FROM ClassSession s JOIN FETCH s.clazz c " +
           "WHERE c.teacher.userId = :teacherId " +
           "AND s.sessionDate >= :start AND s.sessionDate < :end " +
           "ORDER BY s.sessionDate")
    java.util.List<com.example.DoAn.model.ClassSession> findByTeacherAndDateRange(
            @Param("teacherId") Integer teacherId,
            @Param("start") java.time.LocalDateTime start,
            @Param("end") java.time.LocalDateTime end
    );


    @Query("SELECT COUNT(cs) FROM ClassSession cs " +
           "WHERE cs.clazz.teacher.userId = :teacherId " +
           "AND cs.sessionDate >= :startOfDay " +
           "AND cs.sessionDate < :endOfDay " +
           "AND cs.startTime = :startTime " +
           "AND cs.sessionId != :sessionId")
    long countConflictsInDateRange(
            @Param("teacherId") Integer teacherId,
            @Param("startOfDay") java.time.LocalDateTime startOfDay,
            @Param("endOfDay") java.time.LocalDateTime endOfDay,
            @Param("startTime") String startTime,
            @Param("sessionId") Integer sessionId
    );
}
