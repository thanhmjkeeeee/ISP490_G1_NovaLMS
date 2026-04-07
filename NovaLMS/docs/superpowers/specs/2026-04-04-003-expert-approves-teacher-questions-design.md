# SPEC 003 — Expert Approves Teacher Questions

## 1. Overview

**Actor:** Expert
**Flow:** Expert reviews questions created by Teachers (status=PENDING_REVIEW, source=TEACHER_PRIVATE), approves them (→ PUBLISHED), rejects them (→ DRAFT or deletes), and provides feedback notes.
**Related Specs:** SPEC 002 (Teacher creates lesson quiz), SPEC 001 (Expert creates assignment)

---

## 2. Business Rules

| Rule | Description |
|---|---|
| BR-001 | Only users with role `EXPERT` can approve or reject questions |
| BR-002 | Only questions with `status = PENDING_REVIEW` and `source = TEACHER_PRIVATE` appear in the approval queue |
| BR-003 | Approve action: `status → PUBLISHED`, `reviewer_id → expertId`, `reviewed_at → now()` |
| BR-004 | Reject action: `status → DRAFT` (default) OR deleted (if `delete=true`), `reviewer_id → expertId`, `reviewed_at → now()`, `review_note` saved |
| BR-005 | Expert can provide a `review_note` (max 500 chars) on both approve and reject |
| BR-006 | If a PENDING_REVIEW question is currently in an active Lesson Quiz that a student is taking, rejecting it does NOT affect the in-progress attempt |
| BR-007 | When a PENDING_REVIEW question is approved, all active Lesson Quizzes containing that question automatically include it for future student attempts |
| BR-008 | Approve/Reject is one-at-a-time (no bulk batch approve in V1) |
| BR-009 | Expert can filter the approval queue by: skill, CEFR level, teacher name, date range, keyword |
| BR-010 | Teacher receives notification when their question is approved or rejected (email or in-app notification) |

---

## 3. Existing Implementation

**File:** `src/main/java/com/example/DoAn/controller/ExpertReviewController.java`
**File:** `src/main/java/com/example/DoAn/service/IExpertQuestionService.java`

**Existing endpoints:**
```
GET  /api/v1/expert/question-review/pending  → list PENDING_REVIEW questions
POST /api/v1/expert/question-review/{id}/approve → approve + save reviewer info
POST /api/v1/expert/question-review/{id}/reject → reject (status → DRAFT or delete)
```

**Existing Question fields (already have):**
- `reviewerId` — BigInt
- `reviewedAt` — LocalDateTime
- `reviewNote` — String(500)

**What exists but needs enhancement:**
1. **No dedicated approval UI page** — Expert currently approves via API tools (Postman/curl), no Thymeleaf page exists
2. **No notification to teacher** — no email or in-app notification when question is approved/rejected
3. **No filter by teacher name** — existing filter only has skill/cefr/keyword
4. **No preview of question in context** — expert sees question but not what quiz it's going into

---

## 4. Approval Queue Page (New UI)

**URL:** `GET /expert/question-approval`
**Template:** `expert/question-approval.html` (new file)
**Controller:** `ExpertViewController.java` → add `GET /expert/question-approval`

### Layout

