# SPEC 005 — Teacher Grades Assignment: Test Scenarios
**Date:** 2026-04-04
**Feature:** Teacher chấm Bài Kiểm Tra Lớn (4 tab cố định, AI pre-grade SPEAKING/WRITING)
**Spec:** `docs/superpowers/specs/2026-04-04-005-teacher-grades-assignment-design.md`
**Plan:** `docs/superpowers/plans/2026-04-04-005-teacher-grades-assignment-plan.md`

---

## Test Structure

```bash
src/test/java/com/example/DoAn/
├── service/impl/
│   └── TeacherAssignmentGradingServiceTest.java  # Service unit tests
├── controller/
│   └── TeacherAssignmentGradingControllerTest.java  # API tests
└── integration/
    └── TeacherAssignmentGradingFlowTest.java     # Full grading flows
```

---

## 1. Grading Queue Tests

### TC-QUEUE-001: getGradingQueue returns only teacher's class submissions
**Unit Test** | `TeacherAssignmentGradingServiceTest.java`

```
Setup:    Teacher enrolled in class A (owns 5 submissions)
          Teacher NOT enrolled in class B (3 submissions)
Action:    getGradingQueue(teacherId, ...)
Assertion:
  - Returns only submissions from class A
  - Excludes class B submissions
```

### TC-QUEUE-002: getGradingQueue returns only sequential (assignment) quizzes
**Unit Test** | `TeacherAssignmentGradingServiceTest.java`

```
Setup:    5 Assignment (isSequential=true) results, 3 Lesson Quiz results
Action:    getGradingQueue(teacherId, ...)
Assertion:
  - Returns only Assignment results
  - Excludes LESSON_QUIZ results
```

### TC-QUEUE-003: Queue item shows correct per-skill status badges
**Unit Test** | `TeacherAssignmentGradingServiceTest.java`

```
Setup:    Student submission:
          - LISTENING: auto-graded ✅
          - READING: auto-graded ✅
          - SPEAKING: AI done, score available
          - WRITING: AI pending
Action:    getGradingQueue returns AssignmentGradingQueueDTO
Assertion:
  - listening.gradingStatus == "AUTO"
  - reading.gradingStatus == "AUTO"
  - speaking.gradingStatus == "AI_READY"
  - writing.gradingStatus == "AI_PENDING"
```

### TC-QUEUE-004: Queue item with all sections graded
**Unit Test** | `TeacherAssignmentGradingServiceTest.java`

```
Setup:    All 4 sections: LISTENING ✅, READING ✅, SPEAKING ✅ (teacher graded), WRITING ✅
Action:    getGradingQueue
Assertion:
  - speaking.gradingStatus == "GRADED"
  - writing.gradingStatus == "GRADED"
  - isGraded == true
```

### TC-QUEUE-005: Filter queue by assignment (quizId)
**Unit Test** | `TeacherAssignmentGradingServiceTest.java`

```
Setup:    Assignments A (3 submissions) and B (2 submissions)
Action:    getGradingQueue(teacherId, quizId=A.id, ...)
Assertion:
  - Returns only 3 results for Assignment A
```

### TC-QUEUE-006: Filter queue by status
**Unit Test** | `TeacherAssignmentGradingServiceTest.java`

```
Setup:    2 awaiting SPEAKING grade, 1 awaiting WRITING, 2 fully graded
Action:    getGradingQueue(teacherId, ..., status="PENDING_SPEAKING")
Assertion: Returns only 2 awaiting SPEAKING grade
```

---

## 2. Grading Detail Tests

### TC-DETAIL-001: getGradingDetail returns all 4 skill sections
**Unit Test** | `TeacherAssignmentGradingServiceTest.java`

```
Setup:    Assignment result with all 4 sections
Action:    getGradingDetail(resultId, teacherId)
Assertion:
  - Returns AssignmentGradingDetailDTO
  - sections has 4 entries
  - Each section: skill name, gradingStatus, maxScore
```

### TC-DETAIL-002: getGradingDetail shows LISTENING auto-graded details
**Unit Test** | `TeacherAssignmentGradingServiceTest.java`

