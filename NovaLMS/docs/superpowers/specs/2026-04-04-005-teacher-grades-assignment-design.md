# SPEC 005 — Teacher Grades Assignment

## 1. Overview

**Actor:** Teacher
**Flow:** Teacher opens the grading queue for Assignments (Course/Module), sees all student submissions with per-skill status badges, grades SPEAKING and WRITING sections (AI pre-populated), can override auto-graded LISTENING/READING scores, and submits final grades.
**Related Specs:** SPEC 001 (Expert creates Assignment), SPEC 004 (Student takes Assignment), SPEC 006 (Teacher grades Lesson Quiz)

---

## 2. Business Rules

| Rule | Description |
|---|---|
| BR-001 | Only teachers enrolled in a class where the Assignment is opened can view and grade submissions |
| BR-002 | Teacher can only grade QuizResults linked to their enrolled classes |
| BR-003 | LISTENING/READING: auto-graded by system at section submission time |
| BR-004 | SPEAKING/WRITING: AI grades asynchronously (fires after student submission) |
| BR-005 | AI score pre-populates the grading form but does NOT auto-submit — teacher must manually confirm |
| BR-006 | Teacher can override any score (auto-graded or AI-graded) |
| BR-007 | Grading is submitted per student per Assignment — one unified "Grade this student's assignment" action |
| BR-008 | When teacher submits grading, system recalculates total score + updates `passed` status |
| BR-009 | After grading, `QuizResult` is marked as "graded" — student can see final score |
| BR-010 | Teacher can re-open and re-grade any submission (undo/re-grade) |
| BR-011 | Grading queue shows aggregated status per student: which sections need grading |
| BR-012 | AI grading must complete before teacher's grading is submitted (teacher can submit before AI — AI score won't be in form but can be re-graded later) |

---

## 3. Grading Pipeline Overview

```
Student submits Assignment
    │
    ├─ LISTENING section → auto-grade immediately (MC/FILL/MATCH)
    ├─ READING section   → auto-grade immediately
    ├─ SPEAKING section → GroqGradingService.fireAndForget()
    │                      → transcribe + gradeWritingOrSpeaking
    │                      → save aiScore, aiFeedback, aiRubricJson to QuizAnswer
    └─ WRITING section  → GroqGradingService.fireAndForget()
                           → gradeWritingOrSpeaking
                           → save aiScore, aiFeedback, aiRubricJson to QuizAnswer

Teacher opens grading queue
    │
    ├─ Sees per-student row: overall status badges
    │    LISTENING ✅ | READING ✅ | SPEAKING ⏳ AI | WRITING ⏳ AI
    │
    └─ Clicks student → Grading Detail Page
         ├─ LISTENING tab: system score shown, teacher can override
         ├─ READING tab: system score shown, teacher can override
         ├─ SPEAKING tab: AI score pre-populated, teacher inputs final score
         └─ WRITING tab: AI score pre-populated, teacher inputs final score

Teacher submits grading
    │
    ├─ QuizResult.score = sum of all section scores
    ├─ QuizResult.passed = score >= passScore
    ├─ QuizResult.sectionScores = JSON per-skill scores
    └─ Student sees final result
```

---

## 4. Grading Queue Page

**URL:** `GET /teacher/assignment/grading`
**Template:** `teacher/assignment-grading-list.html` (new page)
**Controller:** `TeacherAssignmentGradingController.java` (new)

### Layout

```
┌────────────────────────────────────────────────────────────────┐
│  Teacher Header                                                  │
├────────────────────────────────────────────────────────────────┤
│  Bài kiểm tra lớn → Grading Queue                              │
│                                                                │
│  Filter: [Assignment ▼] [Class ▼] [Status: All ▼] [Search]   │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │ Student          Assignment        Sections      Actions   │ │
│  │─────────────────────────────────────────────────────────│ │
│  │ Nguyen Van A     Midterm Test      🎧✅ 📖✅ 🎤⏳ ✍️⏳    │ │
│  │                                          [Chấm điểm →]   │ │
│  │ Tran Thi B       Midterm Test      🎧✅ 📖✅ 🎤✅ ✍️⏳    │ │
│  │                                          [Chấm điểm →]   │ │
│  │ Le Van C         Midterm Test      🎧✅ 📖✅ 🎤✅ ✍️✅    │ │
│  │                                          [Đã chấm ✅]    │ │
│  └──────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────┘
```

### Status Badge Logic

| Section | Badge | Meaning |
|---|---|---|
| LISTENING | ✅ Auto | Auto-graded, no teacher action needed |
| LISTENING | ✏️ Override | Auto-graded, teacher can still override |
| READING | ✅ Auto | Auto-graded |
| READING | ✏️ Override | Auto-graded, teacher can still override |
| SPEAKING | ⏳ AI Pending | AI grading in progress |
| SPEAKING | 🎤 AI Ready | AI done, teacher can grade |
| SPEAKING | ✅ Done | Teacher submitted grade |
| WRITING | ⏳ AI Pending | AI grading in progress |
| WRITING | ✍️ AI Ready | AI done, teacher can grade |
| WRITING | ✅ Done | Teacher submitted grade |

