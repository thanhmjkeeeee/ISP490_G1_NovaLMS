package com.example.DoAn.repository;

import com.example.DoAn.model.AssignmentSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentSessionRepository extends JpaRepository<AssignmentSession, Long> {

    Optional<AssignmentSession> findByQuizQuizIdAndUserUserId(Integer quizId, Long userId);

    boolean existsByQuizQuizIdAndUserUserId(Integer quizId, Long userId);

    List<AssignmentSession> findByUserUserId(Long userId);

    @Query("SELECT COUNT(s) FROM AssignmentSession s WHERE s.quiz.quizId = :quizId AND s.user.userId = :userId")
    long countByQuizAndUser(@Param("quizId") Integer quizId, @Param("userId") Long userId);

    @Query("SELECT s FROM AssignmentSession s WHERE s.status = 'IN_PROGRESS' AND s.expiresAt < :now")
    List<AssignmentSession> findExpiredSessions(@Param("now") java.time.LocalDateTime now);
}
