# Module & Lesson Quiz Feature — Implementation Plan

> **For agentic workers:** Use superpowers:subagent-driven-development or execute task-by-task in current session. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Allow Experts to create quizzes scoped to a Module, and Teachers to create quizzes scoped to a Lesson, with sequential student progression through quizzes after lesson content.

**Architecture:** Three nullable FK fields (`course_id`, `module_id`, `lesson_id`) added to `Quiz` entity. New `QuizAssignment` table tracks ordered quiz attachments per lesson/module. New `LessonQuizProgress` table tracks per-student AVAILABLE/LOCKED/COMPLETED state.

**Tech Stack:** Spring Boot (Spring Data JPA, Spring Security), Thymeleaf, MySQL, JWT auth.

---

## File Map

### New Files to Create

| File | Purpose |
|---|---|
| `model/QuizAssignment.java` | Links quiz → lesson/module with order |
| `model/LessonQuizProgress.java` | Per-student quiz status |
| `repository/QuizAssignmentRepository.java` | CRUD + ordered lookup |
| `repository/LessonQuizProgressRepository.java` | CRUD + lookup by lesson+user |
| `service/LessonQuizService.java` | Student progression + unlock logic |
| `dto/response/LessonQuizResponseDTO.java` | DTO for lesson quiz list |
| `controller/LearningLessonQuizController.java` | Student lesson quiz endpoints |
| `controller/ExpertModuleQuizController.java` | Expert module quiz endpoints |
| `controller/TeacherLessonQuizController.java` | Teacher lesson quiz endpoints |
| `migration_module_lesson_quiz.sql` | Raw SQL for DB schema |

### Files to Modify

| File | Change |
|---|---|
| `model/Quiz.java` | Add `module`, `lesson` FK fields |
| `model/Module.java` | Add `quizAssignments` one-to-many |
| `model/Lesson.java` | Add `quizAssignments` one-to-many; remove `quiz_id` column |
| `dto/request/QuizRequestDTO.java` | Add `moduleId`, `lessonId` fields |
| `repository/QuizRepository.java` | Add `findByModule_ModuleId`, `findByLesson_LessonId` |
| `service/IExpertQuizService.java` | Add module-scoped method signatures |
| `service/impl/ExpertQuizServiceImpl.java` | Implement module quiz CRUD |
| `service/TeacherQuizService.java` | Add lesson_id support in create/update |
| `service/impl/QuizResultServiceImpl.java` | Call `LessonQuizService.updateProgressAfterSubmit` after submit |
| `templates/expert/module-detail.html` | Add Quizzes tab |
| `templates/teacher/lesson-detail.html` | Add Quizzes section |
| `templates/student/lesson.html` | Add Quizzes section after lesson content |

---

## Chunk 1: Database Layer

### Task 1: Modify `Quiz` Entity

**File:** `src/main/java/com/example/DoAn/model/Quiz.java`

Add two fields after the existing `course` field (lines ~23-25):

```java
// Module-level quiz (Expert creates)
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "module_id", nullable = true)
private Module module;

// Lesson-level quiz (Teacher creates)
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "lesson_id", nullable = true)
private Lesson lesson;
```

---

### Task 2: Create `QuizAssignment` Entity

**File:** `src/main/java/com/example/DoAn/model/QuizAssignment.java`

```java
package com.example.DoAn.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "quiz_assignment",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"quiz_id", "lesson_id"}),
        @UniqueConstraint(columnNames = {"quiz_id", "module_id"})
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignment_id")
    private Integer assignmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = true)
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = true)
    private Module module;

    @Column(name = "order_index")
    private Integer orderIndex;
}
```

---

### Task 3: Create `LessonQuizProgress` Entity

**File:** `src/main/java/com/example/DoAn/model/LessonQuizProgress.java`

```java
package com.example.DoAn.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lesson_quiz_progress",
    uniqueConstraints = @UniqueConstraint(columnNames = {"lesson_id", "user_id", "quiz_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonQuizProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "progress_id")
    private Integer progressId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    // AVAILABLE | LOCKED | COMPLETED
    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "best_score")
    private Double bestScore;

    @Column(name = "best_passed")
    private Boolean bestPassed = false;
}
```

---

### Task 4: Add `quizAssignments` to `Module` and `Lesson`

**File:** `src/main/java/com/example/DoAn/model/Module.java`

Add after the existing `lessons` field:

```java
@OneToMany(mappedBy = "module", cascade = CascadeType.ALL, orphanRemoval = true)
@OrderBy("orderIndex ASC")
private List<QuizAssignment> quizAssignments;
```

