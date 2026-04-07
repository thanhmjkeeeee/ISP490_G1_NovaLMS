# Unified Quiz & Assignment Form — Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Consolidate all expert-side quiz and assignment creation into a single `quiz-create.html` page. A type dropdown (ENTRY_TEST, COURSE_QUIZ, MODULE_QUIZ, LESSON_QUIZ, COURSE_ASSIGNMENT, MODULE_ASSIGNMENT) drives all form behavior. Assignment types show a 1-page tabbed L/R/S/W layout instead of the old 6-step wizard. Student take-quiz forms remain unchanged — they already work correctly.

**Architecture:**
- `QuizCategory` enum replaces raw String category literals throughout the codebase
- `ExpertQuizService` absorbs all assignment-specific logic (skill tagging, publish validation)
- `ExpertQuizController` gains assignment endpoints (skills, preview, publish) as aliases for the existing `ExpertAssignmentController` endpoints (both use the same service)
- `question-bank-browser.html` is the single shared Thymeleaf fragment for bank UI
- `quiz-create.html` is the single creation page for all 6 types
- Old `assignment-create.html`, `assignment-skill-section.html`, `assignment-preview.html` are deleted

**Tech Stack:** Spring Boot, Thymeleaf, Bootstrap 5, Vanilla JS

---

## Chunk 1: Backend Foundation

### Task 1: Create `QuizCategory` enum

**Files:**
- Create: `src/main/java/com/example/DoAn/model/QuizCategory.java`
- Modify: `src/main/java/com/example/DoAn/model/Quiz.java` — add `getQuizCategoryEnum()` helper
- Modify: `src/main/java/com/example/DoAn/service/impl/ExpertQuizServiceImpl.java` — accept all 6 categories, set assignment fields
- Modify: `src/main/java/com/example/DoAn/service/impl/ExpertAssignmentServiceImpl.java` — use enum
- Modify: `src/main/java/com/example/DoAn/controller/ExpertAssignmentController.java` — use enum

- [ ] **Step 1: Create `QuizCategory.java`**

```java
// E:\workspace\ISP490_G1_NovaLMS\NovaLMS\DoAn\src\main\java\com\example\DoAn\model\QuizCategory.java
package com.example.DoAn.model;

public enum QuizCategory {
    ENTRY_TEST("ENTRY_TEST", "Bài kiểm tra đầu vào", false),
    COURSE_QUIZ("COURSE_QUIZ", "Bài kiểm tra khóa học", false),
    MODULE_QUIZ("MODULE_QUIZ", "Bài kiểm tra module", false),
    LESSON_QUIZ("LESSON_QUIZ", "Bài kiểm tra bài học", false),
    COURSE_ASSIGNMENT("COURSE_ASSIGNMENT", "Bài tập lớn khóa học", true),
    MODULE_ASSIGNMENT("MODULE_ASSIGNMENT", "Bài tập lớn module", true);

    private final String value;
    private final String label;
    private final boolean isAssignment;

    QuizCategory(String value, String label, boolean isAssignment) {
        this.value = value;
        this.label = label;
        this.isAssignment = isAssignment;
    }

    public String getValue()                          { return value; }
    public String getLabel()                          { return label; }
    public boolean isAssignment()                     { return isAssignment; }
    public boolean isQuiz()                           { return !isAssignment; }

    /** Fixed LRWS; null for non-assignment types */
    public java.util.List<String> getSkillOrder() {
        if (!isAssignment) return null;
        return java.util.Arrays.asList("LISTENING", "READING", "SPEAKING", "WRITING");
    }

    public static QuizCategory fromValue(String value) {
        if (value == null) return null;
        for (QuizCategory c : values()) {
            if (c.value.equals(value)) return c;
        }
        return null;
    }
}
```

- [ ] **Step 2: Add helper to `Quiz.java`**

Add inside `Quiz.java` after `getQuizCategory()`:
```java
public QuizCategory getQuizCategoryEnum() {
    return QuizCategory.fromValue(this.quizCategory);
}
```

- [ ] **Step 3: Update `ExpertQuizServiceImpl.java`**

Replace the `VALID_CATEGORIES` set:
```java
// OLD (line ~42):
private static final Set<String> VALID_CATEGORIES = Set.of("ENTRY_TEST", "COURSE_QUIZ", "MODULE_QUIZ", "LESSON_QUIZ");

// NEW:
private static final Set<String> VALID_CATEGORIES = Set.of(
    "ENTRY_TEST", "COURSE_QUIZ", "MODULE_QUIZ", "LESSON_QUIZ",
    "COURSE_ASSIGNMENT", "MODULE_ASSIGNMENT"
);
```

In `createQuiz()`, after setting `isHybridEnabled` (around line 65), add:
```java
// Set sequential + skill fields for assignment types
QuizCategory cat = QuizCategory.fromValue(request.getQuizCategory());
if (cat != null && cat.isAssignment()) {
    quiz.setIsSequential(true);
    quiz.setSkillOrder("[\"LISTENING\",\"READING\",\"SPEAKING\",\"WRITING\"]");
}
if (request.getTimeLimitPerSkill() != null) {
    quiz.setTimeLimitPerSkill(objectMapper.writeValueAsString(request.getTimeLimitPerSkill()));
}
```

