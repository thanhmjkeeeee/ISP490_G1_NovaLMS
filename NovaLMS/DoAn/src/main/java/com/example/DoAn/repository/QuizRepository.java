package com.example.DoAn.repository;

import com.example.DoAn.model.Quiz;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Integer> {

    Optional<Quiz> findByQuizCategory(String quizCategory);

    List<Quiz> findByQuizCategoryAndStatus(String category, String status);

    @Query("SELECT q FROM Quiz q WHERE q.course.courseId = :courseId AND q.quizCategory = :category AND q.status = :status")
    Optional<Quiz> findByCourseIdAndCategoryAndStatus(@Param("courseId") Integer courseId, @Param("category") String category, @Param("status") String status);

    Optional<Quiz> findFirstByCourseCourseIdAndQuizCategoryAndStatus(Integer courseId, String quizCategory, String status);

    Page<Quiz> findByUserUserId(Integer userId, Pageable pageable);

    @Query("SELECT q FROM Quiz q WHERE " +
           "(:courseId IS NULL OR q.course.courseId = :courseId) AND " +
           "(:category IS NULL OR q.quizCategory = :category) AND " +
           "(:status IS NULL OR q.status = :status) AND " +
           "(:keyword IS NULL OR q.title LIKE %:keyword%) " +
           "ORDER BY q.createdAt DESC")
    Page<Quiz> findByFilters(
        @Param("courseId") Integer courseId,
        @Param("category") String category,
        @Param("status") String status,
        @Param("keyword") String keyword,
        Pageable pageable
    );
}
