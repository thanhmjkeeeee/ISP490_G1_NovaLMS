# Plan 002 — Teacher Creates Lesson Quiz

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Teacher creates a Lesson Quiz (N skills, any combination) via a 3-step wizard: config → content builder (bank + inline creation) → finish/publish.

**Architecture:** Most endpoints already exist (`TeacherQuizService`, `TeacherQuizApiController`). This plan focuses on: (1) adding `getSkillSummary` to the service, (2) extending the 2-step HTML wizard to 3 steps, (3) adding the content builder page, (4) adding a PENDING_REVIEW badge to the builder sidebar.

**Tech Stack:** Spring Boot 3.3.4, Thymeleaf, existing API endpoints.

---

## File Structure

```
src/main/java/com/example/DoAn/
├── service/impl/
│   └── TeacherQuizServiceImpl.java       MODIFY (add getSkillSummary)
├── dto/response/
│   └── QuizSkillSummaryDTO.java           CREATE
└── views/templates/
    └── teacher/
        ├── quiz-create.html              MODIFY (Step 1: keep config, change redirects)
        ├── quiz-build.html              CREATE (Step 2: content builder)
        ├── quiz-finish.html             CREATE (Step 3: finish + publish)
        └── fragments/
            └── quiz-builder-sidebar.html CREATE (question summary sidebar)
```

---

## Chunk 1: Service Extension

### Task 1: Read existing TeacherQuizServiceImpl

**Files:**
- Read: `src/main/java/com/example/DoAn/service/impl/TeacherQuizServiceImpl.java`

- [ ] **Step 1: Read the entire service file**

Run: Read `TeacherQuizServiceImpl.java` completely. Note the existing methods: `createLessonQuiz`, `publishQuiz`, `addQuestionToQuiz`, `getQuizzes`.

### Task 2: Create `QuizSkillSummaryDTO`

**Files:**
- Create: `src/main/java/com/example/DoAn/dto/response/QuizSkillSummaryDTO.java`

- [ ] **Step 1: Write the DTO**

```java
package com.example.DoAn.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizSkillSummaryDTO {
    private String skill;
    private long totalCount;
    private long publishedCount;
    private long pendingCount;
    private long totalPoints;
}
```

### Task 3: Add `getSkillSummary` to TeacherQuizServiceImpl

**Files:**
- Modify: `src/main/java/com/example/DoAn/service/impl/TeacherQuizServiceImpl.java`

- [ ] **Step 1: Add the method after existing methods**

Find a good location (after `publishQuiz` method). Add:

```java
@Transactional(readOnly = true)
public List<QuizSkillSummaryDTO> getSkillSummary(Integer quizId) {
    List<QuizQuestion> questions = quizQuestionRepository.findByQuizQuizId(quizId);
    Map<String, QuizSkillSummaryDTO> map = new LinkedHashMap<>();

    for (QuizQuestion qq : questions) {
        Question q = qq.getQuestion();
        String skill = q.getSkill() != null ? q.getSkill() : "OTHER";
        map.computeIfAbsent(skill, s -> new QuizSkillSummaryDTO(s, 0, 0, 0, 0));

        QuizSkillSummaryDTO dto = map.get(skill);
        dto.setTotalCount(dto.getTotalCount() + 1);
        if ("PUBLISHED".equals(q.getStatus())) {
            dto.setPublishedCount(dto.getPublishedCount() + 1);
        } else if ("PENDING_REVIEW".equals(q.getStatus())) {
            dto.setPendingCount(dto.getPendingCount() + 1);
        }
        dto.setTotalPoints(dto.getTotalPoints() + qq.getPoints().intValue());
    }

    return new ArrayList<>(map.values());
}
```

Add imports if not already present:

```java
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
```

- [ ] **Step 2: Verify quizQuestionRepository has `findByQuizQuizId`**

Run: Read `QuizQuestionRepository.java`. If `findByQuizQuizId(Integer quizId)` doesn't exist, add it:

```java
List<QuizQuestion> findByQuizQuizId(Integer quizId);
```

---

## Chunk 2: Teacher View Controller — add new routes

### Task 4: Add quiz build and finish routes to TeacherViewController

**Files:**
- Modify: `src/main/java/com/example/DoAn/controller/TeacherViewController.java`

- [ ] **Step 1: Read the controller**

Run: Find and read `TeacherViewController.java` (or wherever teacher pages are routed).

- [ ] **Step 2: Add new routes**

