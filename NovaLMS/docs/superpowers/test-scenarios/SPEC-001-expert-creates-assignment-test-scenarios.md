# SPEC 001 — Expert Creates Assignment: Test Scenarios
**Date:** 2026-04-04
**Feature:** Expert tạo Bài Kiểm Tra Lớn (Course/Module Assignment)
**Spec:** `docs/superpowers/specs/2026-04-04-001-expert-creates-assignment-design.md`
**Plan:** `docs/superpowers/plans/2026-04-04-001-expert-creates-assignment-plan.md`

---

## Test Structure

```bash
src/test/java/com/example/DoAn/
├── service/impl/
│   └── ExpertAssignmentServiceTest.java        # Unit tests
├── controller/
│   └── ExpertAssignmentControllerTest.java      # Integration tests
└── integration/
    └── FullAssignmentFlowTest.java             # End-to-end tests
```

---

## 1. Data Model & Entity Tests

### TC-MODEL-001: Quiz entity has required sequential fields
**Unit Test** | `ExpertAssignmentServiceTest.java`

```
Setup:    Load Quiz entity via ReflectionTestUtils or JPA repository
Action:    Verify fields exist:
          - isSequential (Boolean)
          - skillOrder (String, JSON column)
          - timeLimitPerSkill (String, JSON column)
Assertion: All three fields present, isSequential defaults to false
```

### TC-MODEL-002: QuizQuestion entity has skill field
**Unit Test** | `ExpertAssignmentServiceTest.java`

```
Setup:    Load QuizQuestion entity
Action:    Check for skill field (VARCHAR 20)
Assertion: Field exists
```

### TC-MODEL-003: Question entity has reviewer fields
**Unit Test** | `ExpertAssignmentServiceTest.java`

```
Setup:    Load Question entity
Action:    Check for reviewerId, reviewedAt, reviewNote fields
Assertion: All three fields present
```

---

## 2. Service Layer Tests (`ExpertAssignmentServiceImpl`)

### TC-SVC-001: Create COURSE_ASSIGNMENT sets correct fields
**Unit Test** | `ExpertAssignmentServiceTest.java`

```
Setup:    Mock QuizRepository, UserRepository
          User with role EXPERT
Action:    createAssignment(dto with quizCategory=COURSE_ASSIGNMENT, title="Midterm")
Assertion:
  - quiz.status == "DRAFT"
  - quiz.isSequential == true
  - quiz.quizCategory == "COURSE_ASSIGNMENT"
  - quiz.skillOrder == '["LISTENING","READING","SPEAKING","WRITING"]'
  - quiz.isOpen == false
  - quizRepository.save() called once
```

### TC-SVC-002: Create MODULE_ASSIGNMENT sets correct fields
**Unit Test** | `ExpertAssignmentServiceTest.java`

```
Setup:    Mock QuizRepository, UserRepository
          dto with quizCategory=MODULE_ASSIGNMENT
Action:    createAssignment(dto)
Assertion:
  - quiz.quizCategory == "MODULE_ASSIGNMENT"
  - quiz.isSequential == true
```

### TC-SVC-003: Non-EXPERT user cannot create assignment
**Unit Test** | `ExpertAssignmentServiceTest.java`

```
Setup:    Mock UserRepository → return user with role=TEACHER
Action:    createAssignment(dto, teacherEmail) → expect exception
Assertion: Throws InvalidDataException "Only experts can create assignments"
```

### TC-SVC-004: Create assignment with invalid category
**Unit Test** | `ExpertAssignmentServiceTest.java`

```
Setup:    dto with quizCategory="LESSON_QUIZ"
Action:    createAssignment(dto) → expect exception
Assertion: Throws InvalidDataException "Invalid category for assignment"
```

### TC-SVC-005: Create assignment with per-skill timers
**Unit Test** | `ExpertAssignmentServiceTest.java`

```
Setup:    dto with timeLimitPerSkill={"SPEAKING": 2, "WRITING": 30}
Action:    createAssignment(dto)
Assertion:
  - quiz.timeLimitPerSkill contains SPEAKING:2
  - quiz.timeLimitPerSkill contains WRITING:30
```

