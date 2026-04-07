# SPEC 002 — Hybrid Quiz/Assignment Wizard Integration

## Status: APPROVED

## 1. Overview

**Goal:** Integrate 4-skill sequential Assignment creation flow into the existing `quiz-create.html` wizard. The wizard morphs its stepper and content based on the selected `quizCategory`.

**Actor:** Expert
**Template:** `expert/quiz-create.html` (single file, one URL)
**URL:** `GET /expert/quiz-create`

**Two modes:**

| Mode | Quiz Categories | Stepper |
|---|---|---|
| **Quiz mode** (existing) | `ENTRY_TEST`, `COURSE_QUIZ`, `MODULE_QUIZ` | Cấu hình → Nội dung → Hoàn thiện (3 steps) |
| **Assignment mode** (new) | `COURSE_ASSIGNMENT`, `MODULE_ASSIGNMENT` | Cấu hình → LISTENING → READING → SPEAKING → WRITING → Hoàn thiện (6 steps) |

---

## 2. Architecture

### 2.1 URL & Controller

- **URL:** `GET /expert/quiz-create` (existing)
- **Optional query params:** `?quizId={id}&mode=edit` (reuse for editing existing quiz/assignment)
- **Controller:** `ExpertQuizController` — `getQuizCreatePage()` stays as-is; `onCategoryChange()` JS morphs the UI client-side

### 2.2 Stepper Morphing (JavaScript)

On `onCategoryChange()` when user selects `COURSE_ASSIGNMENT` or `MODULE_ASSIGNMENT`:

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
  // 1. Hide step 2 (content) and step 3 (preview) sections
  document.getElementById('section-step-2').style.display = 'none';
  document.getElementById('section-step-3').style.display = 'none';

  // 2. Inject 4 skill steps into stepper DOM
  // Steps 2-5 become: LISTENING, READING, SPEAKING, WRITING
  // Step 6 becomes: Hoàn thiện

  // 3. Add per-skill time limit inputs to Step 1 form

  // 4. Change submit button label: "Tiếp theo: LISTENING"

  // 5. Show mode notice banner

  _isAssignment = true;
}