Add after existing teacher routes:

```java
@GetMapping("/teacher/quiz/{quizId}/build")
public String quizBuildPage(@PathVariable Integer quizId, Model model) {
    model.addAttribute("quizId", quizId);
    return "teacher/quiz-build";
}

@GetMapping("/teacher/quiz/{quizId}/finish")
public String quizFinishPage(@PathVariable Integer quizId, Model model) {
    model.addAttribute("quizId", quizId);
    return "teacher/quiz-finish";
}
```

---

## Chunk 3: Extend quiz-create.html (Step 1 — config)

### Task 5: Modify teacher/quiz-create.html

**Files:**
- Modify: `src/main/resources/templates/teacher/quiz-create.html`

- [ ] **Step 1: Read the existing template**

Run: Read `teacher/quiz-create.html` completely.

- [ ] **Step 2: Change the form submit to redirect to Step 2 (build page)**

Find the `<form>` submit handler in JavaScript and change it to redirect to the build page instead of submitting directly:

```javascript
// Replace existing form submit logic with:
document.getElementById('quizForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const form = e.target;
    const formData = new FormData(form);
    const body = {
        title: formData.get('title'),
        description: formData.get('description') || '',
        quizCategory: 'LESSON_QUIZ',
        lessonId: parseInt(formData.get('lessonId')),
        classId: parseInt(formData.get('classId')),
        timeLimitMinutes: formData.get('timeLimitMinutes') || null,
        passScore: formData.get('passScore') || null,
        maxAttempts: formData.get('maxAttempts') || null,
        questionOrder: formData.get('questionOrder') || 'FIXED',
        showAnswerAfterSubmit: form.querySelector('[name=showAnswerAfterSubmit]')?.checked ?? true
    };

    const resp = await fetch(`/api/v1/teacher/lessons/${formData.get('lessonId')}/quizzes`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
    });
    const data = await resp.json();
    if (data.success) {
        window.location.href = `/teacher/quiz/${data.data}/build`;
    } else {
        alert('Lỗi: ' + data.message);
    }
});
```

---

## Chunk 4: Create quiz-build.html (Step 2 — content builder)

### Task 6: Create teacher/quiz-build.html

**Files:**
- Create: `src/main/resources/templates/teacher/quiz-build.html`

- [ ] **Step 1: Write the content builder template**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      th:replace="~{fragments/layout :: layout(~{::title}, ~{::#content})}">
<head>
    <title>Xây dựng nội dung quiz</title>