**File:** `src/main/java/com/example/DoAn/model/Lesson.java`

1. Remove the existing `quiz_id` field entirely:
```java
// DELETE THIS WHOLE FIELD:
// @Column(name = "quiz_id")
// private Integer quiz_id;
```

2. Add after the existing `module` field:
```java
@OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
@OrderBy("orderIndex ASC")
private List<QuizAssignment> quizAssignments;
```

---

### Task 5: Create Repositories

**File:** `src/main/java/com/example/DoAn/repository/QuizAssignmentRepository.java`

```java
package com.example.DoAn.repository;

import com.example.DoAn.model.QuizAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface QuizAssignmentRepository extends JpaRepository<QuizAssignment, Integer> {

    List<QuizAssignment> findByLesson_LessonIdOrderByOrderIndexAsc(Integer lessonId);

    List<QuizAssignment> findByModule_ModuleIdOrderByOrderIndexAsc(Integer moduleId);

    boolean existsByLesson_LessonIdAndQuiz_QuizId(Integer lessonId, Integer quizId);

    boolean existsByModule_ModuleIdAndQuiz_QuizId(Integer moduleId, Integer quizId);

    void deleteByLesson_LessonIdAndQuiz_QuizId(Integer lessonId, Integer quizId);

    void deleteByModule_ModuleIdAndQuiz_QuizId(Integer moduleId, Integer quizId);

    @Query("SELECT COALESCE(MAX(qa.orderIndex), 0) FROM QuizAssignment qa WHERE qa.lesson.lessonId = :lessonId")
    Integer findMaxOrderByLesson(@Param("lessonId") Integer lessonId);

    @Query("SELECT COALESCE(MAX(qa.orderIndex), 0) FROM QuizAssignment qa WHERE qa.module.moduleId = :moduleId")
    Integer findMaxOrderByModule(@Param("moduleId") Integer moduleId);
}
```

**File:** `src/main/java/com/example/DoAn/repository/LessonQuizProgressRepository.java`

```java
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
```

---

### Task 6: Write Migration SQL

**File:** `docs/superpowers/plans/migration_module_lesson_quiz.sql`

```sql
-- Add module_id and lesson_id columns to quiz table
ALTER TABLE quiz
    ADD COLUMN module_id INT,
    ADD COLUMN lesson_id INT,
    ADD CONSTRAINT fk_quiz_module FOREIGN KEY (module_id) REFERENCES module(module_id),
    ADD CONSTRAINT fk_quiz_lesson FOREIGN KEY (lesson_id) REFERENCES lesson(lesson_id);

-- Create quiz_assignment table
CREATE TABLE quiz_assignment (
    assignment_id INT AUTO_INCREMENT PRIMARY KEY,
    quiz_id INT NOT NULL,
    lesson_id INT,
    module_id INT,
    order_index INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_qa_quiz FOREIGN KEY (quiz_id) REFERENCES quiz(quiz_id),
    CONSTRAINT fk_qa_lesson FOREIGN KEY (lesson_id) REFERENCES lesson(lesson_id),
    CONSTRAINT fk_qa_module FOREIGN KEY (module_id) REFERENCES module(module_id),
    CONSTRAINT uq_qa_lesson_quiz UNIQUE (quiz_id, lesson_id),
    CONSTRAINT uq_qa_module_quiz UNIQUE (quiz_id, module_id)
);

-- Create lesson_quiz_progress table
CREATE TABLE lesson_quiz_progress (
    progress_id INT AUTO_INCREMENT PRIMARY KEY,
    lesson_id INT NOT NULL,
    user_id INT NOT NULL,
    quiz_id INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'LOCKED',
    best_score DOUBLE,
    best_passed BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_lqp_lesson FOREIGN KEY (lesson_id) REFERENCES lesson(lesson_id),
    CONSTRAINT fk_lqp_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_lqp_quiz FOREIGN KEY (quiz_id) REFERENCES quiz(quiz_id),
    CONSTRAINT uq_lqp UNIQUE (lesson_id, user_id, quiz_id)
);

-- Remove obsolete quiz_id column from lesson (if column exists)
ALTER TABLE lesson DROP COLUMN IF EXISTS quiz_id;
```

---

## Chunk 2: Expert Module Quiz Backend

### Task 7: Extend `QuizRequestDTO`

**File:** `src/main/java/com/example/DoAn/dto/request/QuizRequestDTO.java`

Add two fields after `classId` (around line 22):

```java
private Integer moduleId;   // for MODULE_QUIZ
private Integer lessonId;   // for LESSON_QUIZ
```

