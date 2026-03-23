package com.example.DoAn.repository;

import com.example.DoAn.model.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Integer> {

    List<Question> findByModuleModuleId(Integer moduleId);

    @Query("SELECT COUNT(qq) FROM QuizQuestion qq WHERE qq.question.questionId = :questionId")
    long countQuizUsage(@Param("questionId") Integer questionId);

    @Query("SELECT q FROM Question q WHERE " +
           "(:skill IS NULL OR q.skill = :skill) AND " +
           "(:cefrLevel IS NULL OR q.cefrLevel = :cefrLevel) AND " +
           "(:questionType IS NULL OR q.questionType = :questionType) AND " +
           "(:topic IS NULL OR q.topic LIKE %:topic%) AND " +
           "(:status IS NULL OR q.status = :status) AND " +
           "(:keyword IS NULL OR q.content LIKE %:keyword%) " +
           "ORDER BY q.createdAt DESC")
    Page<Question> findByFilters(
        @Param("skill") String skill,
        @Param("cefrLevel") String cefrLevel,
        @Param("questionType") String questionType,
        @Param("topic") String topic,
        @Param("status") String status,
        @Param("keyword") String keyword,
        Pageable pageable
    );
}
