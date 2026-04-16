package com.example.DoAn.repository;

import com.example.DoAn.model.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Integer> {
    @EntityGraph(attributePaths = {"module"})
    List<Lesson> findByModule_ModuleIdOrderByOrderIndexAsc(Integer moduleId);

    @Query("SELECT COUNT(l) FROM Lesson l WHERE l.module.course.courseId = :courseId")
    long countByModuleCourse_CourseId(@Param("courseId") Integer courseId);

    // Find lesson by quizId via QuizAssignment join table (quiz_id column on lesson removed)
    @Query("SELECT qa.lesson FROM QuizAssignment qa WHERE qa.quiz.quizId = :quizId")
    Optional<Lesson> findByQuizId(@Param("quizId") Integer quizId);

    long countByModule_Course_Expert_UserId(Integer userId);

    @Query("SELECT l FROM Lesson l JOIN l.module m WHERE m.course.courseId = :courseId ORDER BY m.orderIndex ASC, l.orderIndex ASC")
    List<Lesson> findAllByCourseIdSorted(@Param("courseId") Integer courseId);
}