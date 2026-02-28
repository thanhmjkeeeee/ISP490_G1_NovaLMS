package com.example.DoAn.repository;

import com.example.DoAn.model.UserLesson;
import com.example.DoAn.model.UserLessonId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}