Also add import:
```java
import com.example.DoAn.model.QuizCategory;
```

- [ ] **Step 4: Update `ExpertAssignmentServiceImpl.java`**

Add import:
```java
import com.example.DoAn.model.QuizCategory;
```

In `createAssignment()`, replace the category validation block:
```java
// OLD:
if (!"ROLE_EXPERT".equals(expert.getRole().getName())) {
    throw new InvalidDataException("Only experts can create assignments");
}
String category = dto.getQuizCategory();
if (!"COURSE_ASSIGNMENT".equals(category) && !"MODULE_ASSIGNMENT".equals(category)) {
    throw new InvalidDataException("Invalid category for assignment");
}

// NEW:
QuizCategory cat = QuizCategory.fromValue(dto.getQuizCategory());
if (cat == null || !cat.isAssignment()) {
    throw new InvalidDataException("Invalid category for assignment");
}
```

Replace `quiz.setSkillOrder(...)` block:
```java
// OLD:
quiz.setSkillOrder(objectMapper.writeValueAsString(SEQUENTIAL_SKILLS));

// NEW:
quiz.setSkillOrder(objectMapper.writeValueAsString(cat.getSkillOrder()));
```

- [ ] **Step 5: Update `ExpertAssignmentController.java`**

Add import:
```java
import com.example.DoAn.model.QuizCategory;
```

Replace all occurrences of `"COURSE_ASSIGNMENT"` → `QuizCategory.COURSE_ASSIGNMENT.getValue()` and `"MODULE_ASSIGNMENT"` → `QuizCategory.MODULE_ASSIGNMENT.getValue()`.

- [ ] **Step 6: Done** — QuizCategory enum created, applied to Quiz.java, ExpertQuizServiceImpl.java, ExpertAssignmentServiceImpl.java, ExpertAssignmentController.java

---

### Task 2: Extend `ExpertQuizService` to cover assignment operations (skill summaries, publish with validation)

**Files:**
- Modify: `src/main/java/com/example/DoAn/service/IExpertQuizService.java`
- Modify: `src/main/java/com/example/DoAn/service/impl/ExpertQuizServiceImpl.java`
- Modify: `src/main/java/com/example/DoAn/controller/ExpertQuizController.java`
- Modify: `src/main/java/com/example/DoAn/dto/request/QuizRequestDTO.java`

- [ ] **Step 1: Add `timeLimitPerSkill` to `QuizRequestDTO.java`**

Add field:
```java
private java.util.Map<String, Integer> timeLimitPerSkill;
```

Add getter and setter methods. The field represents per-skill time limits, e.g. `{"SPEAKING": 30, "WRITING": 45}`.

- [ ] **Step 2: Add assignment method signatures to `IExpertQuizService.java`**

Add these imports and method signatures:
```java
import java.util.Map;
import com.example.DoAn.dto.response.SkillSectionSummaryDTO;
import com.example.DoAn.dto.response.AssignmentPreviewDTO;
import com.example.DoAn.dto.request.AssignmentQuestionRequestDTO;

// Add inside the interface:
Map<String, SkillSectionSummaryDTO> getSkillSummaries(Integer quizId);
void addQuestionsToSection(Integer quizId, AssignmentQuestionRequestDTO dto, String email);
void removeQuestion(Integer quizId, Integer questionId);
QuizResponseDTO publishAssignment(Integer quizId);
AssignmentPreviewDTO getAssignmentPreview(Integer quizId);
```

- [ ] **Step 3: Implement assignment methods in `ExpertQuizServiceImpl.java`**

Add these methods to the service class (paste before the private helper section at the bottom):

