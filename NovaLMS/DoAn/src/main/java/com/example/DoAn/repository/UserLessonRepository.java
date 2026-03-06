package com.example.DoAn.repository;

import com.example.DoAn.model.UserLesson;
import com.example.DoAn.model.UserLessonId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserLessonRepository extends JpaRepository<UserLesson, UserLessonId> {

    @Query("SELECT CASE WHEN COUNT(ul) > 0 THEN true ELSE false END FROM UserLesson ul " +
            "WHERE ul.user.userId = :userId AND ul.lesson.lessonId = :lessonId AND ul.status = :status")
    boolean existsByUserIdAndLessonIdAndStatus(
            @Param("userId") Integer userId,
            @Param("lessonId") Integer lessonId,
            @Param("status") String status
    );

    @Query("SELECT COUNT(ul) FROM UserLesson ul " +
            "JOIN ul.lesson l " +
            "JOIN l.module m " +
            "JOIN m.course c " +
            "WHERE ul.user.userId = :userId AND c.courseId = :courseId AND ul.status = 'Completed'")
    long countCompletedLessonsByUserIdAndCourseId(
            @Param("userId") Integer userId,
            @Param("courseId") Integer courseId
    );


    // Tìm danh sách ID các bài chưa học
    @Query("SELECT l.lessonId FROM Lesson l JOIN l.module m " +
            "WHERE m.course.courseId = :courseId " +
            "AND l.lessonId NOT IN (SELECT ul.lesson.lessonId FROM UserLesson ul WHERE ul.user.userId = :userId AND ul.status = 'Completed') " +
            "ORDER BY m.orderIndex ASC, l.orderIndex ASC")
    List<Integer> findUncompletedLessonIds(@Param("userId") Integer userId, @Param("courseId") Integer courseId);

    // Tìm danh sách toàn bộ ID bài học của khóa
    @Query("SELECT l.lessonId FROM Lesson l JOIN l.module m " +
            "WHERE m.course.courseId = :courseId " +
            "ORDER BY m.orderIndex ASC, l.orderIndex ASC")
    List<Integer> findAllLessonIdsOfCourse(@Param("courseId") Integer courseId);


    List<UserLesson> findByUser_UserId(Integer userId);
    Optional<UserLesson> findByUser_UserIdAndLesson_LessonId(Integer userId, Integer lessonId);
}