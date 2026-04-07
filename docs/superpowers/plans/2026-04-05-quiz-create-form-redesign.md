# Quiz Create Form Redesign — Implementation Plan

> **For agentic workers:** Use superpowers:subagent-driven-development or superpowers:executing-plans to implement. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Rewrite `quiz-create.html` và `quiz-edit.html` cho Expert: 3 categories (ENTRY_TEST, COURSE_QUIZ, COURSE_ASSIGNMENT), COURSE_QUIZ + COURSE_ASSIGNMENT bắt buộc 4 skill.

**Architecture:**
- `quiz-create.html`: 1 trang duy nhất, dropdown category → `onCategoryChange()` ẩn/hiện section
- `quiz-edit.html`: load từ API, hiện tabs skill cho COURSE_QUIZ/ASSIGNMENT
- Backend: thêm validation ở `publishAssignment()` để reject COURSE_QUIZ chưa đủ 4 skill

**Tech Stack:** Thymeleaf, Bootstrap 5, vanilla JS, Spring Boot REST API

---

## Chunk 1: Backend Validation — `ExpertQuizServiceImpl.java`

**File:** `NovaLMS/DoAn/src/main/java/com/example/DoAn/service/impl/ExpertQuizServiceImpl.java`

**Changelog:** `publishAssignment()` method (line ~592). Thêm logic kiểm tra COURSE_QUIZ đủ 4 skill.

- [ ] **Step 1: Tìm và đọc `publishAssignment()`**

Line ~592-610 trong ExpertQuizServiceImpl.java. Hiện tại chỉ check `!missing.isEmpty()` cho assignment. Cần mở rộng để COURSE_QUIZ cũng phải pass 4-skill check.

- [ ] **Step 2: Sửa `publishAssignment()` thêm COURSE_QUIZ validation**

Thay body method bằng:

```java
@Override
public QuizResponseDTO publishAssignment(Integer quizId) {
    Quiz quiz = findQuiz(quizId);
    if (!"DRAFT".equals(quiz.getStatus())) {
        throw new InvalidDataException("Only DRAFT quizzes can be published");
    }

    // ENTRY_TEST: chỉ cần ≥1 câu (check ở changeStatus đã có, không cần thêm)
    // COURSE_QUIZ + COURSE_ASSIGNMENT: bắt buộc đủ 4 skill
    if ("COURSE_QUIZ".equals(quiz.getQuizCategory()) || "COURSE_ASSIGNMENT".equals(quiz.getQuizCategory())) {
        Map<String, SkillSectionSummaryDTO> summaries = getSkillSummaries(quizId);
        List<String> missing = new java.util.ArrayList<>();
        for (SkillSectionSummaryDTO s : summaries.values()) {
            if (s.getQuestionCount() == 0) missing.add(s.getSkill());
        }
        if (!missing.isEmpty()) {
            throw new InvalidDataException("Missing questions for skills: " + String.join(", ", missing)
                    + ". All 4 skills (LISTENING, READING, SPEAKING, WRITING) are required.");
        }
        // COURSE_ASSIGNMENT: kiểm tra per-skill time set
        if ("COURSE_ASSIGNMENT".equals(quiz.getQuizCategory())) {
            if (quiz.getTimeLimitPerSkill() == null || quiz.getTimeLimitPerSkill().isEmpty()) {
                throw new InvalidDataException("COURSE_ASSIGNMENT requires per-skill time limits to be set.");
            }
        }
    }

    quiz.setStatus("PUBLISHED");
    quiz.setIsOpen(false);
    quizRepository.save(quiz);
    return toResponseDTO(quiz);
}
```

- [ ] **Step 3: Verify build**

Run: `cd NovaLMS/DoAn && ./mvnw compile -q`
Expected: No errors

---

## Chunk 2: Rewrite `quiz-create.html`

**Files:**
- Rewrite: `NovaLMS/DoAn/src/main/resources/templates/expert/quiz-create.html`
- Read reference: current `quiz-create.html` (1587 lines, đã đọc)

### Task 2a: HTML skeleton + Category selector

