package com.example.DoAn.repository;

import com.example.DoAn.model.QuizResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface QuizResultRepository extends JpaRepository<QuizResult, Integer> {
    Optional<QuizResult> findByQuizQuizIdAndUserUserId(Integer quizId, Integer userId);
}