```
Setup:    LISTENING section: 5 questions, student got 4 correct
Action:    getGradingDetail
Assertion:
  - listening.skill == "LISTENING"
  - listening.gradingStatus == "AUTO"
  - listening.maxScore == sum of points
  - listening.teacherScore == 4 (auto-calculated)
  - questions[0].isCorrect == true
  - questions[1].isCorrect == false
```

### TC-DETAIL-003: getGradingDetail shows SPEAKING with AI score
**Unit Test** | `TeacherAssignmentGradingServiceTest.java`

```
Setup:    SPEAKING question: AI scored 7/10, feedback available
Action:    getGradingDetail
Assertion:
  - speaking.gradingStatus == "AI_READY"
  - speaking.aiScore == "7/10"
  - speaking.aiFeedback populated
  - speaking.aiRubricJson populated (JSON)
  - speaking.teacherScore == null (not yet graded by teacher)
```

### TC-DETAIL-004: getGradingDetail shows SPEAKING with null AI score (AI failed)
**Unit Test** | `TeacherAssignmentGradingServiceTest.java`

```
Setup:    AI grading failed (exception, service down)
Action:    getGradingDetail
Assertion:
  - speaking.gradingStatus == "AI_PENDING"
  - speaking.aiScore == null
  - UI should show "⚠️ AI thất bại"
```

### TC-DETAIL-005: getGradingDetail shows correct student answers
**Unit Test** | `TeacherAssignmentGradingServiceTest.java`

```
Setup:    MC question: student chose option B
Action:    getGradingDetail
Assertion:
  - question.studentAnswer contains option B
  - question.correctAnswer visible (for teacher review)
```

### TC-DETAIL-006: getGradingDetail includes audio URL for SPEAKING
**Unit Test** | `TeacherAssignmentGradingServiceTest.java`

```
Setup:    SPEAKING question: student recorded audio
Action:    getGradingDetail
Assertion:
  - question.audioUrl == cloudinary URL
  - Frontend can render audio player
```

### TC-DETAIL-007: Teacher can view another teacher's student's result (same class)
**Unit Test** | `TeacherAssignmentGradingServiceTest.java`

```
Setup:    Student in class C, enrolled teacher A
          Teacher B also enrolled in class C
Action:    Teacher B → getGradingDetail(resultId, teacherBId)
Assertion:
  - Returns detail (both teachers enrolled in same class)
```

### TC-DETAIL-008: Teacher cannot grade another teacher's student's result
**Unit Test** | `TeacherAssignmentGradingServiceTest.java`

```
Setup:    Student in class only enrolled by Teacher A
          Auth as Teacher B
Action:    gradeAssignment → expect exception
Assertion: Throws AccessDeniedException or InvalidDataException
```

---

## 3. Submit Grading Tests

### TC-GRADE-001: gradeAssignment saves per-question teacher scores
**Unit Test** | `TeacherAssignmentGradingServiceTest.java`

```
Setup:    SPEAKING question with 2 points
          AI score: 7/10, teacher overrides: 8/10
Action:    gradeAssignment(resultId, request with pointsAwarded=8, teacherId)
Assertion:
  - QuizAnswer.pointsAwarded == 8
  - QuizAnswer.teacherNote saved
  - QuizAnswer.hasPendingReview == false
  - QuizAnswer.isCorrect recalculated (8 > 0 → true)
```

### TC-GRADE-002: gradeAssignment recalculates total score
**Unit Test** | `TeacherAssignmentGradingServiceTest.java`

```
Setup:    LISTENING: 8/10 (auto)
          READING: 6/10 (auto)
          SPEAKING: teacher graded 7/10
          WRITING: teacher graded 7/10
Action:    gradeAssignment with all sectionScores
Assertion:
  - QuizResult.score == 28
  - correctRate == 28/40 * 100 = 70.00
```

### TC-GRADE-003: gradeAssignment updates passed field
**Unit Test** | `TeacherAssignmentGradingServiceTest.java`

```
Setup:    passScore=70, calculated correctRate=68
Action:    gradeAssignment
Assertion: passed == false

Setup:    passScore=70, calculated correctRate=72
Action:    gradeAssignment
Assertion: passed == true
```

