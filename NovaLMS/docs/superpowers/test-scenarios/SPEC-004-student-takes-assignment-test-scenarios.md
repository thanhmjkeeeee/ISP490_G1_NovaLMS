# SPEC 004 — Student Takes Assignment: Test Scenarios
**Date:** 2026-04-04
**Feature:** Student đi qua 4 phần tuần tự, SPEAKING recording, auto-save, resume
**Spec:** `docs/superpowers/specs/2026-04-04-004-student-takes-assignment-design.md`
**Plan:** `docs/superpowers/plans/2026-04-04-004-student-takes-assignment-plan.md`

---

## Test Structure

```bash
src/test/java/com/example/DoAn/
├── service/impl/
│   └── StudentAssignmentServiceTest.java    # Service logic unit tests
├── controller/
│   └── StudentAssignmentApiControllerTest.java  # API endpoint tests
└── integration/
    └── StudentAssignmentFlowTest.java       # Full end-to-end flows
```

---

## 1. AssignmentSession Entity Tests

### TC-SESSION-001: AssignmentSession created with correct defaults
**Unit Test** | `StudentAssignmentServiceTest.java`

```
Setup:    studentAssignmentService.getAssignmentInfo(quizId, studentEmail)
          First access → session created
Action:    Verify session entity
Assertion:
  - status == "IN_PROGRESS"
  - currentSkillIndex == 0
  - sectionStatuses == {LISTENING: "IN_PROGRESS", READING: "LOCKED", ...}
  - sectionAnswers == "{}"
  - startedAt == now
```

### TC-SESSION-002: AssignmentSession is unique per student per quiz
**Unit Test** | `StudentAssignmentServiceTest.java`

```
Setup:    Student already has session for this quiz
Action:    getAssignmentInfo(quizId, studentEmail) again
Assertion:
  - Returns existing session (not creates new)
  - Only 1 session exists in DB
  - uniqueConstraint enforced
```

---

## 2. Assignment Info & Entry Point Tests

### TC-INFO-001: getAssignmentInfo creates new session on first access
**Unit Test** | `StudentAssignmentServiceTest.java`

```
Setup:    Quiz is PUBLISHED, isOpen=true, student enrolled
          No existing session
Action:    getAssignmentInfo(quizId, studentEmail)
Assertion:
  - New AssignmentSession created
  - session.status == "IN_PROGRESS"
  - session.currentSkillIndex == 0
  - session.startedAt == now
  - Returned DTO: canStart=true, canResume=false
```

### TC-INFO-002: getAssignmentInfo returns existing session on resume
**Unit Test** | `StudentAssignmentServiceTest.java`

```
Setup:    Existing session, status=IN_PROGRESS, currentSkillIndex=2 (SPEAKING)
Action:    getAssignmentInfo(quizId, studentEmail)
Assertion:
  - No new session created
  - canStart=false, canResume=true
  - currentSkillIndex==2
```

### TC-INFO-003: getAssignmentInfo rejects non-sequential quiz
**Unit Test** | `StudentAssignmentServiceTest.java`

```
Setup:    Quiz with isSequential=false (Lesson Quiz)
Action:    getAssignmentInfo(quizId, studentEmail) → expect exception
Assertion: Throws InvalidDataException "This is not a sequential assignment"
```

### TC-INFO-004: getAssignmentInfo rejects unpublished quiz
**Unit Test** | `StudentAssignmentServiceTest.java`

```
Setup:    Quiz with status=DRAFT
Action:    getAssignmentInfo(quizId, studentEmail) → expect exception
Assertion: Throws InvalidDataException "Assignment is not available"
```

### TC-INFO-005: getAssignmentInfo rejects closed quiz
**Unit Test** | `StudentAssignmentServiceTest.java`

```
Setup:    Quiz with isOpen=false
Action:    getAssignmentInfo(quizId, studentEmail) → expect exception
Assertion: Throws InvalidDataException "Assignment is not available"
```

### TC-INFO-006: getAssignmentInfo rejects unenrolled student
**Unit Test** | `StudentAssignmentServiceTest.java`

```
Setup:    Student NOT enrolled in the class
Action:    getAssignmentInfo(quizId, studentEmail) → expect exception
Assertion: Throws InvalidDataException "Bạn chưa đăng ký lớp học này"
```

