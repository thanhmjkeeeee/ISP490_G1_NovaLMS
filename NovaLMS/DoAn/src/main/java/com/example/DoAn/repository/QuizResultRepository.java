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
           "WHERE (qr.passed IS NULL OR qr.status = 'LOCKED') " +
           "AND (q.user.email = :email " +
           "     OR c.teacher.email = :email " +
           "     OR EXISTS (SELECT 1 FROM Clazz tc WHERE tc.course = q.course AND tc.teacher.email = :email)) " +
           "ORDER BY COALESCE(qr.submittedAt, qr.startedAt) ASC")
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

    // ─── Assignment Grading Queue ─────────────────────────────────────────────
    // Returns all QuizResults for assignment-type quizzes (COURSE_ASSIGNMENT /
    // MODULE_ASSIGNMENT) where the teacher is enrolled in the student's class.
    @Query(value = """
        SELECT qr FROM QuizResult qr
        JOIN FETCH qr.quiz q
        LEFT JOIN FETCH qr.user stu
        LEFT JOIN FETCH q.course
        LEFT JOIN FETCH q.clazz c
        LEFT JOIN FETCH c.teacher t
        WHERE qr.assignmentSessionId IS NOT NULL
          AND q.quizCategory IN ('COURSE_ASSIGNMENT', 'MODULE_ASSIGNMENT')
          AND (t.email = :teacherEmail
               OR EXISTS (SELECT 1 FROM Clazz tc
                          WHERE tc.course = q.course
                            AND tc.teacher.email = :teacherEmail))
          AND (:quizId IS NULL OR q.quizId = :quizId)
          AND (:classId IS NULL OR c.classId = :classId)
        ORDER BY qr.submittedAt DESC
        """)
    Page<QuizResult> findAssignmentResultsForTeacher(
            @org.springframework.data.repository.query.Param("teacherEmail") String teacherEmail,
            @org.springframework.data.repository.query.Param("quizId") Integer quizId,
            @org.springframework.data.repository.query.Param("classId") Integer classId,
            Pageable pageable
    );
}
