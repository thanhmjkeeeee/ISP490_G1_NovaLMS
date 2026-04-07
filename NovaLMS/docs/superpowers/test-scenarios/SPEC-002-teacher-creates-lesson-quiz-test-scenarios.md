# SPEC 002 — Teacher Creates Lesson Quiz: Test Scenarios
**Date:** 2026-04-04
**Feature:** Teacher tạo bài quiz cho Lesson (1–4 skills, combo tự do)
**Spec:** `docs/superpowers/specs/2026-04-04-002-teacher-creates-lesson-quiz-design.md`
**Plan:** `docs/superpowers/plans/2026-04-04-002-teacher-creates-lesson-quiz-plan.md`

---

## Test Structure

```bash
src/test/java/com/example/DoAn/
├── service/impl/
│   └── TeacherQuizServiceTest.java          # Unit: getSkillSummary, createLessonQuiz
└── controller/
    └── TeacherLessonQuizControllerTest.java  # Integration tests
```

---

## 1. Service Layer Tests (`TeacherQuizServiceImpl`)

### TC-SVC-001: createLessonQuiz sets isSequential=false
**Unit Test** | `TeacherQuizServiceTest.java`

```
Setup:    Mock dependencies, teacher enrolled in class
Action:    createLessonQuiz(dto with lessonId, classId)
Assertion:
  - quiz.quizCategory == "LESSON_QUIZ"
  - quiz.isSequential == false
  - quiz.lessonId matches input
  - quiz.classId matches input
```

### TC-SVC-002: createLessonQuiz validates teacher enrollment
**Unit Test** | `TeacherQuizServiceTest.java`

```
Setup:    Teacher NOT enrolled in classId
Action:    createLessonQuiz → expect exception
Assertion: Throws InvalidDataException or validation error
```

### TC-SVC-003: createLessonQuiz validates lesson belongs to class
**Unit Test** | `TeacherQuizServiceTest.java`

```
Setup:    lessonId does NOT belong to classId
Action:    createLessonQuiz → expect exception
Assertion: Throws InvalidDataException "lessonId does not belong to classId"
```

### TC-SVC-004: getSkillSummary returns correct breakdown
**Unit Test** | `TeacherQuizServiceTest.java`

```
Setup:    Quiz with 3 LISTENING (2 PUBLISHED, 1 PENDING), 2 READING (all PUBLISHED)
Action:    getSkillSummary(quizId)
Assertion:
  - Returns list of 2 entries (LISTENING, READING)
  - LISTENING: total=3, published=2, pending=1
  - READING: total=2, published=2, pending=0
```

### TC-SVC-005: getSkillSummary handles empty quiz
**Unit Test** | `TeacherQuizServiceTest.java`

```
Setup:    Quiz with no questions
Action:    getSkillSummary(quizId)
Assertion: Returns empty list
```

### TC-SVC-006: publishQuiz allows LESSON_QUIZ with 0 questions
**Unit Test** | `TeacherQuizServiceTest.java`

```
NOTE: This is a spec conflict — SPEC 002 says publish requires >=1 question,
      but the plan says "publish validation ≥1 question".
      Test both behaviors:
```

```
Case A (SPEC says required):
  Setup: quizQuestionRepository.countByQuizQuizId(quizId) = 0
  Action: publishQuiz
  Assertion: HTTP 400 "Quiz must have at least 1 question"

Case B (SPEC says allowed for pending review):
  Setup: quiz has 0 questions
  Action: publishQuiz
  Assertion: Should this be allowed or blocked?
  → Clarify: SPEC 002 BR-006 says "Quiz must have ≥1 question before publishing"
```

### TC-SVC-007: Inline-created question has correct source and status
**Unit Test** | `TeacherQuizServiceTest.java`

```
Setup:    Teacher calls createQuestion via TeacherQuizService inline creation
Action:    Question created and added to quiz
Assertion:
  - question.status == "PENDING_REVIEW"
  - question.source == "TEACHER_PRIVATE"
  - question.userId == teacherId
```

---

## 2. API Controller Tests

### TC-API-001: POST /api/v1/teacher/lessons/{id}/quizzes creates lesson quiz
**Integration Test** | `TeacherLessonQuizControllerTest.java`

```
Setup:    Auth as TEACHER, lesson exists, teacher enrolled
Action:    POST /api/v1/teacher/lessons/{lessonId}/quizzes
          {title: "Quiz B1 Reading", classId: X, timeLimitMinutes: 30}
Assertion:
  - HTTP 201 Created
  - Response: {success:true, data: quizId}
  - Quiz exists with quizCategory=LESSON_QUIZ, isSequential=false
```

### TC-API-002: Non-enrolled teacher cannot create quiz for lesson
**Integration Test** | `TeacherLessonQuizControllerTest.java`

```
Setup:    Teacher NOT enrolled in the class that owns the lesson
Action:    POST /api/v1/teacher/lessons/{lessonId}/quizzes
Assertion: HTTP 403 Forbidden or 400 Bad Request
```