function morphToQuizMode() {
  // Reverse: hide skill sections, restore quiz steps
  _isAssignment = false;
}
```

### 2.3 Navigation Logic (`goToStep(n)`)

```javascript
function goToStep(n) {
  hideAllSections();

  if (_isAssignment) {
    // Assignment mode: 6 steps
    if (n === 1) showSection1();
    if (n === 2) { validateSkill(1); loadSkillSection('LISTENING'); }
    if (n === 3) { validateSkill(2); loadSkillSection('READING'); }
    if (n === 4) { validateSkill(3); loadSkillSection('SPEAKING'); }
    if (n === 5) { validateSkill(4); loadSkillSection('WRITING'); }
    if (n === 6) initStep6_Preview();
  } else {
    // Quiz mode: 3 steps (existing logic)
    if (n === 1) showSection1();
    if (n === 2) initStep2();
    if (n === 3) initStep3();
  }

  updateStepperUI(n);
  _currentStep = n;
}
```

---

## 3. Step 1 — Cấu hình (Configuration)

### 3.1 Changes to Existing Form

| Element | Behavior |
|---|---|
| `quizCategory` dropdown | Add `COURSE_ASSIGNMENT` and `MODULE_ASSIGNMENT` options |
| `courseId` select | Show for `COURSE_QUIZ`, `COURSE_ASSIGNMENT`. Load via `GET /api/v1/expert/modules/courses` |
| `moduleId` select | Show for `MODULE_QUIZ`, `MODULE_ASSIGNMENT`. Load via `GET /api/v1/expert/modules?courseId={id}` |
| `questionOrder` row | Hide for Assignment (always FIXED) |
| `showAnswerAfterSubmit` | Default to `checked` for Assignment |
| Per-skill time limits | Add 4 inputs: LISTENING, READING, SPEAKING, WRITING (minutes). Shown only for Assignment |
| Mode notice banner | Blue info box: "Chế độ: Course Assignment — 4 kỹ năng tuần tự" |
| Submit button | Label changes: "Tiếp theo: LISTENING" for Assignment |

### 3.2 Step 1 Submit Action

```javascript
async function submitStep1() {
  const cat = document.getElementById('quizCategory').value;

  if (_isAssignment) {
    // Call assignment creation API
    const body = {
      title: document.getElementById('title').value,
      description: document.getElementById('description').value || null,
      quizCategory: cat,
      courseId: getCourseId(),
      moduleId: getModuleId(),
      timeLimitPerSkill: {
        LISTENING: parseInt(document.getElementById('timeLimit_LISTENING').value) || null,
        READING:  parseInt(document.getElementById('timeLimit_READING').value) || null,
        SPEAKING: parseInt(document.getElementById('timeLimit_SPEAKING').value) || null,
        WRITING:  parseInt(document.getElementById('timeLimit_WRITING').value) || null
      },
      passScore: parseFloat(document.getElementById('passScore').value) || null,
      maxAttempts: parseInt(document.getElementById('maxAttempts').value) || null,
      showAnswerAfterSubmit: document.getElementById('showAnswerAfterSubmit').checked,
      isSequential: true,
      skillOrder: ['LISTENING', 'READING', 'SPEAKING', 'WRITING'],
      status: 'DRAFT'
    };

    const res = await fetch('/api/v1/expert/assignments', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    });
    const result = await res.json();
    if (res.ok) {
      _quizId = result.data.quizId;
      goToStep(2); // LISTENING
    }
  } else {
    // Existing quiz creation logic
    submitQuiz('DRAFT');
  }
}
```

---

## 4. Steps 2–5 — Skill Sections

### 4.1 Structure (Reuse Split Pane Layout)

Each skill section uses the same split-pane structure:

```
┌──────────────────────────┬───────────────────────────────────────┐
│ LEFT PANEL (35%)         │ RIGHT PANEL (65%)                      │
│ "Đã thêm vào [SKILL]"    │ Tabbed: [Ngân hàng] [Tạo mới]         │
│                          │                                        │
│ - Question cards         │ Bank: table with filters               │
│ - Passage groups         │ Create: inline form                    │
│ - Remove buttons         │                                        │
│ - Counter badge          │                                        │
└──────────────────────────┴───────────────────────────────────────┘
```

### 4.2 Left Panel Logic

```javascript
async function loadSkillSection(skill) {
  _currentSkill = skill;
  showSection(`section-${skill.toLowerCase()}`);

  // Load questions for this skill only
  const res = await fetch(`/api/v1/expert/assignments/${_quizId}/skills`);
  const result = await res.json();

  const skillData = result.data[skill];
  renderSkillQuestions(skill, skillData.questions || []);
  updateSkillBadge(skill, skillData.questionCount);
}
```

### 4.3 Right Panel — Bank Tab

- Filter: `skill` pre-selected to current skill (readonly)
- CEFR filter
- Keyword search
- Pagination (10 per page)
- Add button → `POST /api/v1/expert/assignments/${_quizId}/questions`
  - Body: `{ questionIds: [id1, id2], skill: "LISTENING", itemType: "SINGLE" }`

### 4.4 Right Panel — Create Tab

Allowed question types per skill:

| Skill | Allowed Types |
|---|---|
| LISTENING | `MULTIPLE_CHOICE_SINGLE`, `MULTIPLE_CHOICE_MULTI`, `FILL_IN_BLANK` |
| READING | `MULTIPLE_CHOICE_SINGLE`, `MULTIPLE_CHOICE_MULTI`, `FILL_IN_BLANK`, `MATCHING` |
| SPEAKING | `SPEAKING` only |
| WRITING | `WRITING` only |

On save → `POST /api/v1/expert/questions` → then `POST /api/v1/expert/assignments/${_quizId}/questions`

### 4.5 Validation Guard (Option A)

```javascript
function validateAndNext() {
  if (_currentSkill === null) {
    goToStep(_currentStep + 1);
    return;
  }

  const count = getSkillQuestionCount(_currentSkill);
  if (count === 0) {
    alert(`Skill ${_currentSkill} chưa có câu hỏi nào. Vui lòng thêm ít nhất 1 câu trước khi chuyển bước.`);
    return;
  }
  goToStep(_currentStep + 1);
}
```

Stepper yellow warning state for skills with 0 questions:
```css
.step-item.warning .step-circle { background: #f59e0b; color: white; }
```

---

## 5. Step 6 — Hoàn thiện (Preview & Publish)

### 5.1 Preview Data

```javascript
async function initStep6_Preview() {
  const res = await fetch(`/api/v1/expert/assignments/${_quizId}/preview`);
  const data = await res.json();

  document.getElementById('previewTitle').textContent = data.title;
  renderSkillSummary(data.skillSummaries);
  renderStats(data.totalQuestions, data.totalMinutes, data.passScore);
}

function renderSkillSummary(summaries) {
  // 4 skill cards: LISTENING / READING / SPEAKING / WRITING
  // Each shows: skill badge, question count, time limit, check/warning icon
  // Green border if ≥1 question, yellow border if 0
}
```

### 5.2 Publish

```javascript
async function finalPublish() {
  if (!confirm('Xác nhận xuất bản Assignment này?')) return;

  const res = await fetch(`/api/v1/expert/assignments/${_quizId}/publish`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' }
  });

  if (res.ok) {
    alert('Chúc mừng! Assignment đã được xuất bản thành công.');
    window.location.href = '/expert/quiz-management';
  } else {
    const r = await res.json();
    alert(r.message || 'Lỗi khi xuất bản.');
  }
}
```

---

## 6. API Changes

### 6.1 New Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/expert/assignments` | Create assignment (Step 1) |
| `GET` | `/api/v1/expert/assignments/{quizId}` | Get assignment detail |
| `PUT` | `/api/v1/expert/assignments/{quizId}` | Update assignment config |
| `DELETE` | `/api/v1/expert/assignments/{quizId}` | Delete assignment |
| `GET` | `/api/v1/expert/assignments/{quizId}/skills` | Get per-skill question count |
| `POST` | `/api/v1/expert/assignments/{quizId}/questions` | Add questions to assignment per skill |
| `DELETE` | `/api/v1/expert/assignments/{quizId}/questions/{questionId}` | Remove question from assignment |
| `GET` | `/api/v1/expert/assignments/{quizId}/preview` | Get preview data |
| `PATCH` | `/api/v1/expert/assignments/{quizId}/publish` | Publish with validation |

