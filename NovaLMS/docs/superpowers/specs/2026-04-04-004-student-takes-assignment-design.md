# SPEC 004 — Student Takes Assignment

## 1. Overview

**Actor:** Student
**Flow:** Student opens an Assignment (published by Expert, opened by Teacher), goes through 4 sequential skill sections (LISTENING → READING → SPEAKING → WRITING) with per-skill timers, partial submissions per section, SPEAKING recording with countdown auto-submit, and resume capability.
**Related Specs:** SPEC 001 (Expert creates Assignment), SPEC 005 (Teacher grades Assignment)

---

## 2. Business Rules

| Rule | Description |
|---|---|
| BR-001 | Only students enrolled in a class where the Assignment is opened can take it |
| BR-002 | Assignment must have `status = PUBLISHED` AND `isOpen = true` |
| BR-003 | Assignment must have `isSequential = true` (4-skill sequential) |
| BR-004 | Student can only access the current skill section; previous sections are review-only |
| BR-005 | Each section must be submitted before the next becomes accessible |
| BR-006 | `AssignmentSession` is created on first access — one session per student per quiz |
| BR-007 | Auto-save: answers saved every 30 seconds via `PATCH /api/v1/student/assignment/session/{sessionId}/section` |
| BR-008 | Closing browser mid-section: partial answers are preserved in `AssignmentSession.sectionAnswers` |
| BR-009 | Resume: student returns to `currentSkillIndex` section with saved answers |
| BR-010 | SPEAKING section: unlimited re-recordings (each replaces previous) |
| BR-011 | SPEAKING timer at 0:00 → auto-submits the current recording |
| BR-012 | LISTENING/READING: no per-skill timer (quiz-level timer from `timeLimitMinutes` applies across all sections) |
| BR-013 | SPEAKING: per-skill timer from `timeLimitPerSkill` (e.g. 2 minutes) |
| BR-014 | WRITING: per-skill timer from `timeLimitPerSkill` (e.g. 30 minutes) |
| BR-015 | After all 4 sections submitted → `AssignmentSession.status = COMPLETED` → `QuizResult` created |
| BR-016 | After submission, AI grading fires async for SPEAKING/WRITING answers |
| BR-017 | Student can view their result after all sections completed (not immediately — wait for grading) |
| BR-018 | Quiz-level `maxAttempts` applies to full Assignment (not per section) |
| BR-019 | `questionOrder = RANDOM` applies per section (not globally) |

---

## 3. Data Models

### New Entity: `AssignmentSession`

**File:** `src/main/java/com/example/DoAn/model/AssignmentSession.java`

```java
@Entity
@Table(name = "assignment_session",
       uniqueConstraints = @UniqueConstraint(columnNames = {"quiz_id", "user_id"}))
public class AssignmentSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // IN_PROGRESS | COMPLETED | EXPIRED
    @Column(name = "status")
    private String status = "IN_PROGRESS";

    // 0=LISTENING, 1=READING, 2=SPEAKING, 3=WRITING
    @Column(name = "current_skill_index")
    private Integer currentSkillIndex = 0;

    // JSON: {"LISTENING": {"1": "a", "2": "b", "audio_1": "cloudinary_url"}, ...}
    @Column(name = "section_answers", columnDefinition = "JSON")
    private String sectionAnswers;

    // JSON: {"LISTENING": "COMPLETED", "READING": "IN_PROGRESS", ...}
    @Column(name = "section_statuses", columnDefinition = "JSON")
    private String sectionStatuses;

    // Per-section timer expiry: JSON {"SPEAKING": "2024-04-01T10:05:00", "WRITING": ...}
    @Column(name = "section_expiry", columnDefinition = "JSON")
    private String sectionExpiry;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt; // overall session expiry (quiz-level)
}
```

### Repository

```java
public interface AssignmentSessionRepository
    extends JpaRepository<AssignmentSession, Long> {

    Optional<AssignmentSession> findByQuizQuizIdAndUserUserId(Integer quizId, Long userId);
    boolean existsByQuizQuizIdAndUserUserId(Integer quizId, Long userId);
    List<AssignmentSession> findByUserUserId(Long userId);
}
```