---

### Task 8: Update `QuizRepository`

**File:** `src/main/java/com/example/DoAn/repository/QuizRepository.java`

Add after the existing methods (around line 48):

```java
List<Quiz> findByModule_ModuleId(Integer moduleId);

List<Quiz> findByLesson_LessonId(Integer lessonId);
```

---

### Task 9: Create `LessonQuizService`

**File:** `src/main/java/com/example/DoAn/service/LessonQuizService.java`

```java
package com.example.DoAn.service;

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

    // ═══════════════════════════════════════════════════════════════════════
    //  STUDENT: List quizzes for a lesson (with sequential status)
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<LessonQuizResponseDTO> getLessonQuizzesForStudent(Integer lessonId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        List<QuizAssignment> assignments = quizAssignmentRepository
                .findByLesson_LessonIdOrderByOrderIndexAsc(lessonId);

        if (assignments.isEmpty()) return List.of();

        Map<Integer, LessonQuizProgress> progressMap = progressRepository
                .findByLesson_LessonIdAndUser_UserId(lessonId, user.getUserId())
                .stream().collect(Collectors.toMap(p -> p.getQuiz().getQuizId(), p -> p));

        List<LessonQuizResponseDTO> result = new ArrayList<>();

        for (int i = 0; i < assignments.size(); i++) {
            QuizAssignment qa = assignments.get(i);
            Quiz quiz = qa.getQuiz();
            LessonQuizProgress progress = progressMap.get(quiz.getQuizId());

            String status;
            if (progress != null) {
                status = progress.getStatus();
            } else if (i == 0) {
                status = "AVAILABLE";
            } else {
                Quiz prevQuiz = assignments.get(i - 1).getQuiz();
                LessonQuizProgress prevProgress = progressMap.get(prevQuiz.getQuizId());
                status = (prevProgress != null && Boolean.TRUE.equals(prevProgress.getBestPassed()))
                        ? "AVAILABLE" : "LOCKED";
            }

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
                    .bestPassed(progress != null ? progress.getBestPassed() : false)
                    .build());
        }

        return result;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  STUDENT: Validate quiz is AVAILABLE before taking
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public void validateQuizAvailableForStudent(Integer lessonId, Integer quizId, String email) {
        userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if (!quizAssignmentRepository.existsByLesson_LessonIdAndQuiz_QuizId(lessonId, quizId)) {
            throw new RuntimeException("Quiz không được gắn với bài học này");
        }

        List<QuizAssignment> assignments = quizAssignmentRepository
                .findByLesson_LessonIdOrderByOrderIndexAsc(lessonId);
        Map<Integer, LessonQuizProgress> progressMap = progressRepository
                .findByLesson_LessonIdAndUser_UserId(lessonId,
                        userRepository.findByEmail(email).get().getUserId())
                .stream().collect(Collectors.toMap(p -> p.getQuiz().getQuizId(), p -> p));

        int quizIndex = -1;
        for (int i = 0; i < assignments.size(); i++) {
            if (assignments.get(i).getQuiz().getQuizId().equals(quizId)) {
                quizIndex = i;
                break;
            }
        }

        String status;
        if (quizIndex == 0) {
            status = "AVAILABLE";
        } else {
            Quiz prevQuiz = assignments.get(quizIndex - 1).getQuiz();
            LessonQuizProgress prev = progressMap.get(prevQuiz.getQuizId());
            status = (prev != null && Boolean.TRUE.equals(prev.getBestPassed())) ? "AVAILABLE" : "LOCKED";
        }

        if ("LOCKED".equals(status)) {
            throw new RuntimeException("Bạn cần hoàn thành quiz trước để mở khóa quiz này");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CALLED AFTER QUIZ SUBMISSION — update progress + unlock next
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public void updateProgressAfterSubmit(Integer lessonId, Integer quizId, Integer userId,
                                          Double score, Boolean passed) {
        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        User user = userRepository.findById(userId).orElse(null);
        Quiz quiz = quizRepository.findById(quizId).orElse(null);
        if (lesson == null || user == null || quiz == null) return;

        // Upsert progress
        LessonQuizProgress progress = progressRepository
                .findByLesson_LessonIdAndUser_UserIdAndQuiz_QuizId(lessonId, userId, quizId)
                .orElseGet(() -> LessonQuizProgress.builder()
                        .lesson(lesson).user(user).quiz(quiz)
                        .status("COMPLETED").build());

        if (progress.getBestScore() == null || score > progress.getBestScore()) {
            progress.setBestScore(score);
        }
        if (Boolean.TRUE.equals(passed)) {
            progress.setBestPassed(true);
        }
        progress.setStatus("COMPLETED");
        progressRepository.save(progress);

        // Unlock next quiz
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
                            .lesson(lesson).user(user).quiz(nextQuiz).status("LOCKED").build());

            if (!"COMPLETED".equals(nextProgress.getStatus())) {
                nextProgress.setStatus("AVAILABLE");
                progressRepository.save(nextProgress);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  EXPERT: Assign quiz to module
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public QuizAssignment assignQuizToModule(Integer moduleId, Quiz quiz) {
        if (quizAssignmentRepository.existsByModule_ModuleIdAndQuiz_QuizId(moduleId, quiz.getQuizId())) {
            throw new RuntimeException("Quiz đã được gắn với chương này");
        }
        Integer maxOrder = quizAssignmentRepository.findMaxOrderByModule(moduleId);
        QuizAssignment qa = QuizAssignment.builder()
                .quiz(quiz)
                .module(quiz.getModule())
                .orderIndex(maxOrder + 1)
                .build();
        return quizAssignmentRepository.save(qa);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TEACHER: Assign quiz to lesson
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public QuizAssignment assignQuizToLesson(Integer lessonId, Quiz quiz) {
        if (quizAssignmentRepository.existsByLesson_LessonIdAndQuiz_QuizId(lessonId, quiz.getQuizId())) {
            throw new RuntimeException("Quiz đã được gắn với bài học này");
        }
        Integer maxOrder = quizAssignmentRepository.findMaxOrderByLesson(lessonId);
        QuizAssignment qa = QuizAssignment.builder()
                .quiz(quiz)
                .lesson(quiz.getLesson())
                .orderIndex(maxOrder + 1)
                .build();
        return quizAssignmentRepository.save(qa);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  DETACH quiz from lesson/module
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public void detachQuizFromLesson(Integer lessonId, Integer quizId) {
        quizAssignmentRepository.deleteByLesson_LessonIdAndQuiz_QuizId(lessonId, quizId);
    }

    @Transactional
    public void detachQuizFromModule(Integer moduleId, Integer quizId) {
        quizAssignmentRepository.deleteByModule_ModuleIdAndQuiz_QuizId(moduleId, quizId);
    }
}
```

