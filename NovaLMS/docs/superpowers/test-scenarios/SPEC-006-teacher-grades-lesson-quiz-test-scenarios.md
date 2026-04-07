# SPEC 006 — Teacher Grades Lesson Quiz: Test Scenarios
**Date:** 2026-04-04
**Feature:** Teacher chấm Lesson Quiz (dynamic skill tabs 1–4, AI pre-grade SPEAKING/WRITING)
**Spec:** `docs/superpowers/specs/2026-04-04-006-teacher-grades-lesson-quiz-design.md`
**Plan:** `docs/superpowers/plans/2026-04-04-006-teacher-grades-lesson-quiz-plan.md`

---

## Test Structure

```bash
src/test/java/com/example/DoAn/
├── service/impl/
│   ├── GroqGradingServiceTest.java        # AI grading for QuizAnswer
│   └── TeacherQuizGradingServiceTest.java  # Dynamic skill grading
└── controller/
    └── TeacherQuizGradingApiControllerTest.java  # Endpoint tests
```

---

## 1. QuizAnswer AI Fields Tests

### TC-AI-001: QuizAnswer entity has AI score fields
**Unit Test** | `GroqGradingServiceTest.java`

```
Setup:    Load QuizAnswer entity via reflection
Action:    Verify fields exist
Assertion:
  - aiScore (String, nullable)
  - aiFeedback (TEXT, nullable)
  - aiRubricJson (TEXT, nullable)
  - pendingAiReview (Boolean, default false)
```

### TC-AI-002: gradeQuizAnswer sets AI fields for SPEAKING
**Unit Test** | `GroqGradingServiceTest.java`

```
Setup:    Mock groqClient, quizAnswerRepository
          QuizAnswer with SPEAKING question
          groqClient.transcribe() returns "I wake up at 6..."
          groqClient.gradeWritingOrSpeaking() returns score 7/10, feedback, rubric
Action:    groqGradingService.gradeQuizAnswer(resultId, questionId)
Assertion:
  - quizAnswer.aiScore == "7/10"
  - quizAnswer.aiFeedback populated
  - quizAnswer.aiRubricJson valid JSON
  - quizAnswerRepository.save() called
```

### TC-AI-003: gradeQuizAnswer handles WRITING questions
**Unit Test** | `GroqGradingServiceTest.java`

```
Setup:    WRITING question
          answeredOptions contains text answer
          groqClient.gradeWritingOrSpeaking() returns score 8/10
Action:    gradeQuizAnswer(resultId, questionId)
Assertion:
  - Text answer used (not audio URL)
  - AI score populated
```

### TC-AI-004: gradeQuizAnswer skips null audio URL for SPEAKING
**Unit Test** | `GroqGradingServiceTest.java`

```
Setup:    SPEAKING question, answeredOptions=null (student didn't record)
          groqClient.transcribe() → null/empty
Action:    gradeQuizAnswer(resultId, questionId)
Assertion:
  - Grade with empty text
  - Score may be low/0 (AI handles gracefully)
  - No NPE thrown
```

### TC-AI-005: gradeQuizAnswer handles CEFR fallback
**Unit Test** | `GroqGradingServiceTest.java`

```
Setup:    Question with cefrLevel=null
Action:    gradeQuizAnswer(resultId, questionId)
Assertion:
  - CEFR defaults to B1
  - groqClient.gradeWritingOrSpeaking() called with B1
```

### TC-AI-006: gradeQuizAnswer handles groqClient exception
**Unit Test** | `GroqGradingServiceTest.java`

```
Setup:    groqClient.transcribe() throws IOException
Action:    gradeQuizAnswer → exception caught, logged
Assertion:
  - No exception propagates to caller
  - Log entry created
  - QuizAnswer NOT saved with partial data
```

### TC-AI-007: fireAndForgetForQuizAnswer executes async
**Unit Test** | `GroqGradingServiceTest.java`

