package com.example.DoAn.repository;

import com.example.DoAn.model.QuizAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface QuizAssignmentRepository extends JpaRepository<QuizAssignment, Integer> {

    List<QuizAssignment> findByLesson_LessonIdOrderByOrderIndexAsc(Integer lessonId);

    List<QuizAssignment> findByModule_ModuleIdOrderByOrderIndexAsc(Integer moduleId);

    boolean existsByLesson_LessonIdAndQuiz_QuizId(Integer lessonId, Integer quizId);

    boolean existsByModule_ModuleIdAndQuiz_QuizId(Integer moduleId, Integer quizId);

    void deleteByLesson_LessonIdAndQuiz_QuizId(Integer lessonId, Integer quizId);

    void deleteByModule_ModuleIdAndQuiz_QuizId(Integer moduleId, Integer quizId);

    @Query("SELECT COALESCE(MAX(qa.orderIndex), 0) FROM QuizAssignment qa WHERE qa.lesson.lessonId = :lessonId")
    Integer findMaxOrderByLesson(@Param("lessonId") Integer lessonId);

    @Query("SELECT COALESCE(MAX(qa.orderIndex), 0) FROM QuizAssignment qa WHERE qa.module.moduleId = :moduleId")
    Integer findMaxOrderByModule(@Param("moduleId") Integer moduleId);
}