### Queue API

```
GET /api/v1/teacher/assignment-results?quizId=&classId=&status=&page=0&size=20
```

**Response DTO — `AssignmentGradingQueueDTO`:**

```java
public class AssignmentGradingQueueDTO {
    private Long resultId;
    private Long assignmentSessionId;
    private String studentName;
    private String studentEmail;
    private Long quizId;
    private String quizTitle;
    private Long classId;
    private String className;
    private LocalDateTime submittedAt;
    private String overallStatus; // ALL_AUTO_GRADED | PENDING_SPEAKING | PENDING_WRITING | PENDING_BOTH | ALL_GRADED
    private SectionGradingStatus listening;
    private SectionGradingStatus reading;
    private SectionGradingStatus speaking;
    private SectionGradingStatus writing;
    private BigDecimal autoScore;    // sum of auto-graded (LISTENING+READING)
    private BigDecimal totalScore;   // final total (null if not fully graded)
    private Boolean isGraded;        // teacher submitted final grade
}

public class SectionGradingStatus {
    private String skill;            // LISTENING/READING/SPEAKING/WRITING
    private String gradingStatus;    // AUTO | AI_PENDING | AI_READY | GRADED
    private BigDecimal score;       // score (null if not yet graded)
    private BigDecimal maxScore;     // max possible
    private String aiScore;          // AI score if available (String for display)
    private String aiFeedback;      // AI feedback if available
}
```

### Filter Options

| Filter | Values | Default |
|---|---|---|
| Assignment | All Assignments for teacher's classes | All |
| Class | Teacher's enrolled classes | All |
| Status | Tất cả, Chờ chấm Nói, Chờ chấm Viết, Chờ chấm cả hai, Đã chấm xong | Tất cả |

---

## 5. Grading Detail Page

**URL:** `GET /teacher/assignment/grading/{resultId}`
**Template:** `teacher/assignment-grading-detail.html` (new page)

### Layout (Tab-based)

```
┌─────────────────────────────────────────────────────────────────┐
│  ← Quay lại danh sách    Chấm điểm: Nguyen Van A               │
│  Bài: Midterm Test         Lớp: IELTS 5.5         Ngày: 2024-04-01│
├─────────────────────────────────────────────────────────────────┤
│  Overall: Auto 14/40 pts   |  [LISTENING] [READING] [SPEAKING] [WRITING]│
│                                                              [✏️] │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  [ACTIVE TAB CONTENT — e.g. SPEAKING tab]                      │
│                                                                 │
│  🎤 Phần Nói (Speaking)                                         │
│  ─────────────────────────────────────────────────────────────  │
│  Câu 1: "Describe your daily routine..." (10 pts)              │
│  [Audio: ▶️ student_recording_abc123]                          │
│                                                                 │
│  🤖 AI Score: 7/10                                             │
│  AI Feedback: "Good vocabulary range, minor grammar errors..." │
│  AI Rubric:                                                    │
│    Task Achievement: 7/10                                      │
│    Lexical Resource: 6/10                                      │
│    Pronunciation: 7/10                                         │
│    Fluency: 7/10                                               │
│                                                                 │
│  Điểm của Giáo viên: [ 7 ] / 10                               │
│  Ghi chú: [________________________]                            │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### LISTENING / READING Tab

```
┌──────────────────────────────────────────────────────────────────┐
│  🎧 Phần Nghe (Listening)          [Score: 8/10] [Auto ✅]       │
│  ────────────────────────────────────────────────────────────── │
│  Câu 1: [Multiple Choice]                           ✅ Đúng     │
│  Student: B | Correct: B                                          │
│  ────────────────────────────────────────────────────────────── │
│  Câu 2: [Fill in Blank]                             ❌ Sai      │
│  Student: "went to the store" | Correct: "went to the market"  │
│  Teacher Override: [___] / 2 pts                                 │
│  ────────────────────────────────────────────────────────────── │
│  [Override all wrong answers] → batch override input             │
└──────────────────────────────────────────────────────────────────┘
```

### SPEAKING / WRITING Tab

```
┌──────────────────────────────────────────────────────────────────┐
│  🎤 Phần Nói (Speaking)              [AI Ready ⏳→🎤 Ready]      │
│  ────────────────────────────────────────────────────────────── │
│  Câu 1: SPEAKING — 10 pts maximum                                │
│  ────────────────────────────────────────────────────────────── │
│  [Audio player: ▶️ 0:00 / 1:47 ───●────]                       │
│  Transcript: "I wake up at 6 every morning and..."               │
│  ────────────────────────────────────────────────────────────── │
│  🤖 AI Score: 7/10                                               │
│  🤖 AI Feedback: "Good range of vocabulary. Minor grammar..."   │
│  🤖 AI Rubric Breakdown:                                        │
│    ├ Task Achievement: 7/10                                     │
│    ├ Lexical Resource: 6/10                                     │
│    ├ Pronunciation: 7/10                                        │
│    └ Fluency: 7/10                                              │
│  ────────────────────────────────────────────────────────────── │
│  ✏️ Giáo viên nhập điểm: [ 7 ] / 10                            │
│  Ghi chú: [Khá tốt, cần cải thiện phát âm___________]           │
│  ────────────────────────────────────────────────────────────── │
│  Câu 2: SPEAKING — 10 pts maximum (same structure)              │
│  ...                                                            │
│  ────────────────────────────────────────────────────────────── │
│  Section Total: [ 14 ] / 20 pts (auto-calculated from inputs)  │
└──────────────────────────────────────────────────────────────────┘
```

### Submit Grading Action

```
POST /api/v1/teacher/assignment-results/{resultId}/grade
Body: {
  sectionScores: {
    "LISTENING": 8.0,
    "READING": 6.5,
    "SPEAKING": 7.0,
    "WRITING": 7.5
  },
  gradingItems: [
    { "questionId": 1, "pointsAwarded": 2.0, "teacherNote": "" },
    { "questionId": 2, "pointsAwarded": 0.0, "teacherNote": "Sai đáp án" },
    ...
  ],
  overallNote: "Bài làm khá tốt"
}
```

**Backend (`TeacherAssignmentGradingService.gradeAssignment(resultId, request, teacherEmail)`):**
```
1. Fetch QuizResult by ID
2. Validate teacher has access (enrolled in class)
3. Validate result is linked to an Assignment (assignment_session_id != null)
4. For each SPEAKING/WRITING question in the result:
   a. Save QuizAnswer.pointsAwarded from gradingItems
   b. Save QuizAnswer.teacherNote from gradingItems
   c. Set QuizAnswer.hasPendingReview = false
