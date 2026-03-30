package com.example.DoAn.repository;

import com.example.DoAn.model.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Integer> {

    List<QuizQuestion> findByQuizQuizIdOrderByOrderIndexAsc(Integer quizId);

    void deleteByQuizQuizIdAndQuestionQuestionId(Integer quizId, Integer questionId);

    boolean existsByQuizQuizIdAndQuestionQuestionId(Integer quizId, Integer questionId);

    int countByQuizQuizId(Integer quizId);

    Optional<QuizQuestion> findByQuizQuizIdAndQuestionQuestionId(Integer quizId, Integer questionId);
}
