package com.example.DoAn.repository;

import com.example.DoAn.model.QuizResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

@Repository
public interface QuizResultRepository extends JpaRepository<QuizResult, Integer> {
    Optional<QuizResult> findByQuizQuizIdAndUserUserId(Integer quizId, Integer userId);

    @Query("SELECT qr FROM QuizResult qr WHERE qr.quiz.quizId = :quizId AND qr.user.email = :email")
    Optional<QuizResult> findByQuizIdAndUserEmail(@org.springframework.data.repository.query.Param("quizId") Integer quizId, @org.springframework.data.repository.query.Param("email") String email);


    long countByQuizQuizId(Integer quizId);

    Page<QuizResult> findByPassedIsNullAndQuiz_User_EmailOrderBySubmittedAtAsc(String email, Pageable pageable);

    Page<QuizResult> findByUserEmailOrderBySubmittedAtDesc(String email, Pageable pageable);
}
