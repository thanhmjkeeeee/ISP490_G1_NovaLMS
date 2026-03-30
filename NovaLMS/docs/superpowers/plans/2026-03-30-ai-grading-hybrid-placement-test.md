# AI Grading Hybrid Placement Test — Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Chấm điểm WRITING/SPEAKING trong Entry Test Hybrid bằng Groq AI (Whisper + LLaMA), async, không block user.

**Architecture:** GroqGradingService.fireAndForget() chạy async sau khi student submit. Whisper transcribe audio (SPEAKING), LLaMA grade với rubric 4 tiêu chí. Kết quả update PlacementTestAnswer + recalculate PlacementTestResult.

**Tech Stack:** Groq API (LLaMA 4 Scout + Whisper Large), CompletableFuture, Spring Boot, MediaRecorder API (browser)

---

## Chunk 1: Database & Entity Changes

### Task 1: Thêm fields vào PlacementTestAnswer.java

**Files:**
- Modify: `NovaLMS\DoAn\src\main\java\com\example\DoAn\model\PlacementTestAnswer.java`

- [ ] **Step 1: Đọc file PlacementTestAnswer.java**

Tìm dòng cuối cùng của các field, trước `@PrePersist`.

- [ ] **Step 2: Thêm 4 fields mới sau `answeredOptions`**

```java
// AI Grading
@Column(name = "pending_ai_review")
@Builder.Default
private Boolean pendingAiReview = false;

@Column(name = "ai_score")
private Integer aiScore;

@Column(name = "ai_feedback", columnDefinition = "TEXT")
private String aiFeedback;

@Column(name = "ai_rubric_json", columnDefinition = "TEXT")
private String aiRubricJson;
```

---

### Task 2: Thêm fields vào PlacementTestResult.java

**Files:**
- Modify: `NovaLMS\DoAn\src\main\java\com\example\DoAn\model\PlacementTestResult.java`

- [ ] **Step 1: Đọc file — tìm dòng cuối field trước `@PrePersist`**

- [ ] **Step 2: Thêm 2 fields sau `correctRate`**

```java
// AI-aware scoring (WRITING/SPEAKING included after AI grading)
@Column(name = "total_score_including_ai")
private Integer totalScoreIncludingAi;

@Column(name = "correct_rate_including_ai", precision = 5, scale = 2)
private BigDecimal correctRateIncludingAi;
```

---

### Task 3: ALTER TABLE SQL

**Files:**
- Chạy trực tiếp trên MySQL console hoặc via IDE

```sql
ALTER TABLE placement_test_answer
  ADD COLUMN pending_ai_review BOOLEAN DEFAULT FALSE,
  ADD COLUMN ai_score INT,
  ADD COLUMN ai_feedback TEXT,
  ADD COLUMN ai_rubric_json TEXT;

ALTER TABLE placement_test_result
  ADD COLUMN total_score_including_ai INT,
  ADD COLUMN correct_rate_including_ai DECIMAL(5,2);
```

---

## Chunk 2: Groq Client — API Integration

### Task 4: Tạo GroqClient.java

**Files:**
- Create: `NovaLMS\DoAn\src\main\java\com\example\DoAn\service\GroqClient.java`

- [ ] **Step 1: Tạo file**