```java
@Override
public Map<String, SkillSectionSummaryDTO> getSkillSummaries(Integer quizId) {
    if (!quizRepository.existsById(quizId)) {
        throw new ResourceNotFoundException("Quiz not found");
    }
    LinkedHashMap<String, SkillSectionSummaryDTO> result = new LinkedHashMap<>();
    List<String> skills = Arrays.asList("LISTENING", "READING", "SPEAKING", "WRITING");
    for (String skill : skills) {
        long count = quizQuestionRepository.countByQuizIdAndSkill(quizId, skill);
        result.put(skill, new SkillSectionSummaryDTO(skill, count, 0L,
            count > 0 ? "READY" : "DRAFT"));
    }
    return result;
}

@Override
public void addQuestionsToSection(Integer quizId, AssignmentQuestionRequestDTO dto, String email) {
    findExpert(email);
    Quiz quiz = findQuiz(quizId);
    if (!Boolean.TRUE.equals(quiz.getIsSequential())) {
        throw new InvalidDataException("This quiz does not support section-based question addition");
    }
    String skill = dto.getSkill();
    List<String> validSkills = Arrays.asList("LISTENING", "READING", "SPEAKING", "WRITING");
    if (!validSkills.contains(skill)) {
        throw new InvalidDataException("Invalid skill: " + skill);
    }
    List<QuizQuestion> existing = quizQuestionRepository.findByQuizQuizIdAndSkill(quizId, skill);
    Set<Integer> existingIds = new HashSet<>();
    for (QuizQuestion qq : existing) existingIds.add(qq.getQuestion().getQuestionId());
    int nextOrder = existing.size() + 1;
    for (Integer questionId : dto.getQuestionIds()) {
        if (existingIds.contains(questionId)) continue;
        Question question = questionRepository.findById(questionId)
            .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));
        QuizQuestion qq = QuizQuestion.builder()
            .quiz(quiz).question(question).skill(skill)
            .orderIndex(nextOrder++).points(BigDecimal.ONE)
            .build();
        quizQuestionRepository.save(qq);
    }
}

@Override
public void removeQuestion(Integer quizId, Integer questionId) {
    quizQuestionRepository.findByQuizQuizIdAndQuestionQuestionId(quizId, questionId)
        .ifPresent(quizQuestionRepository::delete);
}

@Override
public QuizResponseDTO publishAssignment(Integer quizId) {
    Quiz quiz = findQuiz(quizId);
    if (!"DRAFT".equals(quiz.getStatus())) {
        throw new InvalidDataException("Only DRAFT quizzes can be published");
    }
    Map<String, SkillSectionSummaryDTO> summaries = getSkillSummaries(quizId);
    List<String> missing = new ArrayList<>();
    for (SkillSectionSummaryDTO s : summaries.values()) {
        if (s.getQuestionCount() == 0) missing.add(s.getSkill());
    }
    if (!missing.isEmpty()) {
        throw new InvalidDataException("Missing questions for skills: " + String.join(", ", missing));
    }
    quiz.setStatus("PUBLISHED");
    quiz.setIsOpen(false);
    quizRepository.save(quiz);
    return toResponseDTO(quiz);
}

@Override
public AssignmentPreviewDTO getAssignmentPreview(Integer quizId) {
    Quiz quiz = findQuiz(quizId);
    Map<String, SkillSectionSummaryDTO> summaries = getSkillSummaries(quizId);
    List<String> missing = new ArrayList<>();
    long total = 0;
    for (SkillSectionSummaryDTO s : summaries.values()) {
        if (s.getQuestionCount() == 0) missing.add(s.getSkill());
        total += s.getQuestionCount();
    }
    Map<String, Integer> timeLimits = null;
    if (quiz.getTimeLimitPerSkill() != null) {
        try {
            timeLimits = objectMapper.readValue(quiz.getTimeLimitPerSkill(),
                new TypeReference<Map<String, Integer>>() {});
        } catch (Exception ignored) {}
    }
    return new AssignmentPreviewDTO(
        quiz.getQuizId(), quiz.getTitle(), quiz.getDescription(), quiz.getQuizCategory(),
        new ArrayList<>(summaries.values()), total,
        quiz.getPassScore() != null ? quiz.getPassScore() : BigDecimal.ZERO,
        timeLimits, quiz.getPassScore(), quiz.getMaxAttempts(),
        quiz.getShowAnswerAfterSubmit(), missing, missing.isEmpty()
    );
}
```

Add these imports at the top if not already present:
```java
import com.example.DoAn.dto.request.AssignmentQuestionRequestDTO;
import com.example.DoAn.dto.response.AssignmentPreviewDTO;
import com.example.DoAn.dto.response.SkillSectionSummaryDTO;
import com.fasterxml.jackson.core.type.TypeReference;
```

- [ ] **Step 4: Add assignment endpoints to `ExpertQuizController.java`**

Add these new endpoints to `ExpertQuizController.java` (paste inside the class, before the closing brace):

```java
// GET /api/v1/expert/quizzes/{quizId}/skills — per-skill question counts
@GetMapping("/{quizId}/skills")
public ResponseData<Map<String, SkillSectionSummaryDTO>> getSkillSummaries(@PathVariable Integer quizId) {
    return ResponseData.success(quizService.getSkillSummaries(quizId));
}

// POST /api/v1/expert/quizzes/{quizId}/questions — add to skill section (assignment mode)
@PostMapping("/{quizId}/questions")
public ResponseData<Void> addQuestionsToSection(
        @PathVariable Integer quizId,
        @RequestBody AssignmentQuestionRequestDTO dto,
        @AuthenticationPrincipal String email) {
    quizService.addQuestionsToSection(quizId, dto, email);
    return ResponseData.success(null);
}

// DELETE /api/v1/expert/quizzes/{quizId}/questions/{questionId}
@DeleteMapping("/{quizId}/questions/{questionId}")
public ResponseData<Void> removeQuestion(
        @PathVariable Integer quizId,
        @PathVariable Integer questionId) {
    quizService.removeQuestion(quizId, questionId);
    return ResponseData.success(null);
}

// GET /api/v1/expert/quizzes/{quizId}/preview — assignment preview
@GetMapping("/{quizId}/preview")
public ResponseData<AssignmentPreviewDTO> getPreview(@PathVariable Integer quizId) {
    return ResponseData.success(quizService.getAssignmentPreview(quizId));
}

// PATCH /api/v1/expert/quizzes/{quizId}/publish — publish assignment
@PatchMapping("/{quizId}/publish")
public ResponseData<QuizResponseDTO> publishAssignment(@PathVariable Integer quizId) {
    return ResponseData.success(quizService.publishAssignment(quizId));
}
```