### 6.2 Existing Endpoints (reuse)

| Endpoint | Usage |
|---|---|
| `GET /api/v1/expert/question-bank` | Bank browser in skill sections (add `skill` param) |
| `POST /api/v1/expert/questions` | Inline question creation |
| `POST /api/v1/expert/questions/groups` | Passage creation |
| `POST /api/v1/upload/audio` | Audio upload for LISTENING/SPEAKING |
| `GET /api/v1/expert/modules/courses` | Load courses for COURSE_ASSIGNMENT |

---

## 7. HTML Template Changes

### 7.1 Stepper

**Before (quiz-create.html):**
```html
<!-- 3 steps hardcoded -->
```

**After:**
```html
<!-- Stepper is dynamically generated by JS based on _isAssignment -->
<div class="stepper-wrap mb-4" id="mainStepper">
  <!-- quiz mode: 3 steps -->
  <!-- assignment mode: 6 steps -->
</div>
```

### 7.2 Step 1 Form

Add after existing config row:
```html
<!-- Assignment mode only: Per-skill time limits -->
<div id="perSkillTimeSection" style="display:none;" class="mb-3">
  <h6 class="fw-bold mt-4 mb-3" style="...">Thời gian từng phần (phút)</h6>
  <div class="row g-3">
    <div class="col-md-3">
      <label class="form-label fw-bold small text-muted">LISTENING</label>
      <input type="number" id="timeLimit_LISTENING" class="form-control" placeholder="Không giới hạn" min="1">
    </div>
    <!-- ... READING, SPEAKING, WRITING ... -->
  </div>
</div>
```

### 7.3 Skill Sections (New HTML)

Append after `#section-step-3`:

```html
<!-- SECTION: LISTENING -->
<div id="section-listening" class="quiz-step-section skill-section" style="display:none;" data-skill="LISTENING">
  <!-- Split pane: left questions + right bank/create tabs -->
</div>

<!-- SECTION: READING -->
<div id="section-reading" class="quiz-step-section skill-section" style="display:none;" data-skill="READING">
</div>

<!-- SECTION: SPEAKING -->
<div id="section-speaking" class="quiz-step-section skill-section" style="display:none;" data-skill="SPEAKING">
</div>

<!-- SECTION: WRITING -->
<div id="section-writing" class="quiz-step-section skill-section" style="display:none;" data-skill="WRITING">
</div>
```

---

## 8. Entity Changes

### 8.1 Quiz.java

```java
@Column(name = "is_sequential")
private Boolean isSequential = false;

@Column(name = "skill_order", columnDefinition = "JSON")
private String skillOrder; // ["LISTENING","READING","SPEAKING","WRITING"]

@Column(name = "time_limit_per_skill", columnDefinition = "JSON")
private String timeLimitPerSkill; // {"LISTENING": 30, "READING": 60, ...}
```