### Quiz Result Changes

```java
// Add to QuizResult entity
@Column(name = "assignment_session_id")
private Long assignmentSessionId;

@Column(name = "section_scores", columnDefinition = "JSON")
private String sectionScores; // {"LISTENING": 8.0, "READING": 7.5, "SPEAKING": null, "WRITING": null}
```

---

## 4. Student Access Flow

### Entry Point

**URL:** `GET /student/assignment/{quizId}`
**Template:** `student/assignment-welcome.html`

**Backend:** `StudentAssignmentController.getAssignment(session, quizId)`

```
1. Validate user is STUDENT
2. Validate quiz exists, status=PUBLISHED, isOpen=true, isSequential=true
3. Validate student is enrolled in a class where this quiz is opened
4. Check maxAttempts: count existing QuizResults for this student+quiz
   → if >= maxAttempts → show "Bạn đã hết lượt làm bài" page
5. Check if AssignmentSession exists for this student+quiz
   → If exists AND status=IN_PROGRESS: redirect to current section
   → If exists AND status=COMPLETED: show "Bạn đã hoàn thành bài này" with link to result
   → If not exists: create new AssignmentSession (status=IN_PROGRESS, currentSkillIndex=0)
     → set sectionStatuses: all 4 = "LOCKED" except first = "IN_PROGRESS"
6. Redirect to current skill section
```

**Welcome page shows:**
- Assignment title
- Skill sections listed: LISTENING, READING, SPEAKING, WRITING
- Per-skill timer info (if set)
- Total time (quiz-level timer)
- "Bắt đầu" button → redirect to first section

---

## 5. Section Page — LISTENING / READING

**URL:** `GET /student/assignment/session/{sessionId}/section/{skill}`
**Template:** `student/assignment-section.html` (reusable for all sections except SPEAKING)

**Example URL:** `/student/assignment/session/5/section/LISTENING`

### Backend: `getSection(sessionId, skill)`

```
1. Fetch AssignmentSession by ID
2. Validate session.user == currentUser
3. Validate session.quiz == this assignment
4. Validate session.status == IN_PROGRESS
5. Validate skill == currentSkillIndex OR is previous section
6. Load questions for this skill from quiz
   → Fetch QuizQuestion entities where skill = {skill}, order by orderIndex
7. Load saved answers from session.sectionAnswers for this skill
8. Return section data DTO
```

### `AssignmentSectionDTO`

```java
public class AssignmentSectionDTO {
    private Long sessionId;
    private String skill;           // LISTENING / READING / SPEAKING / WRITING
    private Integer sectionIndex;   // 0-3
    private Integer currentSkillIndex;
    private Long timerSeconds;      // quiz-level remaining time (seconds)
    private Long speakingTimerSeconds; // SPEAKING-specific timer (if SPEAKING section)
    private String speakingExpiry; // ISO datetime when SPEAKING timer expires
    private List<QuestionPayloadDTO> questions;
    private Map<Integer, Object> savedAnswers; // questionId → answer
    private String sectionStatus;   // IN_PROGRESS / COMPLETED
    private Boolean isLocked;       // true if future section
    private String nextSkill;       // next skill name or null
    private String previousSkill;   // prev skill name or null
}
```

### Frontend Behavior (LISTENING/READING)

- **Quiz-level timer:** shown at top-right, counts down from `timeLimitMinutes` (if set)
- Timer at 0:00 → auto-submits entire assignment (all sections) → treats as final submission
- Show questions for this skill only
- Show saved answers if any
- "Lưu tạm" button → `PATCH /api/v1/student/assignment/session/{sessionId}/section/{skill}` with current answers
- "Nộp phần này" (Submit Section) button
- If this is last section (WRITING): "Nộp bài & Kết thúc"

### Auto-Save