</head>
<body>
<div id="content" class="container-fluid mt-4">
    <div class="row">

        <!-- Left: Question Bank Browser -->
        <div class="col-md-8">
            <h4>Xây dựng nội dung bài quiz</h4>

            <!-- Filters -->
            <div class="card mb-3">
                <div class="card-body">
                    <div class="row g-2">
                        <div class="col-md-3">
                            <select id="filterSkill" class="form-select">
                                <option value="">Kỹ năng</option>
                                <option value="LISTENING">LISTENING</option>
                                <option value="READING">READING</option>
                                <option value="WRITING">WRITING</option>
                                <option value="SPEAKING">SPEAKING</option>
                            </select>
                        </div>
                        <div class="col-md-2">
                            <select id="filterCefr" class="form-select">
                                <option value="">CEFR</option>
                                <option value="A1">A1</option>
                                <option value="A2">A2</option>
                                <option value="B1">B1</option>
                                <option value="B2">B2</option>
                                <option value="C1">C1</option>
                                <option value="C2">C2</option>
                            </select>
                        </div>
                        <div class="col-md-4">
                            <input type="text" id="filterKeyword" class="form-control"
                                   placeholder="Tìm câu hỏi...">
                        </div>
                        <div class="col-md-2">
                            <button class="btn btn-primary w-100" onclick="loadBank(0)">Tìm</button>
                        </div>
                        <div class="col-md-1">
                            <button class="btn btn-success w-100" onclick="openCreateModal()"
                                    title="Tạo câu hỏi mới">
                                <span>+</span>
                            </button>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Bank Questions -->
            <div id="bankContainer">
                <!-- Loaded via JS -->
            </div>

            <nav id="bankPagination" class="mt-3" style="display:none">
                <ul class="pagination justify-content-center" id="bankPaginationList"></ul>
            </nav>
        </div>

        <!-- Right: Sidebar -->
        <div class="col-md-4">
            <div class="card sticky-top" style="top:1rem">
                <div class="card-header fw-bold">📋 Câu hỏi đã thêm</div>
                <div class="card-body">
                    <div id="quizSummary">
                        <div class="text-center text-muted">Chưa có câu hỏi</div>
                    </div>
                    <hr>
                    <a th:href="@{/teacher/quizzes}" class="btn btn-outline-secondary btn-sm w-100 mb-2">
                        ← Quay lại
                    </a>
                    <a th:href="@{/teacher/quiz/{quizId}/finish(quizId=${quizId})}"
                       class="btn btn-primary btn-sm w-100">
                        Hoàn thiện →
                    </a>
                </div>
            </div>
        </div>
    </div>

    <!-- Inline Create Modal -->
    <div class="modal fade" id="createModal" tabindex="-1">
        <div class="modal-dialog modal-lg">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">Tạo câu hỏi mới</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <!-- Skill + Type selector -->
                    <div class="row mb-3">
                        <div class="col-md-4">
                            <label>Kỹ năng *</label>
                            <select id="createSkill" class="form-select" required>
                                <option value="">-- Chọn kỹ năng --</option>
                                <option value="LISTENING">LISTENING</option>
                                <option value="READING">READING</option>
                                <option value="WRITING">WRITING</option>
                                <option value="SPEAKING">SPEAKING</option>
                            </select>
                        </div>
                        <div class="col-md-4">
                            <label>Loại câu hỏi *</label>
                            <select id="createType" class="form-select" required>
                                <option value="">-- Chọn loại --</option>
                                <option value="MULTIPLE_CHOICE_SINGLE">Trắc nghiệm 1 đáp án</option>
                                <option value="MULTIPLE_CHOICE_MULTI">Trắc nghiệm nhiều đáp án</option>
                                <option value="FILL_IN_BLANK">Điền khuyết</option>
                                <option value="MATCHING">Nối cột</option>
                                <option value="WRITING">Viết (Writing)</option>
                                <option value="SPEAKING">Nói (Speaking)</option>
                            </select>
                        </div>
                        <div class="col-md-4">
                            <label>CEFR</label>
                            <select id="createCefr" class="form-select">
                                <option value="B1" selected>B1</option>
                                <option value="A1">A1</option>
                                <option value="A2">A2</option>
                                <option value="B2">B2</option>
                                <option value="C1">C1</option>
                                <option value="C2">C2</option>
                            </select>
                        </div>
                    </div>

                    <!-- Question content -->
                    <div class="mb-3">
                        <label>Nội dung câu hỏi *</label>
                        <textarea id="createContent" class="form-control" rows="4"
                                  placeholder="Nhập nội dung câu hỏi..."></textarea>
                    </div>

                    <!-- Audio for LISTENING -->
                    <div class="mb-3" id="audioUploadSection" style="display:none">
                        <label>File âm thanh (LISTENING)</label>
                        <input type="file" id="createAudio" class="form-control" accept="audio/*">
                        <input type="hidden" id="createAudioUrl">
                    </div>

                    <!-- Answer options (for MC types) -->
                    <div id="optionsSection">
                        <label>Đáp án</label>
                        <div id="optionsList">
                            <div class="input-group mb-2">
                                <div class="input-group-text">
                                    <input type="radio" name="correctOption" value="0" checked>
                                </div>
                                <input type="text" class="form-control" id="opt0"
                                       placeholder="Đáp án A">
                            </div>
                            <div class="input-group mb-2">
                                <div class="input-group-text">
                                    <input type="radio" name="correctOption" value="1">
                                </div>
                                <input type="text" class="form-control" id="opt1"
                                       placeholder="Đáp án B">
                            </div>
                        </div>
                        <button type="button" class="btn btn-outline-secondary btn-sm"
                                onclick="addOption()">+ Thêm đáp án</button>
                    </div>

                    <!-- Single correct text (for FILL_IN_BLANK) -->
                    <div id="fillBlankSection" style="display:none" class="mb-3">
                        <label>Đáp án đúng *</label>
                        <input type="text" id="createCorrectAnswer" class="form-control"
                               placeholder="Nhập đáp án đúng (chuỗi chính xác)">
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                    <button type="button" class="btn btn-primary" onclick="submitCreateForm()">
                        Lưu & Thêm vào Quiz
                    </button>
                </div>
            </div>
        </div>
    </div>
</div>

<script th:inline="javascript">
const quizId = /*[[${quizId}]]*/ 0;
let bankPage = 0;
let createModal, createOptionCount = 2;

