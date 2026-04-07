# SPEC 002 — Hybrid Quiz/Assignment Wizard: Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Integrate 4-skill sequential Assignment creation into `quiz-create.html` by morphing the existing 3-step quiz wizard into a hybrid wizard. Backend is fully implemented — only frontend changes needed.

**Architecture:** Single HTML template + vanilla JS, no new framework. `onCategoryChange()` morphs stepper between 3-step (quiz) and 6-step (assignment) modes. All backend APIs already exist in `IExpertQuizService` / `ExpertQuizController`.

**Tech Stack:** Thymeleaf + vanilla JavaScript + Bootstrap 5 (existing). Existing `quiz-create.html` as the single file to modify.

**Backend Status:** ✅ COMPLETE — all endpoints exist, entities have required fields, service layer handles all business logic.

---

## Chunk 1: Stepper Morphing + Step 1 Enhancement

**Goal:** Make `quiz-create.html` detect `COURSE_ASSIGNMENT`/`MODULE_ASSIGNMENT` selection and morph its UI.

**File:** `DoAn/src/main/resources/templates/expert/quiz-create.html`

---

### Task 1.1: Add assignment options to category dropdown + global state

In the `<script>` block at bottom of `quiz-create.html`, add to `onCategoryChange()`:

- [ ] **Step: Add `COURSE_ASSIGNMENT` and `MODULE_ASSIGNMENT` options to `quizCategory` select**

Find the `<select id="quizCategory">` in the HTML (line ~73). Add these options:
```html
<option value="COURSE_ASSIGNMENT">Course Assignment</option>
<option value="MODULE_ASSIGNMENT">Module Assignment</option>
```

- [ ] **Step: Add `morphToAssignmentMode()` function**

In the `<script>` block, add to `onCategoryChange()` body (replace existing function or extend it):

```javascript
function onCategoryChange() {
    const cat = document.getElementById('quizCategory').value;
    const isAssignment = (cat === 'COURSE_ASSIGNMENT' || cat === 'MODULE_ASSIGNMENT');

    if (isAssignment) {
        morphToAssignmentMode();
    } else {
        morphToQuizMode();
    }
}

function morphToAssignmentMode() {
    // 1. Show per-skill time limit inputs
    document.getElementById('perSkillTimeSection').style.display = 'block';

    // 2. Show mode notice banner
    const notice = document.getElementById('assignmentModeNotice');
    if (notice) notice.style.display = 'flex';

    // 3. Hide quiz-only advanced fields
    const qOrderRow = document.getElementById('questionOrderRow');
    if (qOrderRow) qOrderRow.style.display = 'none';

    // 4. Change submit button label
    document.getElementById('btnNextStep').innerHTML =
        'Tiếp theo: LISTENING <i class="bi bi-arrow-right ms-2"></i>';

    // 5. Change next-step handler
    document.getElementById('btnNextStep').setAttribute('onclick', 'validateAndGoToStep2()');

    _isAssignment = true;
}

function morphToQuizMode() {
    // 1. Hide per-skill time limit inputs
    document.getElementById('perSkillTimeSection').style.display = 'none';

    // 2. Hide mode notice
    const notice = document.getElementById('assignmentModeNotice');
    if (notice) notice.style.display = 'none';

    // 3. Show quiz-only fields
    const qOrderRow = document.getElementById('questionOrderRow');
    if (qOrderRow) qOrderRow.style.display = 'flex';

    // 4. Restore button label
    document.getElementById('btnNextStep').innerHTML =
        'Tiếp theo: Chọn câu hỏi <i class="bi bi-arrow-right ms-2"></i>';

    document.getElementById('btnNextStep').setAttribute('onclick', 'validateAndGoToStep2()');

    _isAssignment = false;
}
```

- [ ] **Step: Add global `_isAssignment` state variable**

At the top of the `<script>` block where `_currentStep` and `_quizId` are declared (around line 368), add:
```javascript
let _isAssignment = false;
```

- [ ] **Step: Add per-skill time limit inputs to Step 1 form**

Find the advanced config row in `section-step-1` (around line 93, after the `Cấu hình nâng cao` heading). Add before the closing `</form>`:

```html
<!-- ASSIGNMENT MODE ONLY: Per-skill time limits -->
<div id="perSkillTimeSection" style="display: none;">
    <h6 class="fw-bold mt-4 mb-3" style="color: #223a58; border-left: 4px solid #136ad5; padding-left: 10px;">
        Thời gian từng phần (phút)
    </h6>
    <div class="row g-3 mb-4">
        <div class="col-md-3">
            <label class="form-label fw-bold small text-muted">LISTENING</label>
            <input type="number" id="timeLimit_LISTENING" class="form-control"
                   placeholder="Không giới hạn" min="1">
        </div>
        <div class="col-md-3">
            <label class="form-label fw-bold small text-muted">READING</label>
            <input type="number" id="timeLimit_READING" class="form-control"
                   placeholder="Không giới hạn" min="1">
        </div>
        <div class="col-md-3">
            <label class="form-label fw-bold small text-muted">SPEAKING</label>
            <input type="number" id="timeLimit_SPEAKING" class="form-control"
                   placeholder="Không giới hạn" min="1">
        </div>
        <div class="col-md-3">
            <label class="form-label fw-bold small text-muted">WRITING</label>
            <input type="number" id="timeLimit_WRITING" class="form-control"
                   placeholder="Không giới hạn" min="1">
        </div>
    </div>
</div>
```

- [ ] **Step: Add mode notice banner to Step 1 form**

Add inside `section-step-1`, before the form start (inside the card body, after opening `<form>`):

```html
<div id="assignmentModeNotice" class="alert d-flex align-items-center gap-2 mb-4"
     style="display:none; background:#eff6ff; border:1px solid #bfdbfe;
            border-radius:12px; color:#1e40af; font-size:0.82rem; padding:10px 14px;">
    <i class="bi bi-info-circle-fill"></i>
    <span><strong>Chế độ:</strong> Course Assignment — 4 kỹ năng tuần tự
        (LISTENING → READING → SPEAKING → WRITING)</span>
</div>
```

- [ ] **Step: Hide `questionOrder` row for assignment mode**

Wrap the existing `questionOrder` row in a div with `id="questionOrderRow"`:
```html
<div id="questionOrderRow" class="col-md-3">
    <label class="form-label fw-bold small text-muted">Thứ tự câu hỏi</label>
    <select id="questionOrder" class="form-select">
        <option value="FIXED">Cố định</option>
        <option value="RANDOM">Ngẫu nhiên</option>
    </select>
</div>
```

- [ ] **Step: Add course loading for COURSE_ASSIGNMENT**

Extend the existing `loadCourses()` or add course loading logic in `onCategoryChange()`:

```javascript
if (cat === 'COURSE_ASSIGNMENT' || cat === 'COURSE_QUIZ') {
    loadCourses();
}
```

Also ensure the `courseId` select shows for both `COURSE_QUIZ` and `COURSE_ASSIGNMENT`:
```javascript
if (courseSection) {
    courseSection.style.display =
        (cat === 'COURSE_QUIZ' || cat === 'COURSE_ASSIGNMENT') ? 'block' : 'none';
}
```

---

## Chunk 2: Dynamic Stepper + Section Visibility

**Goal:** Rewrite `goToStep()` to handle both 3-step and 6-step navigation, and add skill sections HTML.

