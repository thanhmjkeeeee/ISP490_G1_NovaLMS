# Plan 005 — Teacher Grades Assignment

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Teacher views the grading queue for Assignments, sees per-skill status badges, and grades SPEAKING/WRITING sections (AI pre-populated) via a tab-based detail page.

**Architecture:** New `TeacherAssignmentGradingService`, new REST controller, new HTML pages. Uses existing AI grading pipeline from SPEC 004. Unified with SPEC 006 grading via shared templates.

**Tech Stack:** Spring Boot 3.3.4, Spring Data JPA, Thymeleaf.

---

## File Structure

```
src/main/java/com/example/DoAn/
├── service/
│   ├── ITeacherAssignmentGradingService.java  CREATE
│   └── impl/
│       └── TeacherAssignmentGradingServiceImpl.java  CREATE
├── controller/
│   └── TeacherAssignmentGradingController.java  CREATE
├── dto/request/
│   └── AssignmentGradingRequestDTO.java         CREATE
├── dto/response/
│   ├── AssignmentGradingQueueDTO.java           CREATE
│   └── AssignmentGradingDetailDTO.java         CREATE
└── views/templates/
    ├── teacher/
    │   ├── assignment-grading-list.html      CREATE
    │   └── assignment-grading-detail.html   CREATE
```

---

## Chunk 1: DTOs

### Task 1: Create DTOs

**Files:**
- Create: `src/main/java/com/example/DoAn/dto/request/AssignmentGradingRequestDTO.java`
- Create: `src/main/java/com/example/DoAn/dto/response/AssignmentGradingQueueDTO.java`
- Create: `src/main/java/com/example/DoAn/dto/response/AssignmentGradingDetailDTO.java`

- [ ] **Step 1: Write AssignmentGradingRequestDTO**

```java
package com.example.DoAn.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentGradingRequestDTO {
    private Map<String, BigDecimal> sectionScores; // {"LISTENING": 8.0, ...}
    private List<QuestionGradingItem> gradingItems;
    private String overallNote;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionGradingItem {
        private Integer questionId;
        private BigDecimal pointsAwarded;
        private String teacherNote;
    }
}
```

- [ ] **Step 2: Write AssignmentGradingQueueDTO**

```java
package com.example.DoAn.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentGradingQueueDTO {
    private Long resultId;
    private Long assignmentSessionId;
    private String studentName;
    private String studentEmail;
    private Integer quizId;
    private String quizTitle;
    private Long classId;
    private String className;
    private LocalDateTime submittedAt;
    private String overallStatus; // ALL_AUTO | PENDING_SPEAKING | PENDING_WRITING | PENDING_BOTH | ALL_GRADED
    private SectionStatus listening;
    private SectionStatus reading;
    private SectionStatus speaking;
    private SectionStatus writing;
    private BigDecimal autoScore;   // LISTENING + READING sum
    private BigDecimal totalScore;  // final (null if not graded)
    private Boolean isGraded;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectionStatus {
        private String skill;
        private String gradingStatus; // AUTO | AI_PENDING | AI_READY | GRADED
        private BigDecimal score;
        private BigDecimal maxScore;
        private String aiScore;     // e.g. "7/10"
        private String aiFeedback;
    }
}
```