- [ ] **Step 1: Viết `<body>` structure cơ bản**

Header, sidebar, main layout giữ nguyên từ file cũ. Thay nội dung `<main>`:

```html
<main class="dash-main d-flex flex-column" id="dashMain">
    <div class="d-flex justify-content-between align-items-end mb-4">
        <div>
            <h3 class="fw-bold m-0" style="color: #223a58;">Tạo Quiz / Bài kiểm tra</h3>
            <p class="text-muted small m-0">Quy trình tạo đề thi thông minh 3 bước.</p>
        </div>
        <a th:href="@{/expert/quiz-management}" class="btn btn-sm btn-outline-secondary rounded-pill px-3">
            <i class="bi bi-x-lg me-1"></i> Hủy và thoát
        </a>
    </div>

    <!-- Category selector -->
    <div class="card border-0 shadow-sm mb-4">
        <div class="card-body p-4">
            <div class="row g-3">
                <div class="col-md-4">
                    <label class="form-label fw-bold">Loại Quiz <span class="text-danger">*</span></label>
                    <select id="quizCategory" class="form-select form-select-lg border-2" required onchange="onCategoryChange()">
                        <option value="">-- Chọn loại quiz --</option>
                        <option value="ENTRY_TEST">Bài kiểm tra đầu vào (Entry Test)</option>
                        <option value="COURSE_QUIZ">Bài kiểm tra khóa học (Course Quiz)</option>
                        <option value="COURSE_ASSIGNMENT">Bài tập lớn khóa học (Course Assignment)</option>
                    </select>
                </div>
                <div class="col-md-4" id="courseCol" style="display:none">
                    <label class="form-label fw-bold">Khóa học <span class="text-danger">*</span></label>
                    <select id="courseId" class="form-select form-select-lg border-2">
                        <option value="">-- Chọn khóa học --</option>
                    </select>
                </div>
            </div>
        </div>
    </div>

    <!-- Config card -->
    <div id="configSection" class="card border-0 shadow-sm mb-4">
        <div class="card-body p-4">
            <div class="row g-3 mb-3">
                <div class="col-md-6">
                    <label class="form-label fw-bold">Tên Quiz <span class="text-danger">*</span></label>
                    <input type="text" id="title" class="form-control form-control-lg border-2" required placeholder="VD: IELTS A1 - Unit 1 Test">
                </div>
            </div>
            <div class="mb-3">
                <label class="form-label fw-bold">Mô tả</label>
                <textarea id="description" class="form-control border-2" rows="2" placeholder="Mô tả chi tiết quiz..."></textarea>
            </div>

            <h6 class="fw-bold mt-4 mb-3" style="color: #223a58; border-left: 4px solid #136ad5; padding-left: 10px;">Cấu hình nâng cao</h6>
            <div class="row g-3">
                <div class="col-md-3">
                    <label class="form-label fw-bold small text-muted">Thời gian (phút)</label>
                    <input type="number" id="timeLimitMinutes" class="form-control" placeholder="Không giới hạn" min="1">
                </div>
                <div class="col-md-3">
                    <label class="form-label fw-bold small text-muted">Điểm đạt (%)</label>
                    <input type="number" id="passScore" class="form-control" placeholder="VD: 70" min="0" max="100" step="0.01">
                </div>
                <div class="col-md-3">
                    <label class="form-label fw-bold small text-muted">Số lần làm lại</label>
                    <input type="number" id="maxAttempts" class="form-control" placeholder="Không giới hạn" min="1">
                </div>
                <div class="col-md-3">
                    <label class="form-label fw-bold small text-muted">Thứ tự câu hỏi</label>
                    <select id="questionOrder" class="form-select">
                        <option value="FIXED">Cố định</option>
                        <option value="RANDOM">Ngẫu nhiên</option>
                    </select>
                </div>
            </div>

            <!-- Per-skill time limits — ASSIGNMENT only -->
            <div id="perSkillTimeSection" style="display:none;">
                <h6 class="fw-bold mt-4 mb-3" style="color: #223a58; border-left: 4px solid #136ad5; padding-left: 10px;">Thời gian từng phần (phút)</h6>
                <div class="row g-3">
                    <div class="col-md-3">
                        <label class="form-label fw-bold small text-muted">LISTENING</label>
                        <input type="number" id="timeLimit_LISTENING" class="form-control" placeholder="VD: 30" min="1">
                    </div>
                    <div class="col-md-3">
                        <label class="form-label fw-bold small text-muted">READING</label>
                        <input type="number" id="timeLimit_READING" class="form-control" placeholder="VD: 60" min="1">
                    </div>
                    <div class="col-md-3">
                        <label class="form-label fw-bold small text-muted">SPEAKING</label>
                        <input type="number" id="timeLimit_SPEAKING" class="form-control" placeholder="VD: 15" min="1">
                    </div>
                    <div class="col-md-3">
                        <label class="form-label fw-bold small text-muted">WRITING</label>
                        <input type="number" id="timeLimit_WRITING" class="form-control" placeholder="VD: 30" min="1">
                    </div>
                </div>
            </div>

            <div class="mt-3">
                <div class="form-check form-switch" style="padding-left: 2.5rem !important;">
                    <input class="form-check-input" type="checkbox" id="showAnswerAfterSubmit" style="width: 2.5em; height: 1.25em;">
                    <label class="form-check-label fw-bold ms-2" for="showAnswerAfterSubmit">Cho phép xem đáp án sau khi nộp bài</label>
                </div>
            </div>

            <!-- Entry Test warning -->
            <div id="entryTestWarning" class="alert alert-soft-warning border-0 py-3 px-4 mt-4" style="display:none; background-color: #fff9db; color: #856404; border-radius: 12px;">
                <div class="d-flex">
                    <i class="bi bi-exclamation-triangle-fill fs-4 me-3"></i>
                    <div>
                        <h6 class="fw-bold mb-1">Lưu ý cho Entry Test</h6>
                        <p class="small m-0">Chỉ chọn câu hỏi từ ngân hàng, không bắt buộc 4 kỹ năng. Bài thi có thể là bài trắc nghiệm thuần túy.</p>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- Step 2: Question selection -->
    <div id="questionSection" class="card border-0 shadow-sm mb-4">
        <div class="card-body p-0">
            <div class="p-4 border-bottom">
                <h5 class="fw-bold mb-0" style="color: #223a58;">Chọn câu hỏi</h5>
                <p class="text-muted small mb-0" id="skillRequirementHint">Chọn câu hỏi từ ngân hàng. Đủ 4 kỹ năng mới cho xuất bản.</p>
            </div>
            <!-- Skill tabs: shown for COURSE_QUIZ and COURSE_ASSIGNMENT -->
            <div id="skillTabsSection" style="display:none;" class="px-4 pt-3">
                <ul class="nav nav-tabs" role="tablist">
                    <li class="nav-item" role="presentation">
                        <button class="nav-link active" id="tab-listening" data-bs-toggle="tab" data-bs-target="#panel-listening" type="button">
                            <i class="bi bi-ear me-1"></i> LISTENING <span class="badge bg-primary ms-1" id="count-listening">0</span>
                        </button>
                    </li>
                    <li class="nav-item" role="presentation">
                        <button class="nav-link" id="tab-reading" data-bs-toggle="tab" data-bs-target="#panel-reading" type="button">
                            <i class="bi bi-book me-1"></i> READING <span class="badge bg-primary ms-1" id="count-reading">0</span>
                        </button>
                    </li>
                    <li class="nav-item" role="presentation">
                        <button class="nav-link" id="tab-speaking" data-bs-toggle="tab" data-bs-target="#panel-speaking" type="button">
                            <i class="bi bi-mic me-1"></i> SPEAKING <span class="badge bg-primary ms-1" id="count-speaking">0</span>
                        </button>
                    </li>
                    <li class="nav-item" role="presentation">
                        <button class="nav-link" id="tab-writing" data-bs-toggle="tab" data-bs-target="#panel-writing" type="button">
                            <i class="bi bi-pen me-1"></i> WRITING <span class="badge bg-primary ms-1" id="count-writing">0</span>
                        </button>
                    </li>
                </ul>
            </div>

            <div class="tab-content" id="questionTabContent">
                <!-- Tab pane cho từng skill (COURSE_QUIZ / COURSE_ASSIGNMENT) -->
                <div class="tab-pane fade show active" id="panel-listening" role="tabpanel">
                    <div class="p-4" id="skill-panel-listening"></div>
                </div>
                <div class="tab-pane fade" id="panel-reading" role="tabpanel">
                    <div class="p-4" id="skill-panel-reading"></div>
                </div>
                <div class="tab-pane fade" id="panel-speaking" role="tabpanel">
                    <div class="p-4" id="skill-panel-speaking"></div>
                </div>
                <div class="tab-pane fade" id="panel-writing" role="tabpanel">
                    <div class="p-4" id="skill-panel-writing"></div>
                </div>
            </div>
        </div>
    </div>

    <!-- Action buttons -->
    <div class="d-flex justify-content-end gap-3">
        <button type="button" class="btn btn-lg btn-light px-4 fw-bold" style="border-radius: 12px;" onclick="window.history.back()">Hủy</button>
        <button type="button" id="btnSaveDraft" class="btn btn-lg btn-outline-primary px-5 fw-bold" style="border-radius: 12px;" onclick="submitQuiz('DRAFT')">Lưu nháp</button>
        <button type="button" id="btnPublish" class="btn btn-lg text-white px-5 fw-bold" style="background-color: #136ad5; border-radius: 12px;" onclick="submitQuiz('PUBLISHED')">
            <i class="bi bi-rocket-takeoff me-2"></i> Xuất bản
        </button>
    </div>
</main>
```

