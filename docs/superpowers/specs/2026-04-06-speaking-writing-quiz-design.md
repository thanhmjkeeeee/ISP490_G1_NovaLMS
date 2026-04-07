# SPEC: Ghi âm Speaking & Auto-Grading cho Quiz Course

## 1. Mục tiêu

Thêm chức năng **ghi âm cho câu hỏi SPEAKING** và **auto-grading AI cho SPEAKING + WRITING** vào luồng quiz course của student, dựa trên hạ tầng sẵn có từ entry test.

---

## 2. Luồng xử lý

```
Student làm quiz (quiz-take.html)
    │
    ├── MC / Fill-in-blank / Matching
    │       → Auto-grade ngay khi submit → isCorrect = true/false
    │
    ├── SPEAKING
    │       ├── Student nhấn "Ghi âm" → MediaRecorder ghi âm
    │       ├── Timer speaking đếm ngược (quiz-level setting)
    │       ├── Hết giờ → auto-stop → upload Cloudinary → URL lưu vào answeredOptions
    │       └── Submit → pendingAiReview = true → AI grading async
    │
    └── WRITING
            ├── Student nhập text vào textarea
            └── Submit → pendingAiReview = true → AI grading async

Sau submit:
    ├── MC/Fill-in-blank → hiển thị điểm ngay
    ├── SPEAKING/WRITING → "Đang AI chấm..."
    │       → AI chấm xong → hiển thị điểm + feedback
    └── Teacher override bất kỳ lúc nào
```

---

## 3. Thay đổi Database

### 3.1 Quiz.java — thêm field

```java
// Thời gian giới hạn ghi âm cho MỖI câu speaking (giây)
// ví dụ: 120 = 2 phút/câu. null = không giới hạn.
private Integer speakingTimeLimitSeconds;
```

### 3.2 QuizAnswer.java — thêm 3 field

```java
private String audioUrl;              // Cloudinary URL cho SPEAKING
private String aiGradingStatus;        // PENDING | COMPLETED | REVIEWED
private String teacherOverrideScore;  // null = dùng điểm AI, có giá trị = override
```

---

## 4. Frontend: quiz-take.html

### 4.1 Thêm audio UI cho SPEAKING

Copy từ `hybrid-quiz.html`:

```html
<div th:if="${question.questionType == 'SPEAKING'}" class="mb-3">
  <label class="form-label fw-bold">Ghi âm câu trả lời của bạn</label>
  <div class="d-flex gap-2 align-items-center mb-2">
    <button type="button" class="btn btn-outline-danger btn-sm" id="recordBtn"
            onclick="toggleRecording()">
      <i class="bi bi-mic"></i> Ghi âm
    </button>
    <label class="btn btn-outline-secondary btn-sm mb-0">
      <i class="bi bi-upload"></i> Tải lên file
      <input type="file" accept="audio/*" class="d-none" id="audioUpload"
             onchange="handleAudioUpload(this)">
    </label>
  </div>
  <div id="audioStatus" class="small text-muted"></div>
  <audio id="audioPlayback" controls class="d-none w-100 mt-2"></audio>
  <input type="hidden"
         th:id="'answer_' + ${question.questionId}"
         th:name="'q_' + ${question.questionId}"
         value="">
</div>
```

### 4.2 Timer speaking per-question

- Mỗi câu SPEAKING hiển thị countdown badge: `⏱ 02:00`
- Timer bắt đầu khi student bắt đầu ghi âm
- Hết giờ → `mediaRecorder.stop()` → upload blob → set hidden field → auto-submit câu đó
- Thời gian được đọc từ `speakingTimeLimitSeconds` (quiz-level)

### 4.3 Audio upload API

```javascript
// POST /api/v1/student/quiz/audio
async function uploadRecordedAudio(blob) {
  const formData = new FormData();
  formData.append('file', blob, 'recording.webm');
  const res = await fetch('/api/v1/student/quiz/audio', {
    method: 'POST', body: formData
  });
  const result = await res.json();
  if (result.status === 200 && result.audioUrl) {
    // Set hidden answer field
    const input = document.querySelector('[data-speaking-question]');
    if (input) input.value = result.audioUrl;
  }
}
```

---

## 5. Backend: StudentQuizTakingController

### 5.1 Upload audio

```java
@PostMapping("/audio")
public ResponseEntity<?> uploadAudio(@RequestParam("file") MultipartFile file) {
    String audioUrl = fileUploadService.uploadFile(file); // Cloudinary, resource_type=auto
    return ResponseEntity.ok(Map.of("status", 200, "audioUrl", audioUrl));
}
```

### 5.2 Lấy AI grading status