```java
package com.example.DoAn.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
public class GroqClient {

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    private final WebClient webClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public GroqClient() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.groq.com/openai/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    /**
     * Transcribe audio via Whisper Large V3
     * audioUrl: direct URL to audio file (mp3/m4a/wav)
     */
    public String transcribe(String audioUrl) {
        try {
            Map<String, Object> body = Map.of("model", "whisper-large-v3", "audio", audioUrl);
            JsonNode response = webClient.post()
                    .uri("/audio/transcriptions")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();
            return response.path("text").asText("");
        } catch (Exception e) {
            log.error("Groq Whisper transcription failed: {}", e.getMessage());
            throw new RuntimeException("Transcription failed: " + e.getMessage(), e);
        }
    }

    /**
     * Grade WRITING or SPEAKING answer via LLaMA 4 Scout
     * Returns JSON: {"totalScore": 8, "maxScore": 10, "feedback": "...", "rubric": {...}}
     */
    public GradingResponse gradeWritingOrSpeaking(
            String questionPrompt,
            String skill,
            String cefrLevel,
            String studentAnswer,
            String questionType,
            int maxPoints
    ) {
        String rubricJson = buildRubricJson(questionType, maxPoints);
        String systemPrompt = buildSystemPrompt(questionType, rubricJson);
        String userPrompt = buildUserPrompt(questionPrompt, skill, cefrLevel, studentAnswer, maxPoints);

        try {
            Map<String, Object> body = Map.of(
                "model", "llama-4-scout-17b-16e-instruct",
                "messages", java.util.List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.3,
                "max_tokens", 1024
            );

            JsonNode response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();

            String content = response.path("choices").get(0).path("message").path("content").asText();
            // Strip markdown code blocks if present
            content = content.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            return mapper.readValue(content, GradingResponse.class);
        } catch (Exception e) {
            log.error("Groq LLaMA grading failed: {}", e.getMessage());
            throw new RuntimeException("Grading failed: " + e.getMessage(), e);
        }
    }

    // ─── Private helpers ───────────────────────────────────────────

    private String buildRubricJson(String questionType, int maxPoints) {
        int perCriterion = maxPoints / 4;
        if ("WRITING".equals(questionType)) {
            return String.format("""
                {
                  "task_achievement": {"max": %d, "desc": "Hoàn thành đúng yêu cầu đề bài, đủ độ dài"},
                  "lexical_resource": {"max": %d, "desc": "Vốn từ vựng phong phú, phù hợp CEFR"},
                  "grammar": {"max": %d, "desc": "Độ chính xác ngữ pháp, cấu trúc câu"},
                  "coherence_cohesion": {"max": %d, "desc": "Liên kết câu/đoạn mạch lạc, từ nối"}
                }
                """, perCriterion, perCriterion, perCriterion, perCriterion);
        } else {
            return String.format("""
                {
                  "task_achievement": {"max": %d, "desc": "Trả lời đúng câu hỏi, đủ ý"},
                  "lexical_resource": {"max": %d, "desc": "Từ vựng phong phú, phù hợp CEFR"},
                  "pronunciation": {"max": %d, "desc": "Phát âm rõ ràng, đúng trọng âm"},
                  "fluency": {"max": %d, "desc": "Lưu loát, tự nhiên, tốc độ phù hợp"}
                }
                """, perCriterion, perCriterion, perCriterion, perCriterion);
        }
    }

    private String buildSystemPrompt(String questionType, String rubricJson) {
        if ("WRITING".equals(questionType)) {
            return String.format("""
                Bạn là giáo viên tiếng Anh chấm bài WRITING theo rubric CEFR.
                Rubric: %s
                Hãy chấm bài dựa trên 4 tiêu chí trên.
                Trả về DUY NHẤT JSON, không có markdown nào khác:
                {
                  "totalScore": <tổng điểm>,
                  "maxScore": <tổng điểm tối đa>,
                  "feedback": "<nhận xét ngắn 2-3 câu bằng tiếng Việt>",
                  "rubric": {
                    "task_achievement": <điểm 0-%d>,
                    "lexical_resource": <điểm 0-%d>,
                    "grammar": <điểm 0-%d>,
                    "coherence_cohesion": <điểm 0-%d>
                  }
                }
                """, rubricJson, maxPoints/4, maxPoints/4, maxPoints/4, maxPoints/4);
        } else {
            return String.format("""
                Bạn là giáo viên tiếng Anh chấm bài SPEAKING theo rubric CEFR.
                Rubric: %s
                Hãy chấm bài dựa trên 4 tiêu chí trên.
                Trả về DUY NHẤT JSON, không có markdown nào khác:
                {
                  "totalScore": <tổng điểm>,
                  "maxScore": <tổng điểm tối đa>,
                  "feedback": "<nhận xét ngắn 2-3 câu bằng tiếng Việt>",
                  "rubric": {
                    "task_achievement": <điểm 0-%d>,
                    "lexical_resource": <điểm 0-%d>,
                    "pronunciation": <điểm 0-%d>,
                    "fluency": <điểm 0-%d>
                  }
                }
                """, rubricJson, maxPoints/4, maxPoints/4, maxPoints/4, maxPoints/4);
        }
    }

    private String buildUserPrompt(String prompt, String skill, String cefr, String answer, int maxPoints) {
        return String.format("""
            Đề bài: %s
            Skill: %s | CEFR Level: %s
            Câu trả lời của học sinh:
            %s
            Hãy chấm và trả về JSON theo rubric.
            """, prompt, skill, cefr, answer);
    }
}
```