- [ ] **Step 3: Write AssignmentGradingDetailDTO**

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
public class AssignmentGradingDetailDTO {
    private Long resultId;
    private Long assignmentSessionId;
    private String studentName;
    private String quizTitle;
    private String className;
    private LocalDateTime submittedAt;
    private BigDecimal autoScore;   // LISTENING + READING
    private BigDecimal totalScore;
    private Map<String, BigDecimal> sectionScores;
    private List<SkillSectionDetail> sections;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillSectionDetail {
        private String skill;
        private String gradingStatus;
        private BigDecimal maxScore;
        private List<QuestionGradeItem> questions;
        private String aiScore;
        private String aiFeedback;
        private String aiRubricJson;
        private BigDecimal teacherScore; // submitted score
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionGradeItem {
        private Integer questionId;
        private String questionType;
        private String content;
        private BigDecimal maxPoints;
        private Object studentAnswer;    // varies by type
        private String correctAnswer;    // for MC types
        private Boolean isCorrect;
        private String aiScore;
        private String aiFeedback;
        private BigDecimal teacherScore;
        private String teacherNote;
        private String audioUrl;         // for SPEAKING
    }
}
```

---

## Chunk 2: Service Layer

### Task 2: Create ITeacherAssignmentGradingService

**Files:**
- Create: `src/main/java/com/example/DoAn/service/ITeacherAssignmentGradingService.java`

- [ ] **Step 1: Write the interface**

```java
package com.example.DoAn.service;

import com.example.DoAn.dto.request.AssignmentGradingRequestDTO;
import com.example.DoAn.dto.response.AssignmentGradingDetailDTO;
import com.example.DoAn.dto.response.AssignmentGradingQueueDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ITeacherAssignmentGradingService {

    // Get grading queue (list of student submissions)
    Page<AssignmentGradingQueueDTO> getGradingQueue(
        Long teacherId, Long quizId, Long classId, String status, Pageable pageable);

    // Get single submission detail
    AssignmentGradingDetailDTO getGradingDetail(Long resultId, Long teacherId);

    // Submit grading
    void gradeAssignment(Long resultId, AssignmentGradingRequestDTO request, Long teacherId);
}
```

### Task 3: Create TeacherAssignmentGradingServiceImpl

**Files:**
- Create: `src/main/java/com/example/DoAn/service/impl/TeacherAssignmentGradingServiceImpl.java`

- [ ] **Step 1: Write the service implementation**

```java
package com.example.DoAn.service.impl;

import com.example.DoAn.dto.request.AssignmentGradingRequestDTO;
import com.example.DoAn.dto.response.AssignmentGradingDetailDTO;
import com.example.DoAn.dto.response.AssignmentGradingQueueDTO;
import com.example.DoAn.exception.InvalidDataException;
import com.example.DoAn.exception.ResourceNotFoundException;
import com.example.DoAn.model.*;
import com.example.DoAn.repository.*;
import com.example.DoAn.service.ITeacherAssignmentGradingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class TeacherAssignmentGradingServiceImpl implements ITeacherAssignmentGradingService {

    private final QuizResultRepository quizResultRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizRepository quizRepository;
    private final AssignmentSessionRepository sessionRepository;
    private final RegistrationRepository registrationRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<AssignmentGradingQueueDTO> getGradingQueue(
            Long teacherId, Long quizId, Long classId, String status, Pageable pageable) {

        // Find teacher's enrolled classes
        List<Long> classIds = registrationRepository
            .findByUserUserIdAndRole(teacherId, "TEACHER")
            .stream().map(r -> r.getClazz().getClazzId()).toList();

        List<QuizResult> results = quizResultRepository
            .findAll(pageable).getContent();

        // Filter: assignment-type quizzes only, teacher's classes, matching quizId/classId
        List<QuizResult> filtered = results.stream()
            .filter(r -> {
                Quiz q = r.getQuiz();
                if (q == null) return false;
                if (quizId != null && !q.getQuizId().equals(quizId)) return false;
                if (q.getIsSequential() == null || !q.getIsSequential()) return false;
                return true;
            })
            .toList();

        return filtered.stream()
            .map(r -> toQueueDTO(r, status))
            .reduce(new org.springframework.data.domain.PageImpl<>(filtered))
            .orElse(new org.springframework.data.domain.PageImpl<>(filtered));
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentGradingDetailDTO getGradingDetail(Long resultId, Long teacherId) {
        QuizResult result = quizResultRepository.findById(resultId)
            .orElseThrow(() -> new ResourceNotFoundException("Result not found"));

        Quiz quiz = result.getQuiz();
        if (quiz == null) throw new ResourceNotFoundException("Quiz not found");

        // Load all QuizAnswers for this result
        List<QuizAnswer> answers = quizAnswerRepository.findByQuizResultResultId(resultId);

        AssignmentGradingDetailDTO dto = new AssignmentGradingDetailDTO();
        dto.setResultId(resultId);
        dto.setAssignmentSessionId(result.getAssignmentSessionId());
        dto.setStudentName(result.getUser().getFullName());
        dto.setQuizTitle(quiz.getTitle());
        dto.setSubmittedAt(result.getSubmittedAt());

        Map<String, List<QuizAnswer>> bySkill = new LinkedHashMap<>();
        for (QuizAnswer a : answers) {
            Question q = a.getQuestion();
            String skill = q.getSkill() != null ? q.getSkill() : "OTHER";
            bySkill.computeIfAbsent(skill, k -> new ArrayList<>()).add(a);
        }

        List<AssignmentGradingDetailDTO.SkillSectionDetail> sections = new ArrayList<>();
        BigDecimal autoScore = BigDecimal.ZERO;

        for (String skill : Arrays.asList("LISTENING", "READING", "SPEAKING", "WRITING")) {
            List<QuizAnswer> skillAnswers = bySkill.getOrDefault(skill, new ArrayList<>());
            if (skillAnswers.isEmpty()) continue;

            AssignmentGradingDetailDTO.SkillSectionDetail section = new AssignmentGradingDetailDTO.SkillSectionDetail();
            section.setSkill(skill);

            BigDecimal sectionMax = BigDecimal.ZERO;
            BigDecimal sectionScore = BigDecimal.ZERO;
            List<AssignmentGradingDetailDTO.QuestionGradeItem> items = new ArrayList<>();

            for (QuizAnswer a : skillAnswers) {
                Question q = a.getQuestion();
                BigDecimal maxPts = a.getPointsAwarded() != null ? a.getPointsAwarded() : BigDecimal.ONE;
                sectionMax = sectionMax.add(maxPts);

                AssignmentGradingDetailDTO.QuestionGradeItem item =
                    new AssignmentGradingDetailDTO.QuestionGradeItem();
                item.setQuestionId(q.getQuestionId());
                item.setQuestionType(q.getQuestionType());
                item.setContent(q.getContent());
                item.setMaxPoints(maxPts);
                item.setStudentAnswer(a.getAnsweredOptions());
                item.setIsCorrect(a.getIsCorrect());
                item.setAiScore(a.getAiScore());
                item.setAiFeedback(a.getAiFeedback());
                item.setAudioUrl(q.getAudioUrl());
                item.setTeacherScore(a.getPointsAwarded());
                item.setTeacherNote(a.getTeacherNote());

                // For auto-graded: use isCorrect to show score
                if (a.getIsCorrect() != null && a.getPointsAwarded() != null) {
                    sectionScore = sectionScore.add(a.getPointsAwarded());
                }

                items.add(item);
            }

            section.setQuestions(items);
            section.setMaxScore(sectionMax);
            section.setAiScore(skillAnswers.stream()
                .filter(a -> a.getAiScore() != null)
                .findFirst().orElse(new QuizAnswer()).getAiScore());
            section.setAiFeedback(skillAnswers.stream()
                .filter(a -> a.getAiFeedback() != null)
                .findFirst().orElse(new QuizAnswer()).getAiFeedback());

            if ("LISTENING".equals(skill) || "READING".equals(skill)) {
                section.setGradingStatus("AUTO");
                autoScore = autoScore.add(sectionScore);
                section.setTeacherScore(sectionScore);
            } else {
                boolean hasAi = skillAnswers.stream().anyMatch(a -> a.getAiScore() != null);
                boolean hasTeacher = skillAnswers.stream().anyMatch(a -> a.getPointsAwarded() != null && a.getIsCorrect() != null);
                section.setGradingStatus(hasTeacher ? "GRADED" : (hasAi ? "AI_READY" : "AI_PENDING"));
                section.setTeacherScore(sectionScore);
            }

            sections.add(section);
        }

        dto.setSections(sections);
        dto.setAutoScore(autoScore);

        if (result.getSectionScores() != null) {
            dto.setSectionScores(objectMapper.readValue(
                result.getSectionScores(),
                new TypeReference<Map<String, BigDecimal>>() {}));
        }

        return dto;
    }

    @Override
    public void gradeAssignment(Long resultId, AssignmentGradingRequestDTO request, Long teacherId) {
        QuizResult result = quizResultRepository.findById(resultId)
            .orElseThrow(() -> new ResourceNotFoundException("Result not found"));

        // Save per-question scores
        if (request.getGradingItems() != null) {
            for (AssignmentGradingRequestDTO.QuestionGradingItem item : request.getGradingItems()) {
                quizAnswerRepository.findByQuizResultResultIdAndQuestionQuestionId(resultId, item.getQuestionId())
                    .ifPresent(answer -> {
                        answer.setPointsAwarded(item.getPointsAwarded());
                        answer.setTeacherNote(item.getTeacherNote());
                        answer.setIsCorrect(
                            item.getPointsAwarded().compareTo(BigDecimal.ZERO) > 0
                                ? true : false
                        );
                        quizAnswerRepository.save(answer);
                    });
            }
        }

        // Update section scores
        if (request.getSectionScores() != null) {
            result.setSectionScores(objectMapper.writeValueAsString(request.getSectionScores()));
        }

        // Recalculate total
        BigDecimal total = request.getSectionScores() != null
            ? request.getSectionScores().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)
            : BigDecimal.ZERO;
        result.setScore(total.intValue());

        Quiz quiz = result.getQuiz();
        BigDecimal totalMax = quizQuestionRepository
            .findByQuizQuizId(quiz.getQuizId()).stream()
            .map(qq -> qq.getPoints())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        result.setCorrectRate(
            totalMax.compareTo(BigDecimal.ZERO) > 0
                ? total.divide(totalMax, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO
        );

        if (quiz.getPassScore() != null) {
            BigDecimal pct = result.getCorrectRate();
            result.setPassed(pct.compareTo(quiz.getPassScore()) >= 0);
        }

        quizResultRepository.save(result);
    }

    private AssignmentGradingQueueDTO toQueueDTO(QuizResult r, String status) {
        AssignmentGradingQueueDTO dto = new AssignmentGradingQueueDTO();
        dto.setResultId(r.getResultId());
        dto.setAssignmentSessionId(r.getAssignmentSessionId());
        dto.setStudentName(r.getUser().getFullName());
        dto.setStudentEmail(r.getUser().getEmail());
        dto.setQuizId(r.getQuiz().getQuizId());
        dto.setQuizTitle(r.getQuiz().getTitle());
        dto.setSubmittedAt(r.getSubmittedAt());
        dto.setAutoScore(r.getScore() != null ? BigDecimal.valueOf(r.getScore()) : BigDecimal.ZERO);
        dto.setTotalScore(r.getScore() != null ? BigDecimal.valueOf(r.getScore()) : null);
        dto.setIsGraded(r.getPassed() != null); // simplified
        return dto;
    }
}
```

---

## Chunk 3: Controller

### Task 4: Create TeacherAssignmentGradingController

**Files:**
- Create: `src/main/java/com/example/DoAn/controller/TeacherAssignmentGradingController.java`

- [ ] **Step 1: Write the controller**

```java
package com.example.DoAn.controller;

import com.example.DoAn.dto.request.AssignmentGradingRequestDTO;
import com.example.DoAn.dto.response.AssignmentGradingDetailDTO;
import com.example.DoAn.dto.response.AssignmentGradingQueueDTO;
import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.service.ITeacherAssignmentGradingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/teacher/assignment-results")
@RequiredArgsConstructor
public class TeacherAssignmentGradingController {

    private final ITeacherAssignmentGradingService gradingService;

    @GetMapping
    public ResponseEntity<ResponseData<Page<AssignmentGradingQueueDTO>>> getQueue(
            @RequestParam(required = false) Long quizId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication auth) {

        Long teacherId = extractUserId(auth);
        Page<AssignmentGradingQueueDTO> page = gradingService.getGradingQueue(
            teacherId, quizId, classId, status, pageable);
        return ResponseEntity.ok(ResponseData.success(page));
    }

    @GetMapping("/{resultId}")
    public ResponseEntity<ResponseData<AssignmentGradingDetailDTO>> getDetail(
            @PathVariable Long resultId,
            Authentication auth) {
        Long teacherId = extractUserId(auth);
        AssignmentGradingDetailDTO detail = gradingService.getGradingDetail(resultId, teacherId);
        return ResponseEntity.ok(ResponseData.success(detail));
    }

    @PostMapping("/{resultId}/grade")
    public ResponseEntity<ResponseData<Boolean>> grade(
            @PathVariable Long resultId,
            @RequestBody AssignmentGradingRequestDTO request,
            Authentication auth) {
        Long teacherId = extractUserId(auth);
        gradingService.gradeAssignment(resultId, request, teacherId);
        return ResponseEntity.ok(ResponseData.success(true));
    }

    private Long extractUserId(Authentication auth) {
        Object principal = auth.getPrincipal();
        try {
            var method = principal.getClass().getMethod("getId");
            return (Long) method.invoke(principal);
        } catch (Exception e) {
            throw new RuntimeException("Could not extract userId", e);
        }
    }
}
```

### Task 5: Add View Controller routes

**Files:**
- Modify: `src/main/java/com/example/DoAn/controller/TeacherViewController.java`

- [ ] **Step 1: Read the controller**

Run: Find and read `TeacherViewController.java`.

- [ ] **Step 2: Add routes**

```java
@GetMapping("/teacher/assignment/grading")
public String assignmentGradingList() {
    return "teacher/assignment-grading-list";
}

@GetMapping("/teacher/assignment/grading/{resultId}")
public String assignmentGradingDetail(@PathVariable Long resultId, Model model) {
    model.addAttribute("resultId", resultId);
    return "teacher/assignment-grading-detail";
}
```

---

## Chunk 4: HTML Templates

### Task 6: Create assignment-grading-list.html

**Files:**
- Create: `src/main/resources/templates/teacher/assignment-grading-list.html`

- [ ] **Step 1: Write the template**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      th:replace="~{fragments/layout :: layout(~{::title}, ~{::#content})}">
<head><title>Chấm điểm Bài kiểm tra lớn</title></head>
<body>
<div id="content" class="container-fluid mt-4">

    <h3>📋 Chấm điểm — Bài kiểm tra lớn</h3>

    <!-- Filters -->
    <div class="card mb-3">
        <div class="card-body">
            <div class="row g-2">
                <div class="col-md-4">
                    <input type="text" id="filterSearch" class="form-control"
                           placeholder="Tìm theo tên học viên...">
                </div>
                <div class="col-md-2">
                    <button class="btn btn-primary" onclick="loadQueue(0)">Lọc</button>
                </div>
            </div>
        </div>
    </div>

    <!-- Queue Table -->
    <div class="table-responsive">
        <table class="table table-hover align-middle">
            <thead class="table-light">
                <tr>
                    <th>Học viên</th>
                    <th>Bài kiểm tra</th>
                    <th>Ngày nộp</th>
                    <th>LISTENING</th>
                    <th>READING</th>
                    <th>SPEAKING</th>
                    <th>WRITING</th>
                    <th>Điểm tự động</th>
                    <th>Thao tác</th>
                </tr>
            </thead>
            <tbody id="queueBody">
                <!-- Loaded via JS -->
            </tbody>
        </table>
    </div>

    <nav id="paginationNav" class="mt-3" style="display:none">
        <ul class="pagination justify-content-center" id="paginationList"></ul>
    </nav>
</div>

<script>
let currentPage = 0;

async function loadQueue(page = 0) {
    currentPage = page;
    const search = document.getElementById('filterSearch').value;
    let url = `/api/v1/teacher/assignment-results?page=${page}&size=20`;
    if (search) url += `&search=${encodeURIComponent(search)}`;

    const resp = await fetch(url);
    const data = await resp.json();
    renderQueue(data.data);
    renderPagination(data.data);
}

function renderQueue(page) {
    const tbody = document.getElementById('queueBody');
    const content = page.content || [];

    if (!content.length) {
        tbody.innerHTML = '<tr><td colspan="9" class="text-center text-muted py-4">Không có bài nào cần chấm.</td></tr>';
        return;
    }

    tbody.innerHTML = content.map(item => {
        const statusBadge = (skill, status) => {
            const icons = {
                'AUTO': '✅',
                'AI_PENDING': '⏳',
                'AI_READY': '🎤',
                'GRADED': '✅'
            };
            return `<span title="${skill}: ${status}">${icons[status] || '❓'}</span>`;
        };

        return `
        <tr>
            <td>
                <strong>${item.studentName || 'N/A'}</strong>
                <br><small class="text-muted">${item.studentEmail || ''}</small>
            </td>
            <td>${item.quizTitle || 'N/A'}</td>
            <td>${item.submittedAt ? new Date(item.submittedAt).toLocaleDateString('vi-VN') : 'N/A'}</td>
            <td>${statusBadge('LISTENING', item.listening?.gradingStatus || 'AUTO')}</td>
            <td>${statusBadge('READING', item.reading?.gradingStatus || 'AUTO')}</td>
            <td>${statusBadge('SPEAKING', item.speaking?.gradingStatus || 'AI_PENDING')}</td>
            <td>${statusBadge('WRITING', item.writing?.gradingStatus || 'AI_PENDING')}</td>
            <td>
                ${item.autoScore || 0}
                ${item.totalScore ? `/ ${item.totalScore}` : ''}
            </td>
            <td>
                ${item.isGraded
                    ? `<span class="badge bg-success">Đã chấm</span>`
                    : `<a href="/teacher/assignment/grading/${item.resultId}"
                           class="btn btn-primary btn-sm">Chấm điểm →</a>`
                }
            </td>
        </tr>`;
    }).join('');
}

function renderPagination(page) {
    const nav = document.getElementById('paginationNav');
    const list = document.getElementById('paginationList');
    if (!page.totalPages || page.totalPages <= 1) {
        nav.style.display = 'none'; return;
    }
    nav.style.display = 'block';
    list.innerHTML = '';
    for (let i = 0; i < page.totalPages; i++) {
        list.innerHTML += `<li class="page-item ${i===page.number?'active':''}">
            <a class="page-link" href="#" onclick="loadQueue(${i});return false">${i+1}</a>
        </li>`;
    }
}

loadQueue(0);
</script>
</body>
</html>
```

### Task 7: Create assignment-grading-detail.html

**Files:**
- Create: `src/main/resources/templates/teacher/assignment-grading-detail.html`

- [ ] **Step 1: Write the template**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      th:replace="~{fragments/layout :: layout(~{::title}, ~{::#content})}">
<head><title>Chấm điểm chi tiết</title></head>
<body>
<div id="content" class="container mt-4">

    <!-- Header -->
    <div class="d-flex justify-content-between align-items-center mb-3">
        <div>
            <h4>Chấm điểm: <span id="studentName">...</span></h4>
            <small class="text-muted">
                Bài: <span id="quizTitle"></span> |
                Lớp: <span id="className"></span> |
                Ngày: <span id="submittedAt"></span>
            </small>
        </div>
        <a href="/teacher/assignment/grading" class="btn btn-secondary">← Quay lại</a>
    </div>

    <!-- Auto Score Summary -->
    <div class="alert alert-info mb-3">
        Điểm tự động (Nghe + Đọc): <strong id="autoScore">—</strong> |
        Tổng điểm: <strong id="totalScore">—</strong>
    </div>

    <!-- Skill Tabs -->
    <ul class="nav nav-tabs mb-3" id="skillTabs">
        <!-- Tabs loaded via JS -->
    </ul>

    <!-- Tab Content -->
    <div class="tab-content" id="tabContent">
        <!-- Content loaded via JS -->
    </div>

    <!-- Submit -->
    <div class="d-flex justify-content-end mt-4">
        <button class="btn btn-success btn-lg" id="submitGradeBtn" onclick="submitGrade()">
            ✓ Lưu điểm chấm
        </button>
    </div>
</div>

<script>
const resultId = /*[[${resultId}]]*/ 0;
let gradingData = null;
let sectionScores = {};
let gradingItems = [];

async function loadDetail() {
    const resp = await fetch(`/api/v1/teacher/assignment-results/${resultId}`);
    const data = await resp.json();
    if (!data.success) { alert(data.message); return; }
    gradingData = data.data;

    document.getElementById('studentName').textContent = gradingData.studentName;
    document.getElementById('quizTitle').textContent = gradingData.quizTitle;
    document.getElementById('className').textContent = gradingData.className || 'N/A';
    document.getElementById('submittedAt').textContent = gradingData.submittedAt
        ? new Date(gradingData.submittedAt).toLocaleDateString('vi-VN') : 'N/A';
    document.getElementById('autoScore').textContent = gradingData.autoScore || 0;

    if (gradingData.sectionScores) {
        sectionScores = gradingData.sectionScores;
    }

    renderTabs(gradingData.sections);
    renderTabContent(gradingData.sections);
}

function renderTabs(sections) {
    const nav = document.getElementById('skillTabs');
    nav.innerHTML = sections.map((s, i) => `
        <li class="nav-item">
            <button class="nav-link ${i===0?'active':''}"
                    data-bs-toggle="tab"
                    data-bs-target="#skill-${s.skill}"
                    type="button">
                ${getSkillIcon(s.skill)} ${s.skill}
                ${s.gradingStatus === 'GRADED' ? ' ✅' : ''}
                ${s.gradingStatus === 'AI_READY' ? ' 🎤' : ''}
            </button>
        </li>
    `).join('');
}

function getSkillIcon(skill) {
    return {LISTENING:'🎧',READING:'📖',SPEAKING:'🎤',WRITING:'✍️'}[skill] || '📝';
}

function renderTabContent(sections) {
    const container = document.getElementById('tabContent');
    container.innerHTML = sections.map((s, i) => `
        <div class="tab-pane fade ${i===0?'show active':''}" id="skill-${s.skill}">
            ${renderSectionContent(s)}
        </div>
    `).join('');
}

function renderSectionContent(section) {
    const isAutoGraded = section.gradingStatus === 'AUTO';
    const hasAi = section.gradingStatus === 'AI_READY' || section.gradingStatus === 'GRADED';

    return `
    <div class="card">
        <div class="card-header d-flex justify-content-between align-items-center">
            <span>${getSkillIcon(section.skill)} <strong>${section.skill}</strong></span>
            <div>
                <span class="badge ${isAutoGraded?'bg-success':'bg-warning text-dark'}">
                    ${isAutoGraded ? '✅ Tự động' : (section.gradingStatus === 'AI_READY' ? '🎤 AI sẵn sàng' : '⏳ Chờ AI')}
                </span>
            </div>
        </div>
        <div class="card-body">
            ${hasAi && section.aiScore ? `
                <div class="alert alert-secondary mb-3">
                    <strong>🤖 Điểm AI:</strong> ${section.aiScore}
                    ${section.aiFeedback ? `<br><small>${section.aiFeedback}</small>` : ''}
                </div>
            ` : ''}

            ${section.questions.map((q, qi) => `
                <div class="border rounded p-3 mb-3">
                    <div class="d-flex justify-content-between">
                        <h6>Câu ${qi+1} (${q.maxPoints} điểm)</h6>
                        ${q.isCorrect === true ? '<span class="badge bg-success">✅ Đúng</span>' : ''}
                        ${q.isCorrect === false ? '<span class="badge bg-danger">❌ Sai</span>' : ''}
                    </div>
                    <p class="mb-2"><strong>Câu hỏi:</strong> ${q.content}</p>

                    ${q.questionType === 'SPEAKING' && q.audioUrl ? `
                        <div class="mb-2">
                            <label class="form-label">🎤 File ghi âm:</label>
                            <audio controls src="${q.audioUrl}" class="w-100"></audio>
                        </div>
                    ` : ''}

                    ${q.studentAnswer ? `
                        <div class="alert alert-light mb-2">
                            <strong>📝 Câu trả lời:</strong> ${q.studentAnswer}
                        </div>
                    ` : ''}

                    ${!isAutoGraded ? `
                        <div class="row g-2 align-items-center">
                            <div class="col-md-4">
                                <label>Điểm giáo viên:</label>
                                <input type="number" step="0.5" min="0" max="${q.maxPoints}"
                                       class="form-control teacher-score"
                                       data-question-id="${q.questionId}"
                                       data-max="${q.maxPoints}"
                                       value="${q.teacherScore || 0}">
                            </div>
                            <div class="col-md-8">
                                <label>Ghi chú:</label>
                                <input type="text" class="form-control teacher-note"
                                       data-question-id="${q.questionId}"
                                       value="${q.teacherNote || ''}">
                            </div>
                        </div>
                    ` : `
                        <div class="alert ${q.isCorrect ? 'alert-success' : 'alert-danger'} mb-0">
                            Điểm: ${q.teacherScore || q.maxPoints} / ${q.maxPoints}
                        </div>
                    `}
                </div>
            `).join('')}
        </div>
    </div>`;
}

function submitGrade() {
    // Collect scores from inputs
    gradingItems = [];
    sectionScores = {LISTENING: 0, READING: 0, SPEAKING: 0, WRITING: 0};

    document.querySelectorAll('.teacher-score').forEach(input => {
        const qId = parseInt(input.dataset.questionId);
        const score = parseFloat(input.value) || 0;
        const note = document.querySelector(`.teacher-note[data-question-id="${qId}"]`)?.value || '';
        gradingItems.push({ questionId: qId, pointsAwarded: score, teacherNote: note });

        // Find section and add to sectionScores
        const q = gradingData.sections.flatMap(s => s.questions)
            .find(q => q.questionId === qId);
        if (q) {
            const skill = q.questionType === 'SPEAKING' ? 'SPEAKING'
                : q.questionType === 'WRITING' ? 'WRITING'
                : (q.maxPoints > 0 && q.isCorrect === false ? 'READING' : 'LISTENING');
            sectionScores[skill] = (sectionScores[skill] || 0) + score;
        }
    });

    // Add auto-graded sections
    gradingData.sections.forEach(s => {
        if (s.gradingStatus === 'AUTO') {
            const sum = s.questions.reduce((acc, q) => acc + (q.teacherScore || 0), 0);
            sectionScores[s.skill] = sum;
        }
    });

    fetch(`/api/v1/teacher/assignment-results/${resultId}/grade`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            sectionScores: sectionScores,
            gradingItems: gradingItems,
            overallNote: ''
        })
    }).then(r => r.json()).then(data => {
        if (data.success) {
            alert('Đã lưu điểm chấm!');
            window.location.href = '/teacher/assignment/grading';
        } else {
            alert('Lỗi: ' + data.message);
        }
    });
}

loadDetail();
</script>
</body>
</html>
```

---

## Spec Reference

See `docs/superpowers/specs/2026-04-04-005-teacher-grades-assignment-design.md`.