**File:** `DoAn/src/main/resources/templates/expert/quiz-create.html`

---

### Task 2.1: Rewrite `goToStep()` for hybrid mode

- [ ] **Step: Replace the existing `goToStep()` function**

Find the existing `goToStep(step)` function (around line 371). Replace entirely with:

```javascript
function goToStep(step) {
    // UI: hide all sections
    document.querySelectorAll('.quiz-step-section').forEach(s => s.style.display = 'none');

    if (_isAssignment) {
        // Assignment mode: 6 steps
        if (step === 1) {
            document.getElementById('section-step-1').style.display = 'block';
        } else if (step >= 2 && step <= 5) {
            // Steps 2-5 = LISTENING, READING, SPEAKING, WRITING
            const skills = ['LISTENING', 'READING', 'SPEAKING', 'WRITING'];
            const skill = skills[step - 2];
            loadSkillSection(skill);
        } else if (step === 6) {
            initStep6_Preview();
        }
    } else {
        // Quiz mode: 3 steps (existing behavior)
        if (step === 1) {
            document.getElementById('section-step-1').style.display = 'block';
        } else if (step === 2) {
            initStep2();
        } else if (step === 3) {
            initStep3();
        }
    }

    // Update stepper UI
    updateStepperUI(step);
    _currentStep = step;
}
```

- [ ] **Step: Add `updateStepperUI(n)` helper**

Add after `goToStep()`:

```javascript
function updateStepperUI(currentStep) {
    // For assignment mode, regenerate stepper HTML dynamically
    if (_isAssignment) {
        const stepper = document.getElementById('mainStepperInner');
        const skills = ['LISTENING', 'READING', 'SPEAKING', 'WRITING'];
        const totalSteps = 6;

        let html = '<div class="stepper-line"></div>';
        // Step 1: Cấu hình
        html += buildStepItem(1, 'Cấu hình', currentStep);
        // Steps 2-5: Skills
        skills.forEach((skill, idx) => {
            html += buildStepItem(idx + 2, skill, currentStep);
        });
        // Step 6: Hoàn thiện
        html += buildStepItem(6, 'Hoàn thiện', currentStep);

        stepper.innerHTML = html;
    } else {
        // Quiz mode: restore simple 3-step stepper
        const stepper = document.getElementById('mainStepperInner');
        stepper.innerHTML = `
            <div class="stepper-line"></div>
            ${buildStepItem(1, 'Thiết lập', currentStep)}
            ${buildStepItem(2, 'Nội dung', currentStep)}
            ${buildStepItem(3, 'Hoàn thiện', currentStep)}
        `;
    }
}

function buildStepItem(num, label, currentStep) {
    const isCompleted = num < currentStep;
    const isActive = num === currentStep;
    const cls = isCompleted ? 'completed' : (isActive ? 'active' : '');
    const icon = isCompleted ? '<i class="bi bi-check-lg"></i>' : num;
    return `
        <div class="step-item ${cls}" id="step-i-${num}">
            <div class="step-icon">${icon}</div>
            <div class="step-label">${label}</div>
        </div>
    `;
}
```

- [ ] **Step: Update stepper HTML structure in the page**

Find the existing stepper HTML in `section-step-1` (line ~43-59). Replace with:

```html
<div class="quiz-stepper mb-4" id="mainStepper">
    <div class="d-flex justify-content-between position-relative" id="mainStepperInner">
        <!-- Dynamically generated by updateStepperUI() -->
        <!-- Default: quiz mode (3 steps) -->
        <div class="stepper-line"></div>
        <div class="step-item active" id="step-i-1">
            <div class="step-icon">1</div>
            <div class="step-label">Thiết lập</div>
        </div>
        <div class="step-item" id="step-i-2">
            <div class="step-icon">2</div>
            <div class="step-label">Nội dung</div>
        </div>
        <div class="step-item" id="step-i-3">
            <div class="step-icon">3</div>
            <div class="step-label">Hoàn thiện</div>
        </div>
    </div>
</div>
```

- [ ] **Step: Add skill section HTML after `section-step-3`**

Append this HTML before `</main>` (after `section-step-3`, around line 239):

```html
<!-- SKILL SECTIONS (Assignment mode only) -->
<div id="section-listening" class="quiz-step-section skill-section" style="display:none;" data-skill="LISTENING">
    <!-- Populated by loadSkillSection('LISTENING') -->
</div>
<div id="section-reading" class="quiz-step-section skill-section" style="display:none;" data-skill="READING">
</div>
<div id="section-speaking" class="quiz-step-section skill-section" style="display:none;" data-skill="SPEAKING">
</div>
<div id="section-writing" class="quiz-step-section skill-section" style="display:none;" data-skill="WRITING">
</div>
```

---

## Chunk 3: Skill Section Rendering + Question Management

**Goal:** Implement `loadSkillSection()`, `renderSkillQuestions()`, and the bank/create tab UI for each skill.

**File:** `DoAn/src/main/resources/templates/expert/quiz-create.html`

---

### Task 3.1: Implement skill section rendering

- [ ] **Step: Add `loadSkillSection(skill)` function**

Add in the `<script>` block:

```javascript
let _currentSkill = null;
let _skillQuestions = {}; // { LISTENING: [...], READING: [...], ... }

async function loadSkillSection(skill) {
    _currentSkill = skill;
    const section = document.getElementById('section-' + skill.toLowerCase());

    // Build the split-pane HTML
    section.innerHTML = buildSkillSectionHTML(skill);
    section.style.display = 'block';

    // Load questions for this skill from backend
    await loadSkillQuestions(skill);
    renderSkillQuestions(skill);

    // Load bank questions
    await loadBankQuestionsForSkill(skill, 0);

    // Set up validation guard for "Next" button
    const nextBtn = section.querySelector('.btn-next-skill');
    if (nextBtn) {
        nextBtn.setAttribute('onclick', 'validateAndNextSkill()');
    }
}
```

- [ ] **Step: Add `buildSkillSectionHTML(skill)` template function**

