# SPEC 006 — Teacher Grades Lesson Quiz

## 1. Overview

**Actor:** Teacher
**Flow:** Teacher grades a Lesson Quiz submission (N skills, any combination of LISTENING/READING/SPEAKING/WRITING). The grading UI adapts to show only the skills that were included in this specific quiz. Grading pipeline: system auto-grades MC/FILL/MATCH, AI pre-grades SPEAKING/WRITING, teacher confirms/overrides.
**Related Specs:** SPEC 002 (Teacher creates Lesson Quiz), SPEC 005 (Teacher grades Assignment)

---

## 2. Business Rules

| Rule | Description |
|---|---|
| BR-001 | Only teachers enrolled in a class can grade Lesson Quiz submissions from that class |
| BR-002 | `quiz_category = LESSON_QUIZ`, `isSequential = false` |
| BR-003 | Only skills present in the quiz are shown in the grading UI (dynamic — 1 to 4 tabs) |
| BR-004 | Skills are determined by `question.skill` of questions in the quiz |
| BR-005 | MC/FILL/MATCH questions: auto-graded at submission time, teacher can still override |
| BR-006 | SPEAKING/WRITING questions: AI pre-grades asynchronously, teacher confirms/overrides |
| BR-007 | Teacher cannot grade quizzes from classes they are not enrolled in |
| BR-008 | Existing `TeacherQuizGradingApiController` handles this flow — extension only |

---

## 3. Difference from Assignment Grading (SPEC 005)

| Aspect | Assignment Grading (SPEC 005) | Lesson Quiz Grading (SPEC 006) |
|---|---|---|
| Skills | Always 4 fixed tabs | Dynamic: only the N skills in this quiz |
| Skill order | LISTENING → READING → SPEAKING → WRITING | Whatever order questions appear |
| Section scores | Per-skill section scores tracked in `sectionScores` JSON | Per-question scores |
| Skill grouping | Grouped by section tab | Grouped by skill tab (same logic) |
| Teacher submission | One unified submit per student per assignment | One unified submit per student per quiz |
| Existing code | Mostly new | Mostly reuse existing |

---

## 4. Quiz Result Changes for LESSON_QUIZ

**Existing `QuizResult` entity** is used as-is for Lesson Quizzes.

**Grading storage:**
- `QuizAnswer.pointsAwarded` — per question
- `QuizAnswer.hasPendingReview` — false when teacher submits
- `QuizAnswer.aiScore`, `QuizAnswer.aiFeedback`, `QuizAnswer.aiRubricJson` — AI results
- `QuizAnswer.teacherNote` — optional note

**New field for `quiz_result` table:**
```sql
ALTER TABLE quiz_result ADD COLUMN skill_scores JSON;
-- e.g. {"LISTENING": 8.0, "SPEAKING": 7.0}
-- Used for both Assignment and Lesson Quiz
```

---

## 5. Dynamic Skill Detection

**Backend:** `TeacherLessonQuizGradingService.getGradingDetail(resultId, teacherId)`

```
1. Fetch QuizResult → Quiz → QuizQuestion list
2. Extract distinct skills from QuizQuestion.skill field
3. Group QuizAnswers by skill
4. Return: { skills: ["LISTENING", "SPEAKING"], sections: { ... } }
```

---

## 6. Grading Queue Page (Extend Existing)

**Existing:** `teacher/quiz-grading-list.html` + `GET /teacher/quiz/grading`
**Change:** Add "Loại bài" filter — "Bài tập nhỏ" (Lesson Quiz) vs "Bài kiểm tra lớn" (Assignment)

**Extended queue API:**

```
GET /api/v1/teacher/quiz-results/pending?quizType=LESSON_QUIZ&quizId=&classId=&page=0&size=20
```

**Response DTO — `LessonQuizGradingQueueDTO`:**

```java
public class LessonQuizGradingQueueDTO {
    private Long resultId;
    private String studentName;
    private String studentEmail;
    private Long quizId;
    private String quizTitle;
    private String quizCategory;       // LESSON_QUIZ
    private Long classId;
    private String className;
    private Long lessonId;
    private String lessonName;
    private LocalDateTime submittedAt;
    private List<String> skillsPresent;  // e.g. ["LISTENING", "SPEAKING"]
    private Map<String, SkillGradingStatus> skillStatuses;
    // skills not present in quiz are omitted
    private Boolean isGraded;
    private BigDecimal autoScore;     // auto-graded only
    private BigDecimal totalScore;    // final (null if not graded)
}
```

