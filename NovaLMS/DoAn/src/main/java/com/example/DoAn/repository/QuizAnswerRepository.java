package com.example.DoAn.repository;

import com.example.DoAn.model.QuizAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Integer> {
    List<QuizAnswer> findByQuizResultResultId(Integer resultId);
}
