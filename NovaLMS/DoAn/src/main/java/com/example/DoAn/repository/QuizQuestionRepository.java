package com.example.DoAn.repository;

import com.example.DoAn.model.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Integer> {

    List<QuizQuestion> findByQuizQuizIdOrderByOrderIndexAsc(Integer quizId);

    void deleteByQuizQuizIdAndQuestionQuestionId(Integer quizId, Integer questionId);

    boolean existsByQuizQuizIdAndQuestionQuestionId(Integer quizId, Integer questionId);

    int countByQuizQuizId(Integer quizId);

    java.util.Optional<QuizQuestion> findByQuizQuizIdAndQuestionQuestionId(Integer quizId, Integer questionId);

    void deleteByQuizQuizIdAndQuestionGroupGroupId(Integer quizId, Integer groupId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT qq.quiz.quizId) FROM QuizQuestion qq WHERE qq.questionGroup.groupId = :groupId")
    long countByQuestionGroup_GroupId(@org.springframework.data.repository.query.Param("groupId") Integer groupId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(qq) FROM QuizQuestion qq WHERE qq.question.questionId = :questionId")
    long countByQuestion_QuestionId(@org.springframework.data.repository.query.Param("questionId") Integer questionId);
}