- `setInterval(autoSave, 30000)` — every 30 seconds, send current answers via:
- `PATCH /api/v1/student/assignment/session/{sessionId}/section/{skill}`
  - Body: `{ answers: { "1": "a", "2": ["b","c"] } }`
  - Backend: merge answers into `sectionAnswers` JSON, return success
  - Visual feedback: "Đã lưu" toast notification

### Submit Section API

```
POST /api/v1/student/assignment/session/{sessionId}/section/{skill}/submit
Body: { answers: { "1": "a", "2": ["b","c"] } }
```

**Backend:**
```
1. Auto-save all answers (same as PATCH)
2. Auto-grade MC/FILL/MATCH questions for this section:
   → For each answer:
     - MULTIPLE_CHOICE_SINGLE/MULTI: compare selected options to correctAnswer
     - FILL_IN_BLANK: compare trimmed text
     - MATCHING: compare left-right pairs
   → Store per-question isCorrect in sectionAnswers (or a separate answers store)
3. Set sectionStatuses[skill] = "COMPLETED"
4. Set currentSkillIndex = skillIndex + 1
5. Set sectionStatuses[nextSkill] = "IN_PROGRESS"
6. If this is WRITING section (last): set session.status = COMPLETED, create QuizResult
7. Return next skill info OR completion result
```

**If timer hits 0 during a section:**
- `POST /api/v1/student/assignment/session/{sessionId}/auto-submit`
- Same logic as submit section + marks remaining sections as EXPIRED
- `session.status = COMPLETED`, `session.completedAt = now()`

---

## 6. Section Page — SPEAKING (Recording + Timer)

**URL:** `GET /student/assignment/session/{sessionId}/section/SPEAKING`
**Template:** `student/assignment-speaking.html`

### Backend: `getSpeakingSection(sessionId)`

Same as general section, plus:
- Returns `speakingTimerSeconds` (from `timeLimitPerSkill["SPEAKING"]`)
- Returns `speakingExpiry` = `startedAt + timeLimitSeconds`
- Returns question prompt for each SPEAKING question

### SPEAKING Section DTO

```java
public class SpeakingSectionDTO extends AssignmentSectionDTO {
    private Long speakingTimerSeconds; // countdown in seconds (e.g. 120)
    private String speakingExpiry;     // ISO datetime
    private String currentRecordingUrl; // saved audio URL from sectionAnswers
    private Boolean hasRecording;       // true if currentRecordingUrl != null
}
```

### Frontend — Recording UI

```
┌─────────────────────────────────────────────────────────┐
│  🎤 Phần Nói          ⏱ 01:47                          │
│  (countdown from timeLimitPerSkill.SPEAKING)            │
│                                                         │
│  Câu 1: Describe your daily routine in at least 5...   │
│  [Audio prompt: ▶️ Nghe đề bài] (if audioUrl exists)   │
│                                                         │
│  ┌─────────────────────────────────────────────────┐  │
│  │  🔴 [START RECORDING]                           │  │
│  │  (When recording: green border, ⏹ Stop)        │  │
│  │  After stop: ▶️ [Preview recording]              │  │
│  │  [🔄 Record again] (unlimited)                  │  │
│  └─────────────────────────────────────────────────┘  │
│                                                         │
│  ⚠️ Ghi âm sẽ tự động nộp khi hết thời gian            │
│  ⏱ Thời gian còn lại: 01:47                            │
│                                                         │
│  [Lưu tạm]            [Nộp phần này →]               │
└─────────────────────────────────────────────────────────┘
```

### JavaScript Recording Logic

