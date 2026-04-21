package com.example.DoAn.repository;

import com.example.DoAn.model.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Integer> {

    @org.springframework.data.jpa.repository.Query("SELECT qq FROM QuizQuestion qq JOIN FETCH qq.question WHERE qq.quiz.quizId = :quizId ORDER BY qq.orderIndex ASC")
    List<QuizQuestion> findByQuizQuizIdOrderByOrderIndexAsc(@org.springframework.data.repository.query.Param("quizId") Integer quizId);

    List<QuizQuestion> findByQuizQuizId(Integer quizId);

    void deleteByQuizQuizIdAndQuestionQuestionId(Integer quizId, Integer questionId);

    boolean existsByQuizQuizIdAndQuestionQuestionId(Integer quizId, Integer questionId);

    int countByQuizQuizId(Integer quizId);

    java.util.Optional<QuizQuestion> findByQuizQuizIdAndQuestionQuestionId(Integer quizId, Integer questionId);
    java.util.Optional<QuizQuestion> findByQuestion_QuestionId(Integer questionId);

    void deleteByQuizQuizIdAndQuestionGroupGroupId(Integer quizId, Integer groupId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT qq.quiz.quizId) FROM QuizQuestion qq WHERE qq.questionGroup.groupId = :groupId")
    long countByQuestionGroup_GroupId(@org.springframework.data.repository.query.Param("groupId") Integer groupId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(qq) FROM QuizQuestion qq WHERE qq.question.questionId = :questionId")
    long countByQuestion_QuestionId(@org.springframework.data.repository.query.Param("questionId") Integer questionId);

    @org.springframework.data.jpa.repository.Query("SELECT qq FROM QuizQuestion qq JOIN FETCH qq.question q WHERE qq.quiz.quizId = :quizId AND (q.skill = :skill OR q.questionType = :skill)")
    List<QuizQuestion> findByQuizQuizIdAndSkill(@org.springframework.data.repository.query.Param("quizId") Integer quizId, @org.springframework.data.repository.query.Param("skill") String skill);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT q.question.questionId) FROM QuizQuestion q WHERE q.quiz.quizId = :quizId AND (q.question.skill = :skill OR q.question.questionType = :skill)")
    long countByQuizIdAndSkill(@org.springframework.data.repository.query.Param("quizId") Integer quizId, @org.springframework.data.repository.query.Param("skill") String skill);
}