Add required imports:
```java
import java.util.Map;
import com.example.DoAn.dto.request.AssignmentQuestionRequestDTO;
import com.example.DoAn.dto.response.AssignmentPreviewDTO;
import com.example.DoAn.dto.response.SkillSectionSummaryDTO;
```

- [ ] **Step 5: Done** — ExpertQuizService extended with assignment operations; ExpertQuizController updated with new endpoints

---

## Chunk 2: Unified Creation Form & Shared Fragment

### Task 3: Create `question-bank-browser.html` shared fragment

**Files:**
- Create: `src/main/resources/templates/fragments/question-bank-browser.html`

- [ ] **Step 1: Create the fragment file**

```html
<!-- E:\workspace\ISP490_G1_NovaLMS\NovaLMS\DoAn\src\main\resources\templates\fragments\question-bank-browser.html -->
<th:block th:fragment="question-bank-browser(mode, skillFilter)"
          th:with="mode=${mode}, skillFilter=${skillFilter}">

<!-- Filter row -->
<div class="bank-filters row g-2 mb-3">
    <!-- Skill filter: only shown in assignment mode when no pre-filter -->
    <div class="col-md-4" th:if="${mode == 'assignment' and skillFilter == null}">
        <select id="bankSkill" class="form-select form-select-sm" onchange="bankLoad(0)">
            <option value="">-- Tất cả Kỹ năng --</option>
            <option value="LISTENING">Listening</option>
            <option value="READING">Reading</option>
            <option value="WRITING">Writing</option>
            <option value="SPEAKING">Speaking</option>
        </select>
    </div>
    <!-- Hidden input for pre-filtered mode -->
    <input type="hidden" id="bankSkill" th:value="${skillFilter}"
           th:if="${mode == 'assignment' and skillFilter != null}">

    <div class="col-md-3">
        <select id="bankCefr" class="form-select form-select-sm" onchange="bankLoad(0)">
            <option value="">-- CEFR --</option>
            <option value="A1">A1</option><option value="A2">A2</option>
            <option value="B1">B1</option><option value="B2">B2</option>
            <option value="C1">C1</option><option value="C2">C2</option>
        </select>
    </div>
    <div class="col-md-5">
        <input type="text" id="bankKeyword" class="form-control form-control-sm"
               placeholder="Tìm kiếm..." onkeyup="if(event.key==='Enter') bankLoad(0)">
    </div>
    <div class="col-md-1">
        <button class="btn btn-sm btn-primary w-100" onclick="bankLoad(0)">
            <i class="bi bi-search"></i>
        </button>
    </div>
</div>

<!-- Question list table -->
<div class="border rounded bg-white" style="max-height:380px;overflow-y:auto;">
    <table class="table table-hover align-middle mb-0" style="font-size:0.85rem;">
        <thead class="table-light position-sticky top-0">
            <tr>
                <th style="width:50%">Nội dung</th>
                <th>Loại</th>
                <th>CEFR</th>
                <th class="text-end pe-3">Hành động</th>
            </tr>
        </thead>
        <tbody id="bankQuestionsBody">
            <tr><td colspan="4" class="text-center py-5 text-muted">Đang tải...</td></tr>
        </tbody>
    </table>
</div>

<!-- Pagination -->
<nav class="mt-2">
    <ul class="pagination pagination-sm justify-content-center mb-0" id="bankPagination"></ul>
</nav>

</th:block>
```

- [ ] **Step 2: Done** — question-bank-browser.html fragment created

---

### Task 4: Rewrite `quiz-create.html` — unified creation page

**Files:**
- Modify: `src/main/resources/templates/expert/quiz-create.html` (replace entire file)
- Create: `src/main/resources/static/assets/expert/js/quiz-create.js`

- [ ] **Step 1: Create `quiz-create.js`**