document.addEventListener('DOMContentLoaded', () => {
    createModal = new bootstrap.Modal(document.getElementById('createModal'));
    loadBank(0);
    loadSummary();

    // Show/hide sections based on question type
    document.getElementById('createType').addEventListener('change', (e) => {
        const type = e.target.value;
        const isMC = ['MULTIPLE_CHOICE_SINGLE', 'MULTIPLE_CHOICE_MULTI'].includes(type);
        const isFB = type === 'FILL_IN_BLANK';
        const isWS = ['WRITING', 'SPEAKING'].includes(type);
        document.getElementById('optionsSection').style.display = isMC ? 'block' : 'none';
        document.getElementById('fillBlankSection').style.display = isFB ? 'block' : 'none';
        document.getElementById('audioUploadSection').style.display =
            (type === 'SPEAKING' || type === 'LISTENING') ? 'block' : 'none';
    });
});

async function loadBank(page = 0) {
    bankPage = page;
    const skill = document.getElementById('filterSkill').value;
    const cefr = document.getElementById('filterCefr').value;
    const keyword = document.getElementById('filterKeyword').value;

    let url = `/api/v1/teacher/bank-questions?page=${page}&size=10`;
    if (skill) url += `&skill=${skill}`;
    if (cefr) url += `&cefrLevel=${cefr}`;
    if (keyword) url += `&keyword=${encodeURIComponent(keyword)}`;

    const resp = await fetch(url);
    const data = await resp.json();
    renderBank(data.data.content || []);
    renderBankPagination(data.data);
}

function renderBank(questions) {
    const container = document.getElementById('bankContainer');
    if (!questions.length) {
        container.innerHTML = '<div class="alert alert-info">Không tìm thấy câu hỏi nào.</div>';
        return;
    }
    container.innerHTML = questions.map(q => `
        <div class="card mb-2">
            <div class="card-body py-2">
                <div class="d-flex justify-content-between align-items-center">
                    <div>
                        <span class="badge bg-primary me-1">${q.skill || 'N/A'}</span>
                        <span class="badge bg-secondary me-1">${q.questionType}</span>
                        <span class="badge bg-info">${q.cefrLevel}</span>
                        <span class="badge bg-success ms-1">✓ Ngân hàng</span>
                        <p class="mb-0 mt-1" style="font-size:0.9em">
                            ${(q.content||'').substring(0,120)}${(q.content||'').length>120?'...':''}
                        </p>
                    </div>
                    <button class="btn btn-sm btn-outline-success" onclick="addToQuiz(${q.questionId})">
                        + Thêm
                    </button>
                </div>
            </div>
        </div>
    `).join('');
}

function renderBankPagination(page) {
    const nav = document.getElementById('bankPagination');
    const list = document.getElementById('bankPaginationList');
    if (!page.totalPages || page.totalPages <= 1) {
        nav.style.display = 'none'; return;
    }
    nav.style.display = 'block';
    list.innerHTML = '';
    for (let i = 0; i < page.totalPages; i++) {
        list.innerHTML += `<li class="page-item ${i===page.number?'active':''}">
            <a class="page-link" href="#" onclick="loadBank(${i});return false">${i+1}</a>
        </li>`;
    }
}

async function addToQuiz(questionId) {
    const resp = await fetch(`/api/v1/teacher/quizzes/${quizId}/questions`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ questionId: questionId, itemType: 'SINGLE', points: 1.0 })
    });
    const data = await resp.json();
    if (data.success) {
        loadSummary();
        alert('Đã thêm câu hỏi vào quiz!');
    } else {
        alert('Lỗi: ' + data.message);
    }
}

async function loadSummary() {
    // Load existing questions in this quiz
    const resp = await fetch(`/api/v1/teacher/quizzes/${quizId}`);
    const data = await resp.json();
    const quiz = data.data;
    const questions = quiz.quizQuestions || [];
    const summary = {};
    for (const qq of questions) {
        const q = qq.question;
        const skill = q.skill || 'OTHER';
        if (!summary[skill]) summary[skill] = { published: 0, pending: 0 };
        if (q.status === 'PUBLISHED') summary[skill].published++;
        else if (q.status === 'PENDING_REVIEW') summary[skill].pending++;
    }
    renderSummary(summary);
}