### TC-INFO-007: getAssignmentInfo handles maxAttempts exceeded
**Unit Test** | `StudentAssignmentServiceTest.java`

```
Setup:    Student has 3 completed sessions (maxAttempts=3)
          Current attempt = 4th
Action:    getAssignmentInfo(quizId, studentEmail)
Assertion:
  - Returns DTO with attemptsExceeded=true
  - No new session created
  - Student sees "Hết lượt" page
```

### TC-INFO-008: getAssignmentInfo with completed session
**Unit Test** | `StudentAssignmentServiceTest.java`

```
Setup:    Existing session, status=COMPLETED
Action:    getAssignmentInfo(quizId, studentEmail)
Assertion:
  - canResume=false, isCompleted=true
  - Session ID returned
  - No new session created
```

### TC-INFO-009: getAssignmentInfo sets expiresAt from timeLimitMinutes
**Unit Test** | `StudentAssignmentServiceTest.java`

```
Setup:    Quiz.timeLimitMinutes = 120
Action:    getAssignmentInfo → creates session
Assertion:
  - session.expiresAt == startedAt + 120 minutes
```

### TC-INFO-010: AssignmentInfoDTO returns correct skill order
**Unit Test** | `StudentAssignmentServiceTest.java`

```
Setup:    Sequential assignment
Action:    getAssignmentInfo returns AssignmentInfoDTO
Assertion:
  - skillOrder == [LISTENING, READING, SPEAKING, WRITING]
  - timeLimitPerSkill correctly parsed from JSON
```

---

## 3. Section Access & Navigation Tests

### TC-SECTION-001: getSection returns LISTENING questions on first access
**Unit Test** | `StudentAssignmentServiceTest.java`

```
Setup:    Session currentSkillIndex=0, LISTENING section IN_PROGRESS
Action:    getSection(sessionId, "LISTENING", studentEmail)
Assertion:
  - Returns LISTENING questions
  - sectionIndex==0, currentSkillIndex==0
  - sectionStatus=="IN_PROGRESS"
  - nextSkill=="READING"
```

### TC-SECTION-002: getSection blocks future section access
**Unit Test** | `StudentAssignmentServiceTest.java`

```
Setup:    Session at LISTENING (currentSkillIndex=0)
          Student tries to access SPEAKING (index=2)
Action:    getSection(sessionId, "SPEAKING", studentEmail) → expect exception
Assertion: Throws InvalidDataException "Phần này chưa được mở"
```

### TC-SECTION-003: getSection allows previous section (review mode)
**Unit Test** | `StudentAssignmentServiceTest.java`

```
Setup:    Session at WRITING (currentSkillIndex=3), LISTENING=COMPLETED
Action:    getSection(sessionId, "LISTENING", studentEmail)
Assertion:
  - Returns LISTENING questions
  - savedAnswers populated
  - Previous section accessible for review
```

### TC-SECTION-004: getSection returns saved answers
**Unit Test** | `StudentAssignmentServiceTest.java`

```
Setup:    Session has existing answers for LISTENING
          sectionAnswers = {"LISTENING": {"Q1": "option_a", "Q2": "b"}}
Action:    getSection(sessionId, "LISTENING", studentEmail)
Assertion:
  - savedAnswers contains {"Q1": "option_a", "Q2": "b"}
  - Questions include audioUrl/imageUrl correctly
```

### TC-SECTION-005: getSection sets SPEAKING timer correctly
**Unit Test** | `StudentAssignmentServiceTest.java`

```
Setup:    timeLimitPerSkill={"SPEAKING": 2}
Action:    getSection(sessionId, "SPEAKING", studentEmail)
Assertion:
  - speakingTimerSeconds == 120
  - speakingExpiry == startedAt + 2 minutes
```

### TC-SECTION-006: getSection returns correct next/previous skill
**Unit Test** | `StudentAssignmentServiceTest.java`

```
Setup:    Session at READING (currentSkillIndex=1)
Action:    getSection(sessionId, "READING", studentEmail)
Assertion:
  - previousSkill == "LISTENING"
  - nextSkill == "SPEAKING"
```

---

## 4. Auto-Save Tests

### TC-SAVE-001: saveAnswers persists answers to sectionAnswers JSON
**Unit Test** | `StudentAssignmentServiceTest.java`

