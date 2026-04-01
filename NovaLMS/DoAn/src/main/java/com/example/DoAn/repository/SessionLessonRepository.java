package com.example.DoAn.repository;

import com.example.DoAn.model.SessionLesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionLessonRepository extends JpaRepository<SessionLesson, Integer> {
    
    // Kéo toàn bộ Lesson của 1 Session, kèm theo thông tin Lesson (JOIN FETCH) để chống lỗi N+1 Query
    @Query("SELECT sl FROM SessionLesson sl JOIN FETCH sl.lesson WHERE sl.session.sessionId = :sessionId ORDER BY sl.orderIndex ASC")
    List<SessionLesson> findBySession_SessionIdOrderByOrderIndexAsc(@Param("sessionId") Integer sessionId);

    // Phương thức bổ sung để khớp với code hiện tại trong StudentServiceImpl
    @Query("SELECT sl FROM SessionLesson sl JOIN FETCH sl.lesson WHERE sl.session.sessionId = :sessionId ORDER BY sl.orderIndex ASC")
    List<SessionLesson> findByClassSession_SessionIdOrderByOrderIndexAsc(@Param("sessionId") Integer sessionId);
}
