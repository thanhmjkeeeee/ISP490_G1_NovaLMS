# SPEC 002 — Teacher Creates Lesson Quiz

## 1. Overview

**Actor:** Teacher
**Flow:** Teacher creates a quiz for a specific lesson, picks any N skills (1 to 4, any combination), adds questions from the Expert's published question bank and/or creates new inline questions that go to Expert for approval.
**Related Specs:** SPEC 001 (Expert creates Assignment), SPEC 003 (Expert approves teacher questions), SPEC 006 (Teacher grades lesson quiz)

---

## 2. Business Rules

| Rule | Description |
|---|---|
| BR-001 | Only users with role `TEACHER` can create Lesson Quizzes |
| BR-002 | Teacher can only create quizzes for lessons belonging to classes they are enrolled as teacher |
| BR-003 | `quiz_category = LESSON_QUIZ` |
| BR-004 | `is_sequential = false` — no enforced skill order |
| BR-005 | Any combination of 1–4 skills allowed; skills are determined by the questions added |
| BR-006 | Quiz must have ≥1 question before publishing |
| BR-007 | `is_open = false` initially; teacher must toggle open to allow student access |
| BR-008 | Questions from Expert's published bank (status=PUBLISHED, source=EXPERT_BANK) can be added freely |
| BR-009 | Inline-created questions: `status = PENDING_REVIEW`, `source = TEACHER_PRIVATE` |
| BR-010 | PENDING_REVIEW questions can be added to the teacher's quiz, but are not visible in the shared question bank |
| BR-011 | AI-generated questions via Expert AI flow → same rules as inline-created (PENDING_REVIEW) |
| BR-012 | Lesson Quiz does NOT have per-skill timers; only a single quiz-level `timeLimitMinutes` |

---

## 3. Difference: Lesson Quiz vs Assignment

| Aspect | Lesson Quiz (SPEC 002) | Course/Module Assignment (SPEC 001) |
|---|---|---|
| Creator | Teacher | Expert |
| Skills | N skills (1–4, any combo) | Exactly 4, fixed order |
| Sequential | No | Yes (LISTENING → READING → SPEAKING → WRITING) |
| Per-skill timers | No | Yes (configurable per skill) |
| Inline questions | PENDING_REVIEW → Expert must approve | Expert-created = EXPERT_BANK (direct publish) |
| Opens for students | Teacher toggle `isOpen` | Teacher opens per class |
| Grading | Teacher grades their own class's submissions | Teacher grades their own class's submissions |

---

## 4. Wizard Flow (3 Steps)

### Step 1 — Cấu hình (Configuration)

**URL:** `GET /teacher/lesson/{lessonId}/quiz/create`
**Template:** `teacher/quiz-create.html` (modify existing quiz-create to support lesson mode)
**Existing template:** `teacher/quiz-create.html` already exists — extend it to support `lessonId` param

**Form fields:**

| Field | Type | Required | Notes |
|---|---|---|---|
| `title` | text | ✅ | |
| `description` | textarea | ❌ | |
| `category` | hidden | — | `LESSON_QUIZ` |
| `lessonId` | hidden | — | from URL param |
| `classId` | select | ✅ | teacher's enrolled classes |
| `timeLimitMinutes` | number (min) | ❌ | Single quiz-level timer |
| `passScore` | number (%) | ❌ | |
| `maxAttempts` | number | ❌ | |
| `questionOrder` | select | ❌ | `FIXED` (default) or `RANDOM` |
| `showAnswerAfterSubmit` | checkbox | ❌ | Default true |
| `targetSkill` | multi-select | ❌ | LISTENING, READING, WRITING, SPEAKING — informational only |
| `numberOfQuestions` | number | ❌ | Optional target; used in publish validation |

**Validation:**
- `title` not empty
- `classId` must belong to a class the teacher is enrolled in
- `lessonId` must belong to that class
- `timeLimitMinutes` ≥ 1 if provided
- `passScore` 0–100 if provided

On submit → `POST /api/v1/teacher/lessons/{lessonId}/quizzes` → creates quiz in `DRAFT` status → redirects to Step 2.

**Backend:** `TeacherQuizService.createLessonQuiz(QuizRequestDTO, lessonId, teacherEmail)`:
1. Validate teacher is enrolled in `classId`
2. Validate `lessonId` belongs to that class
3. Set `quizCategory = LESSON_QUIZ`
4. Set `isSequential = false`
5. Save → return quizId

---

### Step 2 — Nội dung (Content Builder)

