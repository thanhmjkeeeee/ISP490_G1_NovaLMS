# SPEC 001 — Expert Creates Assignment (Course / Module Assignment)

## 1. Overview

**Actor:** Expert
**Flow:** Expert creates a structured 4-skill Assignment linked to a Course or Module, with inline question creation, question bank browsing, sequential skill enforcement, and per-skill timers.
**Related Specs:** SPEC 003 (Expert approves teacher questions), SPEC 004 (Student takes assignment), SPEC 005 (Teacher grades assignment)

---

## 2. Business Rules

| Rule | Description |
|---|---|
| BR-001 | Assignment must contain exactly 4 skills in fixed order: LISTENING → READING → SPEAKING → WRITING |
| BR-002 | Each skill must have ≥1 question before the Assignment can be published |
| BR-003 | Only users with role `EXPERT` can create Course Assignments and Module Assignments |
| BR-004 | `quiz_category` for Course Assignment = `COURSE_ASSIGNMENT`; for Module Assignment = `MODULE_ASSIGNMENT` |
| BR-005 | Assignment always has `is_sequential = true` |
| BR-006 | `skill_order` is always `["LISTENING","READING","SPEAKING","WRITING"]` (not configurable) |
| BR-007 | `time_limit_per_skill` is optional per skill; only SPEAKING/WRITING typically have a limit |
| BR-008 | Inline-created questions are saved with `status = PENDING_REVIEW`, `source = TEACHER_PRIVATE` |
| BR-009 | Expert-created questions used in Assignment are saved to Expert's own bank |
| BR-010 | Publishing an Assignment requires all 4 skills to have ≥1 question each |
| BR-011 | After publishing, `status = PUBLISHED`, `is_open = false` (Teacher must open later) |
| BR-012 | Audio files for LISTENING/SPEAKING questions upload to Cloudinary via `FileUploadService` |

---

## 3. Quiz Entity Changes

**File:** `src/main/java/com/example/DoAn/model/Quiz.java`

New columns (via JPA fields):

```java
@Column(name = "is_sequential")
private Boolean isSequential = false;

@Column(name = "skill_order", columnDefinition = "JSON")
private String skillOrder; // JSON array, e.g. ["LISTENING","READING","SPEAKING","WRITING"]

@Column(name = "time_limit_per_skill", columnDefinition = "JSON")
private String timeLimitPerSkill; // JSON object, e.g. {"SPEAKING": 2, "WRITING": 30}
```

**Validation rule:** If `quizCategory` is `COURSE_ASSIGNMENT` or `MODULE_ASSIGNMENT`, then `isSequential` must be `true` and `skillOrder` must contain all 4 skills in order.

---

## 4. Question Entity Changes

**File:** `src/main/java/com/example/DoAn/model/Question.java`

New columns (already partially exist — verify and add missing):

```java
@Column(name = "reviewer_id")
private Long reviewerId;

@Column(name = "reviewed_at")
private LocalDateTime reviewedAt;

@Column(name = "review_note")
private String reviewNote; // max 500 chars
```

---

## 5. Wizard Flow (5 Steps)

### Step 1 — Cấu hình (Configuration)

**URL:** `GET /expert/assignment/create?category=COURSE_ASSIGNMENT&courseId={id}`
**Template:** `expert/assignment-create.html`

**Form fields:**

| Field | Type | Required | Notes |
|---|---|---|---|
| `title` | text | ✅ | Quiz name |
| `description` | textarea | ❌ | |
| `category` | hidden | — | `COURSE_ASSIGNMENT` or `MODULE_ASSIGNMENT` |
| `courseId` | select | ✅ if COURSE | Load courses via `GET /api/v1/expert/courses` |
| `moduleId` | select | ✅ if MODULE | Load modules via `GET /api/v1/expert/modules?courseId={id}` |
| `passScore` | number (%) | ❌ | Default null (no pass/fail) |
| `maxAttempts` | number | ❌ | Default null (unlimited) |
| `showAnswerAfterSubmit` | checkbox | ❌ | Default true |
| `timeLimit.SPEAKING` | number (min) | ❌ | e.g. 2 |
| `timeLimit.WRITING` | number (min) | ❌ | e.g. 30 |
| `timeLimit.LISTENING` | number (min) | ❌ | |
| `timeLimit.READING` | number (min) | ❌ | |

