# Plan 001 — Expert Creates Assignment (Course / Module)

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expert creates a structured 4-skill sequential Assignment (COURSE_ASSIGNMENT or MODULE_ASSIGNMENT) via a 6-step wizard.

**Architecture:** Extend the existing `Quiz` entity with sequential/timer fields; reuse existing question bank and question creation flows; new `ExpertAssignmentService` and controller handle assignment-specific logic. `skill` is stored on `QuizQuestion` so the same question can be reused across skill sections.

**Tech Stack:** Spring Boot 3.3.4, Spring Data JPA, Thymeleaf, Cloudinary.

---

## File Structure

```
src/main/java/com/example/DoAn/
├── model/
│   ├── Quiz.java                         MODIFY (add 3 fields)
│   ├── QuizQuestion.java                 MODIFY (add skill field)
│   └── Question.java                    VERIFY reviewer fields exist, add if missing
├── repository/
│   └── QuizQuestionRepository.java       MODIFY (add findByQuizQuizIdAndSkill)
├── dto/request/
│   ├── AssignmentQuestionRequestDTO.java  CREATE
│   └── AssignmentPublishDTO.java         CREATE
├── dto/response/
│   ├── SkillSectionSummaryDTO.java        CREATE
│   └── AssignmentPreviewDTO.java          CREATE
├── service/
│   ├── IExpertAssignmentService.java      CREATE (interface)
│   └── impl/
│       └── ExpertAssignmentServiceImpl.java CREATE
├── controller/
│   └── ExpertAssignmentController.java   CREATE (REST API)
└── views/ (templates/)
    ├── expert/
    │   ├── assignment-list.html           MODIFY (add tabs to quiz-list.html)
    │   ├── assignment-create.html         CREATE (Step 1)
    │   ├── assignment-skill-section.html  CREATE (Steps 2-5)
    │   └── assignment-preview.html        CREATE (Step 6)
    └── fragments/
        └── expert-sidebar.html           MODIFY (add link)
```

---

## Chunk 1: Data Model Changes

### Task 1: Verify Question entity reviewer fields

**Files:**
- Modify: `src/main/java/com/example/DoAn/model/Question.java`

- [ ] **Step 1: Read Question.java to check existing fields**

Run: Read the file completely. Search for `reviewerId`, `reviewedAt`, `reviewNote`.

- [ ] **Step 2: Add missing fields**

If `reviewerId`, `reviewedAt`, `reviewNote` are missing, add them after existing fields:

```java
@Column(name = "reviewer_id")
private Long reviewerId;

@Column(name = "reviewed_at")
private LocalDateTime reviewedAt;

@Column(name = "review_note", length = 500)
private String reviewNote;
```

Add import: `import java.time.LocalDateTime;`

### Task 2: Add sequential fields to Quiz entity

**Files:**
- Modify: `src/main/java/com/example/DoAn/model/Quiz.java`

- [ ] **Step 1: Read Quiz.java and find where to add fields**

Run: Read the file. Find where `isHybridEnabled` or `targetSkill` fields are defined.

- [ ] **Step 2: Add three new fields after existing fields**

Add after `isHybridEnabled` and `targetSkill` fields:

```java
@Column(name = "is_sequential")
private Boolean isSequential = false;

@Column(name = "skill_order", columnDefinition = "JSON")
private String skillOrder; // JSON array e.g. ["LISTENING","READING","SPEAKING","WRITING"]

@Column(name = "time_limit_per_skill", columnDefinition = "JSON")
private String timeLimitPerSkill; // JSON object e.g. {"SPEAKING": 2, "WRITING": 30}
```

### Task 3: Add skill field to QuizQuestion entity

**Files:**
- Modify: `src/main/java/com/example/DoAn/model/QuizQuestion.java`

- [ ] **Step 1: Read QuizQuestion.java**

Run: Read the file. Find existing fields.

- [ ] **Step 2: Add skill field**

Add field after `orderIndex`:

```java
@Column(name = "skill", length = 20)
private String skill; // LISTENING, READING, SPEAKING, WRITING
```

- [ ] **Step 3: Verify database column**