```javascript
// E:\workspace\ISP490_G1_NovaLMS\NovaLMS\DoAn\src\main\resources\static\assets\expert\js\quiz-create.js

// ── State ────────────────────────────────────────────────────────────────────
let _quizId = null;
let _currentBankPage = 0;
let _quizQuestions = [];
let _currentMode = 'quiz';    // 'quiz' | 'assignment'
let _expandedGroups = new Set();
let _lastBankResult = null;
let optionCount = 0;

// ── Bank browser (shared) ────────────────────────────────────────────────────
function bankInit(mode) {
    _currentMode = mode;
    bankLoad(0);
    loadQuizQuestions();
}

async function bankLoad(page) {
    _currentBankPage = page;
    const params = new URLSearchParams();
    const skill = document.getElementById('bankSkill')?.value || '';
    const cefr  = document.getElementById('bankCefr')?.value || '';
    const kw    = document.getElementById('bankKeyword')?.value || '';
    if (skill) params.append('skill', skill);
    if (cefr)  params.append('cefrLevel', cefr);
    if (kw)    params.append('keyword', kw);
    params.append('status', 'PUBLISHED');
    params.append('page', page);
    params.append('size', 10);
    try {
        const res = await fetch(`/api/v1/expert/question-bank?${params}`);
        _lastBankResult = await res.json();
        bankRender(_lastBankResult);
    } catch(e) { console.error(e); }
}

function bankRender(result) {
    const tbody = document.getElementById('bankQuestionsBody');
    if (!result || result.status !== 200 || !result.data?.items?.length) {
        tbody.innerHTML = '<tr><td colspan="4" class="text-center py-5 text-muted">Không tìm thấy câu hỏi.</td></tr>';
        if (document.getElementById('bankPagination')) document.getElementById('bankPagination').innerHTML = '';
        return;
    }
    const existingIds = new Set(_quizQuestions.map(q => q.questionId));
    tbody.innerHTML = result.data.items.map(q => {
        const added = existingIds.has(q.id);
        return `<tr>
            <td class="ps-3">
                <div class="small text-truncate" style="max-width:260px" title="${q.content||''}">${(q.content||'').substring(0,80)}${(q.content||'').length>80?'…':''}</div>
                <div class="extra-small text-muted">${q.skill||'—'} • ${typeName(q.questionType)}</div>
            </td>
            <td><span class="badge bg-light text-dark">${typeName(q.questionType)}</span></td>
            <td><span class="badge bg-info text-dark">${q.cefrLevel||''}</span></td>
            <td class="text-end pe-3">
                ${added
                    ? '<span class="badge bg-success rounded-pill px-3"><i class="bi bi-check-lg"></i> Đã thêm</span>'
                    : `<button class="btn btn-sm btn-primary rounded-pill px-3" onclick="bankAdd(${q.id},'${q.type||'SINGLE'}')">
                        <i class="bi bi-plus-lg"></i> Thêm
                       </button>`
                }
            </td>
        </tr>`;
    }).join('');

    const pg = document.getElementById('bankPagination');
    if (pg) {
        let html = '';
        for (let i = 0; i < result.data.totalPages; i++) {
            html += `<li class="page-item ${i===_currentBankPage?'active':''}">
                <a class="page-link" href="javascript:void(0)" onclick="bankLoad(${i})">${i+1}</a>
            </li>`;
        }
        pg.innerHTML = html;
    }
}

async function bankAdd(id, type) {
    const payload = { questionId: id, itemType: type, points: 1.0 };
    if (_currentMode === 'assignment') {
        const skill = document.getElementById('bankSkill')?.value;
        if (skill) payload.skill = skill;
    }
    try {
        const res = await fetch(`/api/v1/expert/quizzes/${_quizId}/questions`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        if (res.ok) { loadQuizQuestions(); }
        else { const r = await res.json(); alert(r.message || 'Không thể thêm.'); }
    } catch(e) { alert('Lỗi: ' + e.message); }
}

// ── Quiz questions ────────────────────────────────────────────────────────────
async function loadQuizQuestions() {
    if (!_quizId) return;
    try {
        const res = await fetch(`/api/v1/expert/quizzes/${_quizId}`);
        const result = await res.json();
        if (result.status === 200 && result.data) {
            _quizQuestions = result.data.questions || [];
            renderAddedQuestions();
            bankRender(_lastBankResult);
        }
    } catch(e) { console.error(e); }
}

function renderAddedQuestions() {
    const container = document.getElementById('addedQuestionsList');
    const badge = document.getElementById('currentCountBadge');
    if (!container) return;

    const groups = new Map(), lones = [];
    _quizQuestions.forEach(q => {
        if (q.groupId) {
            if (!groups.has(q.groupId)) groups.set(q.groupId, { id: q.groupId, content: q.groupContent, items: [] });
            groups.get(q.groupId).items.push(q);
        } else { lones.push(q); }
    });
    const total = groups.size + lones.length;
    if (badge) badge.textContent = `${total} câu`;

    if (total === 0) {
        container.innerHTML = `<div class="text-center py-5 text-muted"><i class="bi bi-inbox fs-1 d-block mb-2"></i>Chưa có câu hỏi nào.</div>`;
        return;
    }

    let html = '';
    groups.forEach(g => {
        const expanded = _expandedGroups.has(g.id);
        html += `<div class="card mb-3 border" style="border-left:5px solid #0d6efd !important;border-radius:12px">
            <div class="card-header bg-white d-flex justify-content-between align-items-center py-2">
                <div class="d-flex align-items-center gap-2">
                    <button class="btn btn-sm btn-light rounded-circle p-0" style="width:24px;height:24px" onclick="toggleGroup(${g.id})">
                        <i class="bi ${expanded?'bi-dash':'bi-plus'}"></i>
                    </button>
                    <span class="small fw-bold text-primary"><i class="bi bi-journal-text me-1"></i>PASSAGE</span>
                </div>
                <button class="btn btn-sm btn-link text-danger p-0" onclick="removeGroup(${g.id})"><i class="bi bi-trash"></i> Gỡ</button>
            </div>
            <div class="card-body pt-0">
                <div class="small text-muted fst-italic border-bottom pb-2 mb-2">${g.content||'—'}</div>
                <div class="${expanded?'':'d-none'}">
                    ${g.items.map((sq,i) => `
                        <div class="d-flex justify-content-between align-items-center mb-2">
                            <div class="small"><span class="badge bg-light text-dark me-1">${i+1}</span> ${sq.questionContent}</div>
                            <button class="btn btn-sm btn-outline-danger border-0" onclick="removeQuestion(${sq.questionId})"><i class="bi bi-trash"></i></button>
                        </div>`).join('')}
                </div>
            </div>
        </div>`;
    });

    lones.forEach((q, i) => {
        html += `<div class="card mb-2 border shadow-none" style="border-radius:12px">
            <div class="card-body p-3 d-flex justify-content-between align-items-start">
                <div>
                    <div class="small fw-bold text-success mb-1">CÂU LẺ • ${typeName(q.questionType)}</div>
                    <div class="small text-truncate" style="max-width:380px">${q.questionContent||'—'}</div>
                </div>
                <button class="btn btn-sm btn-outline-danger border-0" onclick="removeQuestion(${q.questionId})"><i class="bi bi-trash"></i></button>
            </div>
        </div>`;
    });
    container.innerHTML = html;
}

function toggleGroup(id) {
    _expandedGroups.has(id) ? _expandedGroups.delete(id) : _expandedGroups.add(id);
    renderAddedQuestions();
}

async function removeQuestion(questionId) {
    if (!confirm('Gỡ câu hỏi này?')) return;
    try {
        const res = await fetch(`/api/v1/expert/quizzes/${_quizId}/questions/${questionId}`, { method: 'DELETE' });
        if (res.ok) loadQuizQuestions();
    } catch(e) { alert('Lỗi: ' + e.message); }
}

async function removeGroup(groupId) {
    if (!confirm('Gỡ toàn bộ Passage?')) return;
    try {
        const res = await fetch(`/api/v1/expert/quizzes/${_quizId}/groups/${groupId}`, { method: 'DELETE' });
        if (res.ok) loadQuizQuestions();
    } catch(e) { alert('Lỗi: ' + e.message); }
}

// ── Inline question creation (for assignment skill sections) ──────────────────
function initInlineQuestionForm(skill) {
    const form = document.getElementById('inlineQuestionForm');
    if (!form) return;
    const qType = document.getElementById('questionType');
    const mcDiv = document.getElementById('mcOptions');

    qType?.addEventListener('change', () => {
        const isMC = qType.value === 'MULTIPLE_CHOICE_SINGLE' || qType.value === 'MULTIPLE_CHOICE_MULTI';
        if (mcDiv) mcDiv.style.display = isMC ? 'block' : 'none';
        if (isMC && optionCount === 0) { addOption(); addOption(); addOption(); addOption(); }
    });

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const fd = new FormData(form);
        const isMC = qType.value === 'MULTIPLE_CHOICE_SINGLE' || qType.value === 'MULTIPLE_CHOICE_MULTI';
        const isMulti = qType.value === 'MULTIPLE_CHOICE_MULTI';

        let options = [];
        if (isMC) {
            const correctMap = {};
            const sel = isMulti ? '[name=correctMulti]:checked' : '[name=correctSingle]:checked';
            document.querySelectorAll(sel).forEach(r => { correctMap[r.value] = true; });
            document.querySelectorAll('.option-text').forEach((inp, i) => {
                options.push({ title: inp.value, correct: !!correctMap[inp.dataset.idx], orderIndex: i });
            });
        }

        const payload = {
            content: fd.get('content'),
            questionType: qType.value,
            skill: skill,
            cefrLevel: fd.get('cefrLevel'),
            explanation: fd.get('explanation') || null,
            status: 'PUBLISHED',
            source: 'EXPERT_BANK',
            options: isMC ? options : []
        };

        const resp = await fetch('/api/v1/expert/question-bank', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const data = await resp.json();
        if (resp.ok && data.data?.questionId) {
            await fetch(`/api/v1/expert/quizzes/${_quizId}/questions`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ questionIds: [data.data.questionId], skill: skill, itemType: 'SINGLE' })
            });
            form.reset();
            optionCount = 0;
            if (document.getElementById('optionsList')) document.getElementById('optionsList').innerHTML = '';
            loadQuizQuestions();
        } else {
            alert('Lỗi: ' + (data.message || 'Không thể tạo câu hỏi'));
        }
    });
}

function addOption() {
    if (!document.getElementById('optionsList')) return;
    const qType = document.getElementById('questionType');
    const isMulti = qType?.value === 'MULTIPLE_CHOICE_MULTI';
    const idx = optionCount++;
    document.getElementById('optionsList').insertAdjacentHTML('beforeend', `
        <div class="input-group input-group-sm mb-1">
            <div class="input-group-text">
                <input class="form-check-input m-0 correct-check" type="${isMulti?'checkbox':'radio'}"
                       name="${isMulti?'correctMulti':'correctSingle'}" value="${idx}">
            </div>
            <input type="text" class="form-control option-text" placeholder="Đáp án ${idx+1}" data-idx="${idx}">
            <button type="button" class="btn btn-outline-danger" onclick="this.parentElement.remove()"><i class="bi bi-x"></i></button>
        </div>`);
}

function typeName(type) {
    return ({
        MULTIPLE_CHOICE_SINGLE:'Trắc nghiệm (1)',
        MULTIPLE_CHOICE_MULTI:'Trắc nghiệm (n)',
        FILL_IN_BLANK:'Điền từ', MATCHING:'Nối đáp án',
        WRITING:'Viết', SPEAKING:'Nói'
    })[type] || type || '';
}
```

