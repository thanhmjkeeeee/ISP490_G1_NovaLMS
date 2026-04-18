package com.example.DoAn.repository;

import com.example.DoAn.model.SessionQuiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionQuizRepository extends JpaRepository<SessionQuiz, Integer> {
    
    @Query("SELECT sq FROM SessionQuiz sq JOIN FETCH sq.quiz WHERE sq.session.sessionId = :sessionId")
    List<SessionQuiz> findBySessionSessionIdWithQuiz(@Param("sessionId") Integer sessionId);

    List<SessionQuiz> findBySessionSessionId(Integer sessionId);

    List<SessionQuiz> findBySessionSessionIdOrderByOrderIndexAsc(Integer sessionId);

    List<SessionQuiz> findBySessionSessionIdAndQuiz_QuizCategoryOrderByOrderIndexAsc(Integer sessionId, String quizCategory);

    Optional<SessionQuiz> findBySessionSessionIdAndQuizQuizId(Integer sessionId, Integer quizId);

    boolean existsBySessionSessionIdAndQuizQuizId(Integer sessionId, Integer quizId);

    void deleteBySessionSessionIdAndQuizQuizId(Integer sessionId, Integer quizId);

    int countBySessionSessionId(Integer sessionId);

    @Query("SELECT sq FROM SessionQuiz sq JOIN FETCH sq.session s JOIN FETCH s.clazz WHERE sq.quiz.quizId = :quizId")
    List<SessionQuiz> findAllByQuizId(@Param("quizId") Integer quizId);

    List<SessionQuiz> findBySession_Clazz_ClassId(Integer classId);

    @Query("SELECT COUNT(sq) FROM SessionQuiz sq WHERE sq.session.clazz.course.courseId = :courseId")
    long countByCourseId(@Param("courseId") Integer courseId);

    /**
     * Lấy tất cả SessionQuiz có quiz đang DRAFT và session có ngày học.
     * Dùng cho scheduler tự động publish quiz khi buổi học bắt đầu.
     */
    @Query("SELECT sq FROM SessionQuiz sq " +
           "JOIN FETCH sq.quiz q " +
           "JOIN FETCH sq.session s " +
           "JOIN FETCH s.clazz c " +
           "WHERE q.status = 'DRAFT' " +
           "AND s.sessionDate IS NOT NULL")
    List<SessionQuiz> findDraftQuizzesWithSessions();
}
