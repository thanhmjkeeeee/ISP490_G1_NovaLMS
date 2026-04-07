# Unified Quiz & Assignment Form — Design Spec

**Date:** 2026-04-05
**Status:** Draft
**Author:** Claude

---

## 1. Vision & Goal

Consolidate ALL quiz and assignment creation into **a single `quiz-create.html` page**. A type dropdown drives all form behavior — no more separate assignment wizard or quiz wizard. The same page also serves as the single take-quiz/take-assignment interface for students at `/quiz/{id}`, adapting per type.

**Core principle:** One entity (`Quiz`) with one create page, one take page. Behavior is a function of `quizCategory`.

---

## 2. Quiz Categories (Type Dropdown Options)

| Value | Label | Stepper/Form |
|---|---|---|
| `ENTRY_TEST` | Bài kiểm tra đầu vào | 3-step (config → bank → publish) |
| `COURSE_QUIZ` | Bài kiểm tra khóa học | 3-step |
| `MODULE_QUIZ` | Bài kiểm tra module | 3-step |
| `LESSON_QUIZ` | Bài kiểm tra bài học | 3-step |
| `COURSE_ASSIGNMENT` | Bài tập lớn khóa học | 1-page tabbed (L/R/S/W accordion) |
| `MODULE_ASSIGNMENT` | Bài tập lớn module | 1-page tabbed |

---

## 3. Creation Form — `quiz-create.html`

### 3.1 Type Selector (Always visible, top of form)

```html
<select id="quizCategory">
  <option value="ENTRY_TEST">Bài kiểm tra đầu vào</option>
  <option value="COURSE_QUIZ">Bài kiểm tra khóa học</option>
  <option value="MODULE_QUIZ">Bài kiểm tra module</option>
  <option value="LESSON_QUIZ">Bài kiểm tra bài học</option>
  <option value="COURSE_ASSIGNMENT">Bài tập lớn khóa học</option>
  <option value="MODULE_ASSIGNMENT">Bài tập lớn module</option>
</select>
```

On change → JavaScript hides/shows relevant form sections. No page reload.

### 3.2 Form Sections

#### Section A: Metadata (always shown for all types)
- Title, description
- Course selector (conditional: hides for `LESSON_QUIZ`)
- Module selector (conditional: shown for `MODULE_QUIZ`, `MODULE_ASSIGNMENT`)
- Lesson selector (conditional: shown for `LESSON_QUIZ`)

#### Section B: Quiz Config
- Time limit (minutes)
- Pass score (%)
- Max attempts
- Show answers after submit (toggle)

#### Section C: Assignment-Only Config (shown only when type ∈ {COURSE_ASSIGNMENT, MODULE_ASSIGNMENT})
- Per-skill time limits: 4 inputs (Listening, Reading, Speaking, Writing) in minutes
- Sequential order badge: "Thứ tự: Nghe → Đọc → Nói → Viết" (fixed)

#### Section D: Question Selection
- **Reused question bank browser fragment** — extracted from current `quiz-create.html` and `assignment-skill-section.html`, placed in `fragments/question-bank-browser.html`
- For QUIZ types: flat question list, no skill tagging
- For ASSIGNMENT types: skill-tag filter (L/R/S/W), each question tagged on add

#### Section E: Publish / Save
- Save as Draft / Publish button
- Publish validation:
  - QUIZ: ≥1 question total
  - ASSIGNMENT: all 4 skills must have ≥1 question

---

## 4. Shared Fragment — `fragments/question-bank-browser.html`

**Purpose:** Single source of truth for the question bank browser used by both create and take forms.

```html
<!-- th:fragment="question-bank-browser" -->
<div class="bank-browser" id="question-bank-browser">
  <!-- Search + filter controls -->
  <!-- Question list (paginated, selectable) -->
  <!-- Selected questions panel -->
  <!-- Inline question creation trigger -->
</div>
```

**Attributes to control behavior:**
- `data-mode="quiz"` — flat selection, no skill
- `data-mode="assignment"` — skill filter + skill tagging on add
- `data-skill-filter="LISTENING|READING|SPEAKING|WRITING"` — pre-filter to one skill (used by assignment tab panels)

---

## 5. Take-Quiz Form — `take-quiz.html` (student-facing)

Single page at `GET /quiz/{id}` adapts based on quiz metadata:

| Quiz property | Behavior |
|---|---|
| `isSequential = true` | Show tab/accordion per skill, lock next section until current is done |
| `skillOrder` | Use order for section sequence |
| `timeLimitPerSkill` JSON | Per-section countdown timers (assignment) |
| `timeLimit` | Single countdown for non-sequential quiz |

**Type label shown in header** so student knows what they're taking.

---

## 6. Service Layer

### `QuizService` (merged from ExpertAssignmentService + ExpertQuizService)

Single service class handles all `QuizCategory` values. Methods use `quizCategory` switch to apply assignment-specific logic.

```java
// Pseudo-structure
QuizService {
  createQuiz(QuizCategory category, QuizDto dto) {
    Quiz quiz = new Quiz();
    quiz.setQuizCategory(category);
    quiz.setIsSequential(category.isAssignment());
    if (category.isAssignment()) {
      quiz.setSkillOrder(["LISTENING","READING","SPEAKING","WRITING"]);
      quiz.setTimeLimitPerSkill(dto.getTimeLimitsBySkill());
    }
    // ...
  }

  addQuestion(Quiz quiz, Question question, String skill) {
    if (quiz.getQuizCategory().isAssignment()) {
      // require skill
    }
    // shared logic
  }
}
```

### `QuizCategory` enum or utility

```java
QuizCategory {
  isAssignment()  // COURSE_ASSIGNMENT, MODULE_ASSIGNMENT → true
  isQuiz()       // the other 4 → true
  getSkillOrder() // null for quiz, fixed LRWS for assignment
}
```

---

## 7. Impact Analysis

### Templates to DELETE (no longer needed)
- `assignment-create.html` → absorbed into `quiz-create.html`
- `assignment-skill-section.html` → absorbed (become tab sections)
- `assignment-preview.html` → absorbed (become publish section)
- (Keep `assignment-list.html` — it's the listing page, not creation)

### Templates to MODIFY
- `quiz-create.html` → major restructure: add type selector, conditional sections, use shared fragment
- `take-quiz.html` (or create new) → single take form, add sequential/skill support

### Templates to CREATE
- `fragments/question-bank-browser.html` — extract from existing templates

### Java files to MODIFY
- `ExpertAssignmentController` → merge into `ExpertQuizController` (or keep separate but share service)
- `ExpertAssignmentServiceImpl` → merge into `ExpertQuizServiceImpl`
- `ExpertViewController` → update view mappings

### Java files to CREATE
- `QuizCategory.java` — enum/interface with helper methods
- Updated `Quiz.java` — ensure `isSequential`, `skillOrder`, `timeLimitPerSkill` fields are present

### JavaScript
- Extract question bank browser JS into `question-bank-browser.js` (reusable)
- Update `quiz-create.html` JS to handle type-switching dynamically

---

## 8. Migration Plan

1. Create `QuizCategory` enum with helper methods
2. Create `fragments/question-bank-browser.html` — extract common UI + JS
3. Build unified `quiz-create.html` with type selector, all conditional sections
4. Merge services → single `QuizService`
5. Update controller(s) to point to new unified form
6. Build unified `take-quiz.html` for students
7. Delete old assignment template files
8. Test all 6 category types end-to-end

---

## 9. Open Questions

1. Should MODULE_ASSIGNMENT and COURSE_ASSIGNMENT share the same tabbed L/R/S/W layout, or should they differ?
   - **Decision:** Same layout. Module vs Course only changes the context (module selector vs course selector in metadata).
2. What happens to existing published assignments? Do they migrate automatically?
   - **Decision:** No migration needed — they already use `Quiz` entity with correct category. Only the creation UI changes.
3. LESSON_QUIZ — does it need lesson selector in metadata?
   - **Decision:** Yes, conditional field shown when `LESSON_QUIZ` selected.