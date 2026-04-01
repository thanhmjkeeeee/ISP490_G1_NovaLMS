# Module & Lesson Quiz Feature — Design Specification

**Date:** 2026-03-29
**Status:** Approved
**Authors:** NovaLMS Development Team

---

## 1. Overview

Allow Experts to create quizzes scoped to a **Module** and Teachers to create quizzes scoped to a **Lesson**. Each lesson can have multiple quizzes that students take sequentially (one unlocks only after the previous is passed).

---

## 2. Database Schema

### 2.1 Changes to `Quiz` Entity

Add three nullable scope fields to the existing `Quiz` entity:

| Field | Type | Nullable | Description |
|---|---|---|---|
| `course_id` | FK → Course | Yes | Course-level quiz (existing) |
| `module_id` | FK → Module | Yes | Module-level quiz (NEW) |
| `lesson_id` | FK → Lesson | Yes | Lesson-level quiz (NEW) |

- At least one of `course_id`, `module_id`, or `lesson_id` must be set at creation time.
- The `quiz_category` enum gets two new values: `MODULE_QUIZ`, `LESSON_QUIZ`.
- All three fields are nullable so a quiz can exist independently before being assigned.

### 2.2 New Table: `QuizAssignment`

Tracks which quizzes are attached to which lesson or module, and their sequential order.

```sql
QuizAssignment {
  assignment_id  (PK, Long)
  quiz_id       (FK → Quiz, NOT NULL)
  lesson_id     (FK → Lesson, nullable)
  module_id     (FK → Module, nullable)
  order_index   (Int, NOT NULL)  -- 1-based sequential order within the lesson/module
}
```

- Exactly one of `lesson_id` or `module_id` is set per row.
- Unique constraint: `(quiz_id, lesson_id)` and `(quiz_id, module_id)` — a quiz can only be assigned once per lesson/module.

### 2.3 New Table: `LessonQuizProgress`

Tracks per-student progression through a lesson's quiz sequence.

```sql
LessonQuizProgress {
  progress_id   (PK, Long)
  lesson_id     (FK → Lesson, NOT NULL)
  user_id       (FK → User, NOT NULL)
  quiz_id       (FK → Quiz, NOT NULL)
  status        (Enum: AVAILABLE / LOCKED / COMPLETED, NOT NULL)
  best_score    (Double, nullable)   -- highest score across attempts
  best_passed   (Boolean, NOT NULL)  -- true if user ever passed (score >= passScore)

  -- Unique constraint: (lesson_id, user_id, quiz_id)
}
```

### 2.4 Schema Diagram

```
Course ──── has many ──── Module ──── has many ──── Lesson
   │                                    │              │
   │ 1:1 (nullable)                    │ 1:N          │ 1:N (nullable)
   ▼                                    ▼              ▼
Quiz (course_id)              QuizAssignment     Quiz (lesson_id)
                          (module_id or lesson_id)
                                      │
LessonQuizProgress ─────── (lesson_id)
  (lesson_id, user_id, quiz_id)
```

---

## 3. Quiz Creation & Assignment Flows

### 3.1 Expert — Module Quiz

**Path A: From Quizzes section**
1. Expert navigates to **Quizzes** → clicks **"Create Quiz"**
2. Selects type: `MODULE_QUIZ`, enters title, description, time limit, pass score, max attempts
3. On the question builder page, adds questions via:
   - **Select from bank** — searchable list of EXPERT_BANK / PUBLISHED questions owned by the expert
   - **Create inline** — new question form (type, content, options, correct answer, skill, CEFR level)
   - **AI Generate** — selects skill, CEFR level, count; generates questions via `AIQuestionService`
   - **Excel Import** — uploads `.xlsx` via `ExcelQuestionImportService`
4. Saves as `DRAFT` → reviews → `PUBLISHED`
5. **Assignment step**: Course → Module detail → "Attach Quiz" → picker → sets order index

**Path B: Inline inside module page**
1. Expert opens **Course → Module** detail page
2. Clicks **"Add Quiz"** → modal/page form pre-filled with `module_id` and `MODULE_QUIZ`
3. Quiz is created and automatically assigned to that module at the next order position

### 3.2 Teacher — Lesson Quiz

**Path A: From Quizzes section**
1. Teacher navigates to **My Classes** → selects a class → **Lessons**
2. Clicks **"Create Lesson Quiz"**
3. Selects type: `LESSON_QUIZ`, picks the target lesson, enters quiz config
4. Adds questions via: bank selection, inline creation, AI, or Excel import
5. Inline/AI/Excel questions get `source = TEACHER_PRIVATE`, `status = PENDING_REVIEW`