```
Setup:    Empty session, existing LISTENING answers
Action:    saveAnswers(sessionId, "LISTENING", {1: "a", 2: ["b", "c"]}, email)
Assertion:
  - session.sectionAnswers updated
  - JSON: {"LISTENING": {"1": "a", "2": ["b","c"]}}
  - sessionRepository.save() called
```

### TC-SAVE-002: saveAnswers merges with existing answers
**Unit Test** | `StudentAssignmentServiceTest.java`

```
Setup:    session.sectionAnswers = {"LISTENING": {"1": "old_value"}}
Action:    saveAnswers(sessionId, "LISTENING", {2: "new_answer"}, email)
Assertion:
  - Result: {"LISTENING": {"1": "old_value", "2": "new_answer"}}
  - Old answers preserved, new answers added
```

### TC-SAVE-003: saveAnswers for SPEAKING saves Cloudinary URLs
**Unit Test** | `StudentAssignmentServiceTest.java`

```
Setup:    Audio recording uploaded to Cloudinary
Action:    saveAnswers(sessionId, "SPEAKING", {1: "https://cloudinary.url/audio.webm"}, email)
Assertion:
  - sectionAnswers contains Cloudinary URL for SPEAKING
  - URL format verified
```

---

## 5. Section Submit Tests

### TC-SUBMIT-001: submitSection LISTENING advances to READING
**Unit Test** | `StudentAssignmentServiceTest.java`

```
Setup:    Session at LISTENING (index=0)
Action:    submitSection(sessionId, "LISTENING", answers, email)
Assertion:
  - LISTENING status → "COMPLETED"
  - READING status → "IN_PROGRESS"
  - currentSkillIndex → 1
  - sessionRepository.save() called
  - Returned: nextSkill == "READING"
```

### TC-SUBMIT-002: submitSection WRITING completes assignment
**Unit Test** | `StudentAssignmentServiceTest.java`

```
Setup:    Session at WRITING (index=3), all previous sections COMPLETED
Action:    submitSection(sessionId, "WRITING", answers, email)
Assertion:
  - session.status → "COMPLETED"
  - session.completedAt → now
  - QuizResult created
  - Fire async AI grading for SPEAKING/WRITING answers
  - Returned: completed=true, resultId
```

### TC-SUBMIT-003: submitSection auto-grades MC/FILL/MATCH questions
**Unit Test** | `StudentAssignmentServiceTest.java`

```
Setup:    LISTENING section with 5 MC questions
          Student answers: 3 correct, 2 wrong
Action:    submitSection(sessionId, "LISTENING", answers, email)
Assertion:
  - isCorrect stored per question
  - Auto-score calculated
  - AI grading NOT fired for LISTENING (only SPEAKING/WRITING)
```

### TC-SUBMIT-004: submitSection SPEAKING with audio URLs
**Unit Test** | `StudentAssignmentServiceTest.java`

```
Setup:    SPEAKING section with 2 questions
          Student uploaded 2 audio files
Action:    submitSpeakingSection(sessionId, {1: "url1", 2: "url2"}, email)
Assertion:
  - Same as submitSection but accepts audio URLs
  - Each answer validated as Cloudinary URL
  - marks pendingAiReview=true for each QuizAnswer
  - Fire async AI grading
```

### TC-SUBMIT-005: submitSection prevents duplicate submission
**Unit Test** | `StudentAssignmentServiceTest.java`

```
Setup:    LISTENING section already COMPLETED
Action:    submitSection(sessionId, "LISTENING", answers, email) → expect exception
Assertion: Throws InvalidDataException "Section already submitted"
```

### TC-SUBMIT-006: submitSection wrong student cannot submit
**Unit Test** | `StudentAssignmentServiceTest.java`

```
Setup:    Session belongs to studentA
          Auth as studentB
Action:    submitSection(sessionId, "LISTENING", answers, studentB_email) → expect exception
Assertion: Throws InvalidDataException "Access denied"
```

---

## 6. Auto-Submit (Timer Expiry) Tests

### TC-AUTO-001: autoSubmit expires all incomplete sections
**Unit Test** | `StudentAssignmentServiceTest.java`

