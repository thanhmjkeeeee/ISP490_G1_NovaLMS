package com.example.DoAn.repository;

import com.example.DoAn.model.LessonQuizProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LessonQuizProgressRepository extends JpaRepository<LessonQuizProgress, Integer> {

    Optional<LessonQuizProgress> findByLesson_LessonIdAndUser_UserIdAndQuiz_QuizId(
            Integer lessonId, Integer userId, Integer quizId);

    List<LessonQuizProgress> findByLesson_LessonIdAndUser_UserId(Integer lessonId, Integer userId);
}
