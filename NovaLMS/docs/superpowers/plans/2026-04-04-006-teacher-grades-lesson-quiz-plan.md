# Plan 006 — Teacher Grades Lesson Quiz

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Teacher grades Lesson Quiz submissions (N skills, any combination). Dynamic skill tabs based on actual quiz content. Extends existing grading templates with dynamic rendering.

**Architecture:** Most of this plan extends existing `TeacherQuizGradingServiceImpl` and `TeacherQuizGradingApiController`. Key additions: (1) AI fields on `QuizAnswer`, (2) AI grading for `QuizAnswer` entities, (3) dynamic skill detection, (4) extended grading templates.

**Tech Stack:** Spring Boot 3.3.4, Spring Data JPA, Thymeleaf.

---

## File Structure

```
src/main/java/com/example/DoAn/
├── model/
│   └── QuizAnswer.java                MODIFY (add AI score fields)
├── dto/response/
│   └── LessonQuizGradingDetailDTO.java   CREATE
├── service/impl/
│   └── GroqGradingServiceImpl.java   MODIFY (add gradeQuizAnswer method)
└── views/templates/
    └── teacher/
        ├── quiz-grading-list.html    MODIFY (add Lesson Quiz filter)
        └── quiz-grading-detail.html  MODIFY (dynamic skill tabs)
```

---

## Chunk 1: Add AI Fields to QuizAnswer

### Task 1: Add AI score fields to QuizAnswer

**Files:**
- Modify: `src/main/java/com/example/DoAn/model/QuizAnswer.java`

- [ ] **Step 1: Read QuizAnswer.java**

Run: Read `QuizAnswer.java`.

- [ ] **Step 2: Add new fields after existing fields**

Add after the `answeredOptions` or `isCorrect` field:

```java
@Column(name = "ai_score", length = 20)
private String aiScore; // e.g. "7/10"

@Column(name = "ai_feedback", columnDefinition = "TEXT")
private String aiFeedback;

@Column(name = "ai_rubric_json", columnDefinition = "TEXT")
private String aiRubricJson;
```

Add import: `import jakarta.persistence.Column;`

- [ ] **Step 3: Verify database columns**

Run SQL:

```sql
ALTER TABLE quiz_answer ADD COLUMN ai_score VARCHAR(20);
ALTER TABLE quiz_answer ADD COLUMN ai_feedback TEXT;
ALTER TABLE quiz_answer ADD COLUMN ai_rubric_json TEXT;
```

- [ ] **Step 4: Add `findByQuizResultResultIdAndQuestionQuestionId` to QuizAnswerRepository**

Run: Read `QuizAnswerRepository.java`.

Add method:

```java
Optional<QuizAnswer> findByQuizResultResultIdAndQuestionQuestionId(Long resultId, Integer questionId);
```

---

## Chunk 2: AI Grading for QuizAnswer

### Task 2: Add `gradeQuizAnswer` to GroqGradingServiceImpl

**Files:**
- Modify: `src/main/java/com/example/DoAn/service/impl/GroqGradingServiceImpl.java`

- [ ] **Step 1: Read the existing file**

Run: Read `GroqGradingServiceImpl.java` completely. Note the existing `fireAndForget()` and `grade()` methods.

- [ ] **Step 2: Add QuizAnswerRepository and QuestionRepository imports and fields**

Find the existing fields section. Add:

```java
private final QuizAnswerRepository quizAnswerRepository;
private final QuestionRepository questionRepository;
```

Add imports:

```java
import com.example.DoAn.repository.QuizAnswerRepository;
import com.example.DoAn.repository.QuestionRepository;
```

- [ ] **Step 3: Add `fireAndForgetForQuizAnswer` method**

Find the end of the class and add:

```java
public void fireAndForgetForQuizAnswer(Long quizResultId, Integer questionId) {
    CompletableFuture.runAsync(() -> {
        try {
            gradeQuizAnswer(quizResultId, questionId);
        } catch (Exception e) {
            log.error("Failed to grade quiz answer: quizResultId={}, questionId={}",
                quizResultId, questionId, e);
        }
    }, executorService);
}
```

- [ ] **Step 4: Add `gradeQuizAnswer` transactional method**

