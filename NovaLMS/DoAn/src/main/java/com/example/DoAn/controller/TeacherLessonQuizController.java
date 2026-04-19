package com.example.DoAn.controller;

import com.example.DoAn.dto.request.QuizRequestDTO;
import com.example.DoAn.dto.response.LessonQuizResponseDTO;
import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.model.QuizAssignment;
import com.example.DoAn.model.User;
import com.example.DoAn.repository.*;
import com.example.DoAn.service.LessonQuizService;
import com.example.DoAn.service.TeacherQuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/teacher/lessons")
@RequiredArgsConstructor
public class TeacherLessonQuizController {

    private final LessonQuizService lessonQuizService;
    private final TeacherQuizService teacherQuizService;
    private final QuizAssignmentRepository quizAssignmentRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final ClazzRepository clazzRepository;

    private String getEmail(Principal principal) {
        if (principal instanceof OAuth2AuthenticationToken t) return t.getPrincipal().getAttribute("email");
        return principal != null ? principal.getName() : null;
    }

    // GET /api/v1/teacher/lessons/{lessonId}/quizzes
    @GetMapping("/{lessonId}/quizzes")
    public ResponseData<List<LessonQuizResponseDTO>> getLessonQuizzes(@PathVariable Integer lessonId) {
        List<QuizAssignment> assignments = quizAssignmentRepository
                .findByLesson_LessonIdOrderByOrderIndexAsc(lessonId);

        List<LessonQuizResponseDTO> result = assignments.stream().map(qa -> {
            var q = qa.getQuiz();
            return LessonQuizResponseDTO.builder()
                    .quizId(q.getQuizId())
                    .title(q.getTitle())
                    .description(q.getDescription())
                    .quizCategory(q.getQuizCategory())
                    .status(q.getStatus())
                    .orderIndex(qa.getOrderIndex())
                    .passScore(q.getPassScore())
                    .timeLimitMinutes(q.getTimeLimitMinutes())
                    .maxAttempts(q.getMaxAttempts())
                    .numberOfQuestions(q.getNumberOfQuestions())
                    .build();
        }).collect(Collectors.toList());

        return ResponseData.success("Danh sách quiz của bài học", result);
    }

    // POST /api/v1/teacher/lessons/{lessonId}/quizzes
    @PostMapping("/{lessonId}/quizzes")
    public ResponseData<?> createLessonQuiz(
            @PathVariable Integer lessonId,
            @RequestBody QuizRequestDTO request,
            Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Vui lòng đăng nhập.");

        var lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null) return ResponseData.error(404, "Không tìm thấy bài học");

        // Validate teacher owns a class containing this lesson
        User teacher = userRepository.findByEmail(email).orElse(null);
        if (teacher == null) return ResponseData.error(401, "Vui lòng đăng nhập.");

        boolean ownsClass = clazzRepository.findAllByTeacher_UserId(teacher.getUserId()).stream()
                .anyMatch(c -> c.getCourse() != null
                        && lesson.getModule() != null
                        && lesson.getModule().getCourse() != null
                        && lesson.getModule().getCourse().getCourseId().equals(c.getCourse().getCourseId()));

        if (!ownsClass) return ResponseData.error(403, "Bạn không phải giáo viên của lớp chứa bài học này");

        request.setLessonId(lessonId);
        request.setQuizCategory("LESSON_QUIZ");

        // Auto-derive courseId from lesson's module
        if (lesson.getModule() != null && lesson.getModule().getCourse() != null) {
            request.setCourseId(lesson.getModule().getCourse().getCourseId());
        }

        return teacherQuizService.createQuiz(request, email);
    }

    // PUT /api/v1/teacher/lessons/{lessonId}/quizzes/reorder
    @PutMapping("/{lessonId}/quizzes/reorder")
    @Transactional
    public ResponseData<?> reorderLessonQuizzes(
            @PathVariable Integer lessonId,
            @RequestBody List<Map<String, Integer>> orderList) {
        List<QuizAssignment> assignments = quizAssignmentRepository
                .findByLesson_LessonIdOrderByOrderIndexAsc(lessonId);
        for (Map<String, Integer> item : orderList) {
            Integer quizId = item.get("quizId");
            Integer orderIndex = item.get("orderIndex");
            assignments.stream()
                    .filter(a -> a.getQuiz().getQuizId().equals(quizId))
                    .findFirst()
                    .ifPresent(a -> a.setOrderIndex(orderIndex));
        }
        quizAssignmentRepository.saveAll(assignments);
        return ResponseData.success("Đã sắp xếp lại thứ tự quiz");
    }

    // DELETE /api/v1/teacher/lessons/{lessonId}/quizzes/{quizId}
    @DeleteMapping("/{lessonId}/quizzes/{quizId}")
    public ResponseData<?> detachQuiz(
            @PathVariable Integer lessonId,
            @PathVariable Integer quizId,
            Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Vui lòng đăng nhập.");

        lessonQuizService.detachQuizFromLesson(lessonId, quizId, email);
        return ResponseData.success("Đã gỡ quiz khỏi bài học");
    }
}