### Task 2b: JavaScript logic

- [ ] **Step 1: `onCategoryChange()` — ẩn/hiện section**

```javascript
function onCategoryChange() {
    const cat = document.getElementById('quizCategory').value;

    // Course col: hiện cho COURSE_QUIZ và COURSE_ASSIGNMENT
    document.getElementById('courseCol').style.display =
        (cat === 'COURSE_QUIZ' || cat === 'COURSE_ASSIGNMENT') ? 'block' : 'none';

    // Per-skill time: chỉ ASSIGNMENT
    document.getElementById('perSkillTimeSection').style.display =
        (cat === 'COURSE_ASSIGNMENT') ? 'block' : 'none';

    // Entry test warning
    document.getElementById('entryTestWarning').style.display =
        (cat === 'ENTRY_TEST') ? 'flex' : 'none';

    // Skill tabs: COURSE_QUIZ + COURSE_ASSIGNMENT → show tabs
    const showTabs = (cat === 'COURSE_QUIZ' || cat === 'COURSE_ASSIGNMENT');
    document.getElementById('skillTabsSection').style.display = showTabs ? 'block' : 'none';

    // Hint text
    const hint = document.getElementById('skillRequirementHint');
    if (cat === 'ENTRY_TEST') {
        hint.textContent = 'Chọn câu hỏi từ ngân hàng. Không bắt buộc 4 kỹ năng.';
    } else if (cat === 'COURSE_QUIZ') {
        hint.textContent = 'Bắt buộc đủ 4 kỹ năng (LISTENING, READING, SPEAKING, WRITING) để xuất bản.';
    } else if (cat === 'COURSE_ASSIGNMENT') {
        hint.textContent = 'Bắt buộc đủ 4 kỹ năng. Đặt thời gian từng phần ở Cấu hình nâng cao.';
    } else {
        hint.textContent = 'Chọn câu hỏi từ ngân hàng.';
    }

    // Load courses khi cần
    if (cat === 'COURSE_QUIZ' || cat === 'COURSE_ASSIGNMENT') {
        loadCourses();
    }

    // Init bank panels
    if (showTabs) {
        initSkillPanels();
    }
}
```