---

## 7. Grading Detail Page (Extend Existing)

**Existing:** `teacher/quiz-grading-detail.html` + `GET /teacher/quiz/grading/{resultId}`
**Template change:** Replace hardcoded skill sections with dynamic tabs rendered from `skillsPresent` list.

### Dynamic Tab Logic (Frontend)

```html
<!-- For each skill in result.skillsPresent: render tab -->
<th:block th:each="skill : ${result.skillsPresent}">
    <div th:class="'tab-pane fade' + ${skillStat.first ? ' show active' : ''}"
         th:id="${'skill-' + skill}"
         th:fragment="skillTabContent">

        <!-- LISTENING/READING tab -->
        <div th:if="${skill == 'LISTENING' or skill == 'READING'}">
            <!-- auto-graded questions, teacher override inputs -->
        </div>

        <!-- SPEAKING/WRITING tab -->
        <div th:if="${skill == 'SPEAKING' or skill == 'WRITING'}">
            <!-- AI score display + teacher score input -->
        </div>
    </div>
</th:block>
```

### SPEAKING-specific display

```
<div th:if="${skill == 'SPEAKING'}">
    <!-- Audio player for each SPEAKING question -->
    <audio controls>
        <source th:src="@{${answer.audioUrl}}" type="audio/mpeg">
    </audio>
    <!-- If audioUrl null: "Thí sinh chưa ghi âm" -->
    <!-- AI score + teacher input -->
</div>
```

---

## 8. Submit Grading (Extend Existing)

**Existing:** `POST /api/v1/teacher/quiz-results/{resultId}/grade`

**Extend request DTO — `QuizGradingRequestDTO`:**

```java
public class QuizGradingRequestDTO {
    private List<QuestionGradingItem> gradingItems; // existing
    private Map<String, BigDecimal> skillScores;   // new: per-skill scores for display
    private String overallNote;                     // new: teacher note
}

// Existing
public class QuestionGradingItem {
    private Integer questionId;
    private BigDecimal pointsAwarded;
    private String teacherNote;
}
```

**Backend changes needed:**
1. Save `skillScores` to `quizResult.skillScores` (JSON column)
2. Save `overallNote` to `quizResult` if new field exists
3. Rest of logic remains the same

---

## 9. AI Grading for Lesson Quiz

**Existing:** `GroqGradingServiceImpl` grades `PlacementTestAnswer` entities.
**Extension needed:** Grade `QuizAnswer` entities for Lesson Quiz (SPEAKING/WRITING questions).

### Changes to `GroqGradingServiceImpl`

**Option A — Fire AI grading at submission time (same as Assignment)**

When `StudentQuizTakingController.submit()` processes a LESSON_QUIZ with SPEAKING/WRITING:
```
1. For each SPEAKING/WRITING QuizAnswer in the result:
   → set hasPendingReview = true
2. After tx commits → GroqGradingService.fireAndForget(quizAnswerId, "QUIZ_ANSWER")
```

**New overload in `GroqGradingService`:**
```java
void gradeQuizAnswer(Long quizAnswerId); // grades QuizAnswer, not PlacementTestAnswer
```

**Implementation:**
```
1. Fetch QuizAnswer by ID
2. If SPEAKING: groqClient.transcribe(audioUrl) → transcript
3. groqClient.gradeWritingOrSpeaking(cefrLevel, questionType, transcript/userText)
4. Save QuizAnswer.aiScore, aiFeedback, aiRubricJson
5. Mark hasPendingReview = true (waiting for teacher)
6. Do NOT recalculate QuizResult (teacher must submit grading first)
```

### Changes to `QuizAnswer` Entity

```java
// Add to QuizAnswer entity
private String aiScore;
private String aiFeedback;
private String aiRubricJson; // JSON rubric breakdown
```

---

## 10. Teacher's "My Grading" Dashboard

**New page:** `GET /teacher/my-grading`
**Template:** `teacher/my-grading.html`

**Shows:**
- Tabs: "Bài kiểm tra lớn" (Assignments) | "Bài tập nhỏ" (Lesson Quizzes)
- Summary cards: pending count, done today, avg grading time
- Quick links to most urgent grading queues

---

## 11. Service Layer Changes

### Extend `TeacherQuizGradingService`

**File:** `src/main/java/com/example/DoAn/service/impl/TeacherQuizGradingServiceImpl.java`

**Changes:**

```java
// 1. Extend getPendingList to include skill breakdown
public Page<LessonQuizGradingQueueDTO> getPendingList(Long teacherId,
    String quizCategory, Long quizId, Long classId, Pageable pageable) {
    // Existing: filter by teacher's enrolled classes
    // New: add skillPresent list per result
    // New: add skillStatuses per result
}

// 2. Extend gradeResult to save skillScores JSON
public void gradeResult(Long resultId, QuizGradingRequestDTO request, Long teacherId) {
    // Existing: grade per question
    // New: save request.skillScores to quizResult.skillScores
    // New: save request.overallNote
}
```

### Add `QuizAnswer.aiScore` etc. fields

```java
// Add to QuizAnswer entity
@Column(name = "ai_score")
private String aiScore; // stored as String (e.g. "7/10")

@Column(name = "ai_feedback", columnDefinition = "TEXT")
private String aiFeedback;

@Column(name = "ai_rubric_json", columnDefinition = "TEXT")
private String aiRubricJson;
```

---

## 12. API Endpoints Summary

| Method | Endpoint | Status | Description |
|---|---|---|---|
| `GET` | `/api/v1/teacher/quiz-results/pending` | existing+extend | Grading queue with dynamic skills |
| `GET` | `/api/v1/teacher/quiz-results/{resultId}` | existing+extend | Grading detail with dynamic skill tabs |
| `POST` | `/api/v1/teacher/quiz-results/{resultId}/grade` | existing+extend | Submit grading |
| `GET` | `/api/v1/teacher/quiz-results/{resultId}/ai-status` | new | Poll AI grading status per question |
| `PATCH` | `/api/v1/teacher/quiz-results/{resultId}/ai-refresh` | new | Re-request AI grade |
| `GET` | `/api/v1/teacher/my-grading` | new | Grading dashboard |

---

## 13. Edge Cases

| Case | Handling |
|---|---|
| Quiz has 1 skill only (e.g. LISTENING only) | Show 1 tab in grading detail |
| Quiz has 3 skills (e.g. LISTENING + SPEAKING + WRITING, no READING) | Show 3 tabs matching the actual skills |
| Quiz has all 4 skills | Same as Assignment grading — 4 tabs |
| SPEAKING question but student didn't record (no audioUrl) | Show "Thí sinh chưa ghi âm" → score = 0 |
| AI grading fails | Show "⚠️ AI failed" badge; teacher inputs score manually |
| Quiz has no SPEAKING/WRITING questions | No AI grading fired; all auto-graded |
| Quiz has no MC/FILL/MATCH questions | Auto-score = 0; total is AI + teacher scores only |
| Multiple attempts per student | Each attempt = separate QuizResult; teacher grades each separately |
| Teacher grades a result that was already graded | Allow re-grade; overwrite previous scores |
| TEACHER_PRIVATE question pending approval | If expert hasn't approved yet, question does not appear in quiz for student. If student somehow got it, show "Câu hỏi đang chờ duyệt" → score = null |

---

## 14. Unifying SPEC 005 and SPEC 006

Since both Assignment grading and Lesson Quiz grading use the same `QuizResult` entity and same grading flow, the system uses a single unified grading controller:

**Unified grading controller:** `TeacherGradingController.java`
- `GET /teacher/grading` → lists both Assignment and Lesson Quiz pending items
- `GET /teacher/grading/{resultId}` → dynamic detail page (shows tabs based on quiz type and skills)
- `POST /api/v1/teacher/results/{resultId}/grade` → unified grading endpoint

**URL pattern:**
```
/teacher/grading              → list (filter by type)
/teacher/grading/quiz/{rid}   → Lesson Quiz grading detail
/teacher/grading/assignment/{rid} → Assignment grading detail
```
