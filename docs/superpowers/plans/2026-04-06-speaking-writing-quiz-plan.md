# Speaking + Writing AI Grading cho Quiz Course — Implementation Plan

**Goal:** Thêm ghi âm cho SPEAKING và auto-grading AI cho SPEAKING + WRITING trong quiz course của student.

**Architecture:** Tái sử dụng audio recording UI từ `hybrid-quiz.html`, thêm timer per-question vào `quiz-take.html`, kết nối với `GroqGradingService` đã có sẵn cho async AI grading. Kết quả hiển thị sau submit với polling.

**Tech Stack:** Java Spring Boot, Thymeleaf, MediaRecorder API, Cloudinary, Groq AI (Whisper + LLaMA)

---

## Chunk 1: Database & Entity Changes

**Files:**
- Modify: `src/main/java/com/example/DoAn/model/Quiz.java`
- Modify: `src/main/java/com/example/DoAn/model/QuizAnswer.java`

---

### Task 1: Thêm `speakingTimeLimitSeconds` vào Quiz.java

- [ ] **Step 1: Đọc file `Quiz.java`**
- [ ] **Step 2: Thêm field**

```java
@Column(name = "speaking_time_limit_seconds")
private Integer speakingTimeLimitSeconds;
```

- [ ] **Step 3: Thêm getter/setter**

```java
public Integer getSpeakingTimeLimitSeconds() {
    return speakingTimeLimitSeconds;
}

public void setSpeakingTimeLimitSeconds(Integer speakingTimeLimitSeconds) {
    this.speakingTimeLimitSeconds = speakingTimeLimitSeconds;
}
```

---

### Task 2: Thêm 3 field vào QuizAnswer.java

- [ ] **Step 1: Đọc file `QuizAnswer.java`**
- [ ] **Step 2: Thêm 3 field**

```java
@Column(name = "audio_url")
private String audioUrl;

@Column(name = "ai_grading_status")
private String aiGradingStatus; // PENDING | COMPLETED | REVIEWED

@Column(name = "teacher_override_score")
private String teacherOverrideScore;
```

- [ ] **Step 3: Thêm getter/setter cho 3 field**

---

## Chunk 2: QuizTake HTML — Audio Recording UI

**Files:**
- Modify: `src/main/resources/templates/student/quiz-take.html`
- Read: `src/main/resources/templates/public/hybrid-quiz.html` (copy audio recording logic)

---

### Task 3: Thêm audio recording UI cho SPEAKING question trong quiz-take.html

- [ ] **Step 1: Đọc `quiz-take.html`**, tìm phần render SPEAKING question
- [ ] **Step 2: Thay thế textarea SPEAKING bằng audio UI**

Tìm đoạn hiện tại:
```html
<div th:if="${question.questionType == 'WRITING' or question.questionType == 'SPEAKING'}">
    <textarea class="form-control p-3 border-2" th:name="'q_' + ${question.questionId}" rows="5" ...></textarea>
</div>
```

Thay bằng:

```html
<!-- WRITING: giữ nguyên textarea -->
<div th:if="${question.questionType == 'WRITING'}">
    <textarea class="form-control p-3 border-2" th:name="'q_' + ${question.questionId}" rows="6"
              placeholder="Viết câu trả lời của bạn ở đây..."></textarea>
</div>

<!-- SPEAKING: audio recording UI -->
<div th:if="${question.questionType == 'SPEAKING'}" class="mb-3">
    <label class="form-label fw-bold">Ghi âm câu trả lời của bạn</label>
    <div class="d-flex gap-2 align-items-center mb-2">
        <button type="button" class="btn btn-outline-danger btn-sm" id="recordBtn" onclick="toggleRecording()">
            <i class="bi bi-mic"></i> Ghi âm
        </button>
        <label class="btn btn-outline-secondary btn-sm mb-0">
            <i class="bi bi-upload"></i> Tải lên file
            <input type="file" accept="audio/*" class="d-none" id="audioUpload" onchange="handleAudioUpload(this)">
        </label>
    </div>
    <div id="audioStatus" class="small text-muted"></div>
    <audio id="audioPlayback" controls class="d-none w-100 mt-2"></audio>
    <input type="hidden"
           th:id="'answer_' + ${question.questionId}"
           th:attr="data-speaking-question=${question.questionId}"
           th:name="'q_' + ${question.questionId}"
           value="">
</div>
```

---

### Task 4: Thêm JavaScript recording + timer

- [ ] **Step 1: Copy MediaRecorder JS từ `hybrid-quiz.html`** (phần `toggleRecording`, `uploadRecordedAudio`, `handleAudioUpload`)
- [ ] **Step 2: Thêm speaking timer countdown**

```javascript
// Timer per-question (khi bắt đầu ghi)
let speakingTimerInterval;
function startSpeakingTimer(seconds) {
    let remaining = seconds;
    const badge = document.getElementById('speaking-timer-badge');
    if (badge) {
        speakingTimerInterval = setInterval(() => {
            remaining--;
            const mins = Math.floor(remaining / 60);
            const secs = remaining % 60;
            badge.textContent = `⏱ ${String(mins).padStart(2,'0')}:${String(secs).padStart(2,'0')}`;
            if (remaining <= 0) {
                clearInterval(speakingTimerInterval);
                stopAndUploadRecording(); // auto-stop + upload
            }
        }, 1000);
    }
}
```