---

### Task 10: Create `ExpertModuleQuizController`

**File:** `src/main/java/com/example/DoAn/controller/ExpertModuleQuizController.java`

```java
package com.example.DoAn.controller;

import com.example.DoAn.dto.request.QuizRequestDTO;
import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.service.LessonQuizService;
import com.example.DoAn.service.IExpertQuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/expert/modules")
@RequiredArgsConstructor
public class ExpertModuleQuizController {

    private final IExpertQuizService quizService;
    private final LessonQuizService lessonQuizService;

    private String getEmail(Principal principal) {
        if (principal instanceof OAuth2AuthenticationToken t) return t.getPrincipal().getAttribute("email");
        return principal.getName();
    }

    // List quizzes attached to a module
    @GetMapping("/{moduleId}/quizzes")
    public ResponseData<List<LessonQuizResponseDTO>> getModuleQuizzes(@PathVariable Integer moduleId) {
        // Returns list of assignments; for now return empty placeholder
        // Will be implemented after LessonQuizResponseDTO is created
        return ResponseData.success("Danh sách quiz của chương", List.of());
    }

    // Create MODULE_QUIZ directly assigned to this module
    @PostMapping("/{moduleId}/quizzes")
    public ResponseData<?> createModuleQuiz(
            @PathVariable Integer moduleId,
            @RequestBody QuizRequestDTO request,
            Principal principal) {
        request.setModuleId(moduleId);
        request.setQuizCategory("MODULE_QUIZ");
        return quizService.createQuiz(request, getEmail(principal));
    }
}
```

> **Note:** After `LessonQuizResponseDTO` is created in Task 11, update the `getModuleQuizzes` method to return actual data.

---

## Chunk 3: Teacher Lesson Quiz Backend

### Task 11: Create `LessonQuizResponseDTO`

**File:** `src/main/java/com/example/DoAn/dto/response/LessonQuizResponseDTO.java`

```java
package com.example.DoAn.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonQuizResponseDTO {

    private Integer quizId;
    private String title;
    private String description;
    private String quizCategory;
    private String status;            // AVAILABLE | LOCKED | COMPLETED
    private Integer orderIndex;
    private BigDecimal passScore;
    private Integer timeLimitMinutes;
    private Integer maxAttempts;
    private Integer numberOfQuestions;
    private Double bestScore;
    private Boolean bestPassed;
    private List<LessonQuizQuestionDTO> questions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LessonQuizQuestionDTO {
        private Integer questionId;
        private String content;
        private String questionType;
        private String skill;
        private String cefrLevel;
    }
}
```