### TC-SVC-006: Get skill summaries returns correct counts
**Unit Test** | `ExpertAssignmentServiceTest.java`

```
Setup:    Quiz with 3 LISTENING questions, 2 READING questions, 0 SPEAKING, 1 WRITING
          Mock quizQuestionRepository.countByQuizIdAndSkill()
Action:    getSkillSummaries(quizId)
Assertion:
  - LISTENING: count=3, status=READY
  - READING: count=2, status=READY
  - SPEAKING: count=0, status=DRAFT
  - WRITING: count=1, status=READY
```

### TC-SVC-007: Add questions to skill section
**Unit Test** | `ExpertAssignmentServiceTest.java`

```
Setup:    Mock quizRepository, questionRepository, quizQuestionRepository
          Quiz (isSequential=true), 3 PUBLISHED questions
Action:    addQuestionsToSection(quizId, dto with skill=LISTENING, questionIds=[1,2,3])
Assertion:
  - quizQuestionRepository.save() called 3 times
  - Each saved QuizQuestion has skill=LISTENING
  - orderIndex assigned sequentially
```

### TC-SVC-008: Add questions to non-sequential quiz fails
**Unit Test** | `ExpertAssignmentServiceTest.java`

```
Setup:    Mock quizRepository → quiz with isSequential=false
Action:    addQuestionsToSection(nonSequentialQuizId, dto)
Assertion: Throws InvalidDataException "does not support section-based question addition"
```

### TC-SVC-009: Add invalid skill to section fails
**Unit Test** | `ExpertAssignmentServiceTest.java`

```
Setup:    dto with skill="INVALID_SKILL"
Action:    addQuestionsToSection(quizId, dto)
Assertion: Throws InvalidDataException "Invalid skill: INVALID_SKILL"
```

### TC-SVC-010: Publish assignment with all 4 skills has questions
**Unit Test** | `ExpertAssignmentServiceTest.java`

```
Setup:    Quiz DRAFT with 1 question per skill (LISTENING/READING/SPEAKING/WRITING)
          Mock getSkillSummaries → all counts >= 1
Action:    publishAssignment(quizId)
Assertion:
  - quiz.status == "PUBLISHED"
  - quiz.isOpen == false
  - quizRepository.save() called
```

### TC-SVC-011: Publish assignment missing SPEAKING questions fails
**Unit Test** | `ExpertAssignmentServiceTest.java`

```
Setup:    Quiz DRAFT missing WRITING section (count=0)
          Mock getSkillSummaries → WRITING count=0
Action:    publishAssignment(quizId) → expect exception
Assertion: Throws InvalidDataException containing "Missing questions for skills: WRITING"
```

### TC-SVC-012: Publish non-DRAFT assignment fails
**Unit Test** | `ExpertAssignmentServiceTest.java`

```
Setup:    Quiz with status=PUBLISHED
Action:    publishAssignment(quizId)
Assertion: Throws InvalidDataException "Only DRAFT quizzes can be published"
```

### TC-SVC-013: Get assignments returns only expert's assignments
**Unit Test** | `ExpertAssignmentServiceTest.java`

```
Setup:    3 COURSE_ASSIGNMENT, 2 LESSON_QUIZ in DB
          expertEmail filters correctly
Action:    getAssignments(expertEmail)
Assertion: Returns only COURSE_ASSIGNMENT + MODULE_ASSIGNMENT (excludes LESSON_QUIZ)
          All returned quizzes belong to this expert
```

### TC-SVC-014: Get preview returns missing skills
**Unit Test** | `ExpertAssignmentServiceTest.java`

```
Setup:    Assignment missing LISTENING and SPEAKING sections
Action:    getPreview(quizId)
Assertion:
  - previewDTO.missingSkills contains ["LISTENING", "SPEAKING"]
  - previewDTO.canPublish == false
  - previewDTO.totalQuestions reflects actual count
```

---

## 3. Controller Integration Tests (`ExpertAssignmentController`)