- [ ] **Step 2: Load courses**

```javascript
async function loadCourses() {
    const select = document.getElementById('courseId');
    if (!select || select.options.length > 1) return;
    try {
        const res = await fetch('/api/v1/expert/modules/courses');
        const result = await res.json();
        if (result.status === 200 && result.data?.length) {
            result.data.forEach(c => {
                const opt = document.createElement('option');
                opt.value = c.courseId;
                opt.textContent = c.courseName || c.title || 'Course #' + c.courseId;
                select.appendChild(opt);
            });
        }
    } catch (e) { console.error(e); }
}
```

- [ ] **Step 3: `initSkillPanels()` — build bank panel cho từng tab**

Mỗi skill panel có: filter row (skill filter locked + CEFR + keyword) + table + pagination + đã thêm list.

```javascript
function initSkillPanels() {
    const skills = ['LISTENING', 'READING', 'SPEAKING', 'WRITING'];
    const icons = { LISTENING: 'bi-ear', READING: 'bi-book', SPEAKING: 'bi-mic', WRITING: 'bi-pen' };
    const types = {
        LISTENING: ['MULTIPLE_CHOICE_SINGLE', 'MULTIPLE_CHOICE_MULTI', 'FILL_IN_BLANK'],
        READING:   ['MULTIPLE_CHOICE_SINGLE', 'MULTIPLE_CHOICE_MULTI', 'FILL_IN_BLANK', 'MATCHING'],
        SPEAKING:  ['SPEAKING'],
        WRITING:   ['WRITING']
    };

    skills.forEach(skill => {
        const container = document.getElementById('skill-panel-' + skill.toLowerCase());
        if (!container || container.innerHTML.trim()) return; // đã init rồi

        container.innerHTML = `
            <div class="row g-0" style="min-height: 500px;">
                <div class="col-md-5 border-end p-3" style="background:#f8fafc;">
                    <h6 class="fw-bold mb-3"><i class="bi ${icons[skill]} me-1"></i> Câu hỏi đã thêm</h6>
                    <div id="addedList-${skill}" class="overflow-auto" style="max-height:420px;"></div>
                </div>
                <div class="col-md-7 p-3">
                    <div class="mb-3">
                        <input type="text" id="kw-${skill}" class="form-control" placeholder="Tìm kiếm câu hỏi..." onkeyup="if(event.key==='Enter') loadBankForSkill('${skill}', 0)">
                    </div>
                    <div class="mb-3">
                        <select id="cefr-${skill}" class="form-select form-select-sm" onchange="loadBankForSkill('${skill}', 0)">
                            <option value="">-- Tất cả CEFR --</option>
                            <option>A1</option><option>A2</option><option>B1</option><option>B2</option><option>C1</option><option>C2</option>
                        </select>
                    </div>
                    <div class="mb-2 d-flex justify-content-between align-items-center">
                        <span class="small text-muted">Ngân hàng câu hỏi — ${skill}</span>
                        <button class="btn btn-sm btn-primary" onclick="loadBankForSkill('${skill}', 0)">
                            <i class="bi bi-search"></i> Tìm
                        </button>
                    </div>
                    <div class="overflow-auto" style="max-height:380px;">
                        <table class="table table-hover align-middle mb-0" style="font-size:0.85rem;">
                            <thead class="table-light sticky-top">
                                <tr><th>Nội dung</th><th>Loại</th><th>CEFR</th><th class="text-end">Hành động</th></tr>
                            </thead>
                            <tbody id="bankBody-${skill}">
                                <tr><td colspan="4" class="text-center py-4 text-muted">Nhấn Tìm để tải câu hỏi...</td></tr>
                            </tbody>
                        </table>
                    </div>
                    <div class="mt-2">
                        <nav><ul class="pagination pagination-sm justify-content-center mb-0" id="bankPg-${skill}"></ul></nav>
                    </div>
                </div>
            </div>`;
    });
}
```

