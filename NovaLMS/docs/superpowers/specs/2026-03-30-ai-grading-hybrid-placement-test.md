# AI Grading cho Hybrid Placement Test — WRITING & SPEAKING

## 1. Mục tiêu

Chấm điểm WRITING và SPEAKING questions trong Entry Test Hybrid Placement bằng AI (Groq LLaMA + Whisper), thay vì hardcode `isCorrect = false`.

## 2. Kiến trúc

### 2.1 Async Flow

```
Student submit
  → Lưu answer (isCorrect=null, pendingAiReview=true)
  → Fire & forget: GroqGradingService.fireAndForget()

[Background Thread]
  → Groq Whisper: transcribe audio (SPEAKING)
  → Groq LLaMA: grade với rubric
  → Update PlacementTestAnswer (score, feedback, rubric breakdown)
  → Update PlacementTestResult (recalculate CEFR)
```

### 2.2 Groq Integration

**Config (application.properties):**
```properties
groq.api.key=gs_xxxxx
groq.api.url=https://api.groq.com/openai/v1
```

**Dependency (pom.xml):**
```xml
<dependency>
    <groupId>com.theokanning.openai-groq</groupId>
    <artifactId>groq-java</artifactId>
    <version>0.2.0</version>
</dependency>
```

## 3. Rubric

### WRITING (4 tiêu chí, mỗi tiêu chí 0 – max/4 điểm)

| Tiêu chí | Mô tả |
|---|---|
| `task_achievement` | Hoàn thành đúng yêu cầu đề bài, đủ độ dài |
| `lexical_resource` | Vốn từ vựng phong phú, phù hợp CEFR level |
| `grammar` | Độ chính xác ngữ pháp, cấu trúc câu |
| `coherence_cohesion` | Liên kết câu/đoạn mạch lạc, từ nối |

### SPEAKING (4 tiêu chí, mỗi tiêu chí 0 – max/4 điểm)

| Tiêu chí | Mô tả |
|---|---|
| `task_achievement` | Trả lời đúng câu hỏi, đủ ý |
| `lexical_resource` | Từ vựng phong phú, phù hợp CEFR level |
| `pronunciation` | Phát âm rõ ràng, đúng trọng âm |
| `fluency` | Lưu loát, tự nhiên, tốc độ phù hợp |

## 4. Database Changes

### PlacementTestAnswer — thêm fields

```java
private Boolean isCorrect;        // null = pending, true/false = graded
private Boolean pendingAiReview;  // true = đang chờ AI
private Integer aiScore;           // điểm AI cho câu này
private String aiFeedback;         // feedback tổng
private String aiRubricJson;      // {"task_achievement": 3, "lexical_resource": 2, "grammar": 3, "coherence_cohesion": 2}
```

### PlacementTestResult — thêm fields

```java
// Tổng điểm và CEFR tính cả WRITING/SPEAKING sau khi AI chấm xong
// (hiện tại score/correctRate chỉ tính MC/FILL/MATCH)
private Integer totalScoreIncludingAi;
private BigDecimal correctRateIncludingAi;
```

## 5. Speaking: Upload Audio

### Frontend (hybrid-quiz.html)

- **Nút ghi âm**: MediaRecorder API → blob → POST `/api/v1/public/placement-test/audio`
- **Nút upload file**: `<input type="file">` → POST `/api/v1/public/placement-test/audio`
- Kết quả: `{ audioUrl: "https://..." }` → gán vào `answers[questionId]`

### Backend

```
POST /api/v1/public/placement-test/audio
  Content-Type: multipart/form-data
  Body: file (mp3/m4a/wav, max 10MB)
  → Validate file type và size
  → Upload lên Cloudinary (đã tích hợp sẵn)
  → Trả về { audioUrl }
```

## 6. GroqGradingService

### Interface

```java
public interface GroqGradingService {
    void fireAndForget(Integer resultId, Integer questionId, String questionType);
}
```

### Implementation

```java
@Transactional
public void fireAndForget(Integer resultId, Integer questionId, String questionType) {
    CompletableFuture.runAsync(() -> {
        PlacementTestAnswer answer = findAnswer(resultId, questionId);
        PlacementTestResult result = answer.getPlacementTestResult();
        Question question = answer.getQuestion();

        String studentAnswer = answer.getAnsweredOptions(); // JSON string

        // SPEAKING: transcribe trước
        if ("SPEAKING".equals(questionType)) {
            String audioUrl = studentAnswer; // đang lưu URL
            String transcript = groqClient.transcribe(audioUrl); // Whisper
            studentAnswer = transcript;
        }

        // Grade bằng LLaMA
        GradingResponse grading = groqClient.gradeWritingOrSpeaking(
            question.getContent(),
            question.getSkill(),       // LISTENING/READING/WRITING/SPEAKING
            question.getCefrLevel(),  // A1–C2
            studentAnswer,
            questionType,
            question.getQuizQuestion().getPoints().intValue()
        );

        // Update answer
        answer.setPendingAiReview(false);
        answer.setAiScore(grading.totalScore);
        answer.setAiFeedback(grading.feedback);
        answer.setAiRubricJson(grading.rubricJson);
        answer.setIsCorrect(grading.totalScore >= grading.maxScore * 0.5); // ≥50% = pass
        answerRepository.save(answer);

        // Recalculate result
        recalculateResult(result);
    });
}
```

### Groq Client

```java
public class GroqClient {
    // Whisper transcription
    public String transcribe(String audioUrl);

    // LLaMA grading
    public GradingResponse gradeWritingOrSpeaking(
        String questionPrompt,
        String skill,
        String cefrLevel,
        String studentAnswer,
        String questionType,
        int maxPoints
    );
}

public class GradingResponse {
    int totalScore;
    int maxScore;
    String feedback;
    String rubricJson; // {"task_achievement": 3, ...}
}
```

### Grading Prompt (LLaMA)

System prompt chứa rubric + examples. User prompt chứa question + student answer. Parse JSON response.

## 7. Frontend — hybrid-results.html

| Trạng thái | Hiển thị |
|---|---|
| `pendingAiReview = true` | ⏳ "Đang chấm bằng AI..." |
| `pendingAiReview = false` | Điểm + feedback + breakdown rubric (accordion/expandable) |
| `isCorrect = null` (legacy) | "—" |

## 8. Files to Change

| File | Changes |
|---|---|
| `PlacementTestAnswer.java` | + fields (isCorrect, pendingAiReview, aiScore, aiFeedback, aiRubricJson) |
| `PlacementTestResult.java` | + fields (totalScoreIncludingAi, correctRateIncludingAi) |
| `pom.xml` | + groq-java dependency |
| `application.properties` | + groq.api.key, groq.api.url |
| `GroqGradingService.java` | NEW — interface |
| `GroqGradingServiceImpl.java` | NEW — async grading logic |
| `GroqClient.java` | NEW — Groq API calls (Whisper + LLaMA) |
| `PlacementTestServiceImpl.java` | Save answer with pendingAiReview=true; call fireAndForget() |
| `PlacementTestControllerNN.java` | + POST /audio endpoint |
| `hybrid-quiz.html` | + record + upload audio buttons |
| `hybrid-results.html` | Display AI grading status + results |
| SQL migration | ALTER TABLE placement_test_answer / placement_test_result |

## 9. Fallback

Nếu Groq API fail (timeout, error): vẫn giữ `isCorrect=false`, `pendingAiReview=false`, hiện message "AI grading unavailable".