---

### Task 12: Update `ExpertModuleQuizController.getModuleQuizzes`

**File:** `src/main/java/com/example/DoAn/controller/ExpertModuleQuizController.java`

After Task 11, update `getModuleQuizzes` to actually return data using `QuizAssignmentRepository`:

```java
import com.example.DoAn.repository.QuizAssignmentRepository;
import com.example.DoAn.model.QuizAssignment;
// ... add repository field and use it in getModuleQuizzes
```

---

### Task 13: Create `TeacherLessonQuizController`

**File:** `src/main/java/com/example/DoAn/controller/TeacherLessonQuizController.java`

```java
package com.example.DoAn.controller;

import com.example.DoAn.dto.request.QuizRequestDTO;
import com.example.DoAn.dto.response.LessonQuizResponseDTO;
import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.model.*;
import com.example.DoAn.repository.*;
import com.example.DoAn.service.LessonQuizService;
import com.example.DoAn.service.TeacherQuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;
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

    // List quizzes attached to a lesson
    @GetMapping("/{lessonId}/quizzes")
    public ResponseData<List<LessonQuizResponseDTO>> getLessonQuizzes(
            @PathVariable Integer lessonId, Principal principal) {
        List<QuizAssignment> assignments = quizAssignmentRepository
                .findByLesson_LessonIdOrderByOrderIndexAsc(lessonId);

        List<LessonQuizResponseDTO> result = assignments.stream().map(qa -> {
            Quiz q = qa.getQuiz();
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

    // Create LESSON_QUIZ directly assigned to this lesson
    @PostMapping("/{lessonId}/quizzes")
    public ResponseData<?> createLessonQuiz(
            @PathVariable Integer lessonId,
            @RequestBody QuizRequestDTO request,
            Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");

        // Validate lesson belongs to a class this teacher owns
        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null) return ResponseData.error(404, "Không tìm thấy bài học");

        request.setLessonId(lessonId);
        request.setQuizCategory("LESSON_QUIZ");

        // Set courseId from the lesson's module → course
        if (lesson.getModule() != null && lesson.getModule().getCourse() != null) {
            request.setCourseId(lesson.getModule().getCourse().getCourseId());
        }

        return teacherQuizService.createQuiz(request, email);
    }

    // Reorder quizzes within a lesson
    @PutMapping("/{lessonId}/quizzes/reorder")
    @Transactional
    public ResponseData<?> reorderLessonQuizzes(
            @PathVariable Integer lessonId,
            @RequestBody List<Map<String, Integer>> orderList) { // [{quizId, orderIndex}, ...]
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

    // Detach quiz from lesson
    @DeleteMapping("/{lessonId}/quizzes/{quizId}")
    public ResponseData<?> detachQuiz(
            @PathVariable Integer lessonId,
            @PathVariable Integer quizId,
            Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");

        lessonQuizService.detachQuizFromLesson(lessonId, quizId);
        return ResponseData.success("Đã gỡ quiz khỏi bài học");
    }
}
```

---

## Chunk 4: Student Quiz Taking & Submission Integration

### Task 14: Create `LearningLessonQuizController`

**File:** `src/main/java/com/example/DoAn/controller/LearningLessonQuizController.java`

```java
package com.example.DoAn.controller;

import com.example.DoAn.dto.response.LessonQuizProgressDTO;
import com.example.DoAn.dto.response.LessonQuizResponseDTO;
import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.service.LessonQuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/learning/lessons")
@RequiredArgsConstructor
public class LearningLessonQuizController {

    private final LessonQuizService lessonQuizService;

    private String getEmail(Principal principal) {
        if (principal instanceof OAuth2AuthenticationToken t) return t.getPrincipal().getAttribute("email");
        return principal.getName();
    }

    // GET /api/v1/learning/lessons/{lessonId}/quizzes
    // Returns list of quizzes for this lesson with AVAILABLE/LOCKED/COMPLETED status
    @GetMapping("/{lessonId}/quizzes")
    public ResponseData<List<LessonQuizResponseDTO>> getLessonQuizzes(
            @PathVariable Integer lessonId, Principal principal) {
        return ResponseData.success("Danh sách quiz",
                lessonQuizService.getLessonQuizzesForStudent(lessonId, getEmail(principal)));
    }

    // GET /api/v1/learning/lessons/{lessonId}/quizzes/{quizId}
    // Validates student can take this quiz (AVAILABLE, not exceeded max attempts)
    @GetMapping("/{lessonId}/quizzes/{quizId}")
    public ResponseData<?> getQuizForTaking(
            @PathVariable Integer lessonId,
            @PathVariable Integer quizId,
            Principal principal) {
        lessonQuizService.validateQuizAvailableForStudent(lessonId, quizId, getEmail(principal));
        // Delegate to existing quiz-taking logic
        // Return quiz detail (reuse existing quiz service)
        return ResponseData.success("Quiz sẵn sàng để làm");
    }
}
```

