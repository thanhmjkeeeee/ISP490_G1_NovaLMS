package com.example.DoAn.repository;

import com.example.DoAn.model.QuizResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT qr.quiz.quizId, COUNT(qr) FROM QuizResult qr WHERE qr.user.userId = :userId AND qr.quiz.quizId IN :quizIds GROUP BY qr.quiz.quizId")
    List<Object[]> countAttemptsByUserPerQuiz(@Param("userId") Integer userId, @Param("quizIds") List<Integer> quizIds);


    long countByQuizQuizIdAndUserUserIdAndStatusNot(Integer quizId, Integer userId, String status);

    Optional<QuizResult> findFirstByQuizQuizIdAndUserUserIdOrderBySubmittedAtDesc(Integer quizId, Integer userId);

    Page<QuizResult> findByPassedIsNullAndQuiz_User_EmailOrderBySubmittedAtAsc(String email, Pageable pageable);

    // Tìm quiz results chờ chấm mà teacher có quyền:
    // - Teacher tạo quiz (q.user.email) HOẶC
    // - Teacher được phân công lớp của quiz (q.clazz.teacher.email) HOẶC
    // - Quiz thuộc course mà teacher phụ trách qua bất kỳ lớp nào (EXISTS: tc.course = q.course AND tc.teacher.email = :email)
    // Tìm quiz results chờ chấm mà teacher có quyền
    @Query("SELECT DISTINCT qr FROM QuizResult qr " +
           "JOIN FETCH qr.quiz q " +
           "LEFT JOIN FETCH qr.user stu " +
           "JOIN Registration reg ON reg.user.userId = stu.userId " +
           "LEFT JOIN FETCH q.course " +
           "LEFT JOIN FETCH q.clazz c " +
           "LEFT JOIN FETCH c.teacher " +
           "WHERE (qr.passed IS NULL OR qr.status = 'LOCKED') " +
           "AND q.quizCategory != 'COURSE_ASSIGNMENT' " +
           "AND (:classId IS NULL OR (reg.clazz.classId = :classId AND LOWER(reg.status) = 'approved')) " +
           "AND (q.user.email = :email " +
           "     OR (q.clazz IS NOT NULL AND q.clazz.teacher.email = :email) " +
           "     OR EXISTS (SELECT 1 FROM Clazz tc WHERE tc.course = q.course AND tc.teacher.email = :email) " +
           "     OR EXISTS (SELECT 1 FROM Lesson l JOIN l.module m WHERE l.lessonId = q.lesson.lessonId AND m.course.courseId = reg.clazz.course.courseId)) " +
           "ORDER BY COALESCE(qr.submittedAt, qr.startedAt) ASC")
    Page<QuizResult> findPendingGradingForTeacher(
            @org.springframework.data.repository.query.Param("email") String email,
            @org.springframework.data.repository.query.Param("classId") Integer classId,
            Pageable pageable);

    // Kết quả đã chấm xong (passed IS NOT NULL), teacher có quyền xem
    @Query("SELECT DISTINCT qr FROM QuizResult qr " +
           "JOIN FETCH qr.quiz q " +
           "LEFT JOIN FETCH qr.user stu " +
           "JOIN Registration reg ON reg.user.userId = stu.userId " +
           "LEFT JOIN FETCH q.course " +
           "LEFT JOIN FETCH q.clazz c " +
           "LEFT JOIN FETCH c.teacher " +
           "WHERE qr.passed IS NOT NULL " +
           "AND q.quizCategory != 'COURSE_ASSIGNMENT' " +
           "AND (:classId IS NULL OR (reg.clazz.classId = :classId AND LOWER(reg.status) = 'approved')) " +
           "AND (q.user.email = :email " +
           "     OR (q.clazz IS NOT NULL AND q.clazz.teacher.email = :email) " +
           "     OR EXISTS (SELECT 1 FROM Clazz tc WHERE tc.course = q.course AND tc.teacher.email = :email) " +
           "     OR EXISTS (SELECT 1 FROM Lesson l JOIN l.module m WHERE l.lessonId = q.lesson.lessonId AND m.course.courseId = reg.clazz.course.courseId)) " +
           "ORDER BY qr.submittedAt DESC")
    Page<QuizResult> findGradedForTeacher(
            @org.springframework.data.repository.query.Param("email") String email,
            @org.springframework.data.repository.query.Param("classId") Integer classId,
            Pageable pageable);

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
               (q.quizCategory = 'COURSE_ASSIGNMENT' AND (q.clazz.classId = :classId OR (q.clazz IS NULL AND q.course.courseId = reg.clazz.course.courseId))) 
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
            OR ('PENDING_SPEAKING' IN :status AND qr.passed IS NULL AND (qr.status = 'GRADING' OR EXISTS (SELECT 1 FROM QuizAnswer qa JOIN qa.question qu WHERE qa.quizResult.resultId = qr.resultId AND qu.skill = 'SPEAKING' AND qa.pointsAwarded IS NULL)) AND NOT EXISTS (SELECT 1 FROM QuizAnswer qa JOIN qa.question qu WHERE qa.quizResult.resultId = qr.resultId AND qu.skill = 'WRITING' AND qa.pointsAwarded IS NULL))
            OR ('PENDING_WRITING' IN :status AND qr.passed IS NULL AND (qr.status = 'GRADING' OR EXISTS (SELECT 1 FROM QuizAnswer qa JOIN qa.question qu WHERE qa.quizResult.resultId = qr.resultId AND qu.skill = 'WRITING' AND qa.pointsAwarded IS NULL)) AND NOT EXISTS (SELECT 1 FROM QuizAnswer qa JOIN qa.question qu WHERE qa.quizResult.resultId = qr.resultId AND qu.skill = 'SPEAKING' AND qa.pointsAwarded IS NULL))
            OR ('PENDING_BOTH' IN :status AND qr.passed IS NULL AND (qr.status = 'GRADING' OR (EXISTS (SELECT 1 FROM QuizAnswer qa JOIN qa.question qu WHERE qa.quizResult.resultId = qr.resultId AND qu.skill = 'WRITING' AND qa.pointsAwarded IS NULL) AND EXISTS (SELECT 1 FROM QuizAnswer qa JOIN qa.question qu WHERE qa.quizResult.resultId = qr.resultId AND qu.skill = 'SPEAKING' AND qa.pointsAwarded IS NULL))))
            OR ('ALL_GRADED' IN :status AND qr.passed IS NOT NULL)
          )
        ORDER BY qr.submittedAt DESC
        """)
    Page<QuizResult> findAssignmentResultsForTeacherV2(
            @org.springframework.data.repository.query.Param("classId") Integer classId,
            @org.springframework.data.repository.query.Param("status") java.util.Collection<String> status,
            Pageable pageable
    );

    @Query("SELECT COUNT(DISTINCT qr.resultId) FROM QuizResult qr " +
            "JOIN qr.user u " +
            "JOIN Registration reg ON reg.user.userId = u.userId " +
            "WHERE reg.clazz.classId = :classId " +
            "AND reg.status = 'Approved' " +
            "AND qr.passed IS NULL " +
            "AND qr.status != 'LOCKED' " +
            "AND (qr.quiz.clazz.classId = :classId OR (qr.quiz.clazz IS NULL AND qr.quiz.course.courseId = reg.clazz.course.courseId))")
    long countPendingGradingForClass(@Param("classId") Integer classId);

    @Query("SELECT COUNT(DISTINCT qr.resultId) FROM QuizResult qr " +
            "JOIN qr.user u " +
            "JOIN Registration reg ON reg.user.userId = u.userId " +
            "WHERE reg.clazz.classId = :classId " +
            "AND reg.status = 'Approved' " +
            "AND qr.isUnlockRequested = true " +
            "AND (qr.quiz.clazz.classId = :classId OR (qr.quiz.clazz IS NULL AND qr.quiz.course.courseId = reg.clazz.course.courseId))")
    long countUnlockRequestsForClass(@Param("classId") Integer classId);

    @Query("SELECT qr FROM QuizResult qr " +
            "JOIN FETCH qr.quiz q " +
            "JOIN FETCH qr.user u " +
            "JOIN Registration reg ON reg.user.userId = u.userId " +
            "WHERE reg.clazz.classId = :classId " +
            "AND reg.status = 'Approved' " +
            "AND (qr.status IS NULL OR qr.status IN ('SUBMITTED', 'GRADING', 'GRADED')) " +
            "AND (q.clazz.classId = :classId OR (q.clazz IS NULL AND q.course.courseId = reg.clazz.course.courseId)) " +
            "ORDER BY qr.submittedAt DESC")
    List<QuizResult> findRecentSubmissionsByClassId(@org.springframework.data.repository.query.Param("classId") Integer classId, Pageable pageable);

    // Tính điểm trung bình của học viên trong các lớp được chỉ định và lọc ra những ai dưới mức điểm (threshold)
    @Query("SELECT u.fullName, u.email, AVG(qr.score) " +
            "FROM QuizResult qr " +
            "JOIN qr.user u " +
            "JOIN Registration r ON r.user.userId = u.userId " +
            "WHERE r.clazz.classId IN :classIds " +
            "AND r.status = 'Approved' " +
            "AND qr.score IS NOT NULL " +
            "AND (qr.quiz.clazz.classId IN :classIds OR qr.quiz.course.courseId = r.clazz.course.courseId) " +
            "GROUP BY u.userId, u.fullName, u.email " +
            "HAVING AVG(qr.score) < :threshold " +
            "ORDER BY AVG(qr.score) ASC") // Sắp xếp ai điểm thấp nhất lên đầu
    List<Object[]> findStudentsWithAverageScoreBelow(@Param("classIds") List<Integer> classIds, @Param("threshold") Double threshold);

    @Query("SELECT DISTINCT qr FROM QuizResult qr " +
           "JOIN FETCH qr.quiz q " +
           "JOIN FETCH qr.user stu " +
           "JOIN Registration reg ON reg.user.userId = stu.userId " +
           "LEFT JOIN FETCH q.course " +
           "LEFT JOIN FETCH q.clazz c " +
           "LEFT JOIN FETCH c.teacher " +
           "WHERE qr.isUnlockRequested = true " +
           "AND (:classId IS NULL OR reg.clazz.classId = :classId) " +
           "AND (:classId IS NULL OR (q.clazz.classId = :classId OR (q.clazz IS NULL AND q.course.courseId = reg.clazz.course.courseId))) " +
           "AND LOWER(reg.status) = 'approved' " +
           "AND (q.user.email = :email " +
           "     OR (c IS NOT NULL AND c.teacher.email = :email) " +
           "     OR EXISTS (SELECT 1 FROM Clazz tc WHERE tc.course = q.course AND tc.teacher.email = :email)) " +
           "ORDER BY qr.submittedAt DESC")
    Page<QuizResult> findUnlockRequestsForTeacher(
            @org.springframework.data.repository.query.Param("email") String email,
            @org.springframework.data.repository.query.Param("classId") Integer classId,
            Pageable pageable);

}