```
┌─────────────────────────────────────────────────────────┐
│  Expert Header                                          │
├─────────────────────────────────────────────────────────┤
│  Question Approval Queue         [Filters] [Search]     │
│                                                         │
│  Filter: [Skill ▼] [CEFR ▼] [Teacher ▼] [Date range]  │
│                                                         │
│  ┌──────────────────────────────────────────────────┐  │
│  │ [Q1] Speaking: "Describe your daily routine..." │  │
│  │ Teacher: Nguyen Van A | B1 | 2024-04-01          │  │
│  │ Source: TEACHER_PRIVATE | Created: 2024-04-01    │  │
│  │ [Preview] [Approve ✓] [Reject ✗]                 │  │
│  └──────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────┐  │
│  │ [Q2] ...                                          │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

### Filters

| Filter | Type | API Parameter |
|---|---|---|
| Skill | Single-select | `?skill=LISTENING` |
| CEFR Level | Single-select | `?cefrLevel=B1` |
| Teacher | Single-select | `?teacherEmail={email}` |
| Date range | From–To date | `?fromDate=&toDate=` |
| Keyword | Text search | `?keyword=` (searches `content`, `topic`) |
| Question Type | Single-select | `?questionType=SPEAKING` |

### Pagination

- Default 20 per page
- `GET /api/v1/expert/question-review/pending?page=0&size=20&skill=&cefrLevel=&teacherEmail=&keyword=`
- Response: paginated list with total count

---

## 5. Question Detail Modal (Approval View)

When Expert clicks "Preview" on a question card → opens `expert/question-approval-modal.html` (modal overlay)

**Shows:**
- Full question content
- Question type badge
- Skill badge
- CEFR badge
- Topic
- Tags
- Audio/image attachments (with play button / image preview)
- Answer options (for MC/FILL/MATCH types)
- Passage content (if question belongs to a group)
- **Created by:** teacher name + email + creation date
- **Used in:** list of Lesson Quizzes this question has been added to (optional — only if question is already in a quiz)

**Actions:**
- "Phê duyệt" (Approve) → opens confirmation dialog → enter optional note → confirm
- "Từ chối" (Reject) → opens confirmation dialog → enter note + optional delete checkbox → confirm

### Approve Flow (API)

```
POST /api/v1/expert/question-review/{questionId}/approve
Body: { "reviewNote": "Đạt yêu cầu, đã xuất bản" }
```

**Backend (`ExpertQuestionServiceImpl.approveQuestion`):**
1. Fetch question by ID
2. Validate `status = PENDING_REVIEW` and `source = TEACHER_PRIVATE`
3. Set `status = PUBLISHED`
4. Set `reviewerId = currentExpertId`
5. Set `reviewedAt = LocalDateTime.now()`
6. Set `reviewNote` from request body (optional)
7. Save
8. **Trigger notification** → `NotificationService.send(question.user.email, "QUESTION_APPROVED", ...)`
9. Return 200

### Reject Flow (API)

```
POST /api/v1/expert/question-review/{questionId}/reject
Body: { "reviewNote": "Câu hỏi không phù hợp với tiêu chuẩn", "delete": false }
```

**Backend (`ExpertQuestionServiceImpl.rejectQuestion`):**
1. Fetch question by ID
2. Validate `status = PENDING_REVIEW` and `source = TEACHER_PRIVATE`
3. If `delete = true`: `questionRepository.delete(question)`
4. Else: Set `status = DRAFT`, set reviewer info, save
5. **Trigger notification** → `NotificationService.send(question.user.email, "QUESTION_REJECTED", ...)`
6. Return 200

---

## 6. Notification System (New)

### New Notification Entity (reuse existing if exists)

**Check:** `src/main/java/com/example/DoAn/model/` for existing `Notification.java`

If not exists, new entity:

```java
@Entity
@Table(name = "notification")
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String type; // QUESTION_APPROVED, QUESTION_REJECTED
    private String title;
    private String message;
    private Boolean isRead = false;
    private LocalDateTime createdAt;
    private String link; // e.g. "/teacher/quiz/123/build"
}
```

### Notification Types

| Type | Title | Message | Link |
|---|---|---|---|
| `QUESTION_APPROVED` | "Câu hỏi của bạn đã được phê duyệt" | "Câu hỏi '[content preview...]' đã được Expert phê duyệt và xuất bản." | `/teacher/my-questions` |
| `QUESTION_REJECTED` | "Câu hỏi của bạn bị từ chối" | "Câu hỏi '[content preview...]' đã bị từ chối. Lý do: [reviewNote]" | `/teacher/my-questions` |

### Notification Service

**New:** `INotificationService` + `NotificationServiceImpl`
**New:** `NotificationController` → `GET /api/v1/notifications` (inbox), `PATCH /api/v1/notifications/{id}/read`, `PATCH /api/v1/notifications/read-all`

---

## 7. API Endpoints (Extensions)

| Method | Endpoint | Change |
|---|---|---|
| `GET` | `/api/v1/expert/question-review/pending` | Add filter params: `teacherEmail`, `fromDate`, `toDate`, `questionType` |
| `POST` | `/api/v1/expert/question-review/{id}/approve` | Add `reviewNote` field in request body |
| `POST` | `/api/v1/expert/question-review/{id}/reject` | Add `reviewNote` + `delete` field in request body |
| `GET` | `/api/v1/expert/question-review/{id}` | New — get single question detail for modal |
| `GET` | `/api/v1/notifications` | New — teacher's notification inbox |
| `PATCH` | `/api/v1/notifications/{id}/read` | New — mark as read |

---

## 8. Teacher's View of Their Questions

**New page:** `GET /teacher/my-questions`
**Template:** `teacher/my-questions.html`
**Shows:** list of all questions the teacher created (TEACHER_PRIVATE), with status badges:
- 🟡 "Chờ phê duyệt" — PENDING_REVIEW
- 🟢 "Đã phê duyệt" — PUBLISHED (shows `reviewedAt`, reviewer name)
- 🔴 "Bị từ chối" — DRAFT (shows `reviewNote` from expert)
- Archived — ARCHIVED

Also shows the `reviewNote` from Expert if available.

---

## 9. Queue Summary Card (Expert Dashboard)

**Expert dashboard** (`/expert/dashboard`):
- Add a summary card: "Câu hỏi chờ phê duyệt: X" with a direct link to `/expert/question-approval`
- Badge count updates in real-time via polling

---

## 10. Edge Cases

| Case | Handling |
|---|---|
| Expert approves question that is already in a published quiz | Questions appear in the quiz immediately. Students who haven't started see them. In-progress students are unaffected. |
| Expert rejects question that is already in a published quiz | Same behavior as above — current in-progress attempts are unaffected. Future students don't see the question. |
| Teacher deletes their own PENDING_REVIEW question | Allowed if no student has started the quiz containing it. If quiz is in progress, deletion is blocked. |
| Expert approves question that was created for a specific quiz | Question becomes available in the shared bank (EXPERT_BANK still shows TEACHER_PRIVATE as source). The original quiz automatically includes it for future attempts. |
| Expert tries to approve an already PUBLISHED question | Rejected — 400 error, "Question is already approved" |
| Expert tries to approve a non-TEACHER_PRIVATE question | Rejected — only TEACHER_PRIVATE PENDING_REVIEW questions can be approved via this flow |