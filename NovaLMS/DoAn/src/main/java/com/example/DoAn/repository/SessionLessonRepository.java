package com.example.DoAn.repository;

import com.example.DoAn.model.SessionLesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionLessonRepository extends JpaRepository<SessionLesson, Integer> {
    
    @Query("SELECT sl FROM SessionLesson sl JOIN FETCH sl.lesson WHERE sl.session.clazz.classId = :classId ORDER BY sl.orderIndex ASC")
    List<SessionLesson> findByClassSession_Clazz_ClassIdOrderByOrderIndexAsc(@Param("classId") Integer classId);

    void deleteBySession_Clazz_ClassId(Integer classId);

    List<SessionLesson> findBySessionSessionId(Integer sessionId);

    @Query("SELECT COUNT(sl) FROM SessionLesson sl WHERE sl.session.clazz.course.courseId = :courseId AND sl.lesson.type = :type")
    long countByCourseIdAndLessonType(@Param("courseId") Integer courseId, @Param("type") String type);

    boolean existsBySession_Clazz_ClassId(Integer classId);
}