```javascript
function buildSkillSectionHTML(skill) {
    const skillLabel = skill.charAt(0) + skill.slice(1).toLowerCase();
    const skillIcon = {
        LISTENING: 'bi-ear',
        READING: 'bi-book',
        SPEAKING: 'bi-mic',
        WRITING: 'bi-pen'
    }[skill] || 'bi-question';

    return `
    <div class="card border-0 shadow-sm">
        <div class="card-body p-0">
            <div class="row g-0" style="min-height: 600px;">
                <!-- LEFT: Questions added to this skill -->
                <div class="col-md-5 border-end">
                    <div class="p-3 border-bottom bg-white">
                        <div class="d-flex justify-content-between align-items-center">
                            <h5 class="fw-bold mb-0" style="color: #223a58;">
                                <i class="bi ${skillIcon} me-1"></i> ${skillLabel}
                            </h5>
                            <span class="badge bg-primary rounded-pill" id="skill-count-${skill}">0 câu</span>
                        </div>
                    </div>
                    <div id="addedQuestionsList-${skill}" class="overflow-auto p-3" style="max-height: 520px;">
                        <div class="text-center py-5 text-muted">
                            <i class="bi bi-inbox fs-1 d-block mb-2"></i>
                            Chưa có câu hỏi nào.
                        </div>
                    </div>
                </div>

                <!-- RIGHT: Bank browser + Create -->
                <div class="col-md-7 p-0 bg-light bg-opacity-10 d-flex flex-column">
                    <div class="p-3 border-bottom bg-white">
                        <h5 class="fw-bold mb-0">Ngân hàng câu hỏi</h5>
                    </div>

                    <!-- Inner tabs -->
                    <div class="px-3 pt-2 pb-1 bg-white">
                        <div class="d-flex gap-2">
                            <button class="inner-tab active" id="tab-bank-${skill}"
                                    onclick="switchSkillTab('${skill}', 'bank')">
                                📚 Ngân hàng
                            </button>
                            <button class="inner-tab" id="tab-create-${skill}"
                                    onclick="switchSkillTab('${skill}', 'create')">
                                ✏️ Tạo mới
                            </button>
                        </div>
                    </div>

                    <!-- Filter row -->
                    <div class="px-3 py-2 bg-white">
                        <div class="row g-2">
                            <div class="col-md-4">
                                <select id="modalSkill-${skill}" class="form-select form-select-sm"
                                        onchange="loadBankQuestionsForSkill('${skill}', 0)">
                                    <option value="">-- Tất cả Kỹ năng --</option>
                                    <option value="LISTENING" ${skill==='LISTENING'?'selected':''}>LISTENING</option>
                                    <option value="READING" ${skill==='READING'?'selected':''}>READING</option>
                                    <option value="SPEAKING" ${skill==='SPEAKING'?'selected':''}>SPEAKING</option>
                                    <option value="WRITING" ${skill==='WRITING'?'selected':''}>WRITING</option>
                                </select>
                            </div>
                            <div class="col-md-3">
                                <select id="modalCefr-${skill}" class="form-select form-select-sm"
                                        onchange="loadBankQuestionsForSkill('${skill}', 0)">
                                    <option value="">-- CEFR --</option>
                                    <option>A1</option><option>A2</option>
                                    <option>B1</option><option>B2</option>
                                    <option>C1</option><option>C2</option>
                                </select>
                            </div>
                            <div class="col-md-4">
                                <input type="text" id="modalKeyword-${skill}" class="form-control form-control-sm"
                                       placeholder="Tìm kiếm..."
                                       onkeyup="if(event.key==='Enter') loadBankQuestionsForSkill('${skill}', 0)">
                            </div>
                            <div class="col-md-1">
                                <button class="btn btn-sm btn-primary w-100"
                                        onclick="loadBankQuestionsForSkill('${skill}', 0)">
                                    <i class="bi bi-search"></i>
                                </button>
                            </div>
                        </div>
                    </div>

                    <!-- Bank panel -->
                    <div id="bank-panel-${skill}" class="flex-grow-1 overflow-auto">
                        <table class="table table-hover align-middle mb-0" style="font-size: 0.85rem;">
                            <thead class="table-light sticky-top">
                                <tr>
                                    <th class="ps-4">Nội dung</th>
                                    <th>Loại</th>
                                    <th>CEFR</th>
                                    <th class="text-end pe-4">Hành động</th>
                                </tr>
                            </thead>
                            <tbody id="bankQuestionsBody-${skill}">
                                <tr><td colspan="4" class="text-center py-5 text-muted">Đang tải...</td></tr>
                            </tbody>
                        </table>
                    </div>
                    <div class="p-2 bg-white border-top">
                        <nav><ul class="pagination pagination-sm justify-content-center mb-0"
                               id="bankPagination-${skill}"></ul></nav>
                    </div>

                    <!-- Create panel (hidden) -->
                    <div id="create-panel-${skill}" class="flex-grow-1 overflow-auto p-3" style="display:none;">
                        ${buildCreateFormHTML(skill)}
                    </div>
                </div>
            </div>

            <!-- Bottom nav -->
            <div class="p-3 border-top d-flex justify-content-between align-items-center bg-white">
                <button class="btn btn-outline-secondary px-4" style="border-radius: 12px;"
                        onclick="goToStep(${_getPrevStep(skill)})">
                    <i class="bi bi-arrow-left me-1"></i> Quay lại
                </button>
                <div class="small text-muted">Tự động lưu...</div>
                <button class="btn btn-primary px-5 fw-bold btn-next-skill" style="border-radius: 12px;"
                        onclick="validateAndNextSkill()">
                    ${skill === 'WRITING' ? 'Tiếp theo: Xem trước' : 'Tiếp theo: ' + _getNextSkill(skill)}
                    <i class="bi bi-arrow-right ms-1"></i>
                </button>
            </div>
        </div>
    </div>
    `;
}

function _getPrevStep(skill) {
    const order = ['LISTENING', 'READING', 'SPEAKING', 'WRITING'];
    const idx = order.indexOf(skill);
    return idx === 0 ? 1 : idx + 1; // LISTENING goes back to Step 1 (Config)
}

function _getNextSkill(skill) {
    const order = ['LISTENING', 'READING', 'SPEAKING', 'WRITING'];
    const idx = order.indexOf(skill);
    return idx < order.length - 1 ? order[idx + 1] : null;
}
```

- [ ] **Step: Add `buildCreateFormHTML(skill)` for inline question creation**

```javascript
function buildCreateFormHTML(skill) {
    let questionTypeOptions = '';
    if (skill === 'LISTENING') {
        questionTypeOptions = `
            <option value="MULTIPLE_CHOICE_SINGLE">Trắc nghiệm (1 đáp án)</option>
            <option value="MULTIPLE_CHOICE_MULTI">Trắc nghiệm (nhiều đáp án)</option>
            <option value="FILL_IN_BLANK">Điền từ</option>
        `;
    } else if (skill === 'READING') {
        questionTypeOptions = `
            <option value="MULTIPLE_CHOICE_SINGLE">Trắc nghiệm (1 đáp án)</option>
            <option value="MULTIPLE_CHOICE_MULTI">Trắc nghiệm (nhiều đáp án)</option>
            <option value="FILL_IN_BLANK">Điền từ</option>
            <option value="MATCHING">Nối đáp án</option>
        `;
    } else if (skill === 'SPEAKING') {
        questionTypeOptions = `<option value="SPEAKING">Nói (Speaking)</option>`;
    } else if (skill === 'WRITING') {
        questionTypeOptions = `<option value="WRITING">Viết (Writing)</option>`;
    }

    const audioField = (skill === 'LISTENING' || skill === 'SPEAKING')
        ? `<div class="mb-3">
               <label class="form-label fw-bold small">Audio (tùy chọn)</label>
               <input type="file" id="createAudio-${skill}" class="form-control" accept="audio/*">
           </div>`
        : '';

    return `
    <div class="alert d-flex gap-2 align-items-start mb-3"
         style="background:#eff6ff; border:1px solid #bfdbfe; border-radius:10px;
                font-size:0.82rem; color:#1e40af; padding:10px 14px;">
        <i class="bi bi-lightbulb-fill"></i>
        <div>Câu hỏi tạo mới sẽ được lưu vào ngân hàng và tự động thêm vào Assignment này.</div>
    </div>
    <div class="mb-3">
        <label class="form-label fw-bold small">Loại câu hỏi</label>
        <select id="createType-${skill}" class="form-select form-select-sm"
                style="border-radius:10px;" onchange="onCreateTypeChange('${skill}')">
            ${questionTypeOptions}
        </select>
    </div>
    <div class="mb-3">
        <label class="form-label fw-bold small">Nội dung câu hỏi <span class="text-danger">*</span></label>
        <textarea id="createContent-${skill}" class="form-control" rows="3"
                  style="border-radius:10px;" placeholder="Nhập nội dung..."></textarea>
    </div>
    ${audioField}
    <div id="createOptions-${skill}" class="mb-3">
        <!-- Options rendered by onCreateTypeChange -->
    </div>
    <button class="btn btn-primary w-100 fw-bold" style="border-radius:12px;"
            onclick="createAndAddQuestion('${skill}')">
        <i class="bi bi-check-lg me-1"></i> Lưu & Thêm vào Assignment
    </button>
    `;
}
```