- [ ] **Step 3: Khi nhấn Ghi âm → gọi `startSpeakingTimer(quizSpeakingTimeLimit)`**

---

### Task 5: Thêm speakingTimeLimitSeconds vào model truyền sang template

- [ ] **Step 1: Đọc `StudentQuizTakingController.java`**, tìm method trả về quiz-take view
- [ ] **Step 2: Thêm vào Model**

```java
model.addAttribute("speakingTimeLimit", quiz.getSpeakingTimeLimitSeconds());
```

---

## Chunk 3: Backend API — Audio Upload

**Files:**
- Modify: `src/main/java/com/example/DoAn/controller/StudentQuizTakingController.java`
- Read: `src/main/java/com/example/DoAn/controller/PlacementTestControllerNN.java` (tham khảo endpoint audio)

---

### Task 6: Thêm endpoint upload audio

- [ ] **Step 1: Đọc `StudentQuizTakingController.java`**
- [ ] **Step 2: Thêm method upload audio**

```java
@PostMapping("/audio")
public ResponseEntity<?> uploadAudio(@RequestParam("file") MultipartFile file) {
    try {
        String audioUrl = fileUploadService.uploadFile(file);
        return ResponseEntity.ok(Map.of("status", 200, "audioUrl", audioUrl));
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(Map.of("status", 400, "message", e.getMessage()));
    }
}
```

- [ ] **Step 3: Inject `FileUploadService`** (kiểm tra đã có chưa, nếu chưa thì inject)

---

### Task 7: Thêm endpoint AI status

- [ ] **Step 1: Thêm method get AI status**

```java
@GetMapping("/result/{resultId}/ai-status")
public ResponseEntity<?> getAiStatus(@PathVariable Integer resultId) {
    List<Map<String, Object>> answers = quizAnswerRepository.findByResultResultId(resultId)
        .stream()
        .filter(a -> "WRITING".equals(a.getQuestion().getQuestionType())
                  || "SPEAKING".equals(a.getQuestion().getQuestionType()))
        .map(a -> {
            Map<String, Object> map = new HashMap<>();
            map.put("questionId", a.getQuestion().getQuestionId());
            map.put("aiGradingStatus", a.getAiGradingStatus() != null ? a.getAiGradingStatus() : "PENDING");
            map.put("aiScore", a.getAiScore());
            map.put("aiFeedback", a.getAiFeedback());
            map.put("aiRubricJson", a.getAiRubricJson());
            // Điểm hiển thị: override > AI score > null
            String displayScore = a.getTeacherOverrideScore() != null
                ? a.getTeacherOverrideScore()
                : a.getAiScore();
            map.put("displayScore", displayScore);
            return map;
        })
        .toList();
    return ResponseEntity.ok(Map.of("status", 200, "data", answers));
}
```

- [ ] **Step 2: Thêm `findByResultResultId` vào `QuizAnswerRepository.java`**

```java
List<QuizAnswer> findByResultResultId(Integer resultId);
```

---

## Chunk 4: QuizResultService — lưu audioUrl + AI status

**Files:**
- Modify: `src/main/java/com/example/DoAn/service/impl/QuizResultServiceImpl.java`
- Read: `src/main/java/com/example/DoAn/service/impl/PlacementTestServiceImpl.java` (tham khảo lưu audioUrl)

---

### Task 8: Lưu audioUrl khi submit SPEAKING

- [ ] **Step 1: Đọc `QuizResultServiceImpl.java`**, tìm đoạn xử lý SPEAKING trong `submitQuiz()`
- [ ] **Step 2: Thêm lưu audioUrl**

Tìm đoạn:
```java
if ("SPEAKING".equals(qType)) {
    // thêm vào đây
    quizAnswer.setAudioUrl(answeredOptions);
}
```

- [ ] **Step 3: Thêm khởi tạo aiGradingStatus = "PENDING"** cho WRITING/SPEAKING

```java
if ("WRITING".equals(qType) || "SPEAKING".equals(qType)) {
    quizAnswer.setAiGradingStatus("PENDING");
    quizAnswer.setPendingAiReview(true);
}
```

---

### Task 9: Cập nhật aiGradingStatus = COMPLETED trong GroqGradingServiceImpl

- [ ] **Step 1: Đọc `GroqGradingServiceImpl.java`**, tìm method `doGrade()` cho quiz answer
- [ ] **Step 2: Thêm sau khi lưu AI result**

```java
if (quizAnswer != null) {
    quizAnswer.setAiGradingStatus("COMPLETED");
    quizAnswerRepository.save(quizAnswer);
}
```

- [ ] **Step 3: Tìm xem `fireAndForgetForQuizAnswer()` có gọi `doGrade` không, kiểm tra đường đi**

---

## Chunk 5: Trang kết quả student + Polling