### TC-API-001: POST /api/v1/expert/assignments creates assignment
**Integration Test** | `ExpertAssignmentControllerTest.java`

```
Setup:    Auth as EXPERT user
          Request body: {title, quizCategory=COURSE_ASSIGNMENT, ...}
Action:    POST /api/v1/expert/assignments
Assertion:
  - HTTP 201 Created
  - Response body: {success:true, data: quizId}
  - Quiz exists in DB with correct fields
```

### TC-API-002: POST without EXPERT role returns 403
**Integration Test** | `ExpertAssignmentControllerTest.java`

```
Setup:    Auth as TEACHER user
Action:    POST /api/v1/expert/assignments
Assertion: HTTP 403 Forbidden
```

### TC-API-003: GET /api/v1/expert/assignments/{id} returns assignment
**Integration Test** | `ExpertAssignmentControllerTest.java`

```
Setup:    Create assignment in DB
Action:    GET /api/v1/expert/assignments/{quizId}
Assertion:
  - HTTP 200 OK
  - Response contains quizId, title, quizCategory, isSequential=true
```

### TC-API-004: GET /api/v1/expert/assignments/{id}/skills returns summary
**Integration Test** | `ExpertAssignmentControllerTest.java`

```
Setup:    Assignment with 5 LISTENING, 3 READING questions
Action:    GET /api/v1/expert/assignments/{quizId}/skills
Assertion:
  - HTTP 200 OK
  - Returns Map with 4 keys (LISTENING, READING, SPEAKING, WRITING)
  - LISTENING.count = 5
  - READING.count = 3
```

### TC-API-005: POST /api/v1/expert/assignments/{id}/questions adds questions
**Integration Test** | `ExpertAssignmentControllerTest.java`

```
Setup:    Create 3 PUBLISHED questions in DB
          Create assignment in DB
Action:    POST /api/v1/expert/assignments/{quizId}/questions
          Body: {questionIds: [1,2,3], skill: "LISTENING", itemType: "SINGLE"}
Assertion:
  - HTTP 200 OK
  - 3 QuizQuestion records created in DB
  - Each QuizQuestion.skill == "LISTENING"
```

### TC-API-006: Add PUBLISHED question to assignment succeeds
**Integration Test** | `ExpertAssignmentControllerTest.java`

```
Setup:    Question with status=PUBLISHED
Action:    addQuestionsToSection with that questionId
Assertion: HTTP 200 OK, question added
```

### TC-API-007: Add DRAFT question to assignment fails
**Integration Test** | `ExpertAssignmentControllerTest.java`

```
Setup:    Question with status=DRAFT
Action:    addQuestionsToSection
Assertion: HTTP 400 Bad Request or service throws InvalidDataException
```

### TC-API-008: DELETE /api/v1/expert/assignments/{id}/questions/{qid} removes question
**Integration Test** | `ExpertAssignmentControllerTest.java`

```
Setup:    Assignment with 5 questions
Action:    DELETE /api/v1/expert/assignments/{quizId}/questions/{questionId}
Assertion:
  - HTTP 200 OK
  - QuizQuestion count reduced by 1
  - Question entity NOT deleted (reusable)
```

### TC-API-009: GET /api/v1/expert/assignments/{id}/preview returns full preview
**Integration Test** | `ExpertAssignmentControllerTest.java`

```
Setup:    Assignment with all 4 skill sections populated
Action:    GET /api/v1/expert/assignments/{quizId}/preview
Assertion:
  - HTTP 200 OK
  - Contains title, sections, totalQuestions, timeLimitsPerSkill
  - canPublish == true (all skills present)
```

### TC-API-010: PATCH /api/v1/expert/assignments/{id}/publish publishes successfully
**Integration Test** | `ExpertAssignmentControllerTest.java`

```
Setup:    Assignment in DRAFT with all 4 skills having >= 1 question
Action:    PATCH /api/v1/expert/assignments/{quizId}/publish
Assertion:
  - HTTP 200 OK
  - Quiz status == "PUBLISHED"
  - Quiz isOpen == false
```