- [ ] **Step 2: Tạo GradingResponse.java (DTO)**

**Files:**
- Create: `NovaLMS\DoAn\src\main\java\com\example\DoAn\dto\GradingResponse.java`

```java
package com.example.DoAn.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradingResponse {
    private int totalScore;
    private int maxScore;
    private String feedback;
    private Map<String, Integer> rubric;  // {"task_achievement": 3, ...}
}
```

---

## Chunk 3: GroqGradingService — Async Fire-and-Forget

### Task 5: Tạo GroqGradingService interface

**Files:**
- Create: `NovaLMS\DoAn\src\main\java\com\example\DoAn\service\GroqGradingService.java`

```java
package com.example.DoAn.service;

public interface GroqGradingService {
    /**
     * Fire-and-forget: grade WRITING/SPEAKING answer asynchronously.
     * Does NOT block the HTTP response. Runs in background thread.
     */
    void fireAndForget(Integer placementResultId, Integer questionId, String questionType);
}
```

---

### Task 6: Tạo GroqGradingServiceImpl

**Files:**
- Create: `NovaLMS\DoAn\src\main\java\com\example\DoAn\service\impl\GroqGradingServiceImpl.java`

- [ ] **Step 1: Tạo file**

```java
package com.example.DoAn.service.impl;

import com.example.DoAn.dto.GradingResponse;
import com.example.DoAn.model.*;
import com.example.DoAn.repository.*;
import com.example.DoAn.service.GroqClient;
import com.example.DoAn.service.GroqGradingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroqGradingServiceImpl implements GroqGradingService {

    private final GroqClient groqClient;
    private final PlacementTestAnswerRepository answerRepository;
    private final PlacementTestResultRepository resultRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Async
    public void fireAndForget(Integer placementResultId, Integer questionId, String questionType) {
        try {
            CompletableFuture.runAsync(() -> grade(placementResultId, questionId, questionType));
        } catch (Exception e) {
            log.error("Failed to fire async grading for result={}, question={}: {}",
                placementResultId, questionId, e.getMessage());
        }
    }

    @Transactional
    public void grade(Integer placementResultId, Integer questionId, String questionType) {
        PlacementTestAnswer answer = answerRepository.findByPlacementResultIdAndQuestionQuestionId(
            placementResultId, questionId
        ).orElse(null);
        if (answer == null) {
            log.warn("Answer not found: result={}, question={}", placementResultId, questionId);
            return;
        }

        PlacementTestResult result = answer.getPlacementTestResult();
        Question question = answer.getQuestion();
        String studentAnswer = answer.getAnsweredOptions();

        try {
            // SPEAKING: transcribe audio first
            if ("SPEAKING".equals(questionType)) {
                log.info("Transcribing SPEAKING audio for answer={}", answer.getId());
                String transcript = groqClient.transcribe(studentAnswer);
                studentAnswer = transcript;
            }

            // Grade with LLaMA
            log.info("Grading {} for answer={}", questionType, answer.getId());
            GradingResponse grading = groqClient.gradeWritingOrSpeaking(
                question.getContent(),
                question.getSkill(),
                question.getCefrLevel(),
                studentAnswer,
                questionType,
                question.getQuizQuestion().getPoints().intValue()
            );

            // Update answer
            answer.setPendingAiReview(false);
            answer.setAiScore(grading.getTotalScore());
            answer.setAiFeedback(grading.getFeedback());
            answer.setAiRubricJson(objectMapper.writeValueAsString(grading.getRubric()));
            answer.setIsCorrect(grading.getTotalScore() >= grading.getMaxScore() * 0.5);
            answerRepository.save(answer);

            // Recalculate result
            recalculateResult(result);

            log.info("AI grading done for answer={}, score={}/{}", answer.getId(), grading.getTotalScore(), grading.getMaxScore());

        } catch (Exception e) {
            log.error("AI grading failed for answer={}: {}", answer.getId(), e.getMessage());
            // Fallback: mark as failed but don't crash
            answer.setPendingAiReview(false);
            answer.setAiFeedback("AI grading unavailable. Please contact support.");
            answer.setIsCorrect(false);
            answerRepository.save(answer);
        }
    }

    private void recalculateResult(PlacementTestResult result) {
        List<PlacementTestAnswer> answers = answerRepository.findByPlacementTestResultId(result.getId());

        int totalScore = 0;
        int maxScore = 0;

        for (PlacementTestAnswer a : answers) {
            QuizQuestion qq = a.getQuestion().getQuizQuestion();
            int pts = qq != null && qq.getPoints() != null ? qq.getPoints().intValue() : 1;

            if ("WRITING".equals(a.getQuestion().getQuestionType()) ||
                "SPEAKING".equals(a.getQuestion().getQuestionType())) {
                // AI-graded: use aiScore
                if (a.getAiScore() != null) {
                    totalScore += a.getAiScore();
                    maxScore += pts;
                }
                // If still pending, skip this question
            } else {
                // MC/FILL/MATCH: use isCorrect
                if (Boolean.TRUE.equals(a.getIsCorrect())) {
                    totalScore += pts;
                }
                maxScore += pts;
            }
        }

        BigDecimal rate = maxScore > 0
            ? BigDecimal.valueOf(100.0 * totalScore / maxScore).setScale(2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        result.setTotalScoreIncludingAi(totalScore);
        result.setCorrectRateIncludingAi(rate);
        result.setSuggestedLevel(calculateCEFR(rate.doubleValue()));
        resultRepository.save(result);
    }

    private String calculateCEFR(double rate) {
        if (rate <= 20) return "A1";
        if (rate <= 40) return "A2";
        if (rate <= 60) return "B1";
        if (rate <= 75) return "B2";
        if (rate <= 90) return "C1";
        return "C2";
    }
}
```