**Files:**
- Modify/Create: `src/main/resources/templates/student/quiz-result.html`
- Read: `src/main/resources/templates/public/hybrid-results.html` (copy polling logic)

---

### Task 10: Tạo/cập nhật trang kết quả quiz

- [ ] **Step 1: Kiểm tra `quiz-result.html` có tồn tại không**, đọc file
- [ ] **Step 2: Thêm hiển thị AI status cho SPEAKING/WRITING**

```html
<!-- PENDING -->
<th:block th:if="${aq.aiGradingStatus == 'PENDING'}">
    <span class="badge bg-warning text-dark">
        <i class="bi bi-hourglass-split"></i> Đang AI chấm...
    </span>
</th:block>

<!-- COMPLETED -->
<th:block th:if="${aq.aiGradingStatus == 'COMPLETED'}">
    <div class="ai-score-box">
        <span class="ai-score-val" th:text="${aq.displayScore ?: aq.aiScore}">8</span>
        <span>/</span>
        <span th:text="${aq.maxPoints}">10</span>
        <p class="small text-muted mb-1" th:text="${aq.aiFeedback}">AI feedback here</p>
        <!-- Rubric breakdown -->
        <button class="btn btn-link btn-sm p-0" type="button"
                data-bs-toggle="collapse"
                th:attr="data-bs-target='#rubric-' + ${aq.result.resultId} + '-' + ${aq.question.questionId}">
            Xem chi tiết rubric
        </button>
        <div class="collapse" th:id="'rubric-' + ${aq.result.resultId} + '-' + ${aq.question.questionId}">
            <ul class="list-unstyled rubric-list small mt-1" th:data-rubric="${aq.aiRubricJson}"></ul>
        </div>
    </div>
</th:block>
```

- [ ] **Step 3: Thêm polling JS**

```javascript
// Tự động reload khi AI chấm xong
(function pollAIResults() {
    var pendingBadge = document.querySelector('.bg-warning.text-dark');
    if (!pendingBadge) return; // Không có pending = không cần poll

    setInterval(function() {
        fetch('/api/v1/student/quiz/result/' + resultId + '/ai-status')
            .then(function(r) { return r.json(); })
            .then(function(data) {
                var allDone = data.data.every(function(q) {
                    return q.aiGradingStatus !== 'PENDING';
                });
                if (allDone) location.reload();
            });
    }, 15000);
})();
```

---

## Chunk 6: Teacher Review — hiển thị AI + override

**Files:**
- Modify: `src/main/resources/templates/teacher/quiz-grading-detail.html`
- Modify: `src/main/java/com/example/DoAn/controller/TeacherQuizGradingController.java`

---

### Task 11: Hiển thị AI score trong teacher review

- [ ] **Step 1: Đọc `teacher/quiz-grading-detail.html`**
- [ ] **Step 2: Thêm block hiển thị AI cho SPEAKING/WRITING**

```html
<th:block th:if="${aq.question.questionType == 'SPEAKING' or aq.question.questionType == 'WRITING'}">
    <div class="border rounded p-2 mb-2 bg-light">
        <div class="d-flex justify-content-between align-items-center">
            <span class="badge bg-info text-dark">AI Grading</span>
            <span th:if="${aq.aiGradingStatus == 'PENDING'}" class="badge bg-warning text-dark">
                Đang chấm...
            </span>
            <span th:if="${aq.aiGradingStatus == 'COMPLETED'}" class="badge bg-success">
                AI: ${aq.aiScore}/${aq.maxPoints}
            </span>
        </div>
        <div th:if="${aq.aiFeedback != null}" class="small text-muted mt-1" th:text="${aq.aiFeedback}"></div>
        <!-- Override input -->
        <div class="mt-2">
            <label class="small fw-bold">Override điểm:</label>
            <input type="number" class="form-control form-control-sm d-inline-block w-25"
                   th:attr="data-answer-id=${aq.answerId}"
                   th:value="${aq.teacherOverrideScore ?: aq.aiScore}"
                   placeholder="Điểm override">
        </div>
    </div>
</th:block>
```

---

### Task 12: API override điểm cho teacher

- [ ] **Step 1: Thêm method trong `TeacherQuizGradingController.java`**

```java
@PostMapping("/override-score")
public ResponseEntity<?> overrideScore(@RequestParam Integer answerId,
                                       @RequestParam String score) {
    QuizAnswer answer = quizAnswerRepository.findById(answerId).orElseThrow();
    answer.setTeacherOverrideScore(score);
    answer.setAiGradingStatus("REVIEWED");
    quizAnswerRepository.save(answer);
    return ResponseEntity.ok(Map.of("status", 200, "message", "Đã cập nhật điểm"));
}
```

---

## Tóm tắt thứ tự thực hiện

```
Chunk 1 → Entity + Database (2 tasks)
Chunk 2 → quiz-take.html UI (3 tasks)
Chunk 3 → Backend API audio (2 tasks)
Chunk 4 → QuizResultService logic (2 tasks)
Chunk 5 → Trang kết quả student + polling (1 task)
Chunk 6 → Teacher review (2 tasks)
```