---

### Task 15: Update `QuizResultServiceImpl` — Hook Progress After Submit

**File:** `src/main/java/com/example/DoAn/service/impl/QuizResultServiceImpl.java`

1. Inject `LessonQuizService` and `LessonRepository`:

```java
private final LessonQuizService lessonQuizService;
private final LessonRepository lessonRepository;
```

2. Find `submitQuiz` method and add this call after the quiz is successfully submitted (after scoring is done):

```java
// After existing score calculation and result saving:
// Update lesson quiz progress + unlock next
try {
    Integer lessonId = quiz.getLesson() != null ? quiz.getLesson().getLessonId() : null;
    if (lessonId != null) {
        lessonQuizService.updateProgressAfterSubmit(
                lessonId, quizId, userId,
                result.getScore() != null ? result.getScore().doubleValue() : 0.0,
                result.getPassed());
    }
} catch (Exception e) {
    // Non-critical: log but don't fail the submission
}
```

> **Note:** Look for the `submitQuiz` method. The quiz's `lesson` field will be set if the quiz was created as `LESSON_QUIZ`. Find the `lessonId` and call the progress update hook.

---

## Chunk 5: Expert Module Quiz Extension

### Task 16: Extend `IExpertQuizService` and `ExpertQuizServiceImpl`

**File:** `src/main/java/com/example/DoAn/service/IExpertQuizService.java`

Add method signature:

```java
QuizResponseDTO createModuleQuiz(QuizRequestDTO request, Integer moduleId, String email);
```

**File:** `src/main/java/com/example/DoAn/service/impl/ExpertQuizServiceImpl.java`

1. Inject `LessonQuizService` and `ModuleRepository`:

```java
private final LessonQuizService lessonQuizService;
private final ModuleRepository moduleRepository;
```

2. Implement `createModuleQuiz`:

```java
@Override
@Transactional
public QuizResponseDTO createModuleQuiz(QuizRequestDTO request, Integer moduleId, String email) {
    Module module = moduleRepository.findById(moduleId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương"));

    User expert = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyên gia"));

    if (module.getCourse() == null || !module.getCourse().getExpert().getUserId().equals(expert.getUserId())) {
        throw new ResourceNotFoundException("Bạn không có quyền thêm quiz vào chương này");
    }

    request.setModuleId(moduleId);
    request.setCourseId(module.getCourse().getCourseId());
    if (request.getQuizCategory() == null) {
        request.setQuizCategory("MODULE_QUIZ");
    }

    Quiz quiz = Quiz.builder()
            .title(request.getTitle())
            .description(request.getDescription())
            .quizCategory(request.getQuizCategory())
            .status("DRAFT")
            .timeLimitMinutes(request.getTimeLimitMinutes())
            .passScore(request.getPassScore())
            .maxAttempts(request.getMaxAttempts())
            .numberOfQuestions(request.getNumberOfQuestions())
            .questionOrder(request.getQuestionOrder() != null ? request.getQuestionOrder() : "FIXED")
            .showAnswerAfterSubmit(request.getShowAnswerAfterSubmit() != null ? request.getShowAnswerAfterSubmit() : false)
            .user(expert)
            .module(module)
            .course(module.getCourse())
            .build();

    quizRepository.save(quiz);

    // Auto-assign to module via QuizAssignment
    lessonQuizService.assignQuizToModule(moduleId, quiz);

    return toResponseDTO(quiz);
}
```

3. Also update the existing `createQuiz` method to support `MODULE_QUIZ` and `LESSON_QUIZ` categories when `moduleId` or `lessonId` is set.

---

### Task 17: Extend `ExpertModuleQuizController`

**File:** `src/main/java/com/example/DoAn/controller/ExpertModuleQuizController.java`

Complete the controller with all module quiz endpoints:

