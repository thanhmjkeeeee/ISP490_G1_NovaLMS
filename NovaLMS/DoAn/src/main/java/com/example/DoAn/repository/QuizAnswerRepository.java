package com.example.DoAn.repository;

import com.example.DoAn.model.QuizAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Integer> {
    List<QuizAnswer> findByQuizResultResultId(Integer resultId);
    @org.springframework.data.jpa.repository.Query("SELECT qa FROM QuizAnswer qa JOIN FETCH qa.question WHERE qa.quizResult.resultId = :resultId")
    List<QuizAnswer> findByQuizResultResultIdWithQuestion(@org.springframework.data.repository.query.Param("resultId") Integer resultId);

    QuizAnswer findByQuizResultResultIdAndQuestionQuestionId(Integer resultId, Integer questionId);
}
