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

    boolean existsByClazz_Teacher_UserIdAndSessionDateAndStartTimeAndSessionIdNot(
            Integer teacherId,
            java.time.LocalDateTime sessionDate,
            String startTime,
            Integer sessionId
    );

    @Query(value = "SELECT count(*) FROM class_session cs " +
                   "JOIN class c ON cs.class_id = c.class_id " +
                   "WHERE c.teacher_id = :teacherId " +
                   "AND DATE(cs.session_date) = DATE(:sessionDate) " +
                   "AND cs.start_time = :startTime " +
                   "AND cs.session_id != :sessionId", nativeQuery = true)
    int existsByTeacherDateAndSlotNative(
            @Param("teacherId") Integer teacherId,
            @Param("sessionDate") java.time.LocalDateTime sessionDate,
            @Param("startTime") String startTime,
            @Param("sessionId") Integer sessionId
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