**Path B: Inline inside lesson page**
1. Teacher opens **Class → Lesson** detail page
2. Clicks **"Add Quiz"** → quiz form → auto-assigned to that lesson at next order position

### 3.3 Question Selection Rules

| Question Source | Expert Can Use | Teacher Can Use |
|---|---|---|
| Expert's own published questions (EXPERT_BANK, PUBLISHED) | ✅ | ✅ (read-only) |
| Other experts' published questions | ❌ | ❌ |
| Teacher's own questions (TEACHER_PRIVATE) | ❌ | ✅ (create/edit) |
| AI-generated | ✅ (their own) | ✅ (their own) |
| Excel-imported | ✅ (their own) | ✅ (their own) |

---

## 4. API Endpoints

### 4.1 Expert — Module Quiz

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/expert/modules/{moduleId}/quizzes` | List quizzes attached to a module |
| `POST` | `/api/v1/expert/modules/{moduleId}/quizzes` | Create MODULE_QUIZ and auto-assign to module |
| `GET` | `/api/v1/expert/quizzes/{quizId}` | Quiz detail (existing) |
| `POST` | `/api/v1/expert/quizzes` | Create quiz (existing, add `MODULE_QUIZ` category) |
| `PUT` | `/api/v1/expert/quizzes/{quizId}/assign` | Assign/reassign quiz to a module |
| `PATCH` | `/api/v1/expert/modules/{moduleId}/quizzes/reorder` | Reorder quizzes within a module |
| `DELETE` | `/api/v1/expert/modules/{moduleId}/quizzes/{quizId}` | Detach quiz from module (quiz not deleted) |

### 4.2 Teacher — Lesson Quiz

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/teacher/lessons/{lessonId}/quizzes` | List quizzes attached to a lesson |
| `POST` | `/api/v1/teacher/lessons/{lessonId}/quizzes` | Create LESSON_QUIZ and auto-assign to lesson |
| `PUT` | `/api/v1/teacher/lessons/{lessonId}/quizzes/reorder` | Reorder quizzes within a lesson |
| `DELETE` | `/api/v1/teacher/lessons/{lessonId}/quizzes/{quizId}` | Detach quiz from lesson (quiz not deleted) |
| `GET` | `/api/v1/teacher/quizzes/{quizId}/results` | All student results for a lesson quiz |
| `PATCH` | `/api/v1/teacher/quizzes/{quizId}/results/{resultId}/grade` | Grade WRITING/SPEAKING question |

### 4.3 Student — Quiz Taking

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/learning/lessons/{lessonId}/quizzes` | List available quizzes for this lesson |
| `GET` | `/api/v1/learning/lessons/{lessonId}/quizzes/{quizId}` | Get quiz to take (validates AVAILABLE status) |
| `POST` | `/api/v1/learning/quizzes/{quizId}/submit` | Submit quiz answers, update progress, unlock next |
| `GET` | `/api/v1/learning/lessons/{lessonId}/progress` | Student's full quiz progression map for lesson |

---

## 5. Student Quiz Progression Logic

```
On GET /learning/lessons/{lessonId}/quizzes:
  1. Get all QuizAssignment rows for this lessonId, ordered by orderIndex
  2. For each quiz, look up LessonQuizProgress for (lessonId, userId, quizId)
  3. First quiz: if no progress row exists → status = AVAILABLE
  4. Quiz N (N > 1): status = AVAILABLE iff progress row for quiz N-1 has bestPassed = true
     Otherwise status = LOCKED
  5. Completed quiz → show best score and "Completed" badge

On POST /quizzes/{quizId}/submit:
  1. Auto-grade MCQ/Fill-in-blank/Matching (existing logic)
  2. Mark WRITING/SPEAKING as PENDING_GRADING in QuizAnswer
  3. After grading:
     - Update LessonQuizProgress (bestScore, bestPassed)
     - If bestPassed = true: set next quiz's status = AVAILABLE
```

---

## 6. Grading Flow for WRITING/SPEAKING

```
On POST /quizzes/{quizId}/submit:
  - Auto-grade MCQ/Fill-in-blank/Matching
  - Mark WRITING/SPEAKING answers as "PENDING_GRADING"

On PATCH /teacher/quizzes/{quizId}/results/{resultId}/grade:
  1. Validate: question is WRITING or SPEAKING type
  2. Save score and teacher feedback to QuizAnswer
  3. Recalculate total score for the result
  4. Check: totalScore >= passScore → update result.passed
  5. Update LessonQuizProgress: bestScore, bestPassed
  6. If bestPassed = true and next quiz exists → unlock next quiz