**Validation (frontend + backend):**
- `title` not empty
- `courseId` required for COURSE_ASSIGNMENT
- `moduleId` required for MODULE_ASSIGNMENT
- `passScore` 0–100 if provided
- `maxAttempts` ≥1 if provided

On submit → `POST /api/v1/expert/assignments` → creates quiz in `DRAFT` status, redirects to Step 2.

**Backend:** `ExpertAssignmentService.create(QuizRequestDTO)`:
1. Validate user is EXPERT
2. Set `quizCategory` = passed category
3. Set `isSequential = true`
4. Set `skillOrder = ["LISTENING","READING","SPEAKING","WRITING"]`
5. Parse `timeLimitPerSkill` → JSON string
6. Save → return quizId → redirect to Step 2 URL with quizId

---

### Step 2 — LISTENING

**URL:** `GET /expert/assignment/{quizId}/skill/LISTENING`
**Template:** `expert/assignment-skill-section.html` (reusable for all 4 skills)

**Two actions on this page:**

#### Action A: Browse Question Bank

**Sub-panel:** "Chọn từ ngân hàng câu hỏi"
- Filter by skill (pre-selected to LISTENING), CEFR, keyword
- Calls `GET /api/v1/expert/questions/bank?skill=LISTENING&status=PUBLISHED&page=0&size=20`
- Shows question cards: content preview, type badge, CEFR badge
- Checkbox per question
- "Thêm câu hỏi đã chọn" button → `POST /api/v1/expert/assignments/{quizId}/questions`
  - Body: `{ questionIds: [1,2,3], skill: "LISTENING", itemType: "SINGLE" }`
  - Backend: validates questions are PUBLISHED, adds to `quiz_questions` with `skill` stored in `QuizQuestion`
  - Response: count added

#### Action B: Create New Question Inline

**Sub-panel:** "Tạo câu hỏi mới"
- Reuses existing question creation UI (SINGLE or GROUP/Passage mode)
- Calls existing endpoints: `POST /api/v1/expert/questions` OR `POST /api/v1/expert/questions/groups`
- **Key difference from existing flow:**
  - `moduleId` = null (bank question)
  - `skill` = LISTENING
  - `source` = `EXPERT_BANK`
  - After save, immediately `POST /api/v1/expert/assignments/{quizId}/questions` to add to this Assignment
  - `QuizQuestion` stores `skill = "LISTENING"` (stored on the link, not the question itself)

**For LISTENING questions:**
- Question types allowed: `MULTIPLE_CHOICE_SINGLE`, `MULTIPLE_CHOICE_MULTI`, `FILL_IN_BLANK`
- Can include audio upload: `POST /api/v1/upload/audio` → Cloudinary URL stored in `question.audioUrl`

**Section counter:** Shows "Đã thêm: X câu" at top. Cannot proceed to next step until X ≥ 1.

---

### Step 3 — READING

**URL:** `GET /expert/assignment/{quizId}/skill/READING`

Identical to Step 2, but:
- Pre-selected skill = READING
- Question types allowed: `MULTIPLE_CHOICE_SINGLE`, `MULTIPLE_CHOICE_MULTI`, `FILL_IN_BLANK`, `MATCHING`
- Can include image upload (passage/reading text)
- Audio upload also supported

---

### Step 4 — SPEAKING

**URL:** `GET /expert/assignment/{quizId}/skill/SPEAKING`

**Question types allowed:** `SPEAKING` only

**Inline creation:**
- Question text (prompt for student)
- Optional audio prompt (teacher uploads audio via Cloudinary)
- After save → automatically added to Assignment

---

### Step 5 — WRITING

**URL:** `GET /expert/assignment/{quizId}/skill/WRITING`

**Question types allowed:** `WRITING` only

**Inline creation:**
- Question text (writing prompt)
- After save → automatically added to Assignment

---

### Step 6 — Xem trước & Hoàn thiện (Preview & Publish)

**URL:** `GET /expert/assignment/{quizId}/preview`
**Template:** `expert/assignment-preview.html`

**Shows:**
- Assignment title + description
- Per-skill summary: skill name, question count, estimated time
- Total question count, total points
- Timer summary: per-skill time limits
- "Quay lại sửa" → back to step selection
- **"Xuất bản"** → `PATCH /api/v1/expert/assignments/{quizId}/publish`