**URL:** `GET /teacher/quiz/{quizId}/build`
**Template:** `teacher/quiz-build.html` (modify existing `expert/quiz-create.html` Step 2)

**Two actions:**

#### Action A: Browse Expert Question Bank

**Filter panel:**
- Skill: LISTENING, READING, WRITING, SPEAKING (multi-select)
- CEFR: A1–C2
- Status: `PUBLISHED` only (only published questions are in the bank)
- Source: `EXPERT_BANK` only
- Keyword search
- Module filter (optional)

**API:** `GET /api/v1/teacher/bank-questions?skill=LISTENING&cefr=B1&page=0&size=20`
- Note: This reuses `TeacherQuizApiController.GET /bank-questions` which returns only `PUBLISHED` status questions
- These questions are from the Expert's bank (EXPERT_BANK)

**Question card shows:** content preview, type badge, CEFR badge, skill badge, source badge
**"Thêm"** → `POST /api/v1/teacher/quizzes/{quizId}/questions`
- Body: `{ questionId, itemType: "SINGLE" | "GROUP", points }`
- Backend: validates status=PUBLISHED → adds to `quiz_questions`

#### Action B: Create New Inline Question

**"Tạo câu hỏi mới"** button → opens inline question creation form (reuse existing question creation UI)
**Flow:** `POST /api/v1/teacher/questions` OR `POST /api/v1/teacher/questions/groups`

**Key differences from Expert question creation:**
- `source = TEACHER_PRIVATE` (hardcoded in service)
- `status = PENDING_REVIEW` (hardcoded in service)
- `user_id = teacherId` (set from auth)

**After question saved → automatic prompt:** "Thêm câu hỏi này vào bài quiz hiện tại?"
- Yes → `POST /api/v1/teacher/quizzes/{quizId}/questions` with the new questionId
- No → question saved to teacher's pending list only

**AI generation flow (reuse existing):**
- `POST /api/v1/teacher/ai/generate` → shows preview
- `POST /api/v1/teacher/ai/import` → creates questions as `TEACHER_PRIVATE`, `PENDING_REVIEW` AND adds to current quiz automatically
- Existing `TeacherQuizService` already handles this correctly

**Section summary sidebar:** Shows questions already added, grouped by skill (inferred from `question.skill`). Also shows "⏳ Chờ phê duyệt" badge for PENDING_REVIEW questions.

---

### Step 3 — Hoàn thiện (Finish & Publish)

**URL:** `GET /teacher/quiz/{quizId}/finish`
**Template:** `teacher/quiz-finish.html` (modify existing `expert/quiz-create.html` Step 3)

**Shows:**
- Quiz title + description
- Skill breakdown: which skills are included, question count per skill
- PENDING_REVIEW question count: "⚠️ X câu hỏi đang chờ phê duyệt — sẽ hiển thị sau khi Expert duyệt"
- Total questions, total points
- Timer, pass score, attempts
- **"Lưu bản nháp"** → `PUT /api/v1/teacher/quizzes/{quizId}` (no status change)
- **"Xuất bản"** → `PATCH /api/v1/teacher/quizzes/{quizId}/publish`
  - Validates: quiz must have ≥1 question (regardless of approval status)
  - Sets `status = PUBLISHED`
  - PENDING_REVIEW questions are included in the published quiz and remain PENDING_REVIEW until Expert approves them

**Backend publish validation:**
1. Count `quizQuestionRepository.countByQuizQuizId(quizId)` ≥ 1
2. If not, return 400
3. Set `status = PUBLISHED`
4. Return success

---

## 5. API Endpoints (Existing + Extensions)

| Method | Endpoint | Status | Description |
|---|---|---|---|
| `POST` | `/api/v1/teacher/lessons/{lessonId}/quizzes` | existing | Create lesson quiz |
| `GET` | `/api/v1/teacher/quizzes/{id}` | existing | Get quiz detail |
| `PUT` | `/api/v1/teacher/quizzes/{id}` | existing | Update quiz |
| `DELETE` | `/api/v1/teacher/quizzes/{id}` | existing | Delete quiz |
| `POST` | `/api/v1/teacher/quizzes/{id}/publish` | existing | Publish quiz |
| `PATCH` | `/api/v1/teacher/quizzes/{id}/toggle-open` | existing | Toggle isOpen |
| `GET` | `/api/v1/teacher/bank-questions` | existing | Browse expert bank (PUBLISHED only) |
| `POST` | `/api/v1/teacher/questions` | existing | Create inline question (PENDING_REVIEW) |
| `POST` | `/api/v1/teacher/questions/groups` | existing | Create inline passage |
| `POST` | `/api/v1/teacher/quizzes/{id}/questions` | existing | Add question to quiz |
| `DELETE` | `/api/v1/teacher/quizzes/{id}/questions/{questionId}` | existing | Remove question |
| `POST` | `/api/v1/teacher/ai/generate` | existing | AI generate questions |
| `POST` | `/api/v1/teacher/ai/import` | existing | Import AI questions |
| `GET` | `/api/v1/teacher/lessons/{lessonId}/quizzes` | existing | List quizzes in lesson |
| `PUT` | `/api/v1/teacher/lessons/{lessonId}/quizzes/reorder` | existing | Reorder quizzes |