- [ ] **Step 4: `loadBankForSkill(skill, page)`**

```javascript
let _skillQuestions = {};
let _skillPage = {};
const SKILLS = ['LISTENING', 'READING', 'SPEAKING', 'WRITING'];

async function loadBankForSkill(skill, page) {
    _skillPage[skill] = page;
    const params = new URLSearchParams();
    params.append('skill', skill);
    params.append('status', 'PUBLISHED');
    const cefr = document.getElementById('cefr-' + skill)?.value;
    if (cefr) params.append('cefrLevel', cefr);
    const kw = document.getElementById('kw-' + skill)?.value;
    if (kw) params.append('keyword', kw);
    params.append('page', page);
    params.append('size', 10);

    try {
        const res = await fetch(`/api/v1/expert/question-bank?${params}`);
        const result = await res.json();
        renderBankForSkill(skill, result);
    } catch (e) { console.error(e); }
}

function renderBankForSkill(skill, result) {
    const tbody = document.getElementById('bankBody-' + skill);
    if (!result || result.status !== 200 || !result.data?.items?.length) {
        if (tbody) tbody.innerHTML = '<tr><td colspan="4" class="text-center py-4 text-muted">Không tìm thấy câu hỏi.</td></tr>';
        renderSkillPagination(skill, 0);
        return;
    }
    const existing = new Set((_skillQuestions[skill] || []).map(q => q.questionId));
    if (tbody) {
        tbody.innerHTML = result.data.items.map(q => `
            <tr>
                <td><div class="small text-truncate" style="max-width:200px;">${q.content || '—'}</div></td>
                <td><span class="badge bg-light text-dark">${typeName(q.questionType)}</span></td>
                <td><span class="badge bg-info text-dark">${q.cefrLevel || '—'}</span></td>
                <td class="text-end">
                    ${existing.has(q.id)
                        ? '<span class="badge bg-success px-3 py-2"><i class="bi bi-check-lg me-1"></i>Đã thêm</span>'
                        : `<button class="btn btn-sm btn-primary rounded-pill px-3"
                                   onclick="addToSkill('${skill}', ${q.id})">
                               <i class="bi bi-plus-lg me-1"></i>Thêm
                           </button>`}
                </td>
            </tr>`).join('');
    }
    renderSkillPagination(skill, result.data.totalPages);
    renderAddedList(skill);
}

function renderSkillPagination(skill, totalPages) {
    const pg = document.getElementById('bankPg-' + skill);
    if (!pg) return;
    let html = '';
    const cur = _skillPage[skill] || 0;
    for (let i = 0; i < totalPages; i++) {
        html += `<li class="page-item ${i === cur ? 'active' : ''}">
            <a class="page-link" href="javascript:void(0)" onclick="loadBankForSkill('${skill}', ${i})">${i+1}</a>
        </li>`;
    }
    pg.innerHTML = html;
}
```