### TC-GRADE-004: gradeAssignment with no passScore
**Unit Test** | `TeacherAssignmentGradingServiceTest.java`

```
Setup:    Assignment with passScore=null
Action:    gradeAssignment
Assertion: passed == null (no pass/fail determination)
```

### TC-GRADE-005: Re-grade overwrites previous scores
**Unit Test** | `TeacherAssignmentGradingServiceTest.java`

```
Setup:    Already graded SPEAKING: 6/10
Action:    gradeAssignment with new SPEAKING: 8/10
Assertion:
  - Previous score 6 overwritten
  - New score 8 saved
  - QuizResult.score recalculated
```

### TC-GRADE-006: gradeAssignment marks QuizAnswers as reviewed
**Unit Test** | `TeacherAssignmentGradingServiceTest.java`

```
Setup:    3 SPEAKING, 2 WRITING questions, all pendingAI=true
Action:    gradeAssignment
Assertion:
  - All 5 QuizAnswers: hasPendingReview == false
```

---

## 4. Grading API Endpoint Tests

### TC-API-001: GET /api/v1/teacher/assignment-results
**Integration Test** | `TeacherAssignmentGradingControllerTest.java`

```
Setup:    Auth as TEACHER, enrolled in 2 classes with submissions
Action:    GET /api/v1/teacher/assignment-results?page=0&size=20
Assertion:
  - HTTP 200 OK
  - Page of submissions
  - Per-skill status badges present
```

### TC-API-002: GET /api/v1/teacher/assignment-results/{id}
**Integration Test** | `TeacherAssignmentGradingControllerTest.java`

```
Setup:    Result exists, teacher enrolled
Action:    GET /api/v1/teacher/assignment-results/{resultId}
Assertion:
  - HTTP 200 OK
  - Full detail with 4 skill sections
```

### TC-API-003: POST /api/v1/teacher/assignment-results/{id}/grade
**Integration Test** | `TeacherAssignmentControllerTest.java`

```
Setup:    All 4 sections completed
Action:    POST /api/v1/teacher/assignment-results/{resultId}/grade
          Body: {
            sectionScores: {LISTENING: 8.0, READING: 6.5, SPEAKING: 7.0, WRITING: 7.5},
            gradingItems: [{questionId: 1, pointsAwarded: 2.0, teacherNote: "Tốt"}],
            overallNote: "Bài làm khá tốt"
          }
Assertion:
  - HTTP 200 OK
  - Response: {success: true}
  - Scores persisted to QuizResult
```

### TC-API-004: Teacher grades before AI finishes
**Integration Test** | `TeacherAssignmentGradingControllerTest.java`

```
Setup:    SPEAKING AI still pending
Action:    POST gradeAssignment with manual score for SPEAKING
Assertion:
  - HTTP 200 OK
  - Teacher's score saved
  - AI result (when arrives) is ignored (teacher already graded)
```

### TC-API-005: PATCH /api/v1/teacher/assignment-results/{id}/ai-refresh
**Integration Test** | `TeacherAssignmentGradingControllerTest.java`

```
Setup:    SPEAKING AI grading failed (aiScore=null)
Action:    PATCH /api/v1/teacher/assignment-results/{resultId}/ai-refresh
Assertion:
  - HTTP 200 OK
  - Re-triggers AI grading for failed questions
```

### TC-API-006: Unauthorized user cannot access grading
**Integration Test** | `TeacherAssignmentGradingControllerTest.java`

```
Setup:    Auth as TEACHER not enrolled in class
Action:    GET /api/v1/teacher/assignment-results/{resultId}
Assertion: HTTP 403 Forbidden
```

### TC-API-007: STUDENT cannot access grading endpoint
**Integration Test** | `TeacherAssignmentGradingControllerTest.java`

```
Setup:    Auth as STUDENT
Action:    GET /api/v1/teacher/assignment-results
Assertion: HTTP 403 Forbidden
```

---

## 5. Full Grading Flow Tests

### TC-E2E-001: Complete grading flow with AI pre-populated scores
**Integration Test** | `TeacherAssignmentGradingFlowTest.java`