- [ ] **Step: Add `loadSkillQuestions(skill)` and `renderSkillQuestions(skill)`**

```javascript
async function loadSkillQuestions(skill) {
    if (!_quizId) return;
    try {
        const res = await fetch(`/api/v1/expert/quizzes/${_quizId}/skills`);
        const result = await res.json();
        if (result.status === 200 && result.data) {
            _skillQuestions[skill] = result.data[skill]?.questions || [];
        } else {
            _skillQuestions[skill] = [];
        }
    } catch (e) {
        console.error('Failed to load skill questions:', e);
        _skillQuestions[skill] = [];
    }
}

function renderSkillQuestions(skill) {
    const container = document.getElementById('addedQuestionsList-' + skill);
    const questions = _skillQuestions[skill] || [];
    const countBadge = document.getElementById('skill-count-' + skill);

    if (countBadge) countBadge.textContent = `${questions.length} câu`;

    if (questions.length === 0) {
        container.innerHTML = `
            <div class="text-center py-5 text-muted">
                <i class="bi bi-inbox fs-1 d-block mb-2"></i>
                Chưa có câu hỏi nào. Chọn từ Ngân hàng hoặc Tạo mới.
            </div>`;
        return;
    }

    // Count groups vs lone questions
    const groups = new Map();
    const lones = [];
    questions.forEach(q => {
        if (q.groupId) {
            if (!groups.has(q.groupId)) groups.set(q.groupId, { id: q.groupId, content: q.groupContent, items: [] });
            groups.get(q.groupId).items.push(q);
        } else {
            lones.push(q);
        }
    });

    let html = '';
    groups.forEach(g => {
        html += `
        <div class="card mb-3 border shadow-sm" style="border-radius:12px; border-left:5px solid #7c3aed !important;">
            <div class="card-header bg-white border-bottom-0 pt-3 pb-0 d-flex justify-content-between align-items-start">
                <div class="d-flex align-items-center">
                    <button class="btn btn-sm btn-light me-2 rounded-circle p-0"
                            style="width:24px;height:24px;"
                            onclick="toggleGroup('${skill}', ${g.id})">
                        <i class="bi bi-plus"></i>
                    </button>
                    <div class="small fw-bold" style="color:#6d28d9;"><i class="bi bi-journal-text me-1"></i> BỘ CÂU HỎI</div>
                </div>
                <button class="btn btn-sm btn-link text-danger p-0"
                        onclick="removeGroupFromSkill('${skill}', ${g.id})">
                    <i class="bi bi-trash"></i> Gỡ bộ
                </button>
            </div>
            <div class="card-body pt-2">
                <div class="text-dark small mb-2 border-bottom pb-2" style="font-style:italic;">${g.content || '—'}</div>
                <div id="group-items-${skill}-${g.id}">
                    ${g.items.map((sq, idx) => `
                        <div class="d-flex justify-content-between align-items-center mb-2">
                            <div class="small text-muted">
                                <span class="badge bg-light text-dark me-1">${idx+1}</span>
                                ${sq.questionContent || '—'}
                            </div>
                            <button class="btn btn-sm btn-outline-danger border-0"
                                    onclick="removeQuestionFromSkill('${skill}', ${sq.questionId})">
                                <i class="bi bi-trash"></i>
                            </button>
                        </div>
                    `).join('')}
                </div>
            </div>
        </div>`;
    });

    lones.forEach(q => {
        html += `
        <div class="card mb-2 border shadow-none" style="border-radius:12px;">
            <div class="card-body p-3">
                <div class="d-flex justify-content-between align-items-start">
                    <div style="max-width:85%;">
                        <div class="small fw-bold mb-1" style="color:#166534;">
                            CÂU LẺ • ${typeName(q.questionType)}
                        </div>
                        <div class="text-dark small">${q.questionContent || '—'}</div>
                    </div>
                    <button class="btn btn-sm btn-outline-danger border-0"
                            onclick="removeQuestionFromSkill('${skill}', ${q.questionId})">
                        <i class="bi bi-trash"></i>
                    </button>
                </div>
            </div>
        </div>`;
    });

    container.innerHTML = html;
}

let _expandedSkillGroups = new Set();
function toggleGroup(skill, groupId) {
    const key = skill + '-' + groupId;
    if (_expandedSkillGroups.has(key)) _expandedSkillGroups.delete(key);
    else _expandedSkillGroups.add(key);
    renderSkillQuestions(skill);
}
```

- [ ] **Step: Add `loadBankQuestionsForSkill(skill, page)`**

```javascript
let _skillBankPage = {};

async function loadBankQuestionsForSkill(skill, page) {
    _skillBankPage[skill] = page;
    const params = new URLSearchParams();
    const skillFilter = document.getElementById('modalSkill-' + skill)?.value;
    const cefr = document.getElementById('modalCefr-' + skill)?.value;
    const keyword = document.getElementById('modalKeyword-' + skill)?.value;
    if (skillFilter) params.append('skill', skillFilter);
    if (cefr) params.append('cefrLevel', cefr);
    if (keyword) params.append('keyword', keyword);
    params.append('status', 'PUBLISHED');
    params.append('page', page);
    params.append('size', 10);

    try {
        const res = await fetch(`/api/v1/expert/question-bank?${params}`);
        const result = await res.json();
        renderSkillBankQuestions(skill, result);
    } catch (e) { console.error(e); }
}

function renderSkillBankQuestions(skill, result) {
    const tbody = document.getElementById('bankQuestionsBody-' + skill);
    if (!result || result.status !== 200 || !result.data?.items?.length) {
        tbody.innerHTML = '<tr><td colspan="4" class="text-center py-5 text-muted">Không tìm thấy câu hỏi.</td></tr>';
        return;
    }

    const existingIds = new Set((_skillQuestions[skill] || []).map(q => q.questionId));
    tbody.innerHTML = result.data.items.map(q => {
        const isAdded = existingIds.has(q.id);
        return `
            <tr>
                <td class="ps-4">
                    <div class="text-truncate" style="max-width:250px;" title="${q.content}">${q.content || '—'}</div>
                    <div class="extra-small text-muted">${q.skill || '—'}</div>
                </td>
                <td><span class="badge bg-light text-dark">${typeName(q.questionType)}</span></td>
                <td><span class="badge bg-info text-dark">${q.cefrLevel || '—'}</span></td>
                <td class="text-end pe-4">
                    ${isAdded
                        ? '<span class="badge bg-success rounded-pill px-3 py-2 fw-bold"><i class="bi bi-check-lg me-1"></i>Đã thêm</span>'
                        : `<button class="btn btn-sm btn-primary rounded-pill px-3 fw-bold"
                                   onclick="addQuestionToSkill('${skill}', ${q.id}, '${q.type || 'SINGLE'}')">
                               <i class="bi bi-plus-lg me-1"></i>Thêm
                           </button>`
                    }
                </td>
            </tr>`;
    }).join('');

    // Pagination
    const pg = document.getElementById('bankPagination-' + skill);
    let pgHtml = '';
    for (let i = 0; i < result.data.totalPages; i++) {
        pgHtml += `<li class="page-item ${i === _skillBankPage[skill] ? 'active' : ''}">
            <a class="page-link" href="javascript:void(0)"
               onclick="loadBankQuestionsForSkill('${skill}', ${i})">${i+1}</a>
        </li>`;
    }
    if (pg) pg.innerHTML = pgHtml;
}
```