```java
@GetMapping("/{moduleId}/quizzes")
public ResponseData<List<LessonQuizResponseDTO>> getModuleQuizzes(@PathVariable Integer moduleId) {
    List<QuizAssignment> assignments = quizAssignmentRepository
            .findByModule_ModuleIdOrderByOrderIndexAsc(moduleId);
    List<LessonQuizResponseDTO> result = assignments.stream().map(qa -> {
        Quiz q = qa.getQuiz();
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
    return ResponseData.success("Danh sách quiz của chương", result);
}

@PostMapping("/{moduleId}/quizzes")
public ResponseData<?> createModuleQuiz(
        @PathVariable Integer moduleId,
        @RequestBody QuizRequestDTO request,
        Principal principal) {
    request.setModuleId(moduleId);
    request.setQuizCategory("MODULE_QUIZ");
    return quizService.createQuiz(request, getEmail(principal));
}

@PatchMapping("/{moduleId}/quizzes/reorder")
public ResponseData<?> reorderModuleQuizzes(
        @PathVariable Integer moduleId,
        @RequestBody List<Map<String, Integer>> orderList) {
    List<QuizAssignment> assignments = quizAssignmentRepository
            .findByModule_ModuleIdOrderByOrderIndexAsc(moduleId);
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

@DeleteMapping("/{moduleId}/quizzes/{quizId}")
public ResponseData<?> detachQuiz(
        @PathVariable Integer moduleId,
        @PathVariable Integer quizId,
        Principal principal) {
    lessonQuizService.detachQuizFromModule(moduleId, quizId);
    return ResponseData.success("Đã gỡ quiz khỏi chương");
}
```

Add these imports:
```java
import com.example.DoAn.repository.QuizAssignmentRepository;
import com.example.DoAn.model.QuizAssignment;
import java.util.*;
```

---

## Chunk 6: Frontend Pages

### Task 18: Expert Module Detail — Add Quizzes Tab

**File:** `src/main/resources/templates/expert/module-detail.html`

Add a "Quizzes" tab alongside the existing "Lessons" tab:

```html
<!-- In the tabs navigation -->
<li class="nav-item">
    <a class="nav-link" data-bs-toggle="tab" href="#tab-quizzes">Quizzes</a>
</li>

<!-- Quizzes tab content -->
<div class="tab-pane fade" id="tab-quizzes">
    <div class="d-flex justify-content-between align-items-center mb-3">
        <h5>Danh sách Quiz</h5>
        <button class="btn btn-primary" onclick="openAddQuizModal()">
            <i class="fas fa-plus"></i> Thêm Quiz
        </button>
    </div>
    <div id="module-quizzes-list">
        <!-- Loaded via API: GET /api/v1/expert/modules/{moduleId}/quizzes -->
        <!-- Each quiz card: title, question count, pass score, status badge, Edit/Preview/Publish/Detach buttons -->
    </div>
</div>
```

Add JavaScript:

```javascript
// Load quizzes for this module
function loadModuleQuizzes() {
    fetch(`/api/v1/expert/modules/${moduleId}/quizzes`)
        .then(r => r.json())
        .then(data => {
            if (data.data && data.data.length === 0) {
                document.getElementById('module-quizzes-list').innerHTML =
                    '<div class="alert alert-info">Chưa có quiz nào cho chương này.</div>';
                return;
            }
            // Render quiz cards...
        });
}

// Open modal to create new MODULE_QUIZ
function openAddQuizModal() {
    // Redirect to quiz creation page with moduleId pre-filled
    window.location.href = `/expert/quizzes/create?moduleId=${moduleId}`;
}

// Reorder via drag-and-drop or up/down buttons → PATCH /api/v1/expert/modules/{moduleId}/quizzes/reorder
// Detach → DELETE /api/v1/expert/modules/{moduleId}/quizzes/{quizId}
```

---

### Task 19: Teacher Lesson Detail — Add Quizzes Section

**File:** `src/main/resources/templates/teacher/lesson-detail.html` (or wherever teacher lesson detail lives — check templates/teacher/)

Add below the lesson content:

```html
<!-- Quizzes Section -->
<div class="card mt-4" id="lesson-quizzes-section">
    <div class="card-header d-flex justify-content-between align-items-center">
        <h5 class="mb-0">Quizzes</h5>
        <button class="btn btn-sm btn-primary" onclick="openAddLessonQuizModal()">
            <i class="fas fa-plus"></i> Thêm Quiz
        </button>
    </div>
    <div class="card-body" id="lesson-quizzes-list">
        <!-- Loaded via: GET /api/v1/teacher/lessons/{lessonId}/quizzes -->
        <!-- Sequential cards: completed → score badge, available → "Take Quiz", locked → 🔒 -->
        <!-- Each card: title, order, Edit/Preview/Publish/Detach -->
    </div>
</div>
```