---

## Chunk 4: Modify PlacementTestServiceImpl — Hook AI Grading

### Task 7: Hook fireAndForget vào submitPlacementTest()

**Files:**
- Modify: `NovaLMS\DoAn\src\main\java\com\example\DoAn\service\impl\PlacementTestServiceImpl.java`

Tìm block xử lý WRITING/SPEAKING trong `submitPlacementTest()` (hiện tại hardcode `isCorrect=false`). Thay thế:

- [ ] **Step 1: Đọc submitPlacementTest() — tìm đoạn WRITING/SPEAKING (lines ~181-186)**

```java
if ("WRITING".equals(qType) || "SPEAKING".equals(qType)) {
    isCorrect = false;  // ← THAY THẾ
}
```

- [ ] **Step 2: Thay thế toàn bộ block**

```java
if ("WRITING".equals(qType) || "SPEAKING".equals(qType)) {
    // Lưu answer với pendingAiReview = true
    answer.setPendingAiReview(true);
    // isCorrect = null (chưa có kết quả)
    // Ai không block HTTP — fire async
    groqGradingService.fireAndForget(
        savedResult.getId(),
        question.getQuestionId(),
        qType
    );
}
```

- [ ] **Step 3: Thêm import**

```java
private final GroqGradingService groqGradingService;
```

- [ ] **Step 4: Trong block tính score — bỏ qua WRITING/SPEAKING khi chưa có AI result**

Tìm đoạn `if (Boolean.TRUE.equals(isCorrect)) { score += ... }` và thêm:

```java
// WRITING/SPEAKING chưa có AI result → chưa tính vào score
if (("WRITING".equals(qType) || "SPEAKING".equals(qType)) {
    // skip — sẽ recalculate sau khi AI chấm xong
} else {
    totalGradedQuestions++;
    if (Boolean.TRUE.equals(isCorrect)) {
        score += qq.getPoints() != null ? qq.getPoints().intValue() : 1;
    }
}
```

Tương tự cho block tính `maxScoreAvailable`:

```java
// WRITING/SPEAKING: chưa tính vào maxScoreAvailable (sẽ tính sau AI)
if (!"WRITING".equals(qType) && !"SPEAKING".equals(qType)) {
    maxScoreAvailable += qq.getPoints() != null ? qq.getPoints().intValue() : 1;
}
```

---

## Chunk 5: Audio Upload Endpoint

### Task 8: Thêm POST /audio endpoint

**Files:**
- Modify: `NovaLMS\DoAn\src\main\java\com\example\DoAn\controller\PlacementTestControllerNN.java`

- [ ] **Step 1: Đọc file — tìm chỗ thêm endpoint**

Thêm sau `submitPlacementTest` endpoint:

```java
@PostMapping("/audio")
@ResponseBody
public ResponseEntity<?> uploadAudio(
        @RequestParam("file") MultipartFile file) {
    try {
        // Validate
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("status", 400, "message", "File is empty"));
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("audio/")) {
            return ResponseEntity.badRequest()
                .body(Map.of("status", 400, "message", "Only audio files allowed"));
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            return ResponseEntity.badRequest()
                .body(Map.of("status", 400, "message", "File too large (max 10MB)"));
        }

        // Upload to Cloudinary (đã có sẵn trong project)
        String audioUrl = cloudinaryService.upload(file.getBytes(), file.getContentType(), "audio");

        return ResponseEntity.ok(Map.of(
            "status", 200,
            "audioUrl", audioUrl
        ));
    } catch (Exception e) {
        return ResponseEntity.internalServerError()
            .body(Map.of("status", 500, "message", "Upload failed: " + e.getMessage()));
    }
}
```

- [ ] **Step 2: Thêm import**

```java
import org.springframework.web.multipart.MultipartFile;
```

Tìm `CloudinaryService` đã có sẵn trong project (inject vào controller).

---

## Chunk 6: Frontend — Audio UI trong hybrid-quiz.html

### Task 9: Thêm ghi âm + upload audio buttons

**Files:**
- Modify: `NovaLMS\DoAn\src\main\resources\templates\public\hybrid-quiz.html`

Tìm nơi hiển thị WRITING/SPEAKING question type trong quiz taking page. Thêm UI cho SPEAKING question type:

- [ ] **Step 1: Thêm HTML cho SPEAKING**

Trong phần hiển thị câu hỏi, thêm block cho `questionType === 'SPEAKING'`:

```html
<!-- SPEAKING: record or upload audio -->
<div class="mb-3" th:if="${question.questionType == 'SPEAKING'}">
    <label class="form-label fw-bold">Ghi âm câu trả lời của bạn</label>

    <div class="d-flex gap-2 align-items-center mb-2">
        <!-- Ghi âm -->
        <button type="button" class="btn btn-outline-danger btn-sm" id="recordBtn" onclick="toggleRecording()">
            <i class="bi bi-mic"></i> Ghi âm
        </button>
        <!-- Upload file -->
        <label class="btn btn-outline-secondary btn-sm mb-0">
            <i class="bi bi-upload"></i> Tải lên file
            <input type="file" accept="audio/*" class="d-none" id="audioUpload" onchange="handleAudioUpload(this)">
        </label>
    </div>

    <div id="audioStatus" class="small text-muted"></div>
    <audio id="audioPlayback" controls class="d-none w-100 mt-2"></audio>

    <!-- Hidden input chứa audio URL để submit -->
    <input type="hidden" th:id="'answer_' + ${question.questionId}" value="">
</div>
```

- [ ] **Step 2: Thêm JavaScript functions**

```javascript
let mediaRecorder;
let audioChunks = [];

async function toggleRecording() {
    const btn = document.getElementById('recordBtn');
    if (!mediaRecorder || mediaRecorder.state === 'inactive') {
        // Start recording
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        mediaRecorder = new MediaRecorder(stream);
        audioChunks = [];
        mediaRecorder.ondataavailable = e => audioChunks.push(e.data);
        mediaRecorder.onstop = uploadRecordedAudio;
        mediaRecorder.start();
        btn.innerHTML = '<i class="bi bi-stop-fill"></i> Dừng';
        btn.classList.replace('btn-outline-danger', 'btn-danger');
    } else {
        mediaRecorder.stop();
        mediaRecorder.stream.getTracks().forEach(t => t.stop());
        btn.innerHTML = '<i class="bi bi-mic"></i> Ghi âm';
        btn.classList.replace('btn-danger', 'btn-outline-danger');
    }
}

function uploadRecordedAudio() {
    const blob = new Blob(audioChunks, { type: 'audio/webm' });
    const file = new File([blob], 'recording.webm', { type: 'audio/webm' });
    uploadAudioFile(file);
}

async function handleAudioUpload(input) {
    const file = input.files[0];
    if (!file) return;
    await uploadAudioFile(file);
}

async function uploadAudioFile(file) {
    const formData = new FormData();
    formData.append('file', file);
    try {
        const res = await fetch('/api/v1/public/placement-test/audio', {
            method: 'POST',
            body: formData
        });
        const result = await res.json();
        if (result.status === 200 && result.audioUrl) {
            const audioUrl = result.audioUrl;
            const statusEl = document.getElementById('audioStatus');
            statusEl.textContent = '✅ Đã tải lên thành công!';
            const playback = document.getElementById('audioPlayback');
            playback.src = audioUrl;
            playback.classList.remove('d-none');

            // Set hidden answer input
            const questionId = /* questionId from Thymeleaf */;
            document.getElementById('answer_' + questionId).value = audioUrl;
        } else {
            alert('Upload thất bại: ' + result.message);
        }
    } catch (e) {
        alert('Upload thất bại!');
    }
}
```