```
Setup:    Mock CompletableFuture
Action:    fireAndForgetForQuizAnswer(resultId, questionId)
Assertion:
  - Runs asynchronously (CompletableFuture.runAsync)
  - gradeQuizAnswer called in async thread
```

---

## 2. QuizResult Skill Scores Tests

### TC-SCORE-001: QuizResult has skillScores JSON field
**Unit Test** | `TeacherQuizGradingServiceTest.java`

```
Setup:    Load QuizResult entity
Action:    Verify field exists
Assertion: skillScores field present (JSON column)
```

### TC-SCORE-002: skillScores stored as valid JSON
**Unit Test** | `TeacherQuizGradingServiceTest.java`

```
Setup:    gradeResult with skillScores={"LISTENING": 8.0, "SPEAKING": 7.0}
Action:    Read back quizResult.skillScores
Assertion:
  - Valid JSON string
  - ObjectMapper deserializes correctly
  - Values match submitted
```

### TC-SCORE-003: skillScores used for display (not recalculated)
**Unit Test** | `TeacherQuizGradingServiceTest.java`

```
Setup:    skillScores submitted with custom breakdown
Action:    gradeResult
Assertion: skillScores stored, total calculated from it
```

---

## 3. Dynamic Skill Detection Tests

### TC-SKILL-001: Dynamic skill tabs: quiz with LISTENING only
**Unit Test** | `TeacherQuizGradingServiceTest.java`

```
Setup:    Quiz with only LISTENING questions (1 skill)
Action:    getGradingDetail or getPendingList
Assertion:
  - skillsPresent == ["LISTENING"]
  - Only 1 tab rendered in UI
  - No empty/placeholder tabs
```

### TC-SKILL-002: Dynamic skill tabs: quiz with 3 skills (no READING)
**Unit Test** | `TeacherQuizGradingServiceTest.java`

```
Setup:    Quiz with LISTENING, SPEAKING, WRITING (no READING)
Action:    getGradingDetail
Assertion:
  - skillsPresent == ["LISTENING", "SPEAKING", "WRITING"]
  - READING absent (no tab)
```

### TC-SKILL-003: Dynamic skill tabs: quiz with all 4 skills
**Unit Test** | `TeacherQuizGradingServiceTest.java`

```
Setup:    Full 4-skill quiz
Action:    getGradingDetail
Assertion: skillsPresent == ["LISTENING", "READING", "SPEAKING", "WRITING"]
```

### TC-SKILL-004: Skill detection from QuizQuestion.skill field
**Unit Test** | `TeacherQuizGradingServiceTest.java`

```
Setup:    QuizQuestion entities have skill field populated
Action:    getPendingList
Assertion:
  - Service extracts distinct skills from QuizQuestion.skill
  - Groups answers by skill
```

---

## 4. Grading Queue Extended Tests

### TC-QUEUE-001: Queue shows skill badges for lesson quiz
**Unit Test** | `TeacherQuizGradingServiceTest.java`

```
Setup:    Lesson Quiz with LISTENING+SPEAKING
Action:    getPendingList for LESSON_QUIZ
Assertion:
  - skillStatuses shows LISTENING and SPEAKING
  - READ/WR absent
```

### TC-QUEUE-002: Lesson Quiz vs Assignment filter
**Unit Test** | `TeacherQuizGradingServiceTest.java`

```
Setup:    3 Assignment results, 2 Lesson Quiz results
Action:    getPendingList(quizCategory="LESSON_QUIZ")
Assertion: Returns only Lesson Quiz results
```

### TC-QUEUE-003: Queue shows AI status per skill for lesson quiz
**Unit Test** | `TeacherQuizGradingServiceTest.java`

```
Setup:    Lesson Quiz: LISTENING ✅ auto, SPEAKING ⏳ AI pending
Action:    getPendingList
Assertion:
  - skillStatuses["LISTENING"] == AUTO
  - skillStatuses["SPEAKING"] == AI_PENDING
```

---

## 5. Submit Grading Tests