- [ ] **Step: Add question add/remove/removeGroup functions**

```javascript
async function addQuestionToSkill(skill, questionId, itemType) {
    try {
        const res = await fetch(`/api/v1/expert/quizzes/${_quizId}/section/questions`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                questionIds: [questionId],
                skill: skill,
                itemType: itemType || 'SINGLE'
            })
        });
        if (res.ok) {
            await loadSkillQuestions(skill);
            renderSkillQuestions(skill);
            // Refresh bank
            loadBankQuestionsForSkill(skill, _skillBankPage[skill] || 0);
        } else {
            const r = await res.json();
            alert(r.message || 'Không thể thêm câu hỏi.');
        }
    } catch (e) { alert('Lỗi: ' + e.message); }
}

async function removeQuestionFromSkill(skill, questionId) {
    if (!confirm('Gỡ câu hỏi này khỏi phần ' + skill + '?')) return;
    try {
        const res = await fetch(`/api/v1/expert/quizzes/${_quizId}/section/questions/${questionId}`, {
            method: 'DELETE'
        });
        if (res.ok) {
            await loadSkillQuestions(skill);
            renderSkillQuestions(skill);
            loadBankQuestionsForSkill(skill, _skillBankPage[skill] || 0);
        }
    } catch (e) { alert('Lỗi: ' + e.message); }
}

async function removeGroupFromSkill(skill, groupId) {
    if (!confirm('Gỡ toàn bộ bộ câu hỏi này?')) return;
    // For groups: remove all child questions
    const questions = _skillQuestions[skill] || [];
    const groupQuestions = questions.filter(q => q.groupId === groupId);
    for (const q of groupQuestions) {
        await fetch(`/api/v1/expert/quizzes/${_quizId}/section/questions/${q.questionId}`, {
            method: 'DELETE'
        });
    }
    await loadSkillQuestions(skill);
    renderSkillQuestions(skill);
    loadBankQuestionsForSkill(skill, _skillBankPage[skill] || 0);
}
```

- [ ] **Step: Add `switchSkillTab(skill, tab)` function**

```javascript
function switchSkillTab(skill, tab) {
    // Update tab buttons
    document.getElementById('tab-bank-' + skill).classList.toggle('active', tab === 'bank');
    document.getElementById('tab-create-' + skill).classList.toggle('active', tab === 'create');

    // Show/hide panels
    document.getElementById('bank-panel-' + skill).style.display = tab === 'bank' ? '' : 'none';
    document.getElementById('create-panel-' + skill).style.display = tab === 'create' ? '' : 'none';
}
```

---

## Chunk 4: Validation Guards + Step 6 Preview + Submit Wiring

**Goal:** Connect Step 1 submit to the correct API, add validation guards between skill steps, and implement Step 6 preview.

**File:** `DoAn/src/main/resources/templates/expert/quiz-create.html`

---

### Task 4.1: Wire Step 1 submit for assignment mode

- [ ] **Step: Update `validateAndGoToStep2()` to handle both modes**

Find the existing `validateAndGoToStep2()` (around line 389). Replace with:

```javascript
function validateAndGoToStep2() {
    const title = document.getElementById('title').value;
    if (!title) { alert('Vui lòng nhập tên Quiz'); return; }

    if (_isAssignment) {
        // Assignment: use dedicated endpoint
        submitAssignmentConfig();
    } else {
        // Quiz: existing behavior
        if (!_quizId) {
            submitQuiz('DRAFT');
        } else {
            goToStep(2);
        }
    }
}

async function submitAssignmentConfig() {
    const cat = document.getElementById('quizCategory').value;
    const courseSection = document.getElementById('courseSection');
    const courseIdSelect = document.getElementById('courseId');

    const body = {
        title: document.getElementById('title').value,
        description: document.getElementById('description').value || null,
        quizCategory: cat,
        courseId: (cat === 'COURSE_ASSIGNMENT' && courseIdSelect?.value)
            ? parseInt(courseIdSelect.value) : null,
        moduleId: null,
        timeLimitPerSkill: buildTimeLimitPerSkill(),
        passScore: parseFloat(document.getElementById('passScore')?.value) || null,
        maxAttempts: parseInt(document.getElementById('maxAttempts')?.value) || null,
        showAnswerAfterSubmit: document.getElementById('showAnswerAfterSubmit')?.checked ?? true,
        isSequential: true,
        skillOrder: ['LISTENING', 'READING', 'SPEAKING', 'WRITING'],
        status: 'DRAFT'
    };

    try {
        const res = await fetch('/api/v1/expert/quizzes', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        const result = await res.json();
        if (res.ok) {
            _quizId = result.data.quizId;
            goToStep(2); // LISTENING
        } else {
            alert(result.message || 'Có lỗi xảy ra khi tạo Assignment.');
        }
    } catch (e) {
        alert('Lỗi: ' + e.message);
    }
}

function buildTimeLimitPerSkill() {
    const result = {};
    ['LISTENING', 'READING', 'SPEAKING', 'WRITING'].forEach(skill => {
        const el = document.getElementById('timeLimit_' + skill);
        if (el?.value) result[skill] = parseInt(el.value);
    });
    return Object.keys(result).length > 0 ? result : null;
}
```

- [ ] **Step: Add `validateAndNextSkill()` guard function**

```javascript
function validateAndNextSkill() {
    const skills = ['LISTENING', 'READING', 'SPEAKING', 'WRITING'];
    const currentIdx = skills.indexOf(_currentSkill);
    const questions = _skillQuestions[_currentSkill] || [];
    const count = questions.length;

    if (count === 0) {
        alert(`Phần ${_currentSkill} chưa có câu hỏi nào.\nVui lòng thêm ít nhất 1 câu trước khi chuyển bước.`);
        return;
    }

    if (_currentSkill === 'WRITING') {
        goToStep(6); // WRITING → Preview
    } else {
        goToStep(currentIdx + 3); // LISTENING→3, READING→4, SPEAKING→5
    }
}
```

- [ ] **Step: Add `initStep6_Preview()` and `finalPublish()` for assignment**