```
Setup:
  - Expert created Assignment
  - Teacher opened for class
  - Student completed assignment (all 4 sections)
  - AI grading completed for SPEAKING/WRITING

Flow:
  1. Teacher sees queue
     GET /api/v1/teacher/assignment-results
     → Shows student row with status badges

  2. Teacher opens grading detail
     GET /api/v1/teacher/assignment-results/{resultId}
     → Shows 4-tab view

  3. Teacher reviews LISTENING tab
     → All 3/5 correct
     → No override needed

  4. Teacher reviews SPEAKING tab
     → AI Score: 7/10
     → AI Feedback: "Good range of vocabulary..."
     → Rubric breakdown visible
     → Teacher confirms 7/10

  5. Teacher reviews WRITING tab
     → AI Score: 6/10
     → Teacher overrides with 7/10

  6. Teacher submits grading
     POST /api/v1/teacher/assignment-results/{resultId}/grade
     → QuizResult.score = 27
     → QuizResult.passed = true (passScore=60)
     → Student sees final score

Assertion:
  - QuizResult.score populated
  - QuizResult.passed calculated
  - All 4 sections marked as GRADED
  - Student can view result
```

### TC-E2E-002: Teacher grades partially (AI still pending)
**Integration Test** | `TeacherAssignmentGradingFlowTest.java`

```
Setup:    SPEAKING AI still processing
          LISTENING/READING auto-graded, WRITING AI done

Flow:
  1. Teacher opens result
  2. Grades LISTENING/READING (auto, just confirm)
  3. Grades WRITING (AI done, confirms)
  4. SPEAKING: enters score manually (AI pending)
  5. Submit grading

Assertion:
  - HTTP 200 OK
  - Score includes SPEAKING score
  - When AI finishes → AI result stored but ignored (teacher already graded)
```

### TC-E2E-003: Teacher re-grades after initial submission
**Integration Test** | `TeacherAssignmentGradingFlowTest.java`

```
Setup:    Teacher graded SPEAKING: 6/10
          Teacher realizes student deserved 8/10

Flow:
  1. Teacher re-opens grading detail
  2. Changes SPEAKING score to 8/10
  3. Re-submits grading

Assertion:
  - Previous scores overwritten
  - QuizResult.score updated
  - QuizResult.passed recalculated
  - CorrectRate updated
```

---

## 6. Edge Cases

### TC-EDGE-001: AI grading fails for SPEAKING
**Integration Test** | `TeacherAssignmentGradingControllerTest.java`

```
Setup:    groqClient.transcribe() throws exception
Action:    AI grading attempted → exception logged
Assertion:
  - quizAnswer.aiScore == null
  - UI shows warning badge "⚠️ AI thất bại"
  - Teacher must manually input score
```

### TC-EDGE-002: Student didn't record audio (SPEAKING)
**Integration Test** | `TeacherAssignmentGradingServiceTest.java`

```
Setup:    SPEAKING question: answeredOptions = null/empty
Action:    getGradingDetail
Assertion:
  - question.studentAnswer == null or empty
  - UI shows "Thí sinh chưa ghi âm"
  - Teacher score defaults to 0
```

### TC-EDGE-003: Multiple attempts per student
**Integration Test** | `TeacherAssignmentGradingFlowTest.java`

```
Setup:    Student made 3 attempts (maxAttempts=3)
          Each attempt = separate QuizResult

Flow:
  1. Grade attempt 1
  2. Grade attempt 2
  3. Grade attempt 3

Assertion:
  - Each result graded separately
  - Queue shows all 3 attempts
  - Each has separate resultId
```

### TC-EDGE-004: Attempt to grade non-existent result
**Integration Test** | `TeacherAssignmentGradingControllerTest.java`

```
Setup:    resultId=99999 does not exist
Action:    GET /api/v1/teacher/assignment-results/99999
Assertion: HTTP 404 Not Found
```

### TC-EDGE-005: sectionScores JSON stored correctly
**Unit Test** | `TeacherAssignmentGradingServiceTest.java`

```
Setup:    gradeAssignment with sectionScores
Action:    Read back QuizResult.sectionScores from DB
Assertion:
  - sectionScores is valid JSON
  - ObjectMapper can deserialize to Map<String, BigDecimal>
  - Values match submitted scores
```