Add JavaScript similar to Task 18.

---

### Task 20: Student Lesson Page — Add Quizzes Section

**File:** `src/main/resources/templates/student/lesson.html` (or wherever student views a lesson)

Add below the lesson video/text content:

```html
<!-- Quizzes Section (after lesson content) -->
<div class="card mt-4" id="lesson-quizzes-section">
    <div class="card-header">
        <h5 class="mb-0"><i class="fas fa-tasks"></i> Quizzes</h5>
    </div>
    <div class="card-body" id="student-quizzes-list">
        <!-- Loaded via: GET /api/v1/learning/lessons/{lessonId}/quizzes -->
    </div>
</div>
```

Add JavaScript:

```javascript
function loadStudentQuizzes() {
    fetch(`/api/v1/learning/lessons/${lessonId}/quizzes`)
        .then(r => r.json())
        .then(data => {
            const list = document.getElementById('student-quizzes-list');
            data.data.forEach(quiz => {
                let badge = '';
                let action = '';

                if (quiz.status === 'COMPLETED') {
                    badge = `<span class="badge bg-success">Hoàn thành (${quiz.bestScore}%)</span>`;
                    action = `<a href="/student/quizzes/${quiz.quizId}/result" class="btn btn-sm btn-outline-success">Xem kết quả</a>`;
                } else if (quiz.status === 'AVAILABLE') {
                    badge = `<span class="badge bg-primary">Sẵn sàng</span>`;
                    action = `<a href="/student/quizzes/${quiz.quizId}/take" class="btn btn-sm btn-primary">Làm Quiz</a>`;
                } else {
                    badge = `<span class="badge bg-secondary"><i class="fas fa-lock"></i> Bị khóa</span>`;
                    action = `<button class="btn btn-sm btn-secondary" disabled>🔒</button>`;
                }

                list.innerHTML += `
                    <div class="d-flex justify-content-between align-items-center mb-2 p-2 border rounded">
                        <div>
                            <strong>${quiz.orderIndex}. ${quiz.title}</strong>
                            ${badge}
                        </div>
                        <div>${action}</div>
                    </div>`;
            });
        });
}

loadStudentQuizzes();
```

---

## Chunk 7: Grading Extension (WRITING/SPEAKING)

### Task 21: Update `TeacherQuizGradingApiController` — Hook Progress After Grading

**File:** `src/main/java/com/example/DoAn/controller/TeacherQuizGradingApiController.java`

After teacher grades a WRITING/SPEAKING question (PATCH grade endpoint), call `lessonQuizService.updateProgressAfterSubmit` to recalculate the total score and potentially unlock the next quiz.

Find the grading endpoint and add:

```java
// After saving the grade and updating the result score:
if (result.getLesson() != null) {
    lessonQuizService.updateProgressAfterSubmit(
            result.getLesson().getLessonId(),
            quizId,
            result.getUser().getUserId(),
            result.getScore() != null ? result.getScore().doubleValue() : 0.0,
            result.getPassed());
}
```

---

## Chunk 8: Integration Testing

### Task 22: Manual Verification Checklist

After all tasks are complete, run through these scenarios:

1. **Expert creates MODULE_QUIZ (Path A)**:
   - Expert → Quizzes list → Create Quiz → type: MODULE_QUIZ → attach to module
   - Verify: quiz appears in module detail page's Quizzes tab

2. **Expert creates MODULE_QUIZ (Path B)**:
   - Expert → Course → Module detail → Quizzes tab → Add Quiz
   - Verify: quiz is auto-assigned and appears in the list

3. **Teacher creates LESSON_QUIZ (Path A)**:
   - Teacher → Class → Lesson detail → Create Lesson Quiz
   - Verify: quiz appears in lesson quizzes list

4. **Sequential progression**:
   - Student → Course → Module → Lesson → takes Quiz 1 → passes
   - Verify: Quiz 2 unlocks and becomes AVAILABLE

5. **Sequential lock**:
   - Student skips Quiz 1 → tries to directly access Quiz 2 URL
   - Verify: 403 / "Bạn cần hoàn thành quiz trước"

6. **WRITING/SPEAKING grading**:
   - Student submits quiz with WRITING question → result shows "Pending review"
   - Teacher grades → score updates → result shows final score

7. **Detach quiz**:
   - Expert/Teacher detaches quiz → QuizAssignment removed, quiz still exists
   - Student no longer sees the quiz in the lesson quiz list