```
Setup:    Session IN_PROGRESS
          LISTENING=COMPLETED, READING=IN_PROGRESS, SPEAKING=IN_PROGRESS, WRITING=IN_PROGRESS
          Timer hits 0 during READING
Action:    autoSubmit(sessionId, studentEmail)
Assertion:
  - READING → "EXPIRED"
  - SPEAKING → "EXPIRED"
  - WRITING → "EXPIRED"
  - session.status → "COMPLETED"
  - session.completedAt → now
  - QuizResult created with partial answers
  - Answers submitted so far are preserved
```

### TC-AUTO-002: autoSubmit with no answers filled
**Unit Test** | `StudentAssignmentServiceTest.java`

```
Setup:    Student opened assignment but answered nothing
          Timer expires immediately
Action:    autoSubmit(sessionId, studentEmail)
Assertion:
  - All sections → EXPIRED
  - session.status → "COMPLETED"
  - QuizResult created with score=0
```

### TC-AUTO-003: autoSubmit on already-completed session is no-op
**Unit Test** | `StudentAssignmentServiceTest.java`

```
Setup:    Session status=COMPLETED
Action:    autoSubmit(sessionId, email)
Assertion:
  - No changes made
  - No exception thrown
```

---

## 7. QuizResult Creation Tests

### TC-RESULT-001: QuizResult created with correct fields on completion
**Unit Test** | `StudentAssignmentServiceTest.java`

```
Setup:    Full assignment completed (all 4 sections)
Action:    completeAssignment(sessionId, email)
Assertion:
  - QuizResult created
  - quizResult.quiz == assignment quiz
  - quizResult.user == student
  - quizResult.submittedAt == now
  - quizResult.assignmentSessionId == session.id
  - quizResult.sectionScores JSON created
```

### TC-RESULT-002: QuizResult correctRate calculated correctly
**Unit Test** | `StudentAssignmentServiceTest.java`

```
Setup:    LISTENING: 4/5 correct, READING: 3/5 correct
          No SPEAKING/WRITING answers
Action:    completeAssignment
Assertion:
  - score = 7 (raw sum)
  - correctRate = 70.00 (7/10 * 100)
  - passed = true (if passScore <= 70)
```

### TC-RESULT-003: QuizResult passed field based on passScore
**Unit Test** | `StudentAssignmentServiceTest.java`

```
Setup:    Assignment with passScore=70
          Student score=60
Action:    completeAssignment
Assertion: passed == false
```

### TC-RESULT-004: AI grading fired for SPEAKING/WRITING
**Unit Test** | `StudentAssignmentServiceTest.java`

```
Setup:    Assignment with 2 SPEAKING + 1 WRITING questions
          Mock groqGradingService
Action:    completeAssignment(sessionId, email)
Assertion:
  - groqGradingService.fireAndForgetForQuizAnswer() called
    for EACH SPEAKING question
  - groqGradingService.fireAndForgetForQuizAnswer() called
    for EACH WRITING question
  - Total: 3 calls
```

### TC-RESULT-005: QuizResult NOT created for in-progress session
**Unit Test** | `StudentAssignmentServiceTest.java`

```
Setup:    Session status=IN_PROGRESS (not completed)
Action:    getAssignmentInfo → returns DTO (no result yet)
Assertion: No QuizResult created yet
```

---

## 8. API Controller Tests

### TC-API-001: GET /api/v1/student/assignment/{quizId}
**Integration Test** | `StudentAssignmentApiControllerTest.java`

```
Setup:    Auth as STUDENT, enrolled, no existing session
Action:    GET /api/v1/student/assignment/{quizId}
Assertion:
  - HTTP 200 OK
  - Returns AssignmentInfoDTO
  - canStart == true
```

### TC-API-002: GET /api/v1/student/assignment/session/{id}/section/{skill}
**Integration Test** | `StudentAssignmentApiControllerTest.java`

```
Setup:    Session at LISTENING, enrolled
Action:    GET /api/v1/student/assignment/session/{sessionId}/section/LISTENING
Assertion:
  - HTTP 200 OK
  - Returns AssignmentSectionDTO with LISTENING questions
  - savedAnswers included
```

### TC-API-003: PATCH auto-save endpoint
**Integration Test** | `StudentAssignmentApiControllerTest.java`