```javascript
let _assignmentPreview = null;

async function initStep6_Preview() {
    if (!_quizId) { alert('Không có Assignment ID'); return; }

    // Show section
    const section = document.getElementById('section-step-3');
    section.style.display = 'block';

    // Show the preview card
    const previewCard = document.getElementById('assignmentPreviewCard');
    if (previewCard) previewCard.style.display = 'block';

    try {
        const res = await fetch(`/api/v1/expert/quizzes/${_quizId}/preview`);
        const result = await res.json();
        if (result.status === 200 && result.data) {
            _assignmentPreview = result.data;
            renderAssignmentPreview(result.data);
        }
    } catch (e) { console.error(e); }
}

function renderAssignmentPreview(data) {
    // Title
    const titleEl = document.getElementById('previewTitle');
    if (titleEl) titleEl.textContent = data.title || 'Assignment của bạn';

    // Summary
    const totalMinutes = Object.values(data.timeLimits || {}).reduce((a, b) => a + b, 0);
    const summaryEl = document.getElementById('previewSummary');
    if (summaryEl) summaryEl.textContent =
        `${data.totalQuestions || 0} câu hỏi | ${totalMinutes || '?'} phút | Điểm đạt ${data.passScore || 0}%`;

    // Skill summary cards
    const container = document.getElementById('previewSkillCards');
    if (container && data.skillSummaries) {
        container.innerHTML = data.skillSummaries.map(s => {
            const time = data.timeLimits?.[s.skill] || null;
            const isOk = s.questionCount > 0;
            return `
            <div class="preview-skill ${isOk ? 'ok' : 'missing'}" style="border-radius:12px;
                        border: 2px solid ${isOk ? '#10b981' : '#f59e0b'};
                        background: ${isOk ? '#f0fdf4' : '#fffbeb'};
                        padding: 14px; text-align:center; min-width: 130px;">
                <div class="fw-bold mb-2" style="font-size:0.75rem;
                     color: ${isOk ? '#166534' : '#92400e'};">${s.skill}</div>
                <div class="fw-bold" style="font-size:1.4rem;
                     color: ${isOk ? '#10b981' : '#f59e0b'};">${s.questionCount} câu</div>
                <div class="small text-muted">${time ? time + ' phút' : '—'}</div>
                <div class="mt-1">
                    ${isOk
                        ? '<i class="bi bi-check-circle-fill" style="color:#10b981;"></i>'
                        : '<i class="bi bi-exclamation-triangle-fill" style="color:#f59e0b;"></i>'}
                </div>
            </div>`;
        }).join('');
    }

    // Update final publish button
    const allReady = data.allSkillsReady;
    const publishBtn = document.getElementById('btnFinalPublish');
    if (publishBtn) {
        publishBtn.disabled = !allReady;
        publishBtn.title = allReady
            ? 'Xuất bản Assignment này'
            : 'Cần thêm câu hỏi cho các skill còn thiếu';
    }
}

async function finalPublish() {
    if (!confirm('Xác nhận xuất bản Assignment này?')) return;
    try {
        const res = await fetch(`/api/v1/expert/quizzes/${_quizId}/publish`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' }
        });
        if (res.ok) {
            alert('Chúc mừng! Assignment đã được xuất bản thành công.');
            window.location.href = '/expert/quiz-management';
        } else {
            const r = await res.json();
            alert(r.message || 'Lỗi khi xuất bản: ' + (r.message || ''));
        }
    } catch (e) { alert('Lỗi: ' + e.message); }
}
```

- [ ] **Step: Update Step 3 preview section for assignment**

Find the existing `section-step-3` HTML (around line 223). Replace the success card with a hybrid that shows both quiz and assignment content:

```html
<!-- STEP 3 / STEP 6: Preview -->
<div id="section-step-3" class="quiz-step-section" style="display:none;">
    <!-- Assignment preview card (shown for assignment mode) -->
    <div id="assignmentPreviewCard" class="card border-0 shadow-sm" style="display:none;">
        <div class="card-body p-0">
            <div class="text-center py-5 px-4">
                <div class="mb-4">
                    <i class="bi bi-check-circle-fill text-success" style="font-size: 4.5rem;"></i>
                </div>
                <h3 class="fw-bold" id="previewTitle">Tên Assignment</h3>
                <p class="text-muted mb-4" id="previewSummary">Tóm tắt: 0 câu | 0 phút | Điểm đạt 0%</p>

                <!-- Per-skill summary -->
                <div class="d-flex justify-content-center gap-3 flex-wrap mb-4" id="previewSkillCards">
                    <!-- Filled by renderAssignmentPreview() -->
                </div>

                <div class="d-flex justify-content-center gap-3">
                    <button class="btn btn-lg btn-outline-secondary px-5"
                            onclick="goToStep(${_isAssignment ? 5 : 2})"
                            style="border-radius:12px;">
                        <i class="bi bi-arrow-left me-1"></i> Quay lại sửa
                    </button>
                    <button id="btnFinalPublish"
                            class="btn btn-lg btn-success px-5 fw-bold"
                            onclick="finalPublish()"
                            style="border-radius:12px;">
                        <i class="bi bi-rocket-takeoff me-2"></i> Xuất bản ngay
                    </button>
                </div>
            </div>
        </div>
    </div>

    <!-- Original quiz-only preview (shown for quiz mode) -->
    <div id="quizPreviewCard" class="card border-0 shadow-sm text-center py-5 px-4">
        <div class="py-4">
            <div class="mb-4">
                <i class="bi bi-check-circle-fill text-success" style="font-size: 5rem;"></i>
            </div>
            <h3 class="fw-bold" id="previewTitle">Tên Quiz của bạn</h3>
            <p class="text-muted mb-4" id="previewSummary">Tóm tắt: 0 câu hỏi | 0 phút | Điểm đạt 0%</p>
            <div class="d-flex justify-content-center gap-3">
                <button class="btn btn-lg btn-outline-secondary px-5" onclick="goToStep(2)">
                    <i class="bi bi-arrow-left me-1"></i> Quay lại sửa
                </button>
                <button class="btn btn-lg btn-success px-5 fw-bold" onclick="finalPublish()">
                    <i class="bi bi-rocket-takeoff me-2"></i> Xuất bản ngay
                </button>
            </div>
        </div>
    </div>
</div>
```

- [ ] **Step: Update bottom navigation buttons on skill sections**

The skill sections generated by `buildSkillSectionHTML()` already include `btn-next-skill` with `onclick="validateAndNextSkill()"`. The "Quay lại" button uses `goToStep(_getPrevStep(skill))`.

- [ ] **Step: Update the Step 2 back button in quiz mode**

Find the existing Step 2 back button (around line 214):
```html
<button class="btn btn-outline-secondary px-4" onclick="goToStep(1)">
```

Ensure it's still correct — it calls `goToStep(1)` which is Step 1 Config. ✅

- [ ] **Step: Update `initStep2()` to show quiz-only content in quiz mode**

In `initStep2()` (around line 405), ensure it also hides assignment preview card:
```javascript
function initStep2() {
    // Quiz mode only
    document.getElementById('quizPreviewCard').style.display = 'block';
    document.getElementById('assignmentPreviewCard').style.display = 'none';
    loadQuizQuestions();
    loadBankQuestions(0);
}
```

In `initStep3()` (around line 634):
```javascript
function initStep3() {
    document.getElementById('quizPreviewCard').style.display = 'block';
    document.getElementById('assignmentPreviewCard').style.display = 'none';
    document.getElementById('previewTitle').textContent = document.getElementById('title').value;
    document.getElementById('previewSummary').textContent =
        `Tóm tắt: ${_quizQuestions.length} câu hỏi | ${document.getElementById('timeLimitMinutes').value || '?'} phút | Điểm đạt ${document.getElementById('passScore').value || 0}%`;
}
```