Add after `fireAndForgetForQuizAnswer`:

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void gradeQuizAnswer(Long quizResultId, Integer questionId) {
    QuizAnswer answer = quizAnswerRepository
        .findByQuizResultResultIdAndQuestionQuestionId(quizResultId, questionId)
        .orElse(null);
    if (answer == null) {
        log.warn("QuizAnswer not found: resultId={}, questionId={}", quizResultId, questionId);
        return;
    }

    Question q = answer.getQuestion();
    String userText = null;

    if ("SPEAKING".equals(q.getQuestionType())) {
        String audioUrl = answer.getAnsweredOptions(); // contains Cloudinary URL
        if (audioUrl != null && !audioUrl.isBlank()) {
            try {
                userText = groqClient.transcribe(audioUrl);
            } catch (Exception e) {
                log.warn("Transcription failed for answerId={}: {}", answer.getId(), e.getMessage());
            }
        }
    } else if ("WRITING".equals(q.getQuestionType())) {
        userText = answer.getAnsweredOptions(); // contains text answer
    }

    String cefr = (q.getCefrLevel() != null) ? q.getCefrLevel() : "B1";

    GradingResponse grading = groqClient.gradeWritingOrSpeaking(
        cefr,
        q.getQuestionType(),
        userText != null ? userText : ""
    );

    answer.setAiScore(grading.getTotalScore() + "/" + grading.getMaxScore());
    answer.setAiFeedback(grading.getFeedback());
    if (grading.getRubric() != null) {
        try {
            answer.setAiRubricJson(objectMapper.writeValueAsString(grading.getRubric()));
        } catch (Exception e) {
            log.warn("Failed to serialize rubric: {}", e.getMessage());
        }
    }

    quizAnswerRepository.save(answer);
    log.info("AI graded quiz answer {}: score={}", answer.getId(), grading.getTotalScore());
}
```

Add imports:

```java
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
```

---

## Chunk 3: Extend TeacherQuizGradingServiceImpl

### Task 3: Extend existing grading service for dynamic skills + AI

**Files:**
- Modify: `src/main/java/com/example/DoAn/service/impl/TeacherQuizGradingServiceImpl.java`

- [ ] **Step 1: Read the existing file**

Run: Read `TeacherQuizGradingServiceImpl.java`.

- [ ] **Step 2: Add new imports**

Add:

```java
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.LinkedHashMap;
import java.util.Map;
```

- [ ] **Step 3: Add `getSkillSummary` method to the service**

Find the grading queue method and add after it (or add a new method):

```java
@Transactional(readOnly = true)
public Map<String, SkillSummary> getSkillSummaryForQuiz(Integer quizId) {
    List<QuizQuestion> questions = quizQuestionRepository.findByQuizQuizId(quizId);
    Map<String, SkillSummary> result = new LinkedHashMap<>();
    for (QuizQuestion qq : questions) {
        String skill = qq.getQuestion().getSkill() != null
            ? qq.getQuestion().getSkill() : "OTHER";
        result.computeIfAbsent(skill, k -> new SkillSummary(skill, 0, 0, 0));
        SkillSummary s = result.get(skill);
        s.totalCount++;
        if ("PUBLISHED".equals(qq.getQuestion().getStatus())) s.publishedCount++;
        else if ("PENDING_REVIEW".equals(qq.getQuestion().getStatus())) s.pendingCount++;
    }
    return result;
}

@Data
@AllArgsConstructor
class SkillSummary {
    private String skill;
    private long totalCount;
    private long publishedCount;
    private long pendingCount;
}
```

- [ ] **Step 4: Extend `gradeQuizResult` to save skill scores**

Find the existing `gradeQuizResult` method. Add at the start of the method body:

```java
// Save skill scores if provided
if (request.getSkillScores() != null) {
    quizResult.setSkillScores(
        objectMapper.writeValueAsString(request.getSkillScores()));
}
if (request.getOverallNote() != null) {
    quizResult.setOverallNote(request.getOverallNote());
}
```

Add import: `import lombok.Data;` and `import lombok.AllArgsConstructor;`

- [ ] **Step 5: Add `QuizAnswer` AI fields to grading response**

Extend the existing grading detail DTO to include AI scores. Find where `QuizAnswer` data is mapped and add:

```java
// Add AI score fields to the grading item
gradingItem.setAiScore(answer.getAiScore());
gradingItem.setAiFeedback(answer.getAiFeedback());
gradingItem.setAiRubricJson(answer.getAiRubricJson());
```

---

## Chunk 4: Dynamic Skill Tabs in Grading Templates

### Task 4: Modify quiz-grading-list.html — add Lesson Quiz / Assignment filter

**Files:**
- Modify: `src/main/resources/templates/teacher/quiz-grading-list.html`

- [ ] **Step 1: Read the existing template**

Run: Read `teacher/quiz-grading-list.html`.

- [ ] **Step 2: Add tab filter at the top**

Find the page header section and add tabs:

```html
<!-- Add after the page heading -->
<ul class="nav nav-tabs mb-3" id="quizTypeTabs">
    <li class="nav-item">
        <a class="nav-link active" href="#" data-type="LESSON_QUIZ">📝 Bài tập nhỏ (Quiz)</a>
    </li>
    <li class="nav-item">
        <a class="nav-link" href="#" data-type="ASSIGNMENT">📋 Bài kiểm tra lớn</a>
    </li>