Run the following SQL in MySQL to add the column (if Hibernate ddl-auto=update doesn't add it automatically):

```sql
ALTER TABLE quiz_question ADD COLUMN skill VARCHAR(20);
```

### Task 4: Add `findByQuizQuizIdAndSkill` to QuizQuestionRepository

**Files:**
- Modify: `src/main/java/com/example/DoAn/repository/QuizQuestionRepository.java`

- [ ] **Step 1: Read the repository file**

Run: Read `QuizQuestionRepository.java`.

- [ ] **Step 2: Add new query method**

Add after existing methods:

```java
List<QuizQuestion> findByQuizQuizIdAndSkill(Integer quizId, String skill);

@Query("SELECT COUNT(DISTINCT q.question.questionId) FROM QuizQuestion q WHERE q.quiz.quizId = :quizId AND q.skill = :skill")
long countByQuizIdAndSkill(@Param("quizId") Integer quizId, @Param("skill") String skill);
```

Add import: `import java.util.List;`

---

## Chunk 2: DTOs

### Task 5: Create `AssignmentQuestionRequestDTO`

**Files:**
- Create: `src/main/java/com/example/DoAn/dto/request/AssignmentQuestionRequestDTO.java`

- [ ] **Step 1: Write the DTO**

```java
package com.example.DoAn.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentQuestionRequestDTO {

    @NotEmpty(message = "Question IDs cannot be empty")
    private List<Integer> questionIds;

    @NotNull(message = "Skill is required")
    private String skill; // LISTENING, READING, SPEAKING, WRITING

    private String itemType = "SINGLE"; // SINGLE or GROUP
}
```

### Task 6: Create `AssignmentPublishDTO`

**Files:**
- Create: `src/main/java/com/example/DoAn/dto/request/AssignmentPublishDTO.java`

- [ ] **Step 1: Write the DTO**

```java
package com.example.DoAn.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentPublishDTO {
    private String status; // PUBLISHED or ARCHIVED
}
```

### Task 7: Create `SkillSectionSummaryDTO`

**Files:**
- Create: `src/main/java/com/example/DoAn/dto/response/SkillSectionSummaryDTO.java`

- [ ] **Step 1: Write the DTO**

```java
package com.example.DoAn.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkillSectionSummaryDTO {
    private String skill;
    private long questionCount;
    private long totalPoints;
    private String status; // DRAFT, READY (>=1 question)
}
```

### Task 8: Create `AssignmentPreviewDTO`

**Files:**
- Create: `src/main/java/com/example/DoAn/dto/response/AssignmentPreviewDTO.java`

- [ ] **Step 1: Write the DTO**

```java
package com.example.DoAn.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentPreviewDTO {
    private Integer quizId;
    private String title;
    private String description;
    private String quizCategory;
    private List<SkillSectionSummaryDTO> sections;
    private long totalQuestions;
    private BigDecimal totalPoints;
    private Map<String, Integer> timeLimitsPerSkill; // {"SPEAKING": 2, "WRITING": 30}
    private BigDecimal passScore;
    private Integer maxAttempts;
    private Boolean showAnswerAfterSubmit;
    private List<String> missingSkills; // empty = ready to publish
    private Boolean canPublish;
}
```

---

## Chunk 3: Service Layer

### Task 9: Create `IExpertAssignmentService` interface

**Files:**
- Create: `src/main/java/com/example/DoAn/service/IExpertAssignmentService.java`

- [ ] **Step 1: Write the interface**

```java
package com.example.DoAn.service;

import com.example.DoAn.dto.request.AssignmentQuestionRequestDTO;
import com.example.DoAn.dto.request.QuizRequestDTO;
import com.example.DoAn.dto.response.AssignmentPreviewDTO;
import com.example.DoAn.dto.response.SkillSectionSummaryDTO;
import com.example.DoAn.model.Quiz;

import java.util.List;
import java.util.Map;

public interface IExpertAssignmentService {

    // Create new assignment (Step 1)
    Quiz createAssignment(QuizRequestDTO dto, String expertEmail);

    // Get skill summary with question counts (for wizard steps)
    Map<String, SkillSectionSummaryDTO> getSkillSummaries(Integer quizId);

    // Add questions to a specific skill section (Steps 2-5)
    void addQuestionsToSection(Integer quizId, AssignmentQuestionRequestDTO dto, String expertEmail);

    // Remove a question from assignment
    void removeQuestion(Integer quizId, Integer questionId);

    // Get full preview (Step 6)
    AssignmentPreviewDTO getPreview(Integer quizId);

    // Publish assignment
    void publishAssignment(Integer quizId);

    // Change status (ARCHIVED, etc.)
    void changeStatus(Integer quizId, String status);

    // List assignments (COURSE_ASSIGNMENT + MODULE_ASSIGNMENT)
    List<Quiz> getAssignments(String expertEmail);

    // Get assignment detail
    Quiz getAssignment(Integer quizId, String expertEmail);
}
```

### Task 10: Create `ExpertAssignmentServiceImpl`

**Files:**
- Create: `src/main/java/com/example/DoAn/service/impl/ExpertAssignmentServiceImpl.java`

- [ ] **Step 1: Write the service implementation**

```java
package com.example.DoAn.service.impl;

import com.example.DoAn.dto.request.AssignmentQuestionRequestDTO;
import com.example.DoAn.dto.request.QuizRequestDTO;
import com.example.DoAn.dto.response.AssignmentPreviewDTO;
import com.example.DoAn.dto.response.SkillSectionSummaryDTO;
import com.example.DoAn.exception.InvalidDataException;
import com.example.DoAn.exception.ResourceNotFoundException;
import com.example.DoAn.model.Question;
import com.example.DoAn.model.Quiz;
import com.example.DoAn.model.QuizQuestion;
import com.example.DoAn.model.User;
import com.example.DoAn.repository.QuestionRepository;
import com.example.DoAn.repository.QuizQuestionRepository;
import com.example.DoAn.repository.QuizRepository;
import com.example.DoAn.repository.UserRepository;
import com.example.DoAn.service.IExpertAssignmentService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpertAssignmentServiceImpl implements IExpertAssignmentService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private static final List<String> SEQUENTIAL_SKILLS = Arrays.asList(
        "LISTENING", "READING", "SPEAKING", "WRITING"
    );

    @Override
    public Quiz createAssignment(QuizRequestDTO dto, String expertEmail) {
        User expert = userRepository.findByEmail(expertEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Expert not found"));

        if (!"EXPERT".equals(expert.getRole().getSettingValue())) {
            throw new InvalidDataException("Only experts can create assignments");
        }

        String category = dto.getQuizCategory();
        if (!"COURSE_ASSIGNMENT".equals(category) && !"MODULE_ASSIGNMENT".equals(category)) {
            throw new InvalidDataException("Invalid category for assignment");
        }

        Quiz quiz = new Quiz();
        quiz.setTitle(dto.getTitle());
        quiz.setDescription(dto.getDescription());
        quiz.setQuizCategory(category);
        quiz.setStatus("DRAFT");
        quiz.setUser(expert);
        quiz.setIsSequential(true);
        quiz.setSkillOrder(objectMapper.writeValueAsString(SEQUENTIAL_SKILLS));

        // Parse time limits per skill from JSON/map if provided
        if (dto.getTimeLimitPerSkill() != null) {
            quiz.setTimeLimitPerSkill(objectMapper.writeValueAsString(dto.getTimeLimitPerSkill()));
        }

        if (dto.getPassScore() != null) quiz.setPassScore(dto.getPassScore());
        if (dto.getMaxAttempts() != null) quiz.setMaxAttempts(dto.getMaxAttempts());
        if (dto.getShowAnswerAfterSubmit() != null) quiz.setShowAnswerAfterSubmit(dto.getShowAnswerAfterSubmit());

        return quizRepository.save(quiz);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, SkillSectionSummaryDTO> getSkillSummaries(Integer quizId) {
        Quiz quiz = quizRepository.findById(quizId)
            .orElseThrow(() -> new ResourceNotFoundException("Quiz not found"));

        Map<String, SkillSectionSummaryDTO> result = new LinkedHashMap<>();
        for (String skill : SEQUENTIAL_SKILLS) {
            long count = quizQuestionRepository.countByQuizIdAndSkill(quizId, skill);
            BigDecimal points = BigDecimal.ZERO; // sum from quiz_questions
            SkillSectionSummaryDTO dto = new SkillSectionSummaryDTO(
                skill, count, points,
                count > 0 ? "READY" : "DRAFT"
            );
            result.put(skill, dto);
        }
        return result;
    }

    @Override
    public void addQuestionsToSection(Integer quizId, AssignmentQuestionRequestDTO dto, String expertEmail) {
        Quiz quiz = quizRepository.findById(quizId)
            .orElseThrow(() -> new ResourceNotFoundException("Quiz not found"));

        if (!quiz.getIsSequential()) {
            throw new InvalidDataException("This quiz does not support section-based question addition");
        }

        String skill = dto.getSkill();
        if (!SEQUENTIAL_SKILLS.contains(skill)) {
            throw new InvalidDataException("Invalid skill: " + skill);
        }

        List<QuizQuestion> existing = quizQuestionRepository.findByQuizQuizIdAndSkill(quizId, skill);
        Set<Integer> existingIds = new HashSet<>();
        for (QuizQuestion qq : existing) {
            existingIds.add(qq.getQuestion().getQuestionId());
        }

        for (Integer questionId : dto.getQuestionIds()) {
            if (existingIds.contains(questionId)) continue; // skip duplicates

            Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));

            QuizQuestion qq = new QuizQuestion();
            qq.setQuiz(quiz);
            qq.setQuestion(question);
            qq.setSkill(skill);
            qq.setOrderIndex(existing.size() + 1);
            qq.setPoints(BigDecimal.ONE);
            quizQuestionRepository.save(qq);
        }
    }

    @Override
    public void removeQuestion(Integer quizId, Integer questionId) {
        List<QuizQuestion> toRemove = quizQuestionRepository
            .findByQuizQuizIdAndSkill(quizId, null); // fetch all for this quiz
        // Find by both quiz and question
        quizQuestionRepository.findAll().stream()
            .filter(qq -> qq.getQuiz().getQuizId().equals(quizId)
                      && qq.getQuestion().getQuestionId().equals(questionId))
            .findFirst()
            .ifPresent(quizQuestionRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentPreviewDTO getPreview(Integer quizId) {
        Quiz quiz = quizRepository.findById(quizId)
            .orElseThrow(() -> new ResourceNotFoundException("Quiz not found"));

        Map<String, SkillSectionSummaryDTO> summaries = getSkillSummaries(quizId);
        List<String> missingSkills = new ArrayList<>();
        for (SkillSectionSummaryDTO s : summaries.values()) {
            if (s.getQuestionCount() == 0) {
                missingSkills.add(s.getSkill());
            }
        }

        long totalQuestions = summaries.values().stream()
            .mapToLong(SkillSectionSummaryDTO::getQuestionCount).sum();

        Map<String, Integer> timeLimits = null;
        if (quiz.getTimeLimitPerSkill() != null) {
            timeLimits = objectMapper.readValue(
                quiz.getTimeLimitPerSkill(),
                new TypeReference<Map<String, Integer>>() {}
            );
        }

        return new AssignmentPreviewDTO(
            quiz.getQuizId(),
            quiz.getTitle(),
            quiz.getDescription(),
            quiz.getQuizCategory(),
            new ArrayList<>(summaries.values()),
            totalQuestions,
            quiz.getPassScore() != null ? quiz.getPassScore() : BigDecimal.ZERO,
            timeLimits,
            quiz.getPassScore(),
            quiz.getMaxAttempts(),
            quiz.getShowAnswerAfterSubmit(),
            missingSkills,
            missingSkills.isEmpty()
        );
    }

    @Override
    public void publishAssignment(Integer quizId) {
        Quiz quiz = quizRepository.findById(quizId)
            .orElseThrow(() -> new ResourceNotFoundException("Quiz not found"));

        if (!"DRAFT".equals(quiz.getStatus())) {
            throw new InvalidDataException("Only DRAFT quizzes can be published");
        }

        Map<String, SkillSectionSummaryDTO> summaries = getSkillSummaries(quizId);
        List<String> missing = new ArrayList<>();
        for (SkillSectionSummaryDTO s : summaries.values()) {
            if (s.getQuestionCount() == 0) {
                missing.add(s.getSkill());
            }
        }

        if (!missing.isEmpty()) {
            throw new InvalidDataException("Missing questions for skills: " + String.join(", ", missing));
        }

        quiz.setStatus("PUBLISHED");
        quiz.setIsOpen(false);
        quizRepository.save(quiz);
    }

    @Override
    public void changeStatus(Integer quizId, String status) {
        Quiz quiz = quizRepository.findById(quizId)
            .orElseThrow(() -> new ResourceNotFoundException("Quiz not found"));
        quiz.setStatus(status);
        quizRepository.save(quiz);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Quiz> getAssignments(String expertEmail) {
        return quizRepository.findAll().stream()
            .filter(q -> ("COURSE_ASSIGNMENT".equals(q.getQuizCategory())
                       || "MODULE_ASSIGNMENT".equals(q.getQuizCategory()))
                  && q.getUser().getEmail().equals(expertEmail))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Quiz getAssignment(Integer quizId, String expertEmail) {
        Quiz quiz = quizRepository.findById(quizId)
            .orElseThrow(() -> new ResourceNotFoundException("Quiz not found"));
        if (!quiz.getUser().getEmail().equals(expertEmail)) {
            throw new InvalidDataException("Access denied");
        }
        return quiz;
    }
}
```

**Note:** The `removeQuestion` method above uses a simple approach. After writing the code, test that `quizQuestionRepository.findByQuizQuizIdAndSkill` is called with the correct skill to narrow the query.

---

## Chunk 4: REST API Controller

### Task 11: Create `ExpertAssignmentController`

**Files:**
- Create: `src/main/java/com/example/DoAn/controller/ExpertAssignmentController.java`

- [ ] **Step 1: Write the controller**

```java
package com.example.DoAn.controller;

import com.example.DoAn.dto.request.AssignmentQuestionRequestDTO;
import com.example.DoAn.dto.request.AssignmentPublishDTO;
import com.example.DoAn.dto.request.QuizRequestDTO;
import com.example.DoAn.dto.response.AssignmentPreviewDTO;
import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.dto.response.SkillSectionSummaryDTO;
import com.example.DoAn.exception.InvalidDataException;
import com.example.DoAn.model.Quiz;
import com.example.DoAn.service.IExpertAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/expert/assignments")
@RequiredArgsConstructor
public class ExpertAssignmentController {

    private final IExpertAssignmentService assignmentService;

    @PostMapping
    public ResponseEntity<ResponseData<Integer>> create(
            @Valid @RequestBody QuizRequestDTO dto,
            Authentication auth) {
        Quiz quiz = assignmentService.createAssignment(dto, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ResponseData.success(quiz.getQuizId()));
    }

    @GetMapping("/{quizId}")
    public ResponseEntity<ResponseData<Quiz>> get(@PathVariable Integer quizId, Authentication auth) {
        Quiz quiz = assignmentService.getAssignment(quizId, auth.getName());
        return ResponseData.success(quiz);
    }

    @GetMapping("/{quizId}/skills")
    public ResponseEntity<ResponseData<Map<String, SkillSectionSummaryDTO>>> getSkills(
            @PathVariable Integer quizId) {
        Map<String, SkillSectionSummaryDTO> summaries = assignmentService.getSkillSummaries(quizId);
        return ResponseData.success(summaries);
    }

    @PostMapping("/{quizId}/questions")
    public ResponseEntity<ResponseData<Integer>> addQuestions(
            @PathVariable Integer quizId,
            @Valid @RequestBody AssignmentQuestionRequestDTO dto,
            Authentication auth) {
        assignmentService.addQuestionsToSection(quizId, dto, auth.getName());
        return ResponseData.success(dto.getQuestionIds().size());
    }

    @DeleteMapping("/{quizId}/questions/{questionId}")
    public ResponseEntity<ResponseData<Boolean>> removeQuestion(
            @PathVariable Integer quizId,
            @PathVariable Integer questionId,
            Authentication auth) {
        assignmentService.removeQuestion(quizId, questionId);
        return ResponseData.success(true);
    }

    @GetMapping("/{quizId}/preview")
    public ResponseEntity<ResponseData<AssignmentPreviewDTO>> preview(@PathVariable Integer quizId) {
        AssignmentPreviewDTO preview = assignmentService.getPreview(quizId);
        return ResponseData.success(preview);
    }

    @PatchMapping("/{quizId}/publish")
    public ResponseEntity<ResponseData<Boolean>> publish(@PathVariable Integer quizId, Authentication auth) {
        try {
            assignmentService.publishAssignment(quizId);
            return ResponseData.success(true);
        } catch (InvalidDataException e) {
            return ResponseEntity.badRequest()
                .body(ResponseData.error(e.getMessage()));
        }
    }

    @PatchMapping("/{quizId}/status")
    public ResponseEntity<ResponseData<Boolean>> changeStatus(
            @PathVariable Integer quizId,
            @RequestBody AssignmentPublishDTO dto) {
        assignmentService.changeStatus(quizId, dto.getStatus());
        return ResponseData.success(true);
    }

    @GetMapping
    public ResponseEntity<ResponseData<List<Quiz>>> list(Authentication auth) {
        List<Quiz> assignments = assignmentService.getAssignments(auth.getName());
        return ResponseData.success(assignments);
    }
}
```

---

## Chunk 5: View Controller & Templates

### Task 12: Add view routes to `ExpertViewController`

**Files:**
- Modify: `src/main/java/com/example/DoAn/controller/ExpertViewController.java`

- [ ] **Step 1: Read the controller file**

Run: Read `ExpertViewController.java`. Find where other expert routes are defined.

- [ ] **Step 2: Add new routes**

Add after existing expert routes:

```java
@GetMapping("/expert/assignment/create")
public String assignmentCreate(
        @RequestParam(required = false, defaultValue = "COURSE_ASSIGNMENT") String category,
        @RequestParam(required = false) Integer courseId,
        @RequestParam(required = false) Integer moduleId,
        Model model) {
    model.addAttribute("category", category);
    model.addAttribute("courseId", courseId);
    model.addAttribute("moduleId", moduleId);
    return "expert/assignment-create";
}

@GetMapping("/expert/assignment/{quizId}/skill/{skill}")
public String assignmentSkillSection(
        @PathVariable Integer quizId,
        @PathVariable String skill,
        Model model) {
    model.addAttribute("quizId", quizId);
    model.addAttribute("skill", skill);
    return "expert/assignment-skill-section";
}

@GetMapping("/expert/assignment/{quizId}/preview")
public String assignmentPreview(@PathVariable Integer quizId, Model model) {
    model.addAttribute("quizId", quizId);
    return "expert/assignment-preview";
}

@GetMapping("/expert/assignment-management")
public String assignmentManagement() {
    return "expert/assignment-list";
}
```

### Task 13: Create `expert/assignment-create.html` (Step 1)

**Files:**
- Create: `src/main/resources/templates/expert/assignment-create.html`

- [ ] **Step 1: Create the template**

This is the configuration step. Key sections:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" th:replace="~{fragments/layout :: layout(~{::title}, ~{::#content})}">
<head>
    <title>Tạo Bài Kiểm Tra Lớn</title>
</head>
<body>
<div id="content" class="container mt-4">
    <h3>Tạo Bài Kiểm Tra Lớn</h3>
    <form id="assignmentForm" th:action="@{/api/v1/expert/assignments}" method="post">
        <!-- Title -->
        <div class="mb-3">
            <label class="form-label">Tên bài kiểm tra *</label>
            <input type="text" name="title" class="form-control" required>
        </div>

        <!-- Description -->
        <div class="mb-3">
            <label class="form-label">Mô tả</label>
            <textarea name="description" class="form-control" rows="3"></textarea>
        </div>

        <!-- Hidden category -->
        <input type="hidden" name="quizCategory" th:value="${category}">

        <!-- Course selector (for COURSE_ASSIGNMENT) -->
        <div class="mb-3" th:if="${category == 'COURSE_ASSIGNMENT'}">
            <label class="form-label">Khóa học *</label>
            <select name="courseId" id="courseSelect" class="form-select" required>
                <option value="">-- Chọn khóa học --</option>
            </select>
        </div>

        <!-- Module selector (for MODULE_ASSIGNMENT) -->
        <div class="mb-3" th:if="${category == 'MODULE_ASSIGNMENT'}">
            <label class="form-label">Module *</label>
            <select name="moduleId" id="moduleSelect" class="form-select" required>
                <option value="">-- Chọn module --</option>
            </select>
        </div>

        <!-- Per-skill timers -->
        <hr>
        <h5>Thời gian cho từng phần</h5>
        <div class="row">
            <div class="col-md-3 mb-2">
                <label>LISTENING (phút)</label>
                <input type="number" name="timeLimit.SPEAKING" class="form-control"
                       placeholder="0 = không giới hạn" min="0">
            </div>
            <div class="col-md-3 mb-2">
                <label>READING (phút)</label>
                <input type="number" name="timeLimit.READING" class="form-control" min="0">
            </div>
            <div class="col-md-3 mb-2">
                <label>SPEAKING (phút) *</label>
                <input type="number" name="timeLimit.SPEAKING" class="form-control"
                       placeholder="VD: 2" min="1" required>
            </div>
            <div class="col-md-3 mb-2">
                <label>WRITING (phút) *</label>
                <input type="number" name="timeLimit.WRITING" class="form-control"
                       placeholder="VD: 30" min="1" required>
            </div>
        </div>

        <!-- Other settings -->
        <hr>
        <div class="row">
            <div class="col-md-4 mb-3">
                <label>Điểm đạt (%)</label>
                <input type="number" name="passScore" class="form-control" min="0" max="100">
            </div>
            <div class="col-md-4 mb-3">
                <label>Số lần làm tối đa</label>
                <input type="number" name="maxAttempts" class="form-control" min="1">
            </div>
            <div class="col-md-4 mb-3">
                <label class="form-label d-block">Hiển thị đáp án sau khi nộp</label>
                <input type="checkbox" name="showAnswerAfterSubmit" checked class="form-check-input">
            </div>
        </div>

        <button type="submit" class="btn btn-primary">Tiếp theo: Thêm câu hỏi LISTENING →</button>
    </form>
</div>

<script th:inline="javascript">
document.getElementById('assignmentForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const form = e.target;
    const formData = new FormData(form);

    // Build JSON body
    const body = {
        title: formData.get('title'),
        description: formData.get('description'),
        quizCategory: formData.get('quizCategory'),
        timeLimitPerSkill: {
            LISTENING: formData.get('timeLimit.LISTENING') || null,
            READING: formData.get('timeLimit.READING') || null,
            SPEAKING: formData.get('timeLimit.SPEAKING') || null,
            WRITING: formData.get('timeLimit.WRITING') || null
        },
        passScore: formData.get('passScore') || null,
        maxAttempts: formData.get('maxAttempts') || null,
        showAnswerAfterSubmit: form.querySelector('[name=showAnswerAfterSubmit]').checked
    };

    const resp = await fetch('/api/v1/expert/assignments', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
    });
    const data = await resp.json();
    if (data.success) {
        window.location.href = '/expert/assignment/' + data.data + '/skill/LISTENING';
    } else {
        alert('Lỗi: ' + data.message);
    }
});
</script>
</body>
</html>
```

### Task 14: Create `expert/assignment-skill-section.html` (Steps 2-5)

**Files:**
- Create: `src/main/resources/templates/expert/assignment-skill-section.html`

- [ ] **Step 1: Create the template**

This template handles all 4 skill sections. It reuses the existing question creation partials.

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      th:replace="~{fragments/layout :: layout(~{::title}, ~{::#content})}">
<head><title th:text="${skill} + ' - Thêm câu hỏi'">Skill Section</title></head>
<body>
<div id="content" class="container mt-4">

    <!-- Progress Steps -->
    <div class="d-flex mb-4">
        <div th:each="s, stat : ${T(java.util.Arrays).asList('LISTENING','READING','SPEAKING','WRITING')}"
             class="flex-fill text-center pe-2">
            <div th:class="${s == skill ? 'bg-primary text-white' : 'bg-secondary text-white'}"
                 class="rounded py-2 fw-bold" th:text="${s}">SKILL</div>
        </div>
    </div>

    <h4 th:text="'Phần ' + ${skill}">Phần LISTENING</h4>

    <!-- Section question count -->
    <div class="alert alert-info">
        Đã thêm: <strong id="questionCount">0</strong> câu
        <span class="text-danger" id="missingWarning" style="display:none">
            (Cần ít nhất 1 câu để xuất bản)
        </span>
    </div>

    <!-- Two panels: Bank + Create Inline -->
    <div class="row">
        <!-- Left: Question Bank Browser -->
        <div class="col-md-7">
            <div class="card">
                <div class="card-header">Chọn từ ngân hàng câu hỏi</div>
                <div class="card-body">
                    <div class="mb-2">
                        <input type="text" id="bankSearch" class="form-control"
                               placeholder="Tìm kiếm câu hỏi...">
                    </div>
                    <div id="bankList" class="border rounded p-2" style="max-height:400px;overflow-y:auto">
                        <!-- Loaded via JS -->
                    </div>
                    <button class="btn btn-success mt-2" id="addSelectedBtn" disabled>
                        Thêm câu hỏi đã chọn
                    </button>
                </div>
            </div>
        </div>

        <!-- Right: Inline Create -->
        <div class="col-md-5">
            <div class="card">
                <div class="card-header">Tạo câu hỏi mới</div>
                <div class="card-body">
                    <!-- Reuse existing question creation form partial -->
                    <div th:replace="expert/partial-question-form :: questionForm"></div>
                </div>
            </div>
        </div>
    </div>

    <!-- Navigation -->
    <div class="d-flex justify-content-between mt-4">
        <a th:href="@{/expert/assignment-management}" class="btn btn-secondary">← Quay lại</a>
        <div>
            <button class="btn btn-outline-primary" id="previewBtn">Xem trước</button>
            <a th:href="@{/expert/assignment/{quizId}/preview(quizId=${quizId})}"
               class="btn btn-primary">Hoàn thiện →</a>
        </div>
    </div>
</div>

<script th:inline="javascript">
const quizId = /*[[${quizId}]]*/ 0;
const currentSkill = /*[[${skill}]]*/ 'LISTENING';

const skills = ['LISTENING','READING','SPEAKING','WRITING'];
const currentIdx = skills.indexOf(currentSkill);

// Load questions from bank
async function loadBank() {
    const resp = await fetch(`/api/v1/expert/questions/bank?skill=${currentSkill}&status=PUBLISHED&page=0&size=50`);
    const data = await resp.json();
    renderBank(data.data.content || []);
}
loadBank();

function renderBank(questions) {
    const container = document.getElementById('bankList');
    container.innerHTML = questions.map(q => `
        <div class="form-check">
            <input class="form-check-input bank-q" type="checkbox"
                   value="${q.questionId}" id="q${q.questionId}">
            <label class="form-check-label" for="q${q.questionId}">
                ${q.content.substring(0, 100)}...
                <span class="badge bg-secondary">${q.questionType}</span>
            </label>
        </div>
    `).join('');

    document.querySelectorAll('.bank-q').forEach(cb => {
        cb.addEventListener('change', () => {
            const checked = document.querySelectorAll('.bank-q:checked').length;
            document.getElementById('addSelectedBtn').disabled = checked === 0;
        });
    });
}

document.getElementById('addSelectedBtn').addEventListener('click', async () => {
    const ids = [...document.querySelectorAll('.bank-q:checked')].map(cb => parseInt(cb.value));
    await fetch(`/api/v1/expert/assignments/${quizId}/questions`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ questionIds: ids, skill: currentSkill, itemType: 'SINGLE' })
    });
    alert('Đã thêm ' + ids.length + ' câu');
    loadBank();
    loadSectionSummary();
});

async function loadSectionSummary() {
    const resp = await fetch(`/api/v1/expert/assignments/${quizId}/skills`);
    const data = await resp.json();
    const skillData = data.data[currentSkill];
    document.getElementById('questionCount').textContent = skillData.questionCount;
    document.getElementById('missingWarning').style.display =
        skillData.questionCount === 0 ? 'inline' : 'none';
}
loadSectionSummary();
</script>
</body>
</html>
```

### Task 15: Create `expert/assignment-preview.html` (Step 6)

**Files:**
- Create: `src/main/resources/templates/expert/assignment-preview.html`

- [ ] **Step 1: Create the template**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      th:replace="~{fragments/layout :: layout(~{::title}, ~{::#content})}">
<head><title>Xem trước - Xuất bản</title></head>
<body>
<div id="content" class="container mt-4">
    <h3>Xem trước Bài Kiểm Tra Lớn</h3>
    <div id="previewContent">
        <!-- Loaded via JS -->
    </div>

    <div class="d-flex justify-content-between mt-4">
        <a th:href="@{/expert/assignment/{quizId}/skill/WRITING(quizId=${quizId})}"
           class="btn btn-secondary">← Quay lại sửa</a>
        <div>
            <button class="btn btn-outline-primary" id="publishBtn">Xuất bản</button>
        </div>
    </div>
</div>

<script th:inline="javascript">
const quizId = /*[[${quizId}]]*/ 0;

async function loadPreview() {
    const resp = await fetch(`/api/v1/expert/assignments/${quizId}/preview`);
    const data = await resp.json();
    const p = data.data;
    const sections = p.sections || [];

    document.getElementById('previewContent').innerHTML = `
        <div class="card mb-3">
            <div class="card-body">
                <h4>${p.title}</h4>
                <p>${p.description || ''}</p>
                <p><strong>Loại:</strong> ${p.quizCategory}</p>
                <p><strong>Tổng câu:</strong> ${p.totalQuestions}</p>
            </div>
        </div>

        <div class="row">
            ${sections.map(s => `
                <div class="col-md-3">
                    <div class="card ${s.questionCount === 0 ? 'border-danger' : ''}">
                        <div class="card-header">${s.skill}</div>
                        <div class="card-body">
                            <h5>${s.questionCount} câu</h5>
                            <p class="text-muted">${s.questionCount === 0 ? '⚠️ Cần thêm câu hỏi' : '✓ Sẵn sàng'}</p>
                        </div>
                    </div>
                </div>
            `).join('')}
        </div>

        ${!p.canPublish ? `
            <div class="alert alert-danger mt-3">
                <strong>Không thể xuất bản!</strong> Thiếu câu hỏi cho: ${p.missingSkills.join(', ')}
            </div>
        ` : ''}
    `;

    if (!p.canPublish) {
        document.getElementById('publishBtn').disabled = true;
    }
}
loadPreview();

document.getElementById('publishBtn').addEventListener('click', async () => {
    if (!confirm('Xuất bản bài kiểm tra này?')) return;
    const resp = await fetch(`/api/v1/expert/assignments/${quizId}/publish`, {
        method: 'PATCH'
    });
    const data = await resp.json();
    if (data.success) {
        alert('Xuất bản thành công!');
        window.location.href = '/expert/assignment-management';
    } else {
        alert('Lỗi: ' + data.message);
    }
});
</script>
</body>
</html>
```

### Task 16: Modify expert sidebar to add link

**Files:**
- Modify: `src/main/resources/templates/fragments/expert-sidebar.html`

- [ ] **Step 1: Read the sidebar fragment**

Run: Read `expert-sidebar.html` (or wherever the expert sidebar lives).

- [ ] **Step 2: Add the assignment link**

Add after the quiz management link:

```html
<li class="nav-item">
    <a class="nav-link" th:href="@{/expert/assignment-management}">
        📝 Bài kiểm tra lớn (Assignment)
    </a>
</li>
```

---

## Chunk 6: Integration Points

### Task 17: Wire `ExpertAssignmentController` into `SecurityConfig`

**Files:**
- Modify: `src/main/java/com/example/DoAn/configuration/SecurityConfig.java`

- [ ] **Step 1: Read SecurityConfig**

Run: Read `SecurityConfig.java`. Find the section with expert URL patterns.

- [ ] **Step 2: Add security rule for assignment endpoints**

If not already present, ensure the expert API path is protected:

```java
.requestMatchers("/api/v1/expert/assignments/**").hasAnyAuthority("ROLE_EXPERT")
```

### Task 18: Verify `QuizRequestDTO` has `timeLimitPerSkill`

**Files:**
- Modify: `src/main/java/com/example/DoAn/dto/request/QuizRequestDTO.java`

- [ ] **Step 1: Read QuizRequestDTO**

Run: Read `QuizRequestDTO.java`.

- [ ] **Step 2: Add missing field if not present**

Add after existing fields:

```java
private Map<String, Integer> timeLimitPerSkill;
```

Add import: `import java.util.Map;`

---

## Spec Reference

See `docs/superpowers/specs/2026-04-04-001-expert-creates-assignment-design.md` for full business rules and API contracts.