```javascript
// Browser MediaRecorder API
let mediaRecorder = null;
let audioChunks = [];
let currentRecordingUrl = null;
let speakingTimerInterval = null;
let speakingTimeRemaining = 0;

// Start Recording
async function startRecording() {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
    mediaRecorder = new MediaRecorder(stream, { mimeType: 'audio/webm' });
    audioChunks = [];

    mediaRecorder.ondataavailable = (e) => audioChunks.push(e.data);
    mediaRecorder.onstop = uploadRecording;

    mediaRecorder.start();
    startSpeakingTimer(speakingTimeRemaining);
    updateUI("recording");
}

// Stop Recording
function stopRecording() {
    if (mediaRecorder && mediaRecorder.state === "recording") {
        mediaRecorder.stop();
        mediaRecorder.stream.getTracks().forEach(t => t.stop());
        clearInterval(speakingTimerInterval);
    }
}

// Upload to Cloudinary
async function uploadRecording() {
    const blob = new Blob(audioChunks, { type: 'audio/webm' });
    const formData = new FormData();
    formData.append("file", blob, "speaking_answer.webm");

    // Upload via existing FileUploadService endpoint
    const response = await fetch("/api/v1/upload/audio", {
        method: "POST",
        body: formData
    });
    const data = await response.json();
    currentRecordingUrl = data.url;

    // Update hidden field + preview
    document.getElementById("speaking_answer_1").value = data.url;
    showPlayback(data.url);
}

// Speaking Timer
function startSpeakingTimer(seconds) {
    speakingTimeRemaining = seconds;
    speakingTimerInterval = setInterval(() => {
        speakingTimeRemaining--;
        updateTimerDisplay(speakingTimeRemaining);
        if (speakingTimeRemaining <= 0) {
            clearInterval(speakingTimerInterval);
            if (mediaRecorder && mediaRecorder.state === "recording") {
                stopRecording();
            }
            // Auto-submit section after upload completes
            autoSubmitSection();
        }
    }, 1000);
}
```

### SPEAKING Timer Rules

| Event | Action |
|---|---|
| Student clicks "Start Recording" | Timer starts counting down from `timeLimitPerSkill["SPEAKING"]` |
| Student clicks "Stop Recording" | Timer pauses; student can preview |
| Student clicks "Record again" | Timer resumes OR resets (configurable: see below) |
| Timer reaches 0:00 | Auto-stops recording, auto-uploads, auto-submits section |
| Student submits manually before timer | Timer is cancelled; normal submission |

**Re-record behavior:**
- After first recording saved → "🔄 Ghi âm lại" button appears
- Clicking it → timer resets to full `timeLimitPerSkill["SPEAKING"]` → new recording replaces previous
- Unlimited re-recordings
- The URL in `sectionAnswers` is always the most recent recording

### Submit SPEAKING Section

```
POST /api/v1/student/assignment/session/{sessionId}/section/SPEAKING/submit
Body: {
  answers: {
    "1": "cloudinary_url_of_audio",
    "2": "cloudinary_url_of_audio_2"
  }
}
```

**Backend:**
```
1. Validate each answer is a valid Cloudinary URL
2. For SPEAKING questions: isCorrect = null initially
3. After tx commits → GroqGradingService.fireAndForget() for each SPEAKING answer
   (same as PlacementTest: transcribe → gradeWritingOrSpeaking)
4. Update session statuses (same as other sections)
5. Return next skill (WRITING)
```

---

## 7. Section Page — WRITING

**URL:** `GET /student/assignment/session/{sessionId}/section/WRITING`
**Template:** `student/assignment-writing.html`

### Differences from LISTENING/READING

- Shows `timeLimitPerSkill["WRITING"]` as section-level timer at top
- If `timeLimitPerSkill["WRITING"]` is null → uses quiz-level `timeLimitMinutes`
- Textarea input for WRITING questions (character count shown)
- No answer options — free-text input
- Same auto-save + submit logic

### Writing Timer Auto-Submit

```
If writingTimerSeconds <= 0:
  → same auto-submit logic as other sections
  → session.status = COMPLETED
  → create QuizResult
  → GroqGradingService.fireAndForget() for each WRITING answer
```

---

## 8. Assignment Completion

### After WRITING Section Submitted

```
POST /api/v1/student/assignment/session/{sessionId}/complete
```