</ul>
```

- [ ] **Step 3: Update the loadQueue function to include quizType filter**

Find the `loadQueue` function and add `quizType` parameter:

```javascript
let currentQuizType = 'LESSON_QUIZ';

document.querySelectorAll('#quizTypeTabs .nav-link').forEach(tab => {
    tab.addEventListener('click', (e) => {
        e.preventDefault();
        document.querySelectorAll('#quizTypeTabs .nav-link').forEach(t => t.classList.remove('active'));
        tab.classList.add('active');
        currentQuizType = tab.dataset.type;
        loadQueue(0);
    });
});

async function loadQueue(page = 0) {
    // ... existing code ...
    let url = `/api/v1/teacher/quiz-results/pending?page=${page}&size=20&quizType=${currentQuizType}`;
    // ...
}
```

### Task 5: Modify quiz-grading-detail.html — dynamic skill tabs

**Files:**
- Modify: `src/main/resources/templates/teacher/quiz-grading-detail.html`

- [ ] **Step 1: Read the existing template**

Run: Read `teacher/quiz-grading-detail.html`.

- [ ] **Step 2: Replace hardcoded skill sections with dynamic tabs**

Find where the 4 skill tabs are defined (LISTENING, READING, SPEAKING, WRITING hardcoded). Replace with dynamic rendering based on the API response.

In the JavaScript section (after loading the grading data), find where section tabs are rendered and replace:

```javascript
// Find the section tab rendering in the existing template
// Replace hardcoded tabs with dynamic:
function renderSkillTabs(skills) {
    const nav = document.getElementById('skillTabs');
    nav.innerHTML = skills.map((skill, i) => `
        <li class="nav-item">
            <button class="nav-link ${i===0?'active':''}"
                    data-bs-toggle="tab"
                    data-bs-target="#skill-${skill}"
                    type="button">
                ${getSkillIcon(skill)} ${skill}
            </button>
        </li>
    `).join('');
}

// Call after loading data:
renderSkillTabs(gradingData.skillsPresent || gradingData.skills || []);
```

- [ ] **Step 3: Update tab content rendering**

Replace the hardcoded tab pane divs with dynamic content:

```javascript
// Replace existing hardcoded tab panes with dynamic rendering
function renderSkillTabContent(section) {
    const isSpeaking = section.skill === 'SPEAKING';
    const isWriting = section.skill === 'WRITING';
    const needsGrading = isSpeaking || isWriting;

    return `
    <div class="card mb-3">
        <div class="card-header d-flex justify-content-between">
            <span>${getSkillIcon(section.skill)} <strong>${section.skill}</strong></span>
            ${needsGrading && section.aiScore ? `
                <span class="badge bg-info">🤖 AI: ${section.aiScore}</span>
            ` : ''}
        </div>
        <div class="card-body">
            ${section.questions.map((q, qi) => `
                <div class="border rounded p-3 mb-3">
                    <h6>Câu ${qi+1} (${q.maxPoints} điểm)</h6>
                    <p>${q.content}</p>

                    ${isSpeaking && q.audioUrl ? `
                        <audio controls src="${q.audioUrl}" class="w-100 mb-2"></audio>
                    ` : ''}

                    ${q.studentAnswer ? `<p><strong>Đáp án:</strong> ${q.studentAnswer}</p>` : ''}

                    ${q.aiScore ? `
                        <div class="alert alert-info">
                            🤖 AI: ${q.aiScore}
                            ${q.aiFeedback ? `<br><small>${q.aiFeedback}</small>` : ''}
                        </div>
                    ` : ''}

                    ${needsGrading ? `
                        <div class="row g-2">
                            <div class="col-md-3">
                                <label>Điểm GV:</label>
                                <input type="number" step="0.5" min="0" max="${q.maxPoints}"
                                       class="form-control teacher-score"
                                       data-qid="${q.questionId}"
                                       value="${q.pointsAwarded || 0}">
                            </div>
                            <div class="col-md-9">
                                <label>Ghi chú:</label>
                                <input type="text" class="form-control teacher-note"
                                       data-qid="${q.questionId}"
                                       value="${q.teacherNote || ''}">
                            </div>
                        </div>
                    ` : `
                        <div class="${q.isCorrect ? 'text-success' : 'text-danger'}">
                            ${q.isCorrect ? '✅ Đúng' : '❌ Sai'}
                            — ${q.pointsAwarded || 0} / ${q.maxPoints}
                        </div>
                    `}
                </div>
            `).join('')}
        </div>
    </div>`;
}

function getSkillIcon(skill) {
    return {LISTENING:'🎧',READING:'📖',SPEAKING:'🎤',WRITING:'✍️'}[skill] || '📝';
}
```

Also update the submit function to send skill scores:

```javascript
// In the submitGrade function, add skillScores:
const skillScores = {};
gradingData.sections.forEach(s => {
    skillScores[s.skill] = s.questions.reduce((acc, q) => {
        const input = document.querySelector(`.teacher-score[data-qid="${q.questionId}"]`);
        return acc + (parseFloat(input?.value) || 0);
    }, 0);
});
```

---

## Chunk 5: Fire AI Grading at Quiz Submission

### Task 6: Hook AI grading into StudentQuizTakingController submission

**Files:**
- Modify: `src/main/java/com/example/DoAn/controller/StudentQuizTakingController.java`

- [ ] **Step 1: Read the file**

Run: Read `StudentQuizTakingController.java`. Find the `submitQuiz` or `POST /submit` endpoint.

- [ ] **Step 2: Find where SPEAKING/WRITING questions are handled at submission**

Find the section where `QuizAnswer` entities are created during submission. This is in `QuizResultServiceImpl`, but the key is to fire AI grading after submission.

- [ ] **Step 3: Add AI grading fire call after `submitQuiz` in QuizResultServiceImpl**

Run: Read `QuizResultServiceImpl.java`. Find the `submit` method.

After creating `QuizAnswer` entities for SPEAKING/WRITING questions, add:

```java
// After quiz answers are saved, fire AI grading for SPEAKING/WRITING
for (QuizAnswer answer : answers) {
    Question q = answer.getQuestion();
    if ("SPEAKING".equals(q.getQuestionType()) || "WRITING".equals(q.getQuestionType())) {
        // Mark as pending AI review
        answer.setHasPendingReview(true);
        quizAnswerRepository.save(answer);

        // Fire async AI grading
        try {
            groqGradingService.fireAndForgetForQuizAnswer(
                result.getResultId(), q.getQuestionId());
        } catch (Exception e) {
            log.warn("Failed to fire AI grading for answer {}: {}",
                answer.getId(), e.getMessage());
        }
    }
}
```

This requires injecting `GroqGradingServiceImpl` into `QuizResultServiceImpl`. Add as a field:

```java
private final GroqGradingServiceImpl groqGradingService;
```

Add import: `import com.example.DoAn.service.impl.GroqGradingServiceImpl;`

---

## Chunk 6: Teacher Grading Dashboard

### Task 7: Create unified teacher grading dashboard

**Files:**
- Modify: `src/main/java/com/example/DoAn/controller/TeacherViewController.java`

- [ ] **Step 1: Read the controller**

Run: Read `TeacherViewController.java`.

- [ ] **Step 2: Add grading dashboard route**

```java
@GetMapping("/teacher/grading")
public String gradingDashboard() {
    return "teacher/grading-dashboard";
}
```

- [ ] **Step 3: Create teacher/grading-dashboard.html**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      th:replace="~{fragments/layout :: layout(~{::title}, ~{::#content})}">
<head><title>Bảng điều khiển chấm điểm</title></head>
<body>
<div id="content" class="container mt-4">
    <h3>Bảng điều khiển chấm điểm</h3>

    <div class="row">
        <div class="col-md-6">
            <div class="card text-center">
                <div class="card-body">
                    <h2 id="pendingQuizCount">—</h2>
                    <p>Bài tập nhỏ (Quiz) chờ chấm</p>
                    <a href="/teacher/quiz/grading" class="btn btn-outline-primary">Xem →</a>
                </div>
            </div>
        </div>
        <div class="col-md-6">
            <div class="card text-center">
                <div class="card-body">
                    <h2 id="pendingAssignmentCount">—</h2>
                    <p>Bài kiểm tra lớn chờ chấm</p>
                    <a href="/teacher/assignment/grading" class="btn btn-outline-success">Xem →</a>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
async function loadCounts() {
    // Fetch pending counts from both queues
    const quizResp = await fetch('/api/v1/teacher/quiz-results/pending?size=1');
    const quizData = await quizResp.json();
    document.getElementById('pendingQuizCount').textContent =
        quizData.data.totalElements || 0;

    const assignResp = await fetch('/api/v1/teacher/assignment-results?size=1');
    const assignData = await assignResp.json();
    document.getElementById('pendingAssignmentCount').textContent =
        assignData.data.totalElements || 0;
}
loadCounts();
</script>
</body>
</html>
```

---

## Spec Reference

See `docs/superpowers/specs/2026-04-04-006-teacher-grades-lesson-quiz-design.md`.