### TC-API-003: GET /api/v1/teacher/quizzes/{id} returns quiz with skill summary
**Integration Test** | `TeacherLessonQuizControllerTest.java`

```
Setup:    Lesson quiz with 2 LISTENING, 1 READING questions
Action:    GET /api/v1/teacher/quizzes/{quizId}
Assertion:
  - HTTP 200 OK
  - quiz.title, quizCategory, isSequential=false present
  - quizQuestions list includes questions
```

### TC-API-004: GET /api/v1/teacher/bank-questions returns only PUBLISHED questions
**Integration Test** | `TeacherLessonQuizControllerTest.java`

```
Setup:    Bank has 5 PUBLISHED, 2 PENDING_REVIEW, 3 DRAFT questions
Action:    GET /api/v1/teacher/bank-questions?skill=LISTENING&page=0&size=20
Assertion:
  - HTTP 200 OK
  - Returns only PUBLISHED questions
  - No PENDING_REVIEW questions in response
  - PENDING_REVIEW questions NOT visible to teacher in bank
```

### TC-API-005: Teacher can add PUBLISHED question to lesson quiz
**Integration Test** | `TeacherLessonQuizControllerTest.java`

```
Setup:    Question with status=PUBLISHED, source=EXPERT_BANK
Action:    POST /api/v1/teacher/quizzes/{quizId}/questions
          {questionId: X, itemType: "SINGLE", points: 1.0}
Assertion:
  - HTTP 200 OK
  - Question added to quiz
```

### TC-API-006: Teacher cannot add PENDING_REVIEW question from bank
**Integration Test** | `TeacherLessonQuizControllerTest.java`

```
Setup:    Question with status=PENDING_REVIEW (other teacher's question)
Action:    POST /api/v1/teacher/quizzes/{quizId}/questions
Assertion:
  - If bank-questions correctly filters PUBLISHED only → this question
    is NOT accessible via bank, so the test is about adding directly
  - Add direct question ID (bypass bank): depends on service validation
  - Verify service rejects: "Only PUBLISHED questions can be added"
```

### TC-API-007: DELETE /api/v1/teacher/quizzes/{id}/questions/{qid} removes question
**Integration Test** | `TeacherLessonQuizControllerTest.java`

```
Setup:    Lesson quiz with 5 questions
Action:    DELETE /api/v1/teacher/quizzes/{quizId}/questions/{questionId}
Assertion:
  - HTTP 200 OK
  - QuizQuestion count reduced by 1
  - Question entity still exists in DB
```

### TC-API-008: PATCH toggle-open controls student access
**Integration Test** | `TeacherLessonQuizControllerTest.java`

```
Setup:    Lesson quiz, isOpen=false
Action:    PATCH /api/v1/teacher/quizzes/{quizId}/toggle-open
Assertion:
  - HTTP 200 OK
  - quiz.isOpen == true
  - Student can now access the quiz
```

### TC-API-009: PATCH toggle-open from open to closed
**Integration Test** | `TeacherLessonQuizControllerTest.java`

```
Setup:    Lesson quiz, isOpen=true, students currently taking it
Action:    PATCH /api/v1/teacher/quizzes/{quizId}/toggle-open
Assertion:
  - quiz.isOpen == false
  - Student currently taking quiz sees "Bài đã đóng" on next action
```

### TC-API-010: PATCH publish lesson quiz without questions fails
**Integration Test** | `TeacherLessonQuizControllerTest.java`

```
Setup:    Lesson quiz with 0 questions, status=DRAFT
Action:    PATCH /api/v1/teacher/quizzes/{quizId}/publish
Assertion: HTTP 400 "Quiz must have at least 1 question"
```

### TC-API-011: PATCH publish lesson quiz with questions succeeds
**Integration Test** | `TeacherLessonQuizControllerTest.java`

```
Setup:    Lesson quiz with 3 questions, status=DRAFT
Action:    PATCH /api/v1/teacher/quizzes/{quizId}/publish
Assertion:
  - HTTP 200 OK
  - quiz.status == "PUBLISHED"
  - isOpen still false (teacher must toggle open separately)
```

---

## 3. Full Wizard Flow Tests

### TC-WIZ-001: Full 3-step lesson quiz creation flow
**Integration Test** | `FullTeacherLessonQuizFlowTest.java`

```
Setup:    Expert creates PUBLISHED questions in bank, teacher enrolled in class

Flow:
  Step 1 → POST /api/v1/teacher/lessons/{lessonId}/quizzes
    Body: {title: "B1 Reading Quiz", lessonId: X, classId: Y,
           timeLimitMinutes: 45, passScore: 70}
    → HTTP 201, returns quizId

  Step 2 → GET /teacher/quiz/{quizId}/build
    → GET /api/v1/teacher/bank-questions?skill=READING
    → Add 3 PUBLISHED questions from bank

  Step 2 → Create inline question:
    POST /api/v1/teacher/questions
    Body: {content, questionType, skill, cefrLevel}
    → HTTP 201, questionId returned
    → POST /api/v1/teacher/quizzes/{quizId}/questions
    → Question added with status=PENDING_REVIEW

  Step 3 → GET /teacher/quiz/{quizId}/finish
    → Review skill breakdown, pending count
    → PATCH /api/v1/teacher/quizzes/{quizId}/publish
    → HTTP 200

  Toggle Open → PATCH /api/v1/teacher/quizzes/{quizId}/toggle-open
    → isOpen = true

Assertion:
  - Quiz has 4 questions total (3 bank + 1 inline)
  - Quiz status = PUBLISHED, isOpen = true
  - 1 question has status=PENDING_REVIEW (pending expert approval)
```

### TC-WIZ-002: Quiz with mixed skills (READING + SPEAKING only)
**Integration Test** | `FullTeacherLessonQuizFlowTest.java`

```
Setup:    Teacher creates a READING+SPEAKING quiz only
Flow:     Add 2 READING from bank, 1 SPEAKING inline
          Publish
Assertion:
  - quiz has 3 questions
  - Skills: READING (2), SPEAKING (1)
  - No LISTENING or WRITING
  - isSequential = false
```

### TC-WIZ-003: Teacher creates quiz, adds PENDING question, publishes
**Integration Test** | `FullTeacherLessonQuizFlowTest.java`

```
Setup:    Teacher adds only PENDING_REVIEW questions to quiz
Flow:     Create 3 inline questions (all PENDING_REVIEW)
          Publish
Assertion:
  - Publish succeeds (SPEC 002 BR-014)
  - PENDING_REVIEW questions remain pending
  - Expert approves → questions auto-appear for future students
  - Current students see only approved questions OR quiz shows incomplete
  → Verify behavior matches spec
```

### TC-WIZ-004: Teacher saves draft without publishing
**Integration Test** | `FullTeacherLessonQuizFlowTest.java`

```
Setup:    Teacher creates quiz, adds questions
Flow:     PUT /api/v1/teacher/quizzes/{quizId}
          Body: {title: "Updated Title"}
          (no status change)
Assertion:
  - HTTP 200 OK
  - quiz.status == "DRAFT" (unchanged)
  - Changes persisted
```

---

## 4. Permission & Role Tests

### TC-PERM-001: STUDENT cannot create lesson quiz
**Integration Test** | `TeacherLessonQuizControllerTest.java`

```
Setup:    Auth as STUDENT
Action:    POST /api/v1/teacher/lessons/{lessonId}/quizzes
Assertion: HTTP 403 Forbidden
```

### TC-PERM-002: EXPERT cannot use teacher lesson quiz endpoint
**Integration Test** | `TeacherLessonQuizControllerTest.java`

```
Setup:    Auth as EXPERT
Action:    POST /api/v1/teacher/lessons/{lessonId}/quizzes
Assertion: HTTP 403 Forbidden
```

### TC-PERM-003: Teacher cannot add questions to another teacher's quiz
**Integration Test** | `TeacherLessonQuizControllerTest.java`

```
Setup:    Quiz created by teacherA
Action:    Auth as teacherB → POST /api/v1/teacher/quizzes/{quizA}/questions
Assertion: HTTP 403 Forbidden or 400
```

---

## 5. Edge Cases

### TC-EDGE-001: timeLimitMinutes = 0 is invalid
**Integration Test** | `TeacherLessonQuizControllerTest.java`

```
Setup:    timeLimitMinutes = 0
Action:    createLessonQuiz
Assertion: HTTP 400 or validation error
```

### TC-EDGE-002: passScore outside 0-100 range
**Integration Test** | `TeacherLessonQuizControllerTest.java`

```
Setup:    passScore = 150
Action:    createLessonQuiz
Assertion: HTTP 400 or validation error
```

### TC-EDGE-003: getSkillSummary with all PENDING_REVIEW questions
**Unit Test** | `TeacherQuizServiceTest.java`

```
Setup:    Quiz with 5 questions, all PENDING_REVIEW
Action:    getSkillSummary(quizId)
Assertion:
  - totalCount = 5 for relevant skill
  - publishedCount = 0
  - pendingCount = 5
```

### TC-EDGE-004: Teacher adds same question to quiz twice
**Integration Test** | `TeacherLessonQuizControllerTest.java`

```
Setup:    Question already in quiz
Action:    Add same questionId again
Assertion: Should reject duplicate OR skip silently
          → Verify service behavior matches plan (spec says "skip duplicates")
```

---

## Test Fixtures

```java
private User createTeacherUser() { ... }
private User createExpertUser() { ... }
private Clazz createClassWithTeacher(User teacher) { ... }
private Lesson createLessonInClass(Clazz clazz) { ... }
private List<Question> createPublishedQuestions(String skill, int count) { ... }
private List<Question> createPendingQuestions(User teacher, String skill, int count) { ... }
```