**New endpoint needed:**

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/teacher/assignments` | List Course/Module Assignments assigned to teacher's classes |

---

## 6. Service Layer Changes

**File:** `src/main/java/com/example/DoAn/service/impl/TeacherQuizServiceImpl.java`

**Changes needed:**

1. **Extend `createLessonQuiz`** — set `isSequential = false`, return quizId
2. **Extend `publishQuiz`** — already works for LESSON_QUIZ; no changes needed
3. **Extend `addQuestionToQuiz`** — already accepts PENDING_REVIEW questions for teacher's own quiz (already implemented in existing code)
4. **Add `getSkillSummary(quizId)`** — returns which skills are present in the quiz, question count per skill, PENDING_REVIEW count per skill

---

## 7. UI/UX Changes

### Teacher Quiz List (`teacher/quiz-list.html`)
**Existing:** `teacher/quiz-bank.html` — lists classes with quiz counts
**Change:** Add tabs or section: "Bài quiz bài tập" (Lesson Quizzes) vs "Bài kiểm tra lớn" (Assignments opened for this teacher's classes)

### Lesson Quiz Creation Flow
**Existing:** `teacher/quiz-create.html` — 2-step wizard already exists
**Change:** Extend to support 3-step with bank browser + inline creator (Step 2 = content builder)
- Current Step 1: Config (already done)
- New Step 2: Content Builder (reuse expert's question picker, but restricted to PUBLISHED bank + inline creation)
- Current Step 2 → becomes Step 3: Finish

### Question Status Indicator
Add visual badge in quiz builder sidebar:
- 🟢 "Đã xuất bản" — PUBLISHED questions
- 🟡 "⏳ Chờ duyệt" — PENDING_REVIEW questions (greyed out, tooltip: "Câu hỏi sẽ hiển thị sau khi Expert duyệt")

### Pending Count on Teacher Dashboard
**Existing:** Teacher dashboard should show a notification badge on "Quiz" or "Questions" if there are pending approvals waiting (for Expert to act on).
**Change:** Add to `GET /api/v1/teacher/dashboard` response: `pendingQuestionCount` — number of teacher's own questions in PENDING_REVIEW status.

---

## 8. HTML Templates

| Template | Route | Purpose | Change |
|---|---|---|---|
| `teacher/quiz-create.html` | `GET /teacher/lesson/{id}/quiz/create` | Step 1: Config | Extend existing |
| `teacher/quiz-build.html` | `GET /teacher/quiz/{id}/build` | Step 2: Content builder | New |
| `teacher/quiz-finish.html` | `GET /teacher/quiz/{id}/finish` | Step 3: Finish | Extend existing Step 2 |
| `teacher/quiz-list.html` | `GET /teacher/quizzes` | Quiz list by class | Extend existing |
| `teacher/partial-question-form.html` | Fragment | Inline question creation | Reuse existing question-create partials |

---

## 9. Edge Cases

| Case | Handling |
|---|---|
| Teacher publishes quiz with PENDING_REVIEW questions | Publish allowed — PENDING_REVIEW questions remain pending. Expert approves → questions auto-appear in quiz for future students. Students taking quiz while questions are PENDING_REVIEW see only approved questions (or quiz shows as incomplete) |
| Expert rejects a question while students are taking the quiz | If student already loaded quiz → no change mid-session. If student hasn't started → that question is removed from their quiz view. System checks question status at submission time |
| Teacher adds 0 questions and tries to publish | Rejected — backend validation returns 400 |
| Teacher tries to create quiz for lesson not in their class | Rejected — `lessonRepository` + `classRepository` join validates teacher enrollment |
| PENDING_REVIEW question is approved while a student is mid-quiz | Student sees the new question on their next attempt; current attempt unchanged |
