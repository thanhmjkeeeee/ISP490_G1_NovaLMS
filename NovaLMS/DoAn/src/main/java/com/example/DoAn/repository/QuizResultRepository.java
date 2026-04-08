package com.example.DoAn.repository;

import com.example.DoAn.model.QuizResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuizResultRepository extends JpaRepository<QuizResult, Integer> {
    Optional<QuizResult> findByQuizQuizIdAndUserUserId(Integer quizId, Integer userId);
    
    Optional<QuizResult> findByQuizQuizIdAndUser_EmailAndStatus(Integer quizId, String email, String status);

    @Query("SELECT qr FROM QuizResult qr WHERE qr.quiz.quizId = :quizId AND qr.user.email = :email")
    Optional<QuizResult> findByQuizIdAndUserEmail(@org.springframework.data.repository.query.Param("quizId") Integer quizId, @org.springframework.data.repository.query.Param("email") String email);


    long countByQuizQuizId(Integer quizId);

    long countByQuizQuizIdAndUserUserId(Integer quizId, Integer userId);

    Optional<QuizResult> findFirstByQuizQuizIdAndUserUserIdOrderBySubmittedAtDesc(Integer quizId, Integer userId);

    Page<QuizResult> findByPassedIsNullAndQuiz_User_EmailOrderBySubmittedAtAsc(String email, Pageable pageable);

    // Tìm quiz results chờ chấm mà teacher có quyền:
    // - Teacher tạo quiz (q.user.email) HOẶC
    // - Teacher được phân công lớp của quiz (q.clazz.teacher.email) HOẶC
    // - Quiz thuộc course mà teacher phụ trách qua bất kỳ lớp nào (EXISTS: tc.course = q.course AND tc.teacher.email = :email)
    @Query("SELECT qr FROM QuizResult qr " +
           "JOIN FETCH qr.quiz q " +
           "LEFT JOIN FETCH qr.user stu " +
           "LEFT JOIN FETCH q.course " +
           "LEFT JOIN FETCH q.clazz c " +
           "LEFT JOIN FETCH c.teacher " +
           "WHERE qr.passed IS NULL " +
           "AND (q.user.email = :email " +
           "     OR c.teacher.email = :email " +
           "     OR EXISTS (SELECT 1 FROM Clazz tc WHERE tc.course = q.course AND tc.teacher.email = :email)) " +
           "ORDER BY qr.submittedAt ASC")
    Page<QuizResult> findPendingGradingForTeacher(@org.springframework.data.repository.query.Param("email") String email, Pageable pageable);

    // Kết quả đã chấm xong (passed IS NOT NULL), teacher có quyền xem
    @Query("SELECT qr FROM QuizResult qr " +
           "JOIN FETCH qr.quiz q " +
           "LEFT JOIN FETCH qr.user stu " +
           "LEFT JOIN FETCH q.course " +
           "LEFT JOIN FETCH q.clazz c " +
           "LEFT JOIN FETCH c.teacher " +
           "WHERE qr.passed IS NOT NULL " +
           "AND (q.user.email = :email " +
           "     OR c.teacher.email = :email " +
           "     OR EXISTS (SELECT 1 FROM Clazz tc WHERE tc.course = q.course AND tc.teacher.email = :email)) " +
           "ORDER BY qr.submittedAt DESC")
    Page<QuizResult> findGradedForTeacher(@org.springframework.data.repository.query.Param("email") String email, Pageable pageable);

    Page<QuizResult> findByUserEmailOrderBySubmittedAtDesc(String email, Pageable pageable);

    List<QuizResult> findByUser_Email(String email);

    @Query("SELECT qr FROM QuizResult qr " +
           "JOIN FETCH qr.quiz q " +
           "LEFT JOIN FETCH q.course c " +
           "LEFT JOIN FETCH q.clazz cl " +
           "WHERE qr.user.email = :email " +
           "AND (:category IS NULL OR :category = 'ALL' OR q.quizCategory = :category) " +
           "AND (:keyword IS NULL OR LOWER(q.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY qr.submittedAt DESC")
    Page<QuizResult> findByUserEmailAndCategory(
            @org.springframework.data.repository.query.Param("email") String email,
            @org.springframework.data.repository.query.Param("category") String category,
            @org.springframework.data.repository.query.Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("""
        SELECT DISTINCT qr FROM QuizResult qr
        JOIN FETCH qr.quiz q
        LEFT JOIN FETCH qr.user stu
        JOIN Registration reg ON reg.user.userId = stu.userId
        WHERE reg.clazz.classId = :classId
          AND LOWER(reg.status) = 'approved'
          AND (qr.status IS NULL OR qr.status IN ('SUBMITTED', 'GRADING', 'GRADED'))
          AND (
               q.quizCategory = 'COURSE_ASSIGNMENT' 
               OR EXISTS (
                   SELECT 1 FROM Lesson l 
                   JOIN l.module m 
                   WHERE l.type = 'QUIZ' 
                   AND m.course.courseId = reg.clazz.course.courseId
                   AND l.lessonId = q.lesson.lessonId 
               )
          )
          AND (
            'ALL' IN :status
            OR ('PENDING_SPEAKING' IN :status
                AND EXISTS (SELECT 1 FROM QuizAnswer qa JOIN qa.question qu WHERE qa.quizResult.resultId = qr.resultId AND qu.skill = 'SPEAKING' AND qa.pointsAwarded IS NULL)
                AND NOT EXISTS (SELECT 1 FROM QuizAnswer qa JOIN qa.question qu WHERE qa.quizResult.resultId = qr.resultId AND qu.skill = 'WRITING' AND qa.pointsAwarded IS NULL)
               )
            OR ('PENDING_WRITING' IN :status
                AND EXISTS (SELECT 1 FROM QuizAnswer qa JOIN qa.question qu WHERE qa.quizResult.resultId = qr.resultId AND qu.skill = 'WRITING' AND qa.pointsAwarded IS NULL)
                AND NOT EXISTS (SELECT 1 FROM QuizAnswer qa JOIN qa.question qu WHERE qa.quizResult.resultId = qr.resultId AND qu.skill = 'SPEAKING' AND qa.pointsAwarded IS NULL)
               )
            OR ('PENDING_BOTH' IN :status
                AND EXISTS (SELECT 1 FROM QuizAnswer qa JOIN qa.question qu WHERE qa.quizResult.resultId = qr.resultId AND qu.skill = 'WRITING' AND qa.pointsAwarded IS NULL)
                AND EXISTS (SELECT 1 FROM QuizAnswer qa JOIN qa.question qu WHERE qa.quizResult.resultId = qr.resultId AND qu.skill = 'SPEAKING' AND qa.pointsAwarded IS NULL)
               )
            OR ('ALL_GRADED' IN :status
                AND qr.passed IS NOT NULL
               )
          )
        ORDER BY qr.submittedAt DESC
        """)
    Page<QuizResult> findAssignmentResultsForTeacherV2(
            @org.springframework.data.repository.query.Param("classId") Integer classId,
            @org.springframework.data.repository.query.Param("status") java.util.Collection<String> status,
            Pageable pageable
    );

}