- [ ] **Step 5: `addToSkill(skill, questionId)` + `removeFromSkill()`**

```javascript
async function addToSkill(skill, questionId) {
    if (!_quizId) {
        // Tạo quiz trước rồi mới add
        await submitQuiz('DRAFT');
    }
    try {
        const res = await fetch(`/api/v1/expert/quizzes/${_quizId}/section/questions`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ questionIds: [questionId], skill: skill, itemType: 'SINGLE' })
        });
        if (res.ok) {
            await loadSkillQuestions(skill);
            renderAddedList(skill);
            loadBankForSkill(skill, _skillPage[skill] || 0);
            updateSkillCountBadge(skill);
        } else {
            const r = await res.json();
            alert(r.message || 'Không thể thêm câu hỏi.');
        }
    } catch (e) { alert('Lỗi: ' + e.message); }
}

async function removeFromSkill(skill, questionId) {
    if (!confirm('Gỡ câu hỏi này khỏi phần ' + skill + '?')) return;
    try {
        await fetch(`/api/v1/expert/quizzes/${_quizId}/section/questions/${questionId}`, { method: 'DELETE' });
        await loadSkillQuestions(skill);
        renderAddedList(skill);
        loadBankForSkill(skill, _skillPage[skill] || 0);
        updateSkillCountBadge(skill);
    } catch (e) { alert('Lỗi: ' + e.message); }
}

async function loadSkillQuestions(skill) {
    if (!_quizId) { _skillQuestions[skill] = []; return; }
    try {
        const res = await fetch(`/api/v1/expert/quizzes/${_quizId}/skills`);
        const result = await res.json();
        if (result.status === 200 && result.data) {
            _skillQuestions[skill] = result.data[skill]?.questions || [];
        }
    } catch (e) { _skillQuestions[skill] = []; }
}

function renderAddedList(skill) {
    const container = document.getElementById('addedList-' + skill);
    const questions = _skillQuestions[skill] || [];
    if (questions.length === 0) {
        if (container) container.innerHTML = '<div class="text-center py-4 text-muted small">Chưa có câu hỏi nào.</div>';
        return;
    }
    if (container) {
        container.innerHTML = questions.map((q, i) => `
            <div class="card mb-2 border shadow-none" style="border-radius:10px;">
                <div class="card-body p-2">
                    <div class="d-flex justify-content-between align-items-start">
                        <div style="max-width:85%;">
                            <div class="small text-muted mb-1"><span class="badge bg-light text-dark">${i+1}</span> ${q.questionContent || '—'}</div>
                        </div>
                        <button class="btn btn-sm btn-outline-danger border-0" onclick="removeFromSkill('${skill}', ${q.questionId})">
                            <i class="bi bi-trash"></i>
                        </button>
                    </div>
                </div>
            </div>`).join('');
    }
}

function updateSkillCountBadge(skill) {
    const badge = document.getElementById('count-' + skill.toLowerCase());
    if (badge) {
        const count = (_skillQuestions[skill] || []).length;
        badge.textContent = count;
    }
}
```

