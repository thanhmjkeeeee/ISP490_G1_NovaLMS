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

    List<Quiz> findAllByCourseCourseIdAndQuizCategoryAndStatus(Integer courseId, String quizCategory, String status);

    Page<Quiz> findByUserUserId(Integer userId, Pageable pageable);

    List<Quiz> findByClazz_ClassId(Integer classId);

    List<Quiz> findByClazz_ClassIdAndQuizCategory(Integer classId, String quizCategory);


    // Tìm tất cả quiz theo danh sách courseId (cho teacher xem quiz của các lớp mình)
    @Query("SELECT q FROM Quiz q WHERE q.course.courseId IN :courseIds AND q.quizCategory = 'COURSE_QUIZ' ORDER BY q.createdAt DESC")
    List<Quiz> findAllByCourseCourseIdIn(@Param("courseIds") List<Integer> courseIds);

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

    @Query("SELECT q FROM Quiz q WHERE q.course.courseId = :courseId " +
           "AND (q.quizCategory = 'COURSE_QUIZ' OR q.quizCategory = 'COURSE_ASSIGNMENT') " +
           "AND q.status = 'PUBLISHED' " +
           "AND (q.clazz IS NULL OR q.clazz.classId = :classId)")
    List<Quiz> findQuizzesForStudent(@Param("courseId") Integer courseId, @Param("classId") Integer classId);

    @Query("SELECT DISTINCT q FROM Quiz q " +
           "JOIN q.course c " +
           "JOIN c.classes cl " +
           "WHERE cl.classId = :classId " +
           "AND (" +
           "  q.quizId IN (SELECT l.quiz_id FROM Lesson l WHERE l.type = 'QUIZ') " +
           "  OR q.quizCategory = 'COURSE_ASSIGNMENT'" +
           ")")
    List<Quiz> findExpertQuizzesByClassId(@Param("classId") Integer classId);

}
