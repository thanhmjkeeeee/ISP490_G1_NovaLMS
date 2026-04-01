package com.example.DoAn.service;

import com.example.DoAn.dto.response.LessonQuizResponseDTO;
import com.example.DoAn.exception.InvalidDataException;
import com.example.DoAn.exception.ResourceNotFoundException;
import com.example.DoAn.model.*;
import com.example.DoAn.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LessonQuizService {

    private final QuizAssignmentRepository quizAssignmentRepository;
    private final LessonQuizProgressRepository progressRepository;
    private final QuizRepository quizRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final ModuleRepository moduleRepository;
    private final ClazzRepository clazzRepository;

    // ═══════════════════════════════════════════════════════════════════════
    //  STUDENT: List quizzes for a lesson (with sequential status)
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<LessonQuizResponseDTO> getLessonQuizzesForStudent(Integer lessonId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        List<QuizAssignment> assignments = quizAssignmentRepository
                .findByLesson_LessonIdOrderByOrderIndexAsc(lessonId);

        if (assignments.isEmpty()) return List.of();

        Map<Integer, LessonQuizProgress> progressMap = progressRepository
                .findByLesson_LessonIdAndUser_UserId(lessonId, user.getUserId())
                .stream().collect(Collectors.toMap(
                        p -> p.getQuiz().getQuizId(), p -> p, (a, b) -> a));

        List<LessonQuizResponseDTO> result = new ArrayList<>();

        for (int i = 0; i < assignments.size(); i++) {
            QuizAssignment qa = assignments.get(i);
            Quiz quiz = qa.getQuiz();
            LessonQuizProgress progress = progressMap.get(quiz.getQuizId());

            String status = computeSequentialStatus(i, assignments, progressMap);

            result.add(LessonQuizResponseDTO.builder()
                    .quizId(quiz.getQuizId())
                    .title(quiz.getTitle())
                    .description(quiz.getDescription())
                    .quizCategory(quiz.getQuizCategory())
                    .status(status)
                    .orderIndex(qa.getOrderIndex())
                    .passScore(quiz.getPassScore())
                    .timeLimitMinutes(quiz.getTimeLimitMinutes())
                    .maxAttempts(quiz.getMaxAttempts())
                    .numberOfQuestions(quiz.getNumberOfQuestions())
                    .bestScore(progress != null ? progress.getBestScore() : null)
                    .bestPassed(progress != null ? progress.getBestPassed() : null)
                    .build());
        }

        return result;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  STUDENT: Validate quiz is AVAILABLE before taking
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public void validateQuizAvailableForStudent(Integer lessonId, Integer quizId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        if (!quizAssignmentRepository.existsByLesson_LessonIdAndQuiz_QuizId(lessonId, quizId)) {
            throw new InvalidDataException("Quiz không được gắn với bài học này");
        }

        List<QuizAssignment> assignments = quizAssignmentRepository
                .findByLesson_LessonIdOrderByOrderIndexAsc(lessonId);
        Map<Integer, LessonQuizProgress> progressMap = progressRepository
                .findByLesson_LessonIdAndUser_UserId(lessonId, user.getUserId())
                .stream().collect(Collectors.toMap(
                        p -> p.getQuiz().getQuizId(), p -> p, (a, b) -> a));

        int quizIndex = -1;
        for (int i = 0; i < assignments.size(); i++) {
            if (assignments.get(i).getQuiz().getQuizId().equals(quizId)) {
                quizIndex = i;
                break;
            }
        }

        if (quizIndex < 0) {
            throw new ResourceNotFoundException("Quiz không tồn tại trong bài học này");
        }

        String status = computeSequentialStatus(quizIndex, assignments, progressMap);

        if ("LOCKED".equals(status)) {
            throw new InvalidDataException("Bạn cần hoàn thành quiz trước để mở khóa quiz này");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CALLED AFTER QUIZ SUBMISSION — update progress + unlock next
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public void updateProgressAfterSubmit(Integer lessonId, Integer quizId, Integer userId,
                                          Double score, Boolean passed) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quiz"));

        LessonQuizProgress progress = progressRepository
                .findByLesson_LessonIdAndUser_UserIdAndQuiz_QuizId(lessonId, userId, quizId)
                .orElseGet(() -> LessonQuizProgress.builder()
                        .lesson(lesson).user(user).quiz(quiz)
                        .status("COMPLETED").bestPassed(false).build());

        if (progress.getBestScore() == null || score > progress.getBestScore()) {
            progress.setBestScore(score);
        }
        if (Boolean.TRUE.equals(passed)) {
            progress.setBestPassed(true);
        }
        progress.setStatus("COMPLETED");
        progressRepository.save(progress);

        List<QuizAssignment> assignments = quizAssignmentRepository
                .findByLesson_LessonIdOrderByOrderIndexAsc(lessonId);

        int quizIndex = -1;
        for (int i = 0; i < assignments.size(); i++) {
            if (assignments.get(i).getQuiz().getQuizId().equals(quizId)) {
                quizIndex = i;
                break;
            }
        }

        if (quizIndex >= 0 && quizIndex + 1 < assignments.size()) {
            Quiz nextQuiz = assignments.get(quizIndex + 1).getQuiz();
            LessonQuizProgress nextProgress = progressRepository
                    .findByLesson_LessonIdAndUser_UserIdAndQuiz_QuizId(lessonId, userId, nextQuiz.getQuizId())
                    .orElseGet(() -> LessonQuizProgress.builder()
                            .lesson(lesson).user(user).quiz(nextQuiz)
                            .status("LOCKED").bestPassed(false).build());

            if (!"COMPLETED".equals(nextProgress.getStatus())) {
                nextProgress.setStatus("AVAILABLE");
                progressRepository.save(nextProgress);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ASSIGN / DETACH quiz from module
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public QuizAssignment assignQuizToModule(Integer moduleId, Quiz quiz) {
        if (quizAssignmentRepository.existsByModule_ModuleIdAndQuiz_QuizId(moduleId, quiz.getQuizId())) {
            throw new InvalidDataException("Quiz đã được gắn với chương này");
        }
        com.example.DoAn.model.Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương"));
        Integer maxOrder = quizAssignmentRepository.findMaxOrderByModule(moduleId);
        QuizAssignment qa = QuizAssignment.builder()
                .quiz(quiz)
                .module(module)
                .orderIndex((maxOrder != null ? maxOrder : 0) + 1)
                .build();
        return quizAssignmentRepository.save(qa);
    }

    @Transactional
    public void detachQuizFromModule(Integer moduleId, Integer quizId) {
        com.example.DoAn.model.Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương"));
        if (module.getCourse() == null) {
            throw new InvalidDataException("Chương không thuộc khóa học nào");
        }
        quizAssignmentRepository.deleteByModule_ModuleIdAndQuiz_QuizId(moduleId, quizId);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ASSIGN / DETACH quiz from lesson
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public QuizAssignment assignQuizToLesson(Integer lessonId, Quiz quiz) {
        if (quizAssignmentRepository.existsByLesson_LessonIdAndQuiz_QuizId(lessonId, quiz.getQuizId())) {
            throw new InvalidDataException("Quiz đã được gắn với bài học này");
        }
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học"));
        Integer maxOrder = quizAssignmentRepository.findMaxOrderByLesson(lessonId);
        QuizAssignment qa = QuizAssignment.builder()
                .quiz(quiz)
                .lesson(lesson)
                .orderIndex((maxOrder != null ? maxOrder : 0) + 1)
                .build();
        return quizAssignmentRepository.save(qa);
    }

    @Transactional
    public void detachQuizFromLesson(Integer lessonId, Integer quizId, String email) {
        User teacher = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học"));
        if (lesson.getModule() == null || lesson.getModule().getCourse() == null) {
            throw new InvalidDataException("Bài học không thuộc khóa học nào");
        }
        Integer courseId = lesson.getModule().getCourse().getCourseId();
        boolean ownsCourse = clazzRepository.findAllByTeacher_UserId(teacher.getUserId()).stream()
                .anyMatch(c -> c.getCourse() != null
                        && c.getCourse().getCourseId().equals(courseId));
        if (!ownsCourse) {
            throw new InvalidDataException("Bạn không có quyền gỡ quiz khỏi bài học này");
        }
        quizAssignmentRepository.deleteByLesson_LessonIdAndQuiz_QuizId(lessonId, quizId);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Computes quiz status based on sequential progression rules.
     * First quiz = AVAILABLE. Subsequent quiz = AVAILABLE only if previous quiz bestPassed=true.
     */
    private String computeSequentialStatus(int quizIndex,
                                           List<QuizAssignment> assignments,
                                           Map<Integer, LessonQuizProgress> progressMap) {
        if (quizIndex < 0) return "LOCKED";

        QuizAssignment qa = assignments.get(quizIndex);
        LessonQuizProgress progress = progressMap.get(qa.getQuiz().getQuizId());

        if (progress != null) {
            return progress.getStatus();
        }
        if (quizIndex == 0) {
            return "AVAILABLE";
        }
        Quiz prevQuiz = assignments.get(quizIndex - 1).getQuiz();
        LessonQuizProgress prevProgress = progressMap.get(prevQuiz.getQuizId());
        return (prevProgress != null && Boolean.TRUE.equals(prevProgress.getBestPassed()))
                ? "AVAILABLE" : "LOCKED";
    }
}