- [ ] **Step 2: Rewrite `quiz-create.html`** (replace entire file content)

The new `quiz-create.html` combines all functionality. It uses the existing stepper CSS (kept from the current file) and adds:

1. **Type dropdown** with all 6 options at the top of step 1
2. **Conditional sections** (course selector, module selector, assignment config) driven by JS `onCategoryChange()`
3. **Step 2A (quiz flow)** — flat question selection using the shared fragment
4. **Step 2B (assignment flow)** — 1-page tabbed L/R/S/W, each tab has the bank browser fragment (pre-filtered to that skill) + inline question creation + skill question list
5. **Step 3 (preview/publish)** — works for both flows

Key JS behaviors:
- `onCategoryChange()` — toggles visibility of all conditional sections based on selected type
- `goToStep(n)` — for quiz flow, uses the 3-step stepper; for assignment flow, step 2 shows the tabbed section
- `submitQuiz()` — POST to `/api/v1/expert/quizzes`, stores returned `_quizId`
- `bankInit('quiz')` or `bankInit('assignment')` — initializes the bank browser in appropriate mode
- Assignment tabs each call `bankInit('assignment')` when shown (use Bootstrap tab `shown.bs.tab` event)

The template should include:
- All existing CSS from the current `quiz-create.html` (stepper styles)
- The full form structure described above
- Import `quiz-create.js` via `<script th:src="@{/assets/expert/js/quiz-create.js}"></script>`
- Thymeleaf: loop over `{'LISTENING','READING','SPEAKING','WRITING'}` for assignment tabs using `th:each`