5. Update QuizResult.sectionScores = request.sectionScores
6. Recalculate QuizResult.score = sum of sectionScores
7. Recalculate QuizResult.correctRate = score / totalPossible * 100
8. Set QuizResult.passed = score >= quiz.passScore (if passScore set)
9. Set result as "graded"
10. Return updated result
```

---

## 6. API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/teacher/assignment-results` | Grading queue with per-section status |
| `GET` | `/api/v1/teacher/assignment-results/{resultId}` | Get single result detail for grading |
| `POST` | `/api/v1/teacher/assignment-results/{resultId}/grade` | Submit grading for entire assignment |
| `GET` | `/api/v1/teacher/assignments` | List assignments assigned to teacher's classes |
| `GET` | `/api/v1/teacher/assignments/{quizId}/results` | All student results for one assignment |
| `PATCH` | `/api/v1/teacher/assignment-results/{resultId}/ai-refresh` | Re-request AI grading for a section (if AI failed) |

---

## 7. Service Layer

**New:** `ITeacherAssignmentGradingService` + `TeacherAssignmentGradingServiceImpl`

**Key methods:**

```java
// Get queue
Page<AssignmentGradingQueueDTO> getGradingQueue(Long teacherId, Long quizId,
    Long classId, String status, Pageable pageable);

// Get detail
AssignmentGradingDetailDTO getGradingDetail(Long resultId, Long teacherId);

// Submit grading
void gradeAssignment(Long resultId, AssignmentGradingRequestDTO request, Long teacherId);
```

---

## 8. AI Score Display Logic

| AI Status | UI Display |
|---|---|
| AI not started | "⏳ Đang chấm tự động..." |
| AI in progress | "⏳ Đang chấm tự động..." (polling every 5s) |
| AI done | Shows: score, feedback, rubric breakdown |
| AI failed | "⚠️ Chấm tự động thất bại. Giáo viên vui lòng chấm thủ công." |
| Teacher not yet graded | Score input field editable, pre-filled with AI score |
| Teacher already graded | Score input field shows submitted score, disabled |

---

## 9. HTML Templates

| Template | Route | Purpose |
|---|---|---|
| `teacher/assignment-grading-list.html` | `GET /teacher/assignment/grading` | Queue list |
| `teacher/assignment-grading-detail.html` | `GET /teacher/assignment/grading/{resultId}` | Per-student grading detail |

**Reuse strategy:** Extend existing `teacher/quiz-grading-list.html` and `teacher/quiz-grading-detail.html` — they handle Lesson Quiz grading. Add a tab or route distinction to handle Assignment-specific tabs.

---

## 10. Edge Cases

| Case | Handling |
|---|---|
| Teacher submits grading before AI finishes | Allowed — AI score will not be in form. Teacher inputs score manually. AI result is ignored if teacher already graded. |
| AI fails for a SPEAKING/WRITING question | `aiScore` stays null. Teacher sees warning badge. Teacher must input score manually. |
| Teacher partially grades and closes browser | Partial grades are NOT saved — teacher must complete and submit |
| Student retakes Assignment (multiple attempts) | Each attempt creates new `QuizResult` and new `AssignmentSession`. Teacher grades each attempt separately. |
| Teacher tries to grade another teacher's student's result | Rejected — 403 Forbidden |
| QuizResult already graded, teacher re-opens | Teacher can re-grade. Previous scores are overwritten on new submission. |
| `passScore` not set | `passed` field is null (no pass/fail determination) |