---

## Chunk 7: Frontend — hybrid-results.html AI Grading Display

### Task 10: Hiển thị AI grading status + results

**Files:**
- Modify: `NovaLMS\DoAn\src\main\resources\templates\public\hybrid-results.html`

- [ ] **Step 1: Đọc file — tìm chỗ hiển thị từng section (nơi có `section.skill`)**

Tìm block hiển thị điểm từng phần. Thêm display cho AI-graded questions:

- [ ] **Step 2: Thêm display logic trong vòng lặp sections**

Trong block mỗi section, thêm:

```html
<!-- WRITING or SPEAKING: show AI grading -->
<th:block th:if="${section.questionType == 'WRITING' or section.questionType == 'SPEAKING'}">
    <td colspan="2">
        <th:block th:if="${section.pendingAiReview}">
            <div class="alert alert-warning py-1 px-2 mb-0 small">
                <i class="bi bi-hourglass-split me-1"></i> Đang chấm bằng AI...
            </div>
        </th:block>
        <th:block th:if="${!section.pendingAiReview and section.aiScore != null}">
            <div class="small">
                <div class="mb-1">
                    <span class="badge bg-primary me-1" th:text="${section.aiScore + '/' + section.maxPoints}">8/10</span>
                    <span th:class="${section.isCorrect ? 'text-success' : 'text-danger'}"
                          th:text="${section.isCorrect ? '✅ Đạt' : '❌ Chưa đạt'}">Đạt</span>
                </div>
                <p class="mb-1 text-muted small" th:text="${section.aiFeedback}">Feedback...</p>
                <!-- Rubric breakdown -->
                <button class="btn btn-link btn-sm p-0" type="button"
                        data-bs-toggle="collapse"
                        th:attr="data-bs-target='#rubric-' + ${section.resultId}">
                    <i class="bi bi-chevron-down"></i> Chi tiết rubric
                </button>
                <div class="collapse mt-1" th:id="'rubric-' + ${section.resultId}">
                    <th:block th:if="${section.aiRubricJson != null}">
                        <!-- rubric rendered by JS from JSON -->
                        <ul class="list-unstyled mb-0 small" th:id="'rubric-list-' + ${section.resultId}"></ul>
                    </th:block>
                </div>
            </div>
        </th:block>
    </td>
</th:block>
```

- [ ] **Step 3: Thêm JavaScript để parse rubric JSON → display**

```javascript
// Parse aiRubricJson và render rubric breakdown
document.querySelectorAll('[id^="rubric-list-"]').forEach(el => {
    const resultId = el.id.replace('rubric-list-', '');
    // Get the JSON from data attribute or hidden element
    // Render as list items based on questionType (WRITING vs SPEAKING)
});
```

---

## Verification Checklist

- [ ] Restart server
- [ ] Tạo Entry Test Hybrid có WRITING question
- [ ] Submit quiz → answer lưu với `pendingAiReview=true`
- [ ] Groq grading async → answer updated với `aiScore`, `aiFeedback`, `aiRubricJson`
- [ ] `/hybrid-entry` → làm quiz SPEAKING → ghi âm + upload → submit → AI transcribe + grade
- [ ] `/hybrid-results` → hiển thị đúng pending/graded state