- [ ] **Step 3: Done** — quiz-create.html rewritten, quiz-create.js created

---

### Task 5: Merge assignment list into quiz list; clean up old routes/templates

**Files:**
- Modify: `src/main/java/com/example/DoAn/controller/ExpertViewController.java`
- Modify: `src/main/resources/templates/expert/quiz-list.html`
- Delete: `src/main/resources/templates/expert/assignment-create.html`
- Delete: `src/main/resources/templates/expert/assignment-skill-section.html`
- Delete: `src/main/resources/templates/expert/assignment-preview.html`

- [ ] **Step 1: Update `ExpertViewController.java`**

In `quizCreatePage`, add `quizId` attribute for edit mode:
```java
@GetMapping("/quiz-management/create")
public String quizCreatePage(
        @RequestParam(required = false) Integer moduleId,
        @RequestParam(required = false) Integer quizId,
        Model model) {
    model.addAttribute("isDashboard", true);
    model.addAttribute("moduleId", moduleId);
    model.addAttribute("quizId", quizId);
    return "expert/quiz-create";
}
```

Delete these three route methods (replaced by `quiz-create`):
- `GetMapping("/assignment/create")` — delete entirely
- `GetMapping("/assignment/{quizId}/skill/{skill}")` — delete entirely
- `GetMapping("/assignment/{quizId}/preview")` — delete entirely

Replace `assignmentManagement()` with a redirect so `/expert/assignment-management` goes to the unified quiz list:
```java
@GetMapping("/assignment-management")
public String redirectAssignmentManagement() {
    return "redirect:/expert/quiz-management";
}
```

Delete the `WIZARD_STEPS` constant if it's only used by the deleted routes.

- [ ] **Step 2: Merge assignment-list into `quiz-list.html`**