**Backend publish validation (`ExpertAssignmentService.publish(quizId)`):**
1. Fetch quiz by ID
2. Validate `isSequential = true`
3. Count questions per skill: each skill must have ≥1 question
4. If not, return 400 with missing skills list
5. Set `status = PUBLISHED`, `isOpen = false`
6. Return success

---

## 6. API Endpoints (New)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/expert/assignments` | Create assignment (Step 1) |
| `GET` | `/api/v1/expert/assignments/{quizId}` | Get assignment detail |
| `PUT` | `/api/v1/expert/assignments/{quizId}` | Update assignment config |
| `DELETE` | `/api/v1/expert/assignments/{quizId}` | Delete assignment |
| `GET` | `/api/v1/expert/assignments/{quizId}/skills` | Get skill summary (question count per skill) |
| `POST` | `/api/v1/expert/assignments/{quizId}/questions` | Add questions to assignment by skill |
| `DELETE` | `/api/v1/expert/assignments/{quizId}/questions/{questionId}` | Remove question |
| `GET` | `/api/v1/expert/assignments/{quizId}/preview` | Get preview data |
| `PATCH` | `/api/v1/expert/assignments/{quizId}/publish` | Publish assignment |
| `PATCH` | `/api/v1/expert/assignments/{quizId}/status` | Archive / unpublish |

---

## 7. Service Layer

**New interface:** `IExpertAssignmentService`
**New implementation:** `ExpertAssignmentServiceImpl`

**Key methods:**

```java
// Create assignment (Step 1)
Quiz createAssignment(QuizRequestDTO dto, String expertEmail);

// Get skills with question counts
Map<String, SkillSectionSummary> getSkillSummaries(Integer quizId);

// Add questions to a specific skill section
void addQuestionsToSection(Integer quizId, List<Integer> questionIds, String skill);

// Publish with validation
void publishAssignment(Integer quizId);

// Get full preview
AssignmentPreviewDTO getPreview(Integer quizId);
```

**Validation in `addQuestionsToSection`:**
- Verify `skill` is one of the 4 in `skillOrder`
- Verify question `status = PUBLISHED` (bank) OR `status = DRAFT` (expert-created inline)
- Store `skill` on the `QuizQuestion` entity (add `skill` column to `quiz_question` table)

---

## 8. `quiz_question` Table Changes

Add `skill` column to link questions to their skill section:

```sql
ALTER TABLE quiz_question ADD COLUMN skill VARCHAR(20);
```

This allows the same question (same `question_id`) to be reused across different skill sections if needed, with the skill determined by the `quiz_question.skill` field.

---

## 9. HTML Templates

| Template | Route | Purpose |
|---|---|---|
| `expert/assignment-create.html` | `GET /expert/assignment/create` | Step 1: Config form |
| `expert/assignment-skill-section.html` | `GET /expert/assignment/{id}/skill/{skill}` | Steps 2-5: Per-skill question picker + creator |
| `expert/assignment-preview.html` | `GET /expert/assignment/{id}/preview` | Step 6: Preview + publish |
| `expert/assignment-list.html` | `GET /expert/assignment-management` | List all assignments (reuse/modify existing `quiz-list.html`) |

**Reuse strategy:** Modify existing `expert/quiz-create.html` and `expert/quiz-list.html` to detect `isSequential=true` and render assignment-specific UI. Don't create entirely new templates — extend existing ones.

---

## 10. Open Questions

| # | Question | Decision |
|---|---|---|
| OQ-001 | Can an Expert reuse the same question bank questions across multiple Assignments? | Yes — `QuizQuestion` link is per-quiz; question entity itself is reusable |
| OQ-002 | Can a LISTENING/READING Passage (QuestionGroup) be used in an Assignment? | Yes — existing QuestionGroup flow works; each child question's `QuizQuestion.skill` is set individually |
| OQ-003 | What happens if Expert tries to delete a published Assignment? | Prevent deletion — check `quizResultRepository.existsByQuizQuizId` |
| OQ-004 | Does Expert need a dedicated "Assignment" list page separate from quiz list? | Yes — add tab/filter in existing quiz list: show Assignments separately from Quizzes |