```
Setup:    Session with LISTENING in progress
Action:    PATCH /api/v1/student/assignment/session/{id}/section/LISTENING
          Body: {answers: {"1": "a", "2": "b"}}
Assertion:
  - HTTP 200 OK
  - Answers saved to session
```

### TC-API-004: POST submit LISTENING section
**Integration Test** | `StudentAssignmentApiControllerTest.java`

```
Setup:    Session at LISTENING
Action:    POST /api/v1/student/assignment/session/{id}/section/LISTENING/submit
          Body: {answers: {"1": "a", "2": "b"}}
Assertion:
  - HTTP 200 OK
  - sectionStatus → "COMPLETED"
  - nextSkill returned
```

### TC-API-005: POST submit SPEAKING with audio URLs
**Integration Test** | `StudentAssignmentApiControllerTest.java`

```
Setup:    Session at SPEAKING
Action:    POST /api/v1/student/assignment/session/{id}/section/SPEAKING/submit
          Body: {"1": "https://cloudinary.url/audio1.webm", "2": "url2"}
Assertion:
  - HTTP 200 OK
  - Session advances to WRITING
  - AI grading triggered
```

### TC-API-006: POST complete assignment
**Integration Test** | `StudentAssignmentApiControllerTest.java`

```
Setup:    All 4 sections COMPLETED
Action:    POST /api/v1/student/assignment/session/{id}/complete
Assertion:
  - HTTP 200 OK
  - Returns resultId
  - Session status → COMPLETED
```

### TC-API-007: POST auto-submit on timer expiry
**Integration Test** | `StudentAssignmentApiControllerTest.java`

```
Setup:    Session IN_PROGRESS, time expired
Action:    POST /api/v1/student/assignment/session/{id}/auto-submit
Assertion:
  - HTTP 200 OK
  - All incomplete sections → EXPIRED
  - QuizResult created
```

### TC-API-008: Unauthorized student cannot access assignment
**Integration Test** | `StudentAssignmentApiControllerTest.java`

```
Setup:    Student NOT enrolled in class
Action:    GET /api/v1/student/assignment/{quizId}
Assertion: HTTP 403 Forbidden
```

### TC-API-009: Attempts exceeded returns 400
**Integration Test** | `StudentAssignmentApiControllerTest.java`

```
Setup:    Student used all maxAttempts
Action:    GET /api/v1/student/assignment/{quizId}
Assertion:
  - HTTP 400 Bad Request
  - Error message "Bạn đã hết lượt làm bài"
```

---

## 9. Full End-to-End Flows

### TC-E2E-001: Student completes full assignment
**Integration Test** | `StudentAssignmentFlowTest.java`

```
Setup:    Expert created Assignment, Teacher opened for class
          Student enrolled in class
          5 LISTENING, 5 READING, 2 SPEAKING, 2 WRITING questions
          timeLimitPerSkill: {SPEAKING: 2, WRITING: 30}

Flow:
  1. GET /api/v1/student/assignment/{quizId}
     → Creates session, returns canStart=true

  2. GET /student/assignment/session/{sessionId}/section/LISTENING
     → Shows 5 LISTENING questions
     → Student answers 4/5

  3. PATCH /session/{id}/section/LISTENING (auto-save)
     → Answers saved

  4. POST /session/{id}/section/LISTENING/submit
     → LISTENING COMPLETED, READING unlocked

  5. GET /student/assignment/session/{sessionId}/section/READING
     → Shows 5 READING questions
     → Student answers all 5

  6. POST /session/{id}/section/READING/submit
     → READING COMPLETED, SPEAKING unlocked

  7. GET /student/assignment/session/{sessionId}/section/SPEAKING
     → Shows 2 SPEAKING prompts
     → Student records audio 1 (upload to Cloudinary)
     → Student records audio 2

  8. POST /session/{id}/section/SPEAKING/submit
     → SPEAKING COMPLETED, WRITING unlocked
     → AI grading fires for both audio answers

  9. GET /student/assignment/session/{sessionId}/section/WRITING
     → Shows 2 WRITING prompts

  10. POST /session/{id}/section/WRITING/submit
      → WRITING COMPLETED

  11. POST /session/{id}/complete
      → Session COMPLETED
      → QuizResult created
      → AI grading fires for WRITING answers

  12. GET /student/quiz/result/{resultId}
      → Shows LISTENING/READING scores (auto-graded)
      → Shows SPEAKING/WRITING: "⏳ Đang chấm..."

Assertion:
  - All 4 sections COMPLETED
  - Session status == COMPLETED
  - QuizResult exists with score
  - AI grading triggered (6 total: 2 SPEAKING + 2 WRITING)
  - LISTENING/READING auto-graded
```