function renderSummary(summary) {
    const skills = ['LISTENING', 'READING', 'WRITING', 'SPEAKING'];
    let html = '';
    let hasAny = false;
    for (const skill of skills) {
        const s = summary[skill];
        if (!s) continue;
        hasAny = true;
        html += `
            <div class="mb-2">
                <strong>${skill}</strong>
                <span class="badge bg-success ms-1">${s.published} ✓</span>
                ${s.pending > 0 ? `<span class="badge bg-warning text-dark ms-1">${s.pending} ⏳</span>` : ''}
            </div>`;
    }
    document.getElementById('quizSummary').innerHTML =
        hasAny ? html : '<div class="text-center text-muted">Chưa có câu hỏi</div>';
}

function openCreateModal() {
    document.getElementById('createContent').value = '';
    document.getElementById('createSkill').value = '';
    document.getElementById('createType').value = '';
    document.getElementById('createCorrectAnswer').value = '';
    createOptionCount = 2;
    document.getElementById('optionsList').innerHTML = `
        <div class="input-group mb-2">
            <div class="input-group-text"><input type="radio" name="correctOption" value="0" checked></div>
            <input type="text" class="form-control" id="opt0" placeholder="Đáp án A">
        </div>
        <div class="input-group mb-2">
            <div class="input-group-text"><input type="radio" name="correctOption" value="1"></div>
            <input type="text" class="form-control" id="opt1" placeholder="Đáp án B">
        </div>`;
    createModal.show();
}

function addOption() {
    const letters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
    const container = document.getElementById('optionsList');
    const div = document.createElement('div');
    div.className = 'input-group mb-2';
    div.innerHTML = `
        <div class="input-group-text">
            <input type="radio" name="correctOption" value="${createOptionCount}">
        </div>
        <input type="text" class="form-control" id="opt${createOptionCount}"
               placeholder="Đáp án ${letters[createOptionCount]}">
    `;
    container.appendChild(div);
    createOptionCount++;
}

