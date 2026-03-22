package com.example.DoAn.repository;

import com.example.DoAn.model.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Integer> {
    Optional<Quiz> findByQuizCategory(String quizCategory);
}