### TC-E2E-002: Student resumes mid-assignment
**Integration Test** | `StudentAssignmentFlowTest.java`

```
Setup:    Student completed LISTENING + READING, then closed browser

Flow:
  1. GET /api/v1/student/assignment/{quizId}
     → canResume=true, currentSkillIndex=2 (SPEAKING)

  2. GET /student/assignment/session/{sessionId}/section/SPEAKING
     → savedAnswers empty (SPEAKING not started)
     → Redirects to SPEAKING section

  3. Complete SPEAKING + WRITING

Assertion:
  - LISTENING/READING answers preserved from before
  - No new session created
  - All sections completed successfully
```

### TC-E2E-003: Timer expires during SPEAKING
**Integration Test** | `StudentAssignmentFlowTest.java`

```
Setup:    Student at SPEAKING, recorded 1 of 2 answers
          Speaking timer = 2 minutes

Flow:
  1. Student records audio for question 1
  2. Student does NOT submit, timer expires
  3. System auto-submits SPEAKING with only 1 answer

Assertion:
  - SPEAKING section: question 1 answered, question 2 = null/0
  - Session advances to WRITING
  - Notification or result note: question 2 unanswered
```

### TC-E2E-004: Student exceeds maxAttempts
**Integration Test** | `StudentAssignmentFlowTest.java`

```
Setup:    maxAttempts=2
          Student completed 2 attempts

Flow:
  1. First attempt → completed
  2. Second attempt → completed
  3. Third attempt attempt

Assertion:
  - HTTP 400 or error page
  - "Bạn đã hết lượt làm bài"
  - No new session created
```

---

## 10. Edge Cases

### TC-EDGE-001: Session expires_at honored at quiz level
**Unit Test** | `StudentAssignmentServiceTest.java`

```
Setup:    Quiz.timeLimitMinutes = 120
          Session expiresAt = startedAt + 120 minutes
          Timer expires
Action:    autoSubmit(sessionId, email)
Assertion: Auto-submit triggered when overall timer hits 0
```

### TC-EDGE-002: Re-record SPEAKING replaces previous recording
**Unit Test** | `StudentAssignmentServiceTest.java`

```
Setup:    SPEAKING question 1: existing URL in sectionAnswers
Action:    Student re-records → new URL saved
Assertion: New URL overwrites old URL in sectionAnswers
```

### TC-EDGE-003: Student closes browser during SPEAKING recording
**Unit Test** | `StudentAssignmentServiceTest.java`

```
Setup:    Student recording, closes browser
Action:    getSection(sessionId, "SPEAKING") on resume
Assertion:
  - Previous recording NOT saved (not uploaded)
  - Recording starts fresh
  - Timer NOT paused (browser closed)
```

### TC-EDGE-004: sectionStatuses JSON correctly tracks all states
**Unit Test** | `StudentAssignmentServiceTest.java`

```
Setup:    Session at READING (LISTENING=COMPLETED, READING=IN_PROGRESS)
Action:    After submit LISTENING, read sectionStatuses
Assertion:
  - sectionStatuses["LISTENING"] == "COMPLETED"
  - sectionStatuses["READING"] == "IN_PROGRESS"
  - sectionStatuses["SPEAKING"] == "LOCKED"
  - sectionStatuses["WRITING"] == "LOCKED"
```

---

## Test Fixtures

```java
private User createStudentUser() { ... }
private Quiz createSequentialAssignment(User expert) { ... }
private Quiz createNonSequentialQuiz() { ... }
private AssignmentSession createInProgressSession(Quiz quiz, User student) { ... }
private AssignmentSession createCompletedSession(...) { ... }
private Map<String, List<Question>> createAssignmentQuestions(Quiz quiz) { ... }
// Returns: {LISTENING: [...], READING: [...], SPEAKING: [...], WRITING: [...]}
private Clazz createClassWithStudent(User student, Quiz quiz) { ... }
```