---

## Chunk 5: Inline Question Creation + Polish

**Goal:** Implement `createAndAddQuestion()` and `onCreateTypeChange()` for each skill, plus edge case handling.

**File:** `DoAn/src/main/resources/templates/expert/quiz-create.html`

---

### Task 5.1: Inline question creation flow

- [ ] **Step: Add `onCreateTypeChange(skill)` to render options based on type**

```javascript
function onCreateTypeChange(skill) {
    const type = document.getElementById('createType-' + skill)?.value;
    const container = document.getElementById('createOptions-' + skill);
    if (!container) return;

    if (type === 'MULTIPLE_CHOICE_SINGLE' || type === 'MULTIPLE_CHOICE_MULTI') {
        container.innerHTML = `
            <label class="form-label fw-bold small">Đáp án</label>
            <div id="optionsList-${skill}">
                ${buildOptionRow(skill, 'A', true)}
                ${buildOptionRow(skill, 'B', false)}
            </div>
            <button class="btn btn-sm btn-outline-primary mt-2"
                    onclick="addOptionRow('${skill}')"
                    style="border-radius:8px;">
                <i class="bi bi-plus-lg me-1"></i> Thêm đáp án
            </button>`;
    } else if (type === 'FILL_IN_BLANK') {
        container.innerHTML = `
            <label class="form-label fw-bold small">Đáp án đúng</label>
            <input type="text" id="fillBlankAnswer-${skill}" class="form-control form-control-sm"
                   style="border-radius:10px;" placeholder="Nhập đáp án đúng...">`;
    } else if (type === 'MATCHING') {
        container.innerHTML = `
            <label class="form-label fw-bold small">Cặp đáp án (mỗi dòng: "Câu hỏi - Đáp án")</label>
            <textarea id="matchingPairs-${skill}" class="form-control" rows="4"
                      style="border-radius:10px;"
                      placeholder="apple - quả táo&#10;book - sách"></textarea>`;
    } else if (type === 'SPEAKING') {
        container.innerHTML = `
            <label class="form-label fw-bold small">Gợi ý / Prompt</label>
            <textarea id="speakingPrompt-${skill}" class="form-control" rows="3"
                      style="border-radius:10px;"
                      placeholder="Describe your favorite holiday destination..."></textarea>`;
    } else if (type === 'WRITING') {
        container.innerHTML = `
            <label class="form-label fw-bold small">Yêu cầu bài viết</label>
            <textarea id="writingPrompt-${skill}" class="form-control" rows="4"
                      style="border-radius:10px;"
                      placeholder="Task 1: You should spend about 20 minutes..."></textarea>`;
    } else {
        container.innerHTML = '';
    }
}

function buildOptionRow(skill, letter, isCorrect) {
    return `
    <div class="form-check mb-1 d-flex align-items-center gap-2">
        <input class="form-check-input" type="${skill === 'MULTIPLE_CHOICE_SINGLE' ? 'radio' : 'checkbox'}"
               name="correctOption-${skill}" id="opt-${skill}-${letter}"
               ${isCorrect ? 'checked' : ''} style="width:16px;height:16px;">
        <input type="text" class="form-control form-control-sm"
               id="optText-${skill}-${letter}"
               style="border-radius:8px;flex:1;"
               placeholder="${letter}.">`;
    }

let _optionCount = { };

function addOptionRow(skill) {
    const list = document.getElementById('optionsList-' + skill);
    if (!list) return;
    const letters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
    const idx = list.children.length;
    const letter = letters[idx];
    list.insertAdjacentHTML('beforeend', buildOptionRow(skill, letter, false));
}
```

- [ ] **Step: Add `createAndAddQuestion(skill)` main function**

```javascript
async function createAndAddQuestion(skill) {
    const type = document.getElementById('createType-' + skill)?.value;
    const content = document.getElementById('createContent-' + skill)?.value;

    if (!content?.trim()) {
        alert('Vui lòng nhập nội dung câu hỏi.');
        return;
    }

    let questionPayload = {
        content: content.trim(),
        questionType: type,
        skill: skill,
        status: 'PUBLISHED',
        source: 'EXPERT_BANK'
    };

    // Handle audio upload for LISTENING/SPEAKING
    const audioInput = document.getElementById('createAudio-' + skill);
    if (audioInput?.files?.length > 0) {
        try {
            const uploadRes = await uploadAudio(audioInput.files[0]);
            if (uploadRes?.url) questionPayload.audioUrl = uploadRes.url;
        } catch (e) {
            alert('Upload audio thất bại: ' + e.message);
            return;
        }
    }

    // Type-specific payload
    if (type === 'MULTIPLE_CHOICE_SINGLE' || type === 'MULTIPLE_CHOICE_MULTI') {
        questionPayload.options = buildOptionsPayload(skill, type);
    } else if (type === 'FILL_IN_BLANK') {
        const answer = document.getElementById('fillBlankAnswer-' + skill)?.value;
        if (!answer) { alert('Vui lòng nhập đáp án đúng.'); return; }
        questionPayload.options = [{
            optionText: answer,
            isCorrect: true
        }];
    } else if (type === 'MATCHING') {
        questionPayload.options = parseMatchingPairs(skill);
    }
    // SPEAKING/WRITING: just content + optional prompt
    // audioUrl already set above if present

    try {
        // 1. Create question in bank
        const res = await fetch('/api/v1/expert/questions', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(questionPayload)
        });
        const result = await res.json();

        if (!res.ok) {
            alert(result.message || 'Lỗi khi tạo câu hỏi.');
            return;
        }

        // 2. Immediately add to assignment
        const questionId = result.data?.questionId || result.data?.id;
        if (questionId) {
            await fetch(`/api/v1/expert/quizzes/${_quizId}/section/questions`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    questionIds: [questionId],
                    skill: skill,
                    itemType: 'SINGLE'
                })
            });
        }

        // 3. Refresh skill questions + bank
        await loadSkillQuestions(skill);
        renderSkillQuestions(skill);
        loadBankQuestionsForSkill(skill, _skillBankPage[skill] || 0);

        // 4. Clear form
        document.getElementById('createContent-' + skill).value = '';
        if (document.getElementById('createAudio-' + skill)) {
            document.getElementById('createAudio-' + skill).value = '';
        }
        onCreateTypeChange(skill); // Reset options

        alert('Câu hỏi đã được tạo và thêm vào Assignment!');
    } catch (e) {
        alert('Lỗi: ' + e.message);
    }
}

function buildOptionsPayload(skill, type) {
    const isMulti = type === 'MULTIPLE_CHOICE_MULTI';
    const letters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
    const options = [];
    const list = document.getElementById('optionsList-' + skill);
    if (!list) return options;

    Array.from(list.children).forEach((row, idx) => {
        const letter = letters[idx];
        const textInput = row.querySelector(`#optText-${skill}-${letter}`);
        const checkInput = row.querySelector(`#opt-${skill}-${letter}`);
        if (textInput?.value?.trim()) {
            options.push({
                optionText: textInput.value.trim(),
                isCorrect: isMulti
                    ? (checkInput?.checked ?? false)
                    : (checkInput?.checked ?? false)
            });
        }
    });
    return options;
}