### TC-GRADE-001: gradeQuizResult saves skillScores
**Unit Test** | `TeacherQuizGradingServiceTest.java`

```
Setup:    QuizGradingRequestDTO with skillScores + gradingItems
Action:    gradeQuizResult(resultId, request, teacherId)
Assertion:
  - quizResult.skillScores == JSON of skillScores
  - quizResult.overallNote saved
```

### TC-GRADE-002: gradeQuizResult handles overallNote null
**Unit Test** | `TeacherQuizGradingServiceTest.java`

```
Setup:    Request with overallNote=null
Action:    gradeQuizResult
Assertion: quizResult.overallNote == null (no error)
```

### TC-GRADE-003: Re-grade updates existing scores
**Unit Test** | `TeacherQuizGradingServiceTest.java`

```
Setup:    Previously graded SPEAKING: 6/10
Action:    gradeQuizResult with new SPEAKING: 8/10
Assertion:
  - Previous 6 overwritten → 8
  - skillScores updated
  - QuizResult.score recalculated
```

### TC-GRADE-004: AI grading triggered for lesson quiz submission
**Integration Test** | `TeacherQuizGradingServiceTest.java`

```
Setup:    Mock groqGradingService
          Student submits lesson quiz with SPEAKING/WRITING answers
Action:    QuizResultService.submit() processes quiz
Assertion:
  - groqGradingService.fireAndForgetForQuizAnswer() called
    for each SPEAKING question
  - groqGradingService.fireAndForgetForQuizAnswer() called
    for each WRITING question
```

---

## 6. API Endpoint Tests

### TC-API-001: GET /api/v1/teacher/quiz-results/pending with quizType filter
**Integration Test** | `TeacherQuizGradingApiControllerTest.java`

```
Setup:    Auth as TEACHER
Action:    GET /api/v1/teacher/quiz-results/pending?quizType=LESSON_QUIZ&page=0&size=20
Assertion:
  - HTTP 200 OK
  - Only LESSON_QUIZ results returned
  - Dynamic skill tabs info included
```

### TC-API-002: GET /api/v1/teacher/quiz-results/{id} with dynamic skills
**Integration Test** | `TeacherQuizGradingApiControllerTest.java`

```
Setup:    Lesson Quiz with LISTENING+SPEAKING
Action:    GET /api/v1/teacher/quiz-results/{resultId}
Assertion:
  - skillsPresent == ["LISTENING", "SPEAKING"]
  - Dynamic tab content per skill
  - AI scores visible if available
```

### TC-API-003: POST /api/v1/teacher/quiz-results/{id}/grade with skillScores
**Integration Test** | `TeacherQuizGradingApiControllerTest.java`

```
Setup:    Lesson Quiz: LISTENING+SPEAKING
Action:    POST /api/v1/teacher/quiz-results/{resultId}/grade
          Body: {
            gradingItems: [{questionId: 1, pointsAwarded: 2.0, teacherNote: "Tốt"}],
            skillScores: {LISTENING: 8.0, SPEAKING: 7.0},
            overallNote: "Bài làm khá"
          }
Assertion:
  - HTTP 200 OK
  - skillScores persisted to quizResult
```

### TC-API-004: GET /api/v1/teacher/quiz-results/{id}/ai-status polls AI grading
**Integration Test** | `TeacherQuizGradingApiControllerTest.java`

```
Setup:    AI grading in progress
Action:    GET /api/v1/teacher/quiz-results/{resultId}/ai-status
Assertion:
  - Returns per-question AI status
  - pending = true/false per question
```

### TC-API-005: PATCH re-request AI grade
**Integration Test** | `TeacherQuizGradingApiControllerTest.java`

```
Setup:    AI grading failed (aiScore=null)
Action:    PATCH /api/v1/teacher/quiz-results/{resultId}/ai-refresh
Assertion:
  - HTTP 200 OK
  - AI grading re-triggered for failed questions
```

### TC-API-006: Teacher grades quiz from unenrolled class
**Integration Test** | `TeacherQuizGradingApiControllerTest.java`