1. **Header title** — change to "Quản lý Quiz & Bài tập lớn"
2. **Create button** — single button linking to the unified create page. The type dropdown on that page determines whether it's a quiz or assignment.
   ```html
   <a th:href="@{/expert/quiz-management/create}"
      class="btn btn-sm fw-bold shadow-sm d-flex align-items-center rounded-pill px-3 text-white"
      style="background-color:#136ad5;">
       <i class="bi bi-plus-lg me-1"></i> Tạo Quiz / Bài tập lớn
   </a>
   ```
3. **Category filter** — extend to all 6 types:
   ```html
   <select id="filterCategory" class="form-select form-select-sm" onchange="loadQuizzes()">
       <option value="">Tất cả</option>
       <option value="ENTRY_TEST">Entry Test</option>
       <option value="COURSE_QUIZ">Bài KT Khóa học</option>
       <option value="MODULE_QUIZ">Bài KT Module</option>
       <option value="LESSON_QUIZ">Bài KT Bài học</option>
       <option value="COURSE_ASSIGNMENT">BTL Khóa học</option>
       <option value="MODULE_ASSIGNMENT">BTL Module</option>
   </select>
   ```
4. **`categoryBadge()`** — update to handle all 6:
   ```javascript
   const categoryBadge = (c) => {
       const map = {
           ENTRY_TEST:'bg-info text-dark', COURSE_QUIZ:'bg-primary',
           MODULE_QUIZ:'bg-secondary', LESSON_QUIZ:'bg-dark',
           COURSE_ASSIGNMENT:'bg-warning text-dark', MODULE_ASSIGNMENT:'bg-success'
       };
       const labels = {
           ENTRY_TEST:'Entry Test', COURSE_QUIZ:'Bài KT Khóa học',
           MODULE_QUIZ:'Bài KT Module', LESSON_QUIZ:'Bài KT Bài học',
           COURSE_ASSIGNMENT:'BTL Khóa học', MODULE_ASSIGNMENT:'BTL Module'
       };
       return `<span class="badge ${map[c]||'bg-light'}">${labels[c]||c}</span>`;
   };
   ```
5. **Module/Lesson column** — add a new "Phạm vi" column after "Khóa học" that shows `q.moduleName || q.lessonName || '—'`
6. **Action column** — For ASSIGNMENT types, link to `quiz-create` edit page (same as quiz — both types use the unified form)

- [ ] **Step 3: Done** — ExpertViewController updated, quiz-list.html merged with assignment list, old assignment creation templates deleted

---

## Chunk 3: Verification

### Task 6: End-to-end verification

- [ ] **Step 1: Build the project**

Run: `./mvnw compile` (or `mvn compile`) from `NovaLMS/DoAn`
Expected: Compiles without errors

- [ ] **Step 2: Start the application**

Run: `./mvnw spring-boot:run` (or start from IDE)
Expected: Application starts on port 8080

- [ ] **Step 3: Smoke test all 6 types**

1. Visit `http://localhost:8080/expert/quiz-management/create`
   - [ ] Type dropdown shows all 6 options
   - [ ] Selecting COURSE_ASSIGNMENT shows 4 per-skill time inputs
   - [ ] Selecting COURSE_QUIZ shows course selector
   - [ ] Selecting MODULE_ASSIGNMENT shows module selector
2. Create a COURSE_ASSIGNMENT, navigate through L/R/S/W tabs, add questions, publish
   - [ ] Publish succeeds only when all 4 skills have ≥1 question
   - [ ] Publish fails with helpful error if a skill is empty
3. Create a COURSE_QUIZ, add questions, publish
   - [ ] Publish succeeds with ≥1 question
4. Visit `http://localhost:8080/expert/quiz-management` — existing quizzes and assignments still listed correctly

- [ ] **Step 4: Done** — All 6 types verified, implementation complete

---

## Summary: Files Created / Modified / Deleted

| Action | File |
|---|---|
| CREATE | `model/QuizCategory.java` |
| CREATE | `templates/fragments/question-bank-browser.html` |
| CREATE | `static/assets/expert/js/quiz-create.js` |
| MODIFY | `model/Quiz.java` |
| MODIFY | `service/IExpertQuizService.java` |
| MODIFY | `service/impl/ExpertQuizServiceImpl.java` |
| MODIFY | `service/impl/ExpertAssignmentServiceImpl.java` |
| MODIFY | `controller/ExpertQuizController.java` |
| MODIFY | `controller/ExpertAssignmentController.java` |
| MODIFY | `controller/ExpertViewController.java` |
| MODIFY | `dto/request/QuizRequestDTO.java` |
| REPLACE | `templates/expert/quiz-create.html` |
| MODIFY | `templates/expert/quiz-list.html` (merged assignment list into quiz list) |
| DELETE | `templates/expert/assignment-create.html` |
| DELETE | `templates/expert/assignment-skill-section.html` |
| DELETE | `templates/expert/assignment-preview.html` |
| (keep) | `templates/expert/assignment-list.html` (redirects to quiz-list, kept for compatibility) |
| (keep) | `student/assignment-*` templates (already working) |