### TC-API-011: PATCH publish missing skill returns 400
**Integration Test** | `ExpertAssignmentControllerTest.java`

```
Setup:    Assignment with missing SPEAKING questions
Action:    PATCH /api/v1/expert/assignments/{quizId}/publish
Assertion:
  - HTTP 400 Bad Request
  - Response contains "Missing questions for skills: SPEAKING"
```

### TC-API-012: PATCH /api/v1/expert/assignments/{id}/status changes status
**Integration Test** | `ExpertAssignmentControllerTest.java`

```
Setup:    Published assignment
Action:    PATCH /api/v1/expert/assignments/{quizId}/status
          Body: {status: "ARCHIVED"}
Assertion:
  - HTTP 200 OK
  - Quiz status == "ARCHIVED"
```

### TC-API-013: Unauthorized user cannot access assignments
**Integration Test** | `ExpertAssignmentControllerTest.java`

```
Setup:    No authentication
Action:    GET /api/v1/expert/assignments
Assertion: HTTP 401 Unauthorized
```

---

## 4. Wizard Flow Tests (Step 1 → Publish)

### TC-WIZ-001: Full 6-step assignment creation flow
**Integration Test** | `FullAssignmentFlowTest.java`

```
Setup:    Expert authenticated, course exists in DB
Flow:
  Step 1: POST /api/v1/expert/assignments
          {title: "Final Exam Q1/2026", quizCategory: "COURSE_ASSIGNMENT",
           courseId: X, passScore: 70, maxAttempts: 2,
           timeLimitPerSkill: {SPEAKING: 2, WRITING: 30}}
          → HTTP 201, quizId returned

  Step 2: POST /api/v1/expert/assignments/{quizId}/questions
          skill=LISTENING, 5 questions
          → HTTP 200

  Step 3: POST /api/v1/expert/assignments/{quizId}/questions
          skill=READING, 5 questions
          → HTTP 200

  Step 4: POST /api/v1/expert/assignments/{quizId}/questions
          skill=SPEAKING, 2 questions
          → HTTP 200

  Step 5: POST /api/v1/expert/assignments/{quizId}/questions
          skill=WRITING, 2 questions
          → HTTP 200

  Step 6: GET /api/v1/expert/assignments/{quizId}/preview
          → canPublish == true

  Publish: PATCH /api/v1/expert/assignments/{quizId}/publish
          → HTTP 200, status == "PUBLISHED", isOpen == false

Assertion:
  - Quiz has exactly 14 questions (5+5+2+2)
  - Each skill has correct question count
  - timeLimitPerSkill stored correctly
  - isSequential == true
  - status == "PUBLISHED"
```

### TC-WIZ-002: Cannot skip skill in assignment
**Integration Test** | `FullAssignmentFlowTest.java`

```
Setup:    Expert creates assignment
Flow:     Add LISTENING → Add READING → Publish
          (skip SPEAKING and WRITING)
Assertion: PATCH /publish returns 400 "Missing questions for skills: SPEAKING, WRITING"
```

### TC-WIZ-003: Re-edit published assignment not allowed for publishing
**Integration Test** | `FullAssignmentFlowTest.java`

```
Setup:    Assignment PUBLISHED
Flow:     Try to add more questions
Action:   POST /api/v1/expert/assignments/{quizId}/questions
Assertion:
  - Should allow adding questions to DRAFT only
  - OR allow but note that already-submitted students won't see new questions
  - Business decision: clarify in spec (no spec found for this edge case)
```

---

## 5. API Validation Tests

### TC-VAL-001: Create assignment without title
**Integration Test** | `ExpertAssignmentControllerTest.java`

```
Setup:    Request body without title field
Action:    POST /api/v1/expert/assignments
Assertion:
  - HTTP 400 Bad Request
  - OR @Valid annotation triggers validation error
```

### TC-VAL-002: Create COURSE_ASSIGNMENT without courseId
**Integration Test** | `ExpertAssignmentControllerTest.java`

```
Setup:    Request body with quizCategory=COURSE_ASSIGNMENT, courseId=null
Action:    POST /api/v1/expert/assignments
Assertion: HTTP 400 or service validation error
```

### TC-VAL-003: Create assignment with passScore > 100
**Integration Test** | `ExpertAssignmentControllerTest.java`

```
Setup:    passScore = 150
Action:    POST /api/v1/expert/assignments
Assertion: HTTP 400 or service throws InvalidDataException
```

### TC-VAL-004: Add duplicate question to same skill section
**Integration Test** | `ExpertAssignmentControllerTest.java`

```
Setup:    Question ID 5 already added to LISTENING section
Action:    Add question ID 5 again to LISTENING
Assertion:
  - Service should skip duplicate silently
  - OR return 400 "Question already added"
  - Behavior: spec says "skip duplicates" — verify service handles this
```

---

## 6. Edge Cases

### TC-EDGE-001: Assignment with 0 LISTENING questions, rest filled
**Integration Test** | `ExpertAssignmentControllerTest.java`

```
Setup:    0 LISTENING, 5 READING, 3 SPEAKING, 2 WRITING
Action:    publishAssignment
Assertion: HTTP 400 with "Missing questions for skills: LISTENING"
```

### TC-EDGE-002: Attempt to create assignment with no questions (empty quiz)
**Integration Test** | `ExpertAssignmentControllerTest.java`

```
Setup:    Assignment created but no questions added
Action:    publishAssignment
Assertion: HTTP 400 "Missing questions for skills: LISTENING, READING, SPEAKING, WRITING"
```

### TC-EDGE-003: Expert cannot access another expert's assignment
**Integration Test** | `ExpertAssignmentControllerTest.java`

```
Setup:    Assignment created by expert1
Action:    Auth as expert2 → GET /api/v1/expert/assignments/{expert1QuizId}
Assertion:
  - Either 404 (not found) or 403 (access denied)
  - Verify ownership check in service layer
```

### TC-EDGE-004: timeLimitPerSkill stores as valid JSON
**Unit Test** | `ExpertAssignmentServiceTest.java`

```
Setup:    timeLimitPerSkill = {"LISTENING": 30, "READING": 45, "SPEAKING": 2, "WRITING": 30}
Action:    createAssignment → read back quiz.timeLimitPerSkill
Assertion:
  - Field is valid JSON string
  - Parses back to Map with correct values
  - ObjectMapper can deserialize it
```

### TC-EDGE-005: skillOrder is always 4 skills in fixed order
**Unit Test** | `ExpertAssignmentServiceTest.java`

```
Setup:    Any assignment creation
Action:    Verify quiz.skillOrder
Assertion: skillOrder == ["LISTENING","READING","SPEAKING","WRITING"]
          Length == 4, order is exact
```

---

## 7. Performance & Concurrency

### TC-PERF-001: Add 100 questions to a skill section
**Integration Test** | `ExpertAssignmentControllerTest.java`

```
Setup:    Create 100 PUBLISHED questions
Action:    addQuestionsToSection(quizId, dto with 100 questionIds)
Assertion:
  - Completes within acceptable time (< 5s)
  - All 100 QuizQuestion records created
  - Database consistency maintained
```

### TC-PERF-002: getSkillSummaries on large quiz (500 questions)
**Integration Test** | `ExpertAssignmentControllerTest.java`

```
Setup:    Assignment with 500 questions distributed across skills
Action:    getSkillSummaries(quizId)
Assertion:
  - Query completes within acceptable time
  - Correct counts returned per skill
```

---

## Test Data Setup Helpers

```java
// Test fixtures for ExpertAssignmentServiceTest
private User createExpertUser() { ... }
private User createTeacherUser() { ... }
private Quiz createDraftAssignment(User expert) { ... }
private List<Question> createPublishedQuestions(int count, String skill) { ... }
private QuizRequestDTO createValidAssignmentDTO() { ... }
```

---

## Coverage Target

| Layer | Target |
|---|---|
| Service logic | 90%+ |
| Controller endpoints | 100% |
| Validation rules | 100% |
| Edge cases | 80% |
| Integration flows | 100% |