```
Setup:    Result belongs to class teacher is not enrolled in
Action:    GET /api/v1/teacher/quiz-results/{resultId}
Assertion: HTTP 403 Forbidden
```

---

## 7. Full Grading Flow Tests

### TC-E2E-001: Teacher grades LISTENING+SPEAKING lesson quiz
**Integration Test** | `FullLessonQuizGradingFlowTest.java`

```
Setup:    Lesson Quiz: LISTENING (5 MC), SPEAKING (2 recording)
          Teacher enrolled in class
          Student submitted quiz

Flow:
  1. Teacher views queue
     GET /api/v1/teacher/quiz-results/pending?quizType=LESSON_QUIZ
     → Row: LISTENING ✅, SPEAKING ⏳ AI

  2. Teacher opens detail
     GET /api/v1/teacher/quiz-results/{resultId}
     → 2 tabs: LISTENING | SPEAKING (dynamic!)

  3. LISTENING tab
     → 5 MC questions auto-graded
     → Teacher sees student answers

  4. SPEAKING tab
     → Audio player for each recording
     → AI Score: 7/10 for Q1, 6/10 for Q2
     → AI Feedback visible
     → Teacher confirms/adjusts scores

  5. Submit grading
     POST /api/v1/teacher/quiz-results/{resultId}/grade
     → skillScores={LISTENING: 8.0, SPEAKING: 6.5}
     → QuizResult.score = 14.5

Assertion:
  - Only 2 tabs rendered (dynamic)
  - QuizResult has skillScores
  - Student sees final score
```

### TC-E2E-002: Dynamic tabs - single skill quiz
**Integration Test** | `FullLessonQuizGradingFlowTest.java`

```
Setup:    Lesson Quiz: LISTENING only (10 MC questions)

Flow:
  1. Open grading detail
  2. Only 1 tab: LISTENING

Assertion: Exactly 1 tab rendered
```

### TC-E2E-003: AI failed → teacher manually grades SPEAKING
**Integration Test** | `FullLessonQuizGradingFlowTest.java`

```
Setup:    groqClient unavailable/exception

Flow:
  1. Teacher opens SPEAKING tab
  2. AI score: null
  3. Badge: ⚠️ AI thất bại

Assertion:
  - Teacher manually enters score
  - Submission succeeds
  - Score saved regardless of AI failure
```

---

## 8. Edge Cases

### TC-EDGE-001: TEACHER_PRIVATE question PENDING_REVIEW in quiz
**Unit Test** | `TeacherQuizGradingServiceTest.java`

```
Setup:    Question in quiz with status=PENDING_REVIEW
          Student somehow received this question
Action:    getGradingDetail
Assertion:
  - Question shows with note "Câu hỏi đang chờ duyệt"
  - Score field disabled/hidden
  - Teacher cannot grade pending question
```

### TC-EDGE-002: Quiz with no SPEAKING/WRITING (MC only)
**Integration Test** | `TeacherQuizGradingServiceTest.java`

```
Setup:    Lesson Quiz: LISTENING + READING only
          All MC questions
Action:    QuizResult.submit()
Assertion:
  - No AI grading fired
  - All auto-graded
  - Teacher grading tab shows only auto-graded questions
```

### TC-EDGE-003: Multiple attempts by same student
**Integration Test** | `FullLessonQuizGradingFlowTest.java`

```
Setup:    Student made 2 attempts

Assertion:
  - 2 separate QuizResult records
  - Teacher grades each separately
  - Queue shows both attempts
```

---

## Test Fixtures

```java
private QuizResult createLessonQuizResult(Quiz quiz, User student) { ... }
private List<QuizAnswer> createQuizAnswersWithAI(
    QuizResult result, Map<String, String> aiScores) { ... }
// aiScores: questionId → "8/10"
private void assertDynamicTabs(List<String> expectedSkills,
    AssignmentGradingDetailDTO detail) { ... }
```