function parseMatchingPairs(skill) {
    const textarea = document.getElementById('matchingPairs-' + skill);
    if (!textarea) return [];
    return textarea.value.split('\n').map(line => {
        const parts = line.split('-').map(p => p.trim());
        return {
            optionText: parts[0] || '',
            isCorrect: true
        };
    }).filter(o => o.optionText);
}
```

- [ ] **Step: Add `uploadAudio(file)` helper**

```javascript
async function uploadAudio(file) {
    const formData = new FormData();
    formData.append('file', file);
    const res = await fetch('/api/v1/upload/audio', { method: 'POST', body: formData });
    if (!res.ok) throw new Error('Upload failed');
    return await res.json();
}
```

---

## Chunk 6: Module Assignment Support + Edge Cases

**Goal:** Support `MODULE_ASSIGNMENT` with module selector, and handle edge cases.

**File:** `DoAn/src/main/resources/templates/expert/quiz-create.html`

---

### Task 6.1: Module selector + edge cases

- [ ] **Step: Add module selector for MODULE_ASSIGNMENT**

Extend `onCategoryChange()` to handle `MODULE_ASSIGNMENT`:

```javascript
function onCategoryChange() {
    const cat = document.getElementById('quizCategory').value;
    const courseSection = document.getElementById('courseSection');
    const moduleSection = document.getElementById('moduleSection');

    const isAssignment = (cat === 'COURSE_ASSIGNMENT' || cat === 'MODULE_ASSIGNMENT');

    if (isAssignment) {
        morphToAssignmentMode();
    } else {
        morphToQuizMode();
    }

    // Course selector
    if (courseSection) {
        courseSection.style.display =
            (cat === 'COURSE_QUIZ' || cat === 'COURSE_ASSIGNMENT') ? 'block' : 'none';
    }

    // Module selector (new)
    if (moduleSection) {
        moduleSection.style.display = (cat === 'MODULE_QUIZ' || cat === 'MODULE_ASSIGNMENT') ? 'block' : 'none';
        if (cat === 'MODULE_QUIZ' || cat === 'MODULE_ASSIGNMENT') {
            loadModules();
        }
    }

    // Load courses for COURSE_ASSIGNMENT
    if (cat === 'COURSE_ASSIGNMENT') {
        loadCourses();
    }
}
```

Add module selector HTML (after `courseSection`):
```html
<div class="col-md-3" id="moduleSection" style="display:none">
    <label class="form-label fw-bold">Chương (Module) <span class="text-danger">*</span></label>
    <select id="moduleId" class="form-select form-select-lg border-2">
        <option value="">-- Chọn chương --</option>
    </select>
</div>
```

Add `loadModules()` function:
```javascript
async function loadModules() {
    const courseId = document.getElementById('courseId')?.value;
    if (!courseId) return;
    const select = document.getElementById('moduleId');
    if (!select) return;

    select.innerHTML = '<option value="">-- Đang tải... --</option>';
    try {
        const res = await fetch(`/api/v1/expert/modules?courseId=${courseId}`);
        const result = await res.json();
        if (result.status === 200 && result.data?.length) {
            select.innerHTML = '<option value="">-- Chọn chương --</option>';
            result.data.forEach(m => {
                select.innerHTML += `<option value="${m.moduleId}">${m.moduleName}</option>`;
            });
        } else {
            select.innerHTML = '<option value="">-- Không có chương --</option>';
        }
    } catch (e) {
        select.innerHTML = '<option value="">-- Lỗi tải --</option>';
    }
}
```

Update `submitAssignmentConfig()` to include `moduleId`:
```javascript
    const body = {
        // ... existing fields ...
        moduleId: (cat === 'MODULE_ASSIGNMENT' && document.getElementById('moduleId')?.value)
            ? parseInt(document.getElementById('moduleId').value) : null,
        // ...
    };
```

- [ ] **Step: Handle "Edit existing assignment" case**

If `quizId` is passed from controller (via Thymeleaf `/*[[${quizId}]]*/`), detect existing quiz type and morph to correct mode:

```javascript
(function() {
    const moduleId = /*[[${moduleId}]]*/ null;
    const quizId = /*[[${quizId}]]*/ null;

    if (quizId) {
        // Editing existing quiz — detect type from existing data
        fetch(`/api/v1/expert/quizzes/${quizId}`)
            .then(r => r.json())
            .then(result => {
                if (result.data?.quizCategory === 'COURSE_ASSIGNMENT' ||
                    result.data?.quizCategory === 'MODULE_ASSIGNMENT') {
                    _isAssignment = true;
                    morphToAssignmentMode();
                    // Restore form values from result.data
                    document.getElementById('title').value = result.data.title || '';
                    document.getElementById('quizCategory').value = result.data.quizCategory;
                    // Restore time limits
                    if (result.data.timeLimitPerSkill) {
                        Object.entries(result.data.timeLimitPerSkill).forEach(([skill, mins]) => {
                            const el = document.getElementById('timeLimit_' + skill);
                            if (el) el.value = mins;
                        });
                    }
                    _quizId = quizId;
                }
            });
    }

    if (moduleId) {
        document.getElementById('quizCategoryCol').style.display = 'none';
    }
})();
```

- [ ] **Step: Fix "Back" navigation on skill sections for LISTENING**

The LISTENING "Quay lại" button should go back to Step 1 (Config). Update `goToStep()` to show Step 1 for assignment back nav:

In `goToStep()`, the assignment mode already handles `step === 1` → `section-step-1`. ✅

---

## Verification & Testing

After implementing all chunks:

1. **Start the app:** `./mvnw spring-boot:run` from `NovaLMS/DoAn`
2. **Navigate to:** `http://localhost:8080/expert/quiz-create`
3. **Test flow A (Quiz mode):**
   - [ ] Select `ENTRY_TEST` → stepper stays 3 steps → Step 2 shows existing split pane
   - [ ] Select `COURSE_QUIZ` → stepper stays 3 steps → course selector shows
4. **Test flow B (Assignment mode):**
   - [ ] Select `COURSE_ASSIGNMENT` → stepper morphs to 6 steps → per-skill time inputs appear → mode notice shows
   - [ ] Fill form + click "Tiếp theo: LISTENING" → Step 2 LISTENING split pane shows
   - [ ] Add question from bank → left panel updates → counter badge updates
   - [ ] Click "Tiếp theo" with 0 questions → alert blocks navigation
   - [ ] Add ≥1 question → "Tiếp theo" goes to READING
   - [ ] Complete all 4 skills → Step 6 preview shows all skill cards with ✓
   - [ ] Click "Xuất bản ngay" → success → redirect to quiz-management
5. **Test flow C (MODULE_ASSIGNMENT):**
   - [ ] Select `MODULE_ASSIGNMENT` → module selector appears → course loads → module loads
   - [ ] Fill form → creates assignment with moduleId

---

## File Changes Summary

| File | Change |
|---|---|
| `expert/quiz-create.html` | **MAJOR** — morph stepper, add skill sections, per-skill time inputs, validation guards, inline creation, module selector |

**No backend files need changing** — all service layer, controllers, entities, and DTOs are already implemented.

---

## Commit Strategy

- **Commit 1:** Stepper morphing + Step 1 enhancement (Chunks 1-2)
- **Commit 2:** Skill section rendering + bank browser (Chunk 3)
- **Commit 3:** Validation guards + Step 6 preview + submit wiring (Chunk 4)
- **Commit 4:** Inline question creation + module support + polish (Chunks 5-6)