Student view:
  - WRITING/SPEAKING questions show "Pending teacher review" until graded
  - Final score updates after teacher submits grade
```

---

## 7. UI Pages

### 7.1 Expert Panel

**Module Detail Page** (`/expert/courses/{courseId}/modules/{moduleId}`)
- Existing module content at top
- **"Quizzes" tab**: quiz cards in order, each showing title, question count, pass score, status badge
- "Add Quiz" → modal/page (Path B), auto-assigns to this module
- Drag handles for reorder; Quick actions: Edit, Preview, Publish/Unpublish, Detach

**Quizzes List Page** (`/expert/quizzes`)
- Filter: All / Course Quiz / Module Quiz
- Module Quiz rows show "Assigned to: [Module Name]" column
- "Create Quiz" → step 1: choose type (COURSE_QUIZ / MODULE_QUIZ / ENTRY_TEST)

### 7.2 Teacher Panel

**Lesson Detail Page** (inside Class view)
- Lesson content at top
- **"Quizzes" section** below lesson content: sequential quiz cards
  - Completed → score + ✅ badge
  - Available → "Take Quiz" button
  - Locked → 🔒 + "Complete [Quiz Name] to unlock"
  - "Add Quiz" button → modal (Path B)

**Lesson Quiz Management Page** (`/teacher/lessons/{lessonId}/quizzes`)
- Full quiz list for this lesson; reorder, edit, publish/unpublish, detach
- "Grade Pending" badge when WRITING/SPEAKING questions need grading

**Grading Page** (`/teacher/quizzes/{quizId}/results`)
- Table of student results (score, submitted time)
- Expand row → each question shown
- WRITING/SPEAKING: student answer, score input, feedback textarea, "Submit Grade" button

### 7.3 Student Experience

**Lesson Page** (`/student/courses/{courseId}/lessons/{lessonId}`)
- Lesson content (video + text) as normal
- After content: **"Quizzes" section** with sequential quiz cards
- "Take Quiz" → navigates to `/student/quizzes/{quizId}/take` (separate page)
- After submit: result screen → "Back to Lesson" → next quiz unlocks

---

## 8. Edge Cases

| Scenario | Handling |
|---|---|
| Expert deletes a module with quizzes | Soft-delete module; quizzes are detached (module_id set to null) but not deleted |
| Teacher deletes a lesson with quizzes | Same — quizzes detached but not deleted |
| Quiz is published then detached | Quiz status stays PUBLISHED; can be reattached later |
| Student navigates directly to lesson URL | Validate progress — if previous quiz not passed, return 403 / show locked state |
| Quiz published with 0 questions | Prevent publishing if `number_of_questions = 0` |
| Teacher uses another expert's question | Reject — only own EXPERT_BANK/PUBLISHED + own TEACHER_PRIVATE allowed |
| Max attempts reached | Disable "Take Quiz" button; show "Attempts exhausted" |
| AI generation fails | Show error, allow retry; no partial save |
| Excel import has errors | Return per-row validation report; allow fix and re-upload |
| Student submits quiz with WRITING/SPEAKING | Show "Pending teacher review" for those questions; score updates after grading |
| Quiz belongs to both course and lesson | Not allowed — exactly one of course_id / module_id / lesson_id is set per quiz |

---

## 9. Acceptance Criteria

- [ ] Expert can create a MODULE_QUIZ and attach it to a specific module via inline or assign flow
- [ ] Teacher can create a LESSON_QUIZ and attach it to a specific lesson via inline or assign flow
- [ ] Questions can be added via: select from bank, create inline, AI generate, Excel import
- [ ] Multiple quizzes per module/lesson are supported
- [ ] Quizzes appear in sequential order on the lesson page
- [ ] Quiz N+1 is LOCKED until Quiz N is passed (bestScore >= passScore)
- [ ] First quiz starts as AVAILABLE
- [ ] Student can take AVAILABLE quizzes; LOCKED quizzes are not accessible
- [ ] Student sees "Pending teacher review" for WRITING/SPEAKING until graded
- [ ] Teacher can grade WRITING/SPEAKING questions from the results page
- [ ] Teacher can view all student results for their lesson quizzes
- [ ] Quiz detachment removes the QuizAssignment row but does not delete the quiz
- [ ] Published quiz with 0 questions cannot be saved/submitted
- [ ] Max attempts enforcement works correctly
