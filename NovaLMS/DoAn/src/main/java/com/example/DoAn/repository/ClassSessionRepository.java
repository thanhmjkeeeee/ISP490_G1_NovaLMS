package com.example.DoAn.repository;

import com.example.DoAn.model.ClassSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassSessionRepository extends JpaRepository<ClassSession, Integer> {

    List<ClassSession> findByClazzClassIdOrderBySessionNumberAsc(Integer classId);
    
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByClazz_ClassId(Integer classId);

    @Query("SELECT COUNT(s) FROM ClassSession s WHERE s.clazz.classId = :classId")

    int countByClassId(@Param("classId") Integer classId);

    @Query("SELECT s FROM ClassSession s JOIN FETCH s.clazz c LEFT JOIN FETCH c.course " +
           "WHERE c.teacher.userId = :teacherId " +
           "AND s.sessionDate >= :start AND s.sessionDate < :end " +
           "ORDER BY s.sessionDate")
    java.util.List<com.example.DoAn.model.ClassSession> findByTeacherAndDateRange(
            @Param("teacherId") Integer teacherId,
            @Param("start") java.time.LocalDateTime start,
            @Param("end") java.time.LocalDateTime end
    );

    @Query("SELECT s FROM ClassSession s " +
           "JOIN FETCH s.clazz c " +
           "LEFT JOIN FETCH c.teacher t " +
           "LEFT JOIN FETCH c.course co " +
           "WHERE s.sessionId = :sessionId")
    Optional<ClassSession> findWithDetailsById(@Param("sessionId") Integer sessionId);


    @Query("SELECT COUNT(cs) FROM ClassSession cs " +
           "WHERE cs.clazz.teacher.userId = :teacherId " +
           "AND (cs.clazz.status IS NULL OR (upper(cs.clazz.status) <> 'CLOSED' AND upper(cs.clazz.status) <> 'CANCELLED')) " +
           "AND cs.sessionDate >= :startOfDay " +
           "AND cs.sessionDate < :endOfDay " +
           "AND cs.startTime = :startTime " +
           "AND cs.sessionId <> :sessionId")
    long countConflictsInDateRange(
            @Param("teacherId") Integer teacherId,
            @Param("startOfDay") java.time.LocalDateTime startOfDay,
            @Param("endOfDay") java.time.LocalDateTime endOfDay,
            @Param("startTime") String startTime,
            @Param("sessionId") Integer sessionId
    );

    /**
     * Buổi học của GV trong khoảng thời gian (dùng quét trùng lịch). Loại lớp đóng/hủy.
     */
    @Query("SELECT s FROM ClassSession s JOIN FETCH s.clazz c WHERE c.teacher.userId = :teacherId "
            + "AND (c.status IS NULL OR (upper(c.status) <> 'CLOSED' AND upper(c.status) <> 'CANCELLED')) "
            + "AND (:excludeClassId IS NULL OR c.classId <> :excludeClassId) "
            + "AND (:excludeSessionId IS NULL OR s.sessionId <> :excludeSessionId) "
            + "AND s.sessionDate >= :from AND s.sessionDate < :to "
            + "ORDER BY s.sessionDate ASC")
    List<ClassSession> findTeacherSessionsForConflictScan(
            @Param("teacherId") Integer teacherId,
            @Param("from") java.time.LocalDateTime from,
            @Param("to") java.time.LocalDateTime to,
            @Param("excludeClassId") Integer excludeClassId,
            @Param("excludeSessionId") Integer excludeSessionId
    );
}