- [ ] **Step 6: `submitQuiz(status)` + `typeName()`**

```javascript
let _quizId = null;
let _currentCategory = null;

async function submitQuiz(status) {
    const cat = document.getElementById('quizCategory').value;
    if (!cat) { alert('Vui lòng chọn loại quiz.'); return; }
    _currentCategory = cat;

    if ((cat === 'COURSE_QUIZ' || cat === 'COURSE_ASSIGNMENT') && !document.getElementById('courseId').value) {
        alert('Vui lòng chọn khóa học.'); return;
    }

    const body = {
        title: document.getElementById('title').value,
        description: document.getElementById('description').value || null,
        quizCategory: cat,
        courseId: parseInt(document.getElementById('courseId').value) || null,
        timeLimitMinutes: parseInt(document.getElementById('timeLimitMinutes').value) || null,
        passScore: parseFloat(document.getElementById('passScore').value) || null,
        maxAttempts: parseInt(document.getElementById('maxAttempts').value) || null,
        questionOrder: document.getElementById('questionOrder').value,
        showAnswerAfterSubmit: document.getElementById('showAnswerAfterSubmit').checked,
        isSequential: (cat === 'COURSE_ASSIGNMENT'),
        skillOrder: (cat === 'COURSE_QUIZ' || cat === 'COURSE_ASSIGNMENT')
            ? ['LISTENING', 'READING', 'SPEAKING', 'WRITING'] : null,
        timeLimitPerSkill: buildTimeLimitPerSkill(),
        status: status
    };

    try {
        if (!_quizId) {
            const res = await fetch('/api/v1/expert/quizzes', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(body)
            });
            const result = await res.json();
            if (!res.ok) { alert(result.message || 'Có lỗi xảy ra.'); return; }
            _quizId = result.data.quizId;
        } else {
            // Update
            await fetch(`/api/v1/expert/quizzes/${_quizId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(body)
            });
        }

        if (status === 'PUBLISHED') {
            await publishQuiz();
        } else {
            alert('Đã lưu nháp!');
            window.location.href = '/expert/quiz-management';
        }
    } catch (e) { alert('Lỗi: ' + e.message); }
}

function buildTimeLimitPerSkill() {
    const result = {};
    ['LISTENING', 'READING', 'SPEAKING', 'WRITING'].forEach(s => {
        const el = document.getElementById('timeLimit_' + s);
        if (el?.value) result[s] = parseInt(el.value);
    });
    return Object.keys(result).length > 0 ? result : null;
}