### 8.2 QuizQuestion.java

```java
@Column(name = "skill")
private String skill; // LISTENING | READING | SPEAKING | WRITING (null for quiz mode)
```

### 8.3 Question.java (verify existing, add if missing)

```java
@Column(name = "reviewer_id")
private Long reviewerId;

@Column(name = "reviewed_at")
private LocalDateTime reviewedAt;

@Column(name = "review_note")
private String reviewNote; // max 500
```

---

## 9. Service Layer

### 9.1 New: `IExpertAssignmentService`

```java
public interface IExpertAssignmentService {
    Quiz createAssignment(QuizRequestDTO dto, String expertEmail);
    Quiz getAssignment(Integer quizId);
    void updateAssignment(Integer quizId, QuizRequestDTO dto);
    void deleteAssignment(Integer quizId);
    Map<String, SkillSectionDTO> getSkillSummaries(Integer quizId);
    void addQuestionsToSection(Integer quizId, List<Integer> questionIds, String skill);
    void removeQuestion(Integer quizId, Integer questionId);
    AssignmentPreviewDTO getPreview(Integer quizId);
    void publishAssignment(Integer quizId);
    void updateStatus(Integer quizId, String status);
}
```

### 9.2 Validation in `publishAssignment`

```java
public void publishAssignment(Integer quizId) {
    Quiz quiz = quizRepository.findById(quizId)
        .orElseThrow(() -> new ResourceNotFoundException("Quiz not found"));

    if (!quiz.getIsSequential()) {
        throw new IllegalArgumentException("Invalid assignment");
    }

    Map<String, SkillSectionDTO> summaries = getSkillSummaries(quizId);
    List<String> missingSkills = summaries.entrySet().stream()
        .filter(e -> e.getValue().getQuestionCount() == 0)
        .map(Map.Entry::getKey)
        .toList();

    if (!missingSkills.isEmpty()) {
        throw new IllegalArgumentException(
            "Các skill chưa có câu hỏi: " + String.join(", ", missingSkills));
    }

    quiz.setStatus(QuizStatus.PUBLISHED);
    quiz.setIsOpen(false);
    quizRepository.save(quiz);
}
```

---

## 10. Implementation Order

### Phase 1: HTML Template Changes
1. Update stepper JS to support morphing
2. Add Assignment options to `quizCategory` dropdown
3. Add per-skill time limit inputs (hidden by default)
4. Add mode notice banner
5. Add 4 skill sections (split pane HTML)
6. Update `goToStep()` to handle 6 steps

### Phase 2: JavaScript Logic
1. `morphToAssignmentMode()` / `morphToQuizMode()`
2. `loadSkillSection(skill)`
3. `renderSkillQuestions(skill, questions)`
4. `validateAndNext()` with guard
5. `initStep6_Preview()` / `renderSkillSummary()`
6. `finalPublish()` for assignment

### Phase 3: Backend API
1. `IExpertAssignmentService` interface + implementation
2. `ExpertAssignmentController` — new REST controller
3. Add `skill` column to `QuizQuestion`
4. Add `isSequential`, `skillOrder`, `timeLimitPerSkill` to `Quiz`
5. Publish validation

### Phase 4: Integration & Polish
1. Wire up Step 1 submit to new API
2. Wire up question add/remove per skill
3. Stepper yellow warning state for empty skills
4. Test full flow: create → add questions → publish

---

## 11. File Changes Summary

| File | Change |
|---|---|
| `expert/quiz-create.html` | Major: morph stepper, add skill sections, add per-skill time inputs |
| `Quiz.java` | Add: `isSequential`, `skillOrder`, `timeLimitPerSkill` |
| `QuizQuestion.java` | Add: `skill` column |
| `Question.java` | Add: `reviewerId`, `reviewedAt`, `reviewNote` |
| `IExpertAssignmentService.java` | New interface |
| `ExpertAssignmentServiceImpl.java` | New implementation |
| `ExpertAssignmentController.java` | New REST controller |
| `ExpertQuizController.java` | Minor: pass `isSequential` flag to template |
| `ExpertQuizServiceImpl.java` | Minor: handle assignment categories |

---

## 12. Related Specs

- SPEC 001: Expert creates assignment (business rules reference)
- SPEC 003: Expert approves teacher questions
- SPEC 004: Student takes assignment (student-facing)
- SPEC 005: Teacher grades assignment