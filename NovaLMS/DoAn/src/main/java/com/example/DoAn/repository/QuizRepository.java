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

    @Query("SELECT q FROM Quiz q " +
           "LEFT JOIN FETCH q.course " +
           "LEFT JOIN FETCH q.module " +
           "LEFT JOIN FETCH q.lesson " +
           "LEFT JOIN FETCH q.user " +
           "WHERE q.quizId = :quizId")
    Optional<Quiz> findByIdWithDetails(@Param("quizId") Integer quizId);

    Optional<Quiz> findByQuizCategory(String quizCategory);

    List<Quiz> findByQuizCategoryAndStatus(String category, String status);

    @Query("SELECT q FROM Quiz q WHERE q.course.courseId = :courseId AND q.quizCategory = :category AND q.status = :status")
    Optional<Quiz> findByCourseIdAndCategoryAndStatus(@Param("courseId") Integer courseId, @Param("category") String category, @Param("status") String status);

    Optional<Quiz> findFirstByCourseCourseIdAndQuizCategoryAndStatus(Integer courseId, String quizCategory, String status);

    List<Quiz> findAllByCourseCourseIdAndQuizCategoryAndStatus(Integer courseId, String quizCategory, String status);

    Page<Quiz> findByUserUserId(Integer userId, Pageable pageable);

    List<Quiz> findByClazz_ClassId(Integer classId);

    List<Quiz> findByClazz_ClassIdAndQuizCategory(Integer classId, String quizCategory);


    // Tìm tất cả quiz theo danh sách courseId (cho teacher xem quiz của các lớp mình + bài của expert đã publish)
    @Query("SELECT q FROM Quiz q WHERE q.course.courseId IN :courseIds " +
           "AND (q.quizCategory IN ('COURSE_QUIZ', 'COURSE_ASSIGNMENT', 'MODULE_QUIZ', 'LESSON_QUIZ', 'MODULE_ASSIGNMENT')) " +
           "AND (q.user.userId = :userId OR q.status = 'PUBLISHED') " +
           "ORDER BY q.createdAt DESC")
    List<Quiz> findAllVisibleForTeacher(@Param("courseIds") List<Integer> courseIds, @Param("userId") Integer userId);

    @Query("SELECT q FROM Quiz q " +
           "LEFT JOIN FETCH q.course " +
           "LEFT JOIN FETCH q.module " +
           "LEFT JOIN FETCH q.lesson " +
           "WHERE " +
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

    @Query("SELECT q FROM Quiz q " +
           "JOIN q.course c " +
           "JOIN c.classes cl " +
           "LEFT JOIN FETCH q.module m " +
           "LEFT JOIN FETCH q.user u " +
           "WHERE cl.classId = :classId " +
           "AND q.status = 'PUBLISHED' " +
           "AND q.user.role.value = 'ROLE_EXPERT' " +
           "AND q.quizCategory IN ('COURSE_ASSIGNMENT', 'COURSE_QUIZ', 'MODULE_QUIZ', 'LESSON_QUIZ', 'MODULE_ASSIGNMENT')")
    List<Quiz> findExpertQuizzesByClassId(@Param("classId") Integer classId);

    boolean existsByTitleAndUser_UserId(String title, Integer userId);

    boolean existsByTitleAndUser_UserIdAndQuizIdNot(String title, Integer userId, Integer quizId);

}