**Backend (`StudentAssignmentService.completeAssignment(sessionId)`):**
```
1. Validate all 4 sections are COMPLETED
2. Set session.status = COMPLETED
3. Set session.completedAt = now()
4. Calculate raw scores per section (MC/FILL/MATCH only)
5. Create QuizResult:
   - quiz_id = session.quiz.id
   - user_id = session.user.id
   - score = sum of auto-graded section scores
   - correctRate = calculated
   - passed = (score >= passScore if passScore set)
   - submittedAt = now()
   - assignment_session_id = session.id
6. Fire async AI grading for SPEAKING + WRITING:
   → GroqGradingService.fireAndForget() per SPEAKING/WRITING question answer
7. Return completion page
```

**Completion page (`student/assignment-complete.html`):**
- "Bạn đã hoàn thành bài kiểm tra!"
- Summary: sections completed, auto-graded score shown
- Note: "Phần Nói và Viết đang được chấm điểm tự động. Kết quả cuối cùng sẽ được công bố sau khi Giáo viên duyệt."
- Link to: "/student/quiz/result/{resultId}" (shows partial results with pending indicator)

---

## 9. Result Page

**URL:** `GET /student/quiz/result/{resultId}`
**Template:** `student/quiz-result.html` (extend existing result page)

### Additional for Assignment

```
- Assignment header with skill sections
- Per-section score cards:
  - LISTENING: score + ✅ (auto-graded)
  - READING: score + ✅ (auto-graded)
  - SPEAKING: ⏳ "Đang chấm..." or AI score + teacher review pending
  - WRITING: ⏳ "Đang chấm..." or AI score + teacher review pending
- Final score: auto-graded sections shown; pending sections show "—"
- "Chờ giáo viên duyệt" badge for SPEAKING/WRITING
```

---

## 10. API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/student/assignment/{quizId}` | Get assignment info, create/open session |
| `GET` | `/api/v1/student/assignment/session/{sessionId}` | Get session status (for resume) |
| `GET` | `/api/v1/student/assignment/session/{sessionId}/section/{skill}` | Get section data + questions |
| `PATCH` | `/api/v1/student/assignment/session/{sessionId}/section/{skill}` | Auto-save answers |
| `POST` | `/api/v1/student/assignment/session/{sessionId}/section/{skill}/submit` | Submit section |
| `POST` | `/api/v1/student/assignment/session/{sessionId}/complete` | Complete assignment (WRITING final submit) |
| `POST` | `/api/v1/student/assignment/session/{sessionId}/auto-submit` | Timer expired → force submit all |

---

## 11. Audio Upload Endpoint (reuse)

**Existing:** `POST /api/v1/upload/audio` → `FileUploadService.upload(MultipartFile)`
**Reuse:** Used for both SPEAKING answer upload and LISTENING question audio

---

## 12. Student Assignment List Page

**URL:** `GET /student/my-assignments`
**Template:** `student/my-assignments.html` (new page)

**Shows:**
- List of all Assignments opened for student's classes
- Status per assignment: Not Started | In Progress | Completed | Expired
- Current skill progress if In Progress
- Due date if any
- "Làm bài" button → opens assignment

---

## 13. Edge Cases

| Case | Handling |
|---|---|
| Student closes browser mid-SPEAKING recording | Audio not saved; on resume, recording starts fresh. Timer NOT paused. |
| Browser loses internet during recording upload | Show error toast; retry upload; if retry fails 3x, show "Không thể tải file lên. Vui lòng thử lại." |
| Timer hits 0 during WRITING section | Auto-submit with current answers; any blank questions = 0 |
| Student tries to skip sections | Not allowed — API validates `currentSkillIndex` |
| Student tries to submit same section twice | Second submission is rejected — section already COMPLETED |
| Teacher closes assignment while student is taking it | `isOpen = false` → student sees "Bài đã đóng" on next action |
| Student has 0 remaining attempts | Redirect to "Hết lượt" page with link to result if any |
| Assignment has no questions for a skill | This should not happen — publish validation prevents it |
| Multiple tabs open for same assignment | `AssignmentSession` is one per student per quiz; second tab redirects to first tab's state |