async function submitCreateForm() {
    const skill = document.getElementById('createSkill').value;
    const questionType = document.getElementById('createType').value;
    const content = document.getElementById('createContent').value;
    const cefr = document.getElementById('createCefr').value;

    if (!skill || !questionType || !content) {
        alert('Vui lòng điền đầy đủ thông tin');
        return;
    }

    // Build question body
    const body = { skill, questionType, content, cefrLevel: cefr };

    if (['MULTIPLE_CHOICE_SINGLE', 'MULTIPLE_CHOICE_MULTI'].includes(questionType)) {
        body.answerOptions = [];
        for (let i = 0; i < createOptionCount; i++) {
            const opt = document.getElementById('opt' + i);
            if (opt && opt.value.trim()) {
                const correct = document.querySelector(`input[name=correctOption][value=${i}]`).checked;
                body.answerOptions.push({ title: opt.value.trim(), correctAnswer: correct });
            }
        }
    } else if (questionType === 'FILL_IN_BLANK') {
        body.correctAnswer = document.getElementById('createCorrectAnswer').value.trim();
    }

    // Create question
    const resp = await fetch('/api/v1/teacher/questions', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
    });
    const data = await resp.json();
    if (!data.success) {
        alert('Lỗi tạo câu hỏi: ' + data.message);
        return;
    }

    // Add to quiz
    const addResp = await fetch(`/api/v1/teacher/quizzes/${quizId}/questions`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ questionId: data.data, itemType: 'SINGLE', points: 1.0 })
    });
    const addData = await addResp.json();
    if (addData.success) {
        createModal.hide();
        loadSummary();
        loadBank(bankPage);
    } else {
        alert('Câu hỏi đã tạo nhưng chưa thêm vào quiz: ' + addData.message);
    }
}
</script>
</body>
</html>
```

---

## Chunk 5: Create quiz-finish.html (Step 3 — finish & publish)

### Task 7: Create teacher/quiz-finish.html

**Files:**
- Create: `src/main/resources/templates/teacher/quiz-finish.html`

- [ ] **Step 1: Write the finish page**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      th:replace="~{fragments/layout :: layout(~{::title}, ~{::#content})}">
<head><title>Hoàn thiện bài quiz</title></head>
<body>
<div id="content" class="container mt-4" style="max-width:700px">
    <h3>Hoàn thiện bài quiz</h3>
    <div id="quizSummary"></div>

    <div class="d-flex justify-content-between mt-4">
        <a th:href="@{/teacher/quiz/{quizId}/build(quizId=${quizId})}"
           class="btn btn-secondary">← Quay lại sửa</a>
        <div>
            <button class="btn btn-outline-primary" id="saveDraftBtn">Lưu bản nháp</button>
            <button class="btn btn-success" id="publishBtn">Xuất bản</button>
        </div>
    </div>
</div>

<script th:inline="javascript">
const quizId = /*[[${quizId}]]*/ 0;

async function loadQuiz() {
    const resp = await fetch(`/api/v1/teacher/quizzes/${quizId}`);
    const data = await resp.json();
    const quiz = data.data;
    const questions = quiz.quizQuestions || [];

    const summary = {};
    let pendingCount = 0;
    for (const qq of questions) {
        const q = qq.question;
        const skill = q.skill || 'OTHER';
        if (!summary[skill]) summary[skill] = 0;
        summary[skill]++;
        if (q.status === 'PENDING_REVIEW') pendingCount++;
    }

    const skillList = Object.entries(summary).map(([skill, count]) =>
        `<span class="badge bg-primary me-2">${skill}: ${count}</span>`
    ).join('');

    document.getElementById('quizSummary').innerHTML = `
        <div class="card">
            <div class="card-body">
                <h4>${quiz.title}</h4>
                <p>${quiz.description || ''}</p>
                <hr>
                <p><strong>Kỹ năng:</strong> ${skillList || '<em>Chưa có câu hỏi</em>'}</p>
                <p><strong>Tổng câu hỏi:</strong> ${questions.length}</p>
                ${quiz.timeLimitMinutes ? `<p><strong>Thời gian:</strong> ${quiz.timeLimitMinutes} phút</p>` : ''}
                ${quiz.passScore ? `<p><strong>Điểm đạt:</strong> ${quiz.passScore}%</p>` : ''}
                ${quiz.maxAttempts ? `<p><strong>Lần làm tối đa:</strong> ${quiz.maxAttempts}</p>` : ''}
                ${pendingCount > 0 ? `
                    <div class="alert alert-warning mt-3">
                        ⚠️ <strong>${pendingCount} câu hỏi</strong> đang chờ phê duyệt từ Expert.
                        Các câu hỏi này sẽ hiển thị sau khi Expert duyệt.
                    </div>
                ` : ''}
            </div>
        </div>
    `;

    if (questions.length === 0) {
        document.getElementById('publishBtn').disabled = true;
        document.getElementById('publishBtn').title = 'Cần ít nhất 1 câu hỏi';
    }
}
loadQuiz();

document.getElementById('saveDraftBtn').addEventListener('click', async () => {
    const resp = await fetch(`/api/v1/teacher/quizzes/${quizId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ title: document.querySelector('.card h4').textContent })
    });
    alert('Đã lưu bản nháp!');
});

document.getElementById('publishBtn').addEventListener('click', async () => {
    if (!confirm('Xuất bản bài quiz này?')) return;
    const resp = await fetch(`/api/v1/teacher/quizzes/${quizId}/publish`, {
        method: 'POST'
    });
    const data = await resp.json();
    if (data.success) {
        alert('Xuất bản thành công!');
        window.location.href = '/teacher/quizzes';
    } else {
        alert('Lỗi: ' + data.message);
    }
});
</script>
</body>
</html>
```

---

## Chunk 6: Add PENDING_REVIEW badge to teacher dashboard

### Task 8: Extend teacher dashboard with pending question count

**Files:**
- Modify: `src/main/java/com/example/DoAn/controller/TeacherApiController.java` (or wherever teacher dashboard API is)

- [ ] **Step 1: Find the teacher dashboard API**

Run: Grep for `teacher/dashboard` or `teacherApi` to find the right controller.

- [ ] **Step 2: Add pending question count to the response**

Find the dashboard endpoint and add `pendingQuestionCount` to the response:

```java
// Add after fetching enrolled classes count
long pendingQuestionCount = questionRepository.countBySourceAndStatus(
    "TEACHER_PRIVATE", "PENDING_REVIEW");
model.addAttribute("pendingQuestionCount", pendingQuestionCount);
```

Or if it's a REST endpoint, add to the response DTO.

In the HTML template for teacher sidebar/header, add:

```html
<span th:if="${pendingQuestionCount > 0}"
      class="badge bg-warning text-dark"
      th:text="${pendingQuestionCount} + ' câu chờ duyệt'">0</span>
```

---

## Spec Reference

See `docs/superpowers/specs/2026-04-04-002-teacher-creates-lesson-quiz-design.md`.