```java
@GetMapping("/result/{resultId}/ai-status")
public ResponseEntity<?> getAiStatus(@PathVariable Integer resultId) {
    List<Map<String, Object>> answers = quizAnswerRepository.findByResultResultId(resultId)
        .stream()
        .filter(a -> "WRITING".equals(a.getQuestion().getQuestionType())
                  || "SPEAKING".equals(a.getQuestion().getQuestionType()))
        .map(a -> Map.of(
            "questionId", a.getQuestion().getQuestionId(),
            "aiGradingStatus", a.getAiGradingStatus() != null ? a.getAiGradingStatus() : "PENDING",
            "aiScore", a.getAiScore(),
            "aiFeedback", a.getAiFeedback(),
            "aiRubricJson", a.getAiRubricJson()
        ))
        .toList();
    return ResponseEntity.ok(answers);
}
```

---

## 6. Backend: QuizResultServiceImpl

### 6.1 SPEAKING — lưu audioUrl

```java
if ("SPEAKING".equals(questionType)) {
    quizAnswer.setAudioUrl(answeredOptions); // Cloudinary URL
}
```

### 6.2 AI grading async — giữ nguyên

Logic `fireAndForgetForQuizAnswer()` đã có sẵn. Sau khi AI chấm xong:

```java
answer.setAiGradingStatus("COMPLETED");
answerRepository.save(answer);
```

---

## 7. Trang kết quả student

### 7.1 Hiển thị pending

```html
<th:block th:if="${aq.aiGradingStatus == 'PENDING'}">
  <span class="badge bg-warning text-dark">
    <i class="bi bi-hourglass-split"></i> Đang AI chấm...
  </span>
</th:block>
```

### 7.2 Hiển thị kết quả AI

```html
<th:block th:if="${aq.aiGradingStatus == 'COMPLETED'}">
  <span th:text="${aq.aiScore}">8</span>/<span th:text="${aq.maxPoints}">10</span>
  <p class="small text-muted" th:text="${aq.aiFeedback}">AI feedback...</p>
  <!-- Rubric breakdown (collapsible) -->
</th:block>
```

### 7.3 Polling

```javascript
setInterval(function() {
  fetch('/api/v1/student/quiz/result/' + resultId + '/ai-status')
    .then(r => r.json())
    .then(data => {
      var allDone = data.every(q => q.aiGradingStatus !== 'PENDING');
      if (allDone) location.reload();
    });
}, 15000);
```

---

## 8. Teacher Review

### 8.1 Hiển thị điểm AI

Mở rộng `teacher/quiz-grading-detail.html`:

- Hiển thị `aiScore` + `aiFeedback` + `aiRubricJson` cho SPEAKING/WRITING
- Badge màu: vàng = PENDING, xanh = COMPLETED, xanh đậm = REVIEWED

### 8.2 Override điểm

- Teacher nhập điểm override → lưu vào `teacherOverrideScore`
- `aiGradingStatus = 'REVIEWED'`
- Điểm hiển thị cho student = `teacherOverrideScore ?? aiScore`

---

## 9. Tái sử dụng từ Entry Test

| Thành phần | File nguồn | Tái sử dụng |
|---|---|---|
| Audio recording UI | `hybrid-quiz.html` | Copy vào `quiz-take.html` |
| MediaRecorder JS | `hybrid-quiz.html` | Copy vào `quiz-take.html` |
| Audio upload endpoint | `PlacementTestControllerNN` | Tạo mới cho student-quiz |
| GroqClient transcription | `GroqClient.java` | Tái sử dụng |
| GroqClient grading | `GroqClient.java` | Tái sử dụng |
| Fire-and-forget async | `GroqGradingServiceImpl` | Tái sử dụng |
| Rubric display JS | `hybrid-results.html` | Copy vào trang kết quả |

---

## 10. Tóm tắt files cần thay đổi

| File | Thay đổi |
|---|---|
| `model/Quiz.java` | + speakingTimeLimitSeconds |
| `model/QuizAnswer.java` | + audioUrl, aiGradingStatus, teacherOverrideScore |
| `repository/QuizAnswerRepository.java` | + findByResultResultId |
| `templates/student/quiz-take.html` | + audio recording UI + timer JS |
| `controller/StudentQuizTakingController.java` | + upload audio API + ai-status API |
| `controller/StudentQuizApiController.java` | Tái sử dụng submit logic |
| `service/impl/QuizResultServiceImpl.java` | + lưu audioUrl, aiGradingStatus |
| `service/impl/GroqGradingServiceImpl.java` | + cập nhật aiGradingStatus = COMPLETED |
| `templates/student/quiz-result.html` | + hiển thị AI pending/completed + polling |
| `templates/teacher/quiz-grading-detail.html` | + AI score display + override |
| `controller/TeacherQuizGradingController.java` | + override logic |
