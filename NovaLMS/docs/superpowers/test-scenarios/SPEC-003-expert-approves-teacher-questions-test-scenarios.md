# SPEC 003 — Expert Approves Teacher Questions: Test Scenarios
**Date:** 2026-04-04
**Feature:** Expert duyệt câu hỏi Teacher (PENDING_REVIEW → APPROVED/REJECTED) + Notification
**Spec:** `docs/superpowers/specs/2026-04-04-003-expert-approves-teacher-questions-design.md`
**Plan:** `docs/superpowers/plans/2026-04-04-003-expert-approves-questions-plan.md`

---

## Test Structure

```bash
src/test/java/com/example/DoAn/
├── service/
│   ├── NotificationServiceTest.java          # Notification entity + service
│   └── ExpertReviewServiceTest.java          # Approval/rejection logic
├── controller/
│   └── ExpertReviewControllerTest.java       # API endpoints
└── integration/
    └── ExpertApprovalFlowTest.java          # End-to-end approval flow
```

---

## 1. Notification Entity & Service Tests

### TC-NOTIF-001: Notification saved with correct fields
**Unit Test** | `NotificationServiceTest.java`

```
Setup:    Call notificationService.send(userId=5, type="QUESTION_APPROVED",
          title="...", message="...", link="/teacher/my-questions")
Action:    Verify notificationRepository.save() called
Assertion:
  - notification.userId == 5
  - notification.type == "QUESTION_APPROVED"
  - notification.isRead == false
  - notification.createdAt == now
  - link matches
```

### TC-NOTIF-002: sendQuestionApproved creates correct notification
**Unit Test** | `NotificationServiceTest.java`

```
Setup:    Mock notificationRepository
Action:    notificationService.sendQuestionApproved(teacherId=10, "What is...", quizId=5)
Assertion:
  - notification.type == "QUESTION_APPROVED"
  - notification.title == "Câu hỏi của bạn đã được phê duyệt"
  - notification.message contains "What is..."
  - notification.link == "/teacher/my-questions"
```

### TC-NOTIF-003: sendQuestionRejected creates correct notification with reviewNote
**Unit Test** | `NotificationServiceTest.java`

```
Setup:    reviewNote = "Câu hỏi không phù hợp với tiêu chuẩn"
Action:    notificationService.sendQuestionRejected(teacherId=10, "What is...", reviewNote)
Assertion:
  - notification.type == "QUESTION_REJECTED"
  - notification.message contains "What is..."
  - notification.message contains reviewNote
  - notification.link == "/teacher/my-questions"
```

### TC-NOTIF-004: sendQuestionRejected without reviewNote
**Unit Test** | `NotificationServiceTest.java`

```
Setup:    reviewNote = null
Action:    notificationService.sendQuestionRejected(teacherId=10, "What is...", null)
Assertion:
  - notification.message still generated (content truncated to 80 chars)
  - No crash, no null pointer
```

### TC-NOTIF-005: getInbox returns paginated notifications
**Unit Test** | `NotificationServiceTest.java`

```
Setup:    User 5 has 30 notifications (10 unread, 20 read)
          Page request: page=0, size=10
Action:    notificationService.getInbox(5L, pageable)
Assertion:
  - Returns Page with 10 items
  - Items sorted by createdAt DESC
  - Only userId=5's notifications
```

### TC-NOTIF-006: getUnreadCount returns correct count
**Unit Test** | `NotificationServiceTest.java`

```
Setup:    User 5 has 7 unread notifications
Action:    notificationService.getUnreadCount(5L)
Assertion: Returns 7
```

### TC-NOTIF-007: getTopUnread returns max 5
**Unit Test** | `NotificationServiceTest.java`

```
Setup:    User has 12 unread notifications
Action:    notificationService.getTopUnread(5L)
Assertion:
  - Returns exactly 5 notifications
  - All unread
  - Sorted by createdAt DESC
```

### TC-NOTIF-008: markAsRead marks single notification
**Unit Test** | `NotificationServiceTest.java`

```
Setup:    Notification id=99 exists
Action:    notificationService.markAsRead(99L)
Assertion:
  - notificationRepository.markAsRead(99) called
  - isRead == true for notification 99
```

### TC-NOTIF-009: markAllAsRead marks all user notifications as read
**Unit Test** | `NotificationServiceTest.java`

```
Setup:    User 5 has 15 unread notifications
Action:    notificationService.markAllAsRead(5L)
Assertion:
  - notificationRepository.markAllAsRead(5) called
  - getUnreadCount(5) == 0
```

---

## 2. Expert Review Service Tests

### TC-REV-001: Approve question sets PUBLISHED status
**Unit Test** | `ExpertReviewServiceTest.java`

```
Setup:    Question: status=PENDING_REVIEW, source=TEACHER_PRIVATE, reviewerId=null
          Expert user
Action:    expertQuestionService.approveQuestion(questionId, expertEmail, "Good work!")
Assertion:
  - question.status == "PUBLISHED"
  - question.reviewerId == expertId
  - question.reviewedAt == now
  - question.reviewNote == "Good work!"
  - questionRepository.save() called
```

### TC-REV-002: Approve fires notification to teacher
**Unit Test** | `ExpertReviewServiceTest.java`

```
Setup:    Mock notificationService, question with userId=teacherId
Action:    expertQuestionService.approveQuestion(id, expertEmail, null)
Assertion: notificationService.sendQuestionApproved(teacherId, ...) called once
```

### TC-REV-003: Approve already PUBLISHED question fails
**Unit Test** | `ExpertReviewServiceTest.java`

```
Setup:    Question: status=PUBLISHED
Action:    approveQuestion → expect exception
Assertion: Throws InvalidDataException "Question is not pending review"
```

### TC-REV-004: Approve non-TEACHER_PRIVATE question fails
**Unit Test** | `ExpertReviewServiceTest.java`

```
Setup:    Question: status=PENDING_REVIEW, source=EXPERT_BANK
Action:    approveQuestion → expect exception
Assertion: Throws InvalidDataException "Only teacher-private questions can be approved here"
```

### TC-REV-005: Reject question sets status to DRAFT
**Unit Test** | `ExpertReviewServiceTest.java`

```
Setup:    Question: status=PENDING_REVIEW
Action:    rejectQuestion(id, expertEmail, "Wrong CEFR level", delete=false)
Assertion:
  - question.status == "DRAFT"
  - question.reviewerId == expertId
  - question.reviewNote == "Wrong CEFR level"
  - question NOT deleted
  - notificationService.sendQuestionRejected() called
```

### TC-REV-006: Reject with delete=true removes question
**Unit Test** | `ExpertReviewServiceTest.java`

```
Setup:    Question id=55
Action:    rejectQuestion(id, expertEmail, "Spam content", delete=true)
Assertion:
  - questionRepository.delete(question) called
  - Question no longer exists in DB
  - Notification sent to teacher
```

### TC-REV-007: Reject fires notification with review note
**Unit Test** | `ExpertReviewServiceTest.java`

```
Setup:    reviewNote = "Content violates guidelines"
Action:    rejectQuestion(...)
Assertion: notificationService.sendQuestionRejected(teacherId, content, "Content violates guidelines")
          called once
```

### TC-REV-008: getPendingQuestionsWithFilters by skill
**Unit Test** | `ExpertReviewServiceTest.java`

```
Setup:    10 PENDING_REVIEW questions: 4 LISTENING, 3 READING, 3 SPEAKING
          skill=LISTENING filter
Action:    getPendingQuestionsWithFilters(skill="LISTENING", ...)
Assertion: Returns only 4 LISTENING questions
```

### TC-REV-009: getPendingQuestionsWithFilters by CEFR
**Unit Test** | `ExpertReviewServiceTest.java`

```
Setup:    Questions: 5 B1, 3 B2, 2 C1
          cefrLevel=B2 filter
Action:    getPendingQuestionsWithFilters(cefrLevel="B2", ...)
Assertion: Returns only 3 B2 questions
```

### TC-REV-010: getPendingQuestionsWithFilters by teacher email
**Unit Test** | `ExpertReviewServiceTest.java`

```
Setup:    Questions created by teacherA@example.com (5) and teacherB@example.com (3)
          teacherEmail=teacherA filter
Action:    getPendingQuestionsWithFilters(teacherEmail="teacherA@example.com", ...)
Assertion: Returns only 5 questions from teacherA
```

### TC-REV-011: getPendingQuestionsWithFilters by keyword
**Unit Test** | `ExpertReviewServiceTest.java`

```
Setup:    Questions with content containing "climate", "weather", "temperature"
          keyword="climate"
Action:    getPendingQuestionsWithFilters(keyword="climate", ...)
Assertion: Returns only 1 question with "climate" in content
```

### TC-REV-012: getPendingQuestionsWithFilters by date range
**Unit Test** | `ExpertReviewServiceTest.java`

```
Setup:    Questions created: 2 today, 3 yesterday, 1 last week
          fromDate=today, toDate=today
Action:    getPendingQuestionsWithFilters(fromDate=..., toDate=..., ...)
Assertion: Returns only 2 questions from today
```

### TC-REV-013: getPendingQuestionsWithFilters combined filters
**Unit Test** | `ExpertReviewServiceTest.java`

```
Setup:    Questions matching various combinations
          skill=LISTENING, cefrLevel=B1, teacherEmail=teacherA
Action:    getPendingQuestionsWithFilters(skill="LISTENING", cefrLevel="B1",
          teacherEmail="teacherA@...", ...)
Assertion: Returns questions matching ALL three criteria (AND logic)
```

### TC-REV-014: PENDING_REVIEW question in published quiz — approve still works
**Unit Test** | `ExpertReviewServiceTest.java`

```
Setup:    Question in LESSON_QUIZ that is already PUBLISHED
          Quiz is published and isOpen=true
          Student has already started the quiz
Action:    approveQuestion(questionId, ...)
Assertion:
  - Question status → PUBLISHED
  - Students who haven't started see the question on next attempt
  - In-progress students unaffected
```

---

## 3. Notification API Controller Tests

### TC-NOTIF-API-001: GET /api/v1/notifications returns inbox
**Integration Test** | `NotificationControllerTest.java`

```
Setup:    User 5 has 15 notifications
          Auth as user 5
Action:    GET /api/v1/notifications?page=0&size=10
Assertion:
  - HTTP 200 OK
  - Page of 10 notifications
  - Only userId=5's notifications
```

### TC-NOTIF-API-002: GET notifications without auth returns 401
**Integration Test** | `NotificationControllerTest.java`

```
Setup:    No authentication
Action:    GET /api/v1/notifications
Assertion: HTTP 401 Unauthorized
```

### TC-NOTIF-API-003: GET /api/v1/notifications/unread-count
**Integration Test** | `NotificationControllerTest.java`

```
Setup:    User has 7 unread notifications
Action:    GET /api/v1/notifications/unread-count
Assertion:
  - HTTP 200 OK
  - Response: {success:true, data: {count: 7}}
```

### TC-NOTIF-API-004: GET /api/v1/notifications/top returns top 5
**Integration Test** | `NotificationControllerTest.java`

```
Setup:    User has 12 unread notifications
Action:    GET /api/v1/notifications/top
Assertion:
  - HTTP 200 OK
  - Returns 5 notifications
  - All unread
```

### TC-NOTIF-API-005: PATCH /api/v1/notifications/{id}/read marks as read
**Integration Test** | `NotificationControllerTest.java`

```
Setup:    Notification id=55 owned by user 5
          Auth as user 5
Action:    PATCH /api/v1/notifications/55/read
Assertion:
  - HTTP 200 OK
  - notification.isRead == true
```

### TC-NOTIF-API-006: PATCH read without ownership returns 403
**Integration Test** | `NotificationControllerTest.java`

```
Setup:    Notification owned by user 5
          Auth as user 6
Action:    PATCH /api/v1/notifications/55/read
Assertion: HTTP 403 Forbidden
```

### TC-NOTIF-API-007: PATCH /api/v1/notifications/read-all marks all as read
**Integration Test** | `NotificationControllerTest.java`

```
Setup:    User has 8 unread notifications
          Auth as that user
Action:    PATCH /api/v1/notifications/read-all
Assertion:
  - HTTP 200 OK
  - GET unread-count returns 0
```

---

## 4. Expert Review API Tests

### TC-API-REV-001: GET /api/v1/expert/question-review/pending returns queue
**Integration Test** | `ExpertReviewControllerTest.java`

```
Setup:    25 PENDING_REVIEW questions in DB
          Auth as EXPERT
Action:    GET /api/v1/expert/question-review/pending?page=0&size=20
Assertion:
  - HTTP 200 OK
  - Page of 20 questions
  - Only status=PENDING_REVIEW, source=TEACHER_PRIVATE
```

### TC-API-REV-002: POST /api/v1/expert/question-review/{id}/approve
**Integration Test** | `ExpertReviewControllerTest.java`

```
Setup:    Question id=10, status=PENDING_REVIEW, source=TEACHER_PRIVATE
          Teacher userId=5
          Auth as EXPERT
Action:    POST /api/v1/expert/question-review/10/approve
          Body: {reviewNote: "Excellent question!"}
Assertion:
  - HTTP 200 OK
  - Question status == "PUBLISHED"
  - Notification created for teacher (userId=5)
  - Notification type == "QUESTION_APPROVED"
```

### TC-API-REV-003: POST approve without reviewNote
**Integration Test** | `ExpertReviewControllerTest.java`

```
Setup:    Question PENDING_REVIEW
Action:    POST /api/v1/expert/question-review/{id}/approve
          Body: {}  (empty body)
Assertion:
  - HTTP 200 OK
  - reviewNote saved as null
```

### TC-API-REV-004: POST /api/v1/expert/question-review/{id}/reject (soft delete)
**Integration Test** | `ExpertReviewControllerTest.java`

```
Setup:    Question id=15
          Auth as EXPERT
Action:    POST /api/v1/expert/question-review/15/reject
          Body: {reviewNote: "Wrong format", delete: false}
Assertion:
  - HTTP 200 OK
  - Question status == "DRAFT"
  - Question NOT deleted
  - Notification created for teacher with reviewNote
```

### TC-API-REV-005: POST /api/v1/expert/question-review/{id}/reject (hard delete)
**Integration Test** | `ExpertReviewControllerTest.java`

```
Setup:    Question id=20
Action:    POST /api/v1/expert/question-review/{id}/reject
          Body: {reviewNote: "Spam", delete: true}
Assertion:
  - HTTP 200 OK
  - Question deleted from DB
  - Notification sent to teacher
```

### TC-API-REV-006: Reject without reviewNote still works
**Integration Test** | `ExpertReviewControllerTest.java`

```
Setup:    Body: {delete: false}
Action:    POST /api/v1/expert/question-review/{id}/reject
Assertion:
  - HTTP 200 OK
  - reviewNote saved as null
  - Notification generated with truncated content
```

### TC-API-REV-007: TEACHER cannot access approval queue
**Integration Test** | `ExpertReviewControllerTest.java`

```
Setup:    Auth as TEACHER
Action:    GET /api/v1/expert/question-review/pending
Assertion: HTTP 403 Forbidden
```

### TC-API-REV-008: STUDENT cannot access approval queue
**Integration Test** | `ExpertReviewControllerTest.java`

```
Setup:    Auth as STUDENT
Action:    GET /api/v1/expert/question-review/pending
Assertion: HTTP 403 Forbidden
```

---

## 5. Full End-to-End Approval Flow Tests

### TC-E2E-001: Complete approval flow — approve → notification → bank
**Integration Test** | `ExpertApprovalFlowTest.java`

```
Setup:
  - Expert creates bank questions
  - Teacher creates lesson quiz
  - Teacher adds inline question (status=PENDING_REVIEW, source=TEACHER_PRIVATE)
  - Expert receives notification

Flow:
  1. Teacher creates inline question
     POST /api/v1/teacher/questions
     → Question created: status=PENDING_REVIEW, source=TEACHER_PRIVATE

  2. Teacher adds question to quiz
     POST /api/v1/teacher/quizzes/{quizId}/questions
     → Question added to quiz (still PENDING_REVIEW)

  3. Expert checks pending queue
     GET /api/v1/expert/question-review/pending
     → Question appears in queue

  4. Expert approves
     POST /api/v1/expert/question-review/{qId}/approve
     Body: {reviewNote: "Đạt yêu cầu"}

  5. Teacher checks notification
     GET /api/v1/notifications/top
     → Notification: "Câu hỏi của bạn đã được phê duyệt"

Assertion:
  - Question status == PUBLISHED
  - Teacher notification created and delivered
  - Question now in shared bank (TEACHER_PRIVATE source preserved)
```

### TC-E2E-002: Complete rejection flow — reject → notification → teacher view
**Integration Test** | `ExpertApprovalFlowTest.java`

```
Setup:    Teacher creates 2 inline questions

Flow:
  1. Expert rejects first question
     POST /api/v1/expert/question-review/{q1}/reject
     Body: {reviewNote: "Câu hỏi không đúng CEFR", delete: false}

  2. Expert deletes second question
     POST /api/v1/expert/question-review/{q2}/reject
     Body: {reviewNote: "Spam", delete: true}

  3. Teacher checks notifications
     GET /api/v1/notifications/top

Assertion:
  - 2 notifications received
  - q1 notification: message contains "Câu hỏi không đúng CEFR"
  - q2 notification: message contains "Spam"

  4. Teacher views own questions
     GET /teacher/my-questions
     → q1 shows status=DRAFT with reviewNote
     → q2 NOT visible (deleted)
```

### TC-E2E-003: Expert approves question already in published quiz
**Integration Test** | `ExpertApprovalFlowTest.java`

```
Setup:
  - Teacher creates lesson quiz (PUBLISHED, isOpen=true)
  - Teacher adds PENDING_REVIEW question to quiz
  - Student A already started quiz
  - Student B has not started

Flow:
  1. Student A is mid-quiz (current questions loaded, before new question approved)
  2. Expert approves question
  3. Student B starts quiz

Assertion:
  - Student A: unchanged (already loaded questions)
  - Student B: sees new question in quiz
  - Quiz remains PUBLISHED
```

---

## 6. Teacher "My Questions" Page Tests

### TC-MYQ-001: Teacher sees all their questions with correct status badges
**Integration Test** | `TeacherQuestionControllerTest.java`

```
Setup:    Teacher has: 3 PUBLISHED, 5 PENDING_REVIEW, 2 DRAFT (rejected)
Action:    GET /api/v1/teacher/questions/my
Assertion:
  - Returns all 10 questions
  - PUBLISHED questions: badge "Đã phê duyệt"
  - PENDING_REVIEW: badge "Chờ phê duyệt"
  - DRAFT: badge "Bị từ chối"
```

### TC-MYQ-002: Filter by status
**Integration Test** | `TeacherQuestionControllerTest.java`

```
Setup:    Teacher has mixed questions
Action:    GET /api/v1/teacher/questions/my?status=PENDING_REVIEW
Assertion: Returns only PENDING_REVIEW questions
```

### TC-MYQ-003: Show reviewNote for rejected questions
**Integration Test** | `TeacherQuestionControllerTest.java`

```
Setup:    Question rejected with reviewNote="Wrong format"
Action:    GET /api/v1/teacher/questions/my
Assertion:
  - Question with DRAFT status shows reviewNote="Wrong format"
  - reviewedAt shows rejection date
  - reviewer info visible
```

### TC-MYQ-004: Teacher cannot see another teacher's questions
**Integration Test** | `TeacherQuestionControllerTest.java`

```
Setup:    teacherB's questions in DB
Action:    Auth as teacherA → GET /api/v1/teacher/questions/my
Assertion: Returns only teacherA's questions, not teacherB's
```

---

## 7. Edge Cases

### TC-EDGE-001: Approve question that doesn't exist
**Unit Test** | `ExpertReviewServiceTest.java`

```
Setup:    questionId=9999 not found
Action:    approveQuestion(9999, ...)
Assertion: Throws ResourceNotFoundException
```

### TC-EDGE-002: Reject already PUBLISHED question
**Unit Test** | `ExpertReviewServiceTest.java`

```
Setup:    Question already PUBLISHED
Action:    rejectQuestion(id, ...) → expect exception
Assertion: Throws InvalidDataException "Question is not pending review"
```

### TC-EDGE-003: Notification content truncation (>80 chars)
**Unit Test** | `NotificationServiceTest.java`

```
Setup:    questionContent = "A".repeat(200)
Action:    sendQuestionApproved(teacherId, questionContent, ...)
Assertion:
  - notification.message starts with 80 chars + "..."
  - No crash
```

### TC-EDGE-004: Teacher deletes their own PENDING_REVIEW question
**Unit Test** | `ExpertReviewServiceTest.java`

```
Setup:    Teacher tries to delete question while it's in an active quiz
Action:    Depends on spec — no deletion endpoint defined in SPEC 003
Assertion: Clarify: Is there a delete endpoint for teacher's own questions?
          → Not in spec, add to test scenario for future clarification
```

### TC-EDGE-005: Expert approves non-existent question ID
**Integration Test** | `ExpertReviewControllerTest.java`

```
Setup:    Question id=99999 does not exist
Action:    POST /api/v1/expert/question-review/99999/approve
Assertion: HTTP 404 Not Found
```

---

## Test Fixtures

```java
private User createTeacherUser(Long id, String email) { ... }
private User createExpertUser(Long id, String email) { ... }
private Question createPendingQuestion(User teacher, String skill, String content) { ... }
private Question createPublishedQuestion(User expert, String skill) { ... }
private Notification createNotification(Long userId, String type) { ... }
private void assertNotification(Notification n, String type, Long userId) { ... }
```