async function publishQuiz() {
    try {
        const res = await fetch(`/api/v1/expert/quizzes/${_quizId}/publish`, { method: 'PATCH' });
        if (res.ok) {
            alert('Chúc mừng! Quiz đã được xuất bản thành công.');
            window.location.href = '/expert/quiz-management';
        } else {
            const r = await res.json();
            alert(r.message || 'Lỗi khi xuất bản.');
        }
    } catch (e) { alert('Lỗi: ' + e.message); }
}

function typeName(type) {
    const map = {
        MULTIPLE_CHOICE_SINGLE: 'Trắc nghiệm (1)',
        MULTIPLE_CHOICE_MULTI: 'Trắc nghiệm (n)',
        FILL_IN_BLANK: 'Điền từ',
        MATCHING: 'Nối đáp án',
        WRITING: 'Viết',
        SPEAKING: 'Nói'
    };
    return map[type] || type || '—';
}
```

---

## Chunk 3: Rewrite `quiz-edit.html`

**File:** Rewrite: `NovaLMS/DoAn/src/main/resources/templates/expert/quiz-edit.html`

- [ ] **Step 1: Rewrite HTML + JS**

Load quiz từ `GET /api/v1/expert/quizzes/${quizId}`. Set category vào dropdown, hiện tabs skill nếu COURSE_QUIZ/ASSIGNMENT. Edit form giống create nhưng readonly category.

Key JS functions:
- `loadQuiz()` — fetch quiz, populate fields, detect category → show/hide skill tabs
- `renderSkillTabsForEdit()` — build tab panels cho từng skill với số câu hiện tại
- `updateQuiz()` — PUT `/api/v1/expert/quizzes/{quizId}`

```javascript
const quizId = /*[[${quizId}]]*/ 0;

async function loadQuiz() {
    const res = await fetch(`/api/v1/expert/quizzes/${quizId}`);
    const result = await res.json();
    if (result.status !== 200) { alert('Không tìm thấy quiz'); return; }
    const q = result.data;

    document.getElementById('title').value = q.title || '';
    document.getElementById('description').value = q.description || '';
    document.getElementById('quizCategory').value = q.quizCategory;
    document.getElementById('status').value = q.status;
    document.getElementById('timeLimitMinutes').value = q.timeLimitMinutes || '';
    document.getElementById('passScore').value = q.passScore || '';
    document.getElementById('maxAttempts').value = q.maxAttempts || '';
    document.getElementById('questionOrder').value = q.questionOrder || 'FIXED';
    document.getElementById('showAnswerAfterSubmit').checked = q.showAnswerAfterSubmit || false;

    const cat = q.quizCategory;
    if (cat === 'COURSE_QUIZ' || cat === 'COURSE_ASSIGNMENT') {
        document.getElementById('skillTabsEdit').style.display = 'block';
        await loadSkillCountsEdit();
    }

    if (q.hasAttempts) {
        document.getElementById('attemptWarning').classList.remove('d-none');
    }
}

async function loadSkillCountsEdit() {
    const res = await fetch(`/api/v1/expert/quizzes/${quizId}/skills`);
    const result = await res.json();
    if (result.status === 200 && result.data) {
        ['LISTENING', 'READING', 'SPEAKING', 'WRITING'].forEach(s => {
            const badge = document.getElementById('edit-count-' + s.toLowerCase());
            if (badge && result.data[s]) badge.textContent = result.data[s].questionCount;
        });
    }
}

loadQuiz();
```

- [ ] **Step 2: Save changes**

PUT `/api/v1/expert/quizzes/${quizId}` với body tương tự `submitQuiz()`.

---

## Chunk 4: Verify

- [ ] **Step 1: Build project**

Run: `cd NovaLMS/DoAn && ./mvnw compile -q`
Expected: No compile errors

- [ ] **Step 2: Check file sizes reasonable**

`quiz-create.html` nên < 500 lines (loại bỏ logic cũ phức tạp)
`quiz-edit.html` nên < 250 lines