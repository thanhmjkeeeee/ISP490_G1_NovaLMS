package com.example.DoAn.repository;

import com.example.DoAn.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Integer> {

    List<Question> findByModuleModuleId(Integer moduleId);

    /**
     * Check if a question with the same content (case-insensitive), skill, and CEFR level already exists.
     * Used for duplicate detection before AI generation import or manual creation.
     */
    boolean existsByContentIgnoreCaseAndSkillAndCefrLevel(String content, String skill, String cefrLevel);

    @Query("SELECT COUNT(qq) FROM QuizQuestion qq WHERE qq.question.questionId = :questionId")
    long countQuizUsage(@Param("questionId") Integer questionId);

    @Query("SELECT COUNT(q) FROM Question q WHERE q.module.course.courseId = :courseId")
    long countByModule_Course_CourseId(@Param("courseId") Integer courseId);

    long countByUser_UserId(Integer userId);

    long countByUser_UserIdAndSourceAndStatus(Integer userId, String source, String status);

    @Query("SELECT q FROM Question q WHERE " +
           "(:skill IS NULL OR q.skill = :skill) AND " +
           "(:cefrLevel IS NULL OR q.cefrLevel = :cefrLevel) AND " +
           "(:questionType IS NULL OR q.questionType = :questionType) AND " +
           "(:topic IS NULL OR q.topic LIKE %:topic%) AND " +
           "(:status IS NULL OR q.status = :status) AND " +
           "(:keyword IS NULL OR q.content LIKE %:keyword% OR q.topic LIKE %:keyword%) AND " +
           "q.questionGroup IS NULL " +
           "ORDER BY q.createdAt DESC")
    List<Question> findAllLoneQuestions(
        @Param("skill") String skill,
        @Param("cefrLevel") String cefrLevel,
        @Param("questionType") String questionType,
        @Param("topic") String topic,
        @Param("status") String status,
        @Param("keyword") String keyword
    );
}
