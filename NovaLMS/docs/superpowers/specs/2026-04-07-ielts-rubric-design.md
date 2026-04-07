# SPEC 007: IELTS Rubric + Detailed Grading UI

**Date:** 2026-04-07
**Status:** Approved
**Author:** Claude Code
**Parent Feature:** AI Grading (WRITING/SPEAKING)

---

## 1. Problem

The current AI grading system for WRITING/SPEAKING questions:

1. **Rubric is too vague** — only 4 criteria with generic `desc` and no band descriptors. AI scoring is inconsistent and not transparent.
2. **UI shows raw JSON** — `{"task_achievement": 3, ...}` displayed in `<pre>` tags, making it unreadable for students and teachers.

---

## 2. Goals

1. Grade WRITING/SPEAKING answers using the **official IELTS 9-band scale** (0–9, 0.5 increments).
2. Provide **5-band descriptors** per criterion (matching IELTS official format) so AI must justify each score.
3. Display rubric as a **structured breakdown with progress bars** on both student and teacher UI.
4. **Backward compatible** — existing `aiRubricJson` records remain readable (raw JSON fallback).

---

## 3. Design

### 3.1 IELTS 9-Band Rubric

#### WRITING Task — 4 Criteria × 9 Bands

| Criterion | Bands |
|---|---|
| **Task Achievement** | 0–9 |
| **Lexical Resource** | 0–9 |
| **Grammatical Range & Accuracy** | 0–9 |
| **Coherence & Cohesion** | 0–9 |

#### SPEAKING Task — 4 Criteria × 9 Bands

| Criterion | Bands |
|---|---|
| **Fluency & Cohesion** | 0–9 |
| **Lexical Resource** | 0–9 |
| **Grammatical Range & Accuracy** | 0–9 |
| **Pronunciation** | 0–9 |

#### Score Conversion

```
Overall Band (0–9) = Sum of 4 criteria / 4
Display Score (thang 10 hoặc 20) = Overall Band × 2.5
VD: Band 7.0 × 2.5 = 17.5/20 → hiển thị 8.75/10
```

#### Band Descriptors (viết tắt — đầy đủ trong code)

| Band | Label (VI) | Mô tả (EN) |
|---|---|---|
| 9 | Expert User | Fullflexible, precise, natural |
| 8 | Very Good User | Fluent, wide vocabulary, minor lexical errors |
| 7 | Good User | Handles complex language well, generally effective |
| 6 | Competent User | Effective, some errors but overall OK |
| 5 | Modest User | Partially effective, noticeable errors |
| 4 | Limited User | Frequent errors, limited range |
| 3 | Extremely Limited User | Can communicate in familiar contexts only |
| 2 | Intermittent User | Very limited, frequent breakdowns |
| 1 | Non User | Cannot use language effectively |
| 0 | Did not attempt | No assessable language |

---

### 3.2 Backend Changes

#### `GradingResponse.java` — new structure

```java
@Data @Builder
class RubricCriterion {
    double score;             // 0–9 (có 0.5)
    double max;               // always 9
    String bandLabel;         // VD: "Band 7.5"
    String bandDescription;   // mô tả band đạt được (tiếng Anh)
    String aiReasoning;      // giải thích ngắn bằng tiếng Việt
}

@Data
class GradingResponse {
    double overallBand;                     // 0–9, VD: 6.875
    double displayScore;                     // overallBand × 2.5
    double maxScore;                        // 10 hoặc 20
    String feedback;                        // nhận xét tổng bằng tiếng Việt
    String overallBandDescriptor;          // VD: "Good User (7.0)"
    Map<String, RubricCriterion> rubric;  // 4 tiêu chí
}
```

#### `GroqClient.java` — prompt changes

1. **`buildRubricJson()`**: Build full 9-band descriptor JSON per criterion
2. **`buildSystemPrompt()`**: Instruct LLaMA to return structured JSON with band justification per criterion
3. **`gradeWritingOrSpeaking()`**: Parse new response structure, compute `overallBand` and `displayScore`

**Rubric JSON format gửi cho AI (WRITING example):**

```json
{
  "task_achievement": {
    "max": 9,
    "bands": {
      "0": "Content is irrelevant or incomprehensible",
      "1": "Barely communicates the task",
      "2": "Partially addresses the task",
      "3": "Task not fully achieved",
      "4": "Suggests argument but insufficiently developed",
      "5": "Presents some relevant ideas but development is limited",
      "6": "Addresses the task adequately, some development",
      "7": "Covers requirements, well-developed, clear progression",
      "8": "Satisfies all requirements, very good development",
      "9": "Fully addresses all task requirements with complete clarity"
    }
  },
  "lexical_resource": { ... },
  "grammatical_range": { ... },
  "coherence_cohesion": { ... }
}
```

**Response JSON format mong đợi:**

```json
{
  "overallBand": 6.5,
  "displayScore": 16.25,
  "maxScore": 20,
  "feedback": "Bài viết đáp ứng tốt yêu cầu đề bài. Cần cải thiện vocabulary và grammar.",
  "overallBandDescriptor": "Competent User (6.5)",
  "rubric": {
    "task_achievement": {
      "score": 7.0, "max": 9, "bandLabel": "Band 7.0",
      "bandDescription": "Covers requirements, well-developed, clear progression",
      "aiReasoning": "Bạn trình bày luận điểm chính rõ ràng, có ví dụ minh họa phù hợp..."
    },
    "lexical_resource": { "score": 6.0, ... },
    "grammatical_range": { "score": 6.5, ... },
    "coherence_cohesion": { "score": 6.5, ... }
  }
}
```

#### `GroqGradingServiceImpl.java` — save changes

In `doGradeQuizAnswer()`:
- `answer.setAiScore(displayScore + "/" + maxScore)` — format mới `"16.25/20"` hoặc `"8.13/10"`
- `answer.setAiRubricJson(objectMapper.writeValueAsString(grading.getRubric()))` — lưu Map mới (backward-compatible)
- Gọi `recalculateQuizResult()` với logic mới ( chia 4 criteria, tính overall band → convert)

---

### 3.3 Frontend Changes

#### `student/quiz-result.html`

**Thay thế raw `<pre>` rubric bằng structured breakdown:**

```html
<!-- AI Grading Result Block -->
<div th:if="${question.aiGradingStatus == 'COMPLETED'}"
     class="mt-3 p-3 rounded border border-success bg-success bg-opacity-5"
     th:id="'ai-grade-' + ${question.questionId}">
  <!-- Overall Band + Score -->
  <div class="d-flex align-items-center gap-3 mb-3">
    <span class="badge bg-primary fs-6">
      <i class="bi bi-robot"></i> IELTS <span th:text="${question.questionType}">WRITING</span>
    </span>
    <span class="fs-5 fw-bold text-primary">
      <span th:text="${question.aiScore}">8.13/10</span>
    </span>
  </div>

  <!-- Rubric Breakdown -->
  <div class="rubric-breakdown" th:attr="data-rubric=${question.aiRubricJson}">
    <!-- Rendered by JavaScript (see below) -->
  </div>

  <p class="text-muted mb-2 small" th:if="${question.aiFeedback != null}"
     th:text="${question.aiFeedback}"></p>
</div>
```

**JavaScript render function:**

```javascript
function renderRubricBreakdown(rubricJson, container, isTeacher) {
    const rubric = JSON.parse(rubricJson);
    const CRITERION_LABELS = {
        task_achievement: '📝 Task Achievement',
        lexical_resource: '📖 Lexical Resource',
        grammatical_range: '🔤 Grammatical Range & Accuracy',
        coherence_cohesion: '🔗 Coherence & Cohesion',
        fluency_cohesion: '🎤 Fluency & Cohesion',
        pronunciation: '🔊 Pronunciation'
    };
    const BAND_COLORS = (score) =>
        score <= 4 ? '#dc3545' : score <= 6 ? '#ffc107' : '#198754';

    let html = '<div class="rubric-breakdown">';
    for (const [key, criterion] of Object.entries(rubric)) {
        const label = CRITERION_LABELS[key] || key;
        const pct = (criterion.score / criterion.max) * 100;
        const color = BAND_COLORS(criterion.score);
        html += `
        <div class="mb-3">
          <div class="d-flex justify-content-between align-items-center mb-1">
            <span class="fw-semibold small">${label}</span>
            <span class="badge bg-secondary">${criterion.bandLabel}</span>
          </div>
          <div class="progress" style="height:10px">
            <div class="progress-bar" role="progressbar"
                 style="width:${pct}%; background-color:${color}">
            </div>
          </div>
          <div class="d-flex justify-content-between small mt-1">
            <span class="text-muted" style="font-size:0.75rem">${criterion.score}/${criterion.max}</span>
            <span class="text-muted" style="font-size:0.75rem; font-style:italic">${criterion.bandDescription}</span>
          </div>
          ${isTeacher && criterion.aiReasoning ? `
          <div class="mt-1 p-2 bg-light rounded small text-secondary"
               style="font-size:0.75rem">💡 ${criterion.aiReasoning}</div>` : ''}
        </div>`;
    }
    html += '</div>';
    container.innerHTML = html;
}

// Usage:
document.querySelectorAll('.rubric-breakdown').forEach(el => {
    renderRubricBreakdown(el.dataset.rubric, el, /* isTeacher */ false);
});
```

#### `teacher/quiz-grading-detail.html`

Tương tự, nhưng `isTeacher = true` để hiển thị `aiReasoning` + nút override điểm.

---

## 4. Backward Compatibility

- Old `aiRubricJson` format `{"task_achievement": 3, ...}` → JavaScript fallback hiển thị `<pre>` tag như hiện tại
- Old `aiScore` format `"8/10"` → hiển thị bình thường, không break
- DB schema **KHÔNG thay đổi**

---

## 5. Files to Modify

| File | Change |
|---|---|
| `dto/GradingResponse.java` | Thay `Map<String, Integer>` → `Map<String, RubricCriterion>` + thêm fields mới |
| `service/GroqClient.java` | Rewrite prompts với 9-band descriptors + parse response mới |
| `service/impl/GroqGradingServiceImpl.java` | Compute overallBand, displayScore, save đúng format |
| `templates/student/quiz-result.html` | Thay raw `<pre>` bằng JS render breakdown |
| `templates/teacher/quiz-grading-detail.html` | Thay raw `<pre>` bằng JS render breakdown (teacher mode) |

---

## 6. Acceptance Criteria

- [ ] AI trả về structured JSON với `overallBand` (0–9) và 4 criteria × band descriptors
- [ ] `aiScore` hiển thị dạng `"8.13/10"` (hoặc `"16.25/20"`)
- [ ] UI student hiển thị breakdown với progress bar + band description (không thấy AI reasoning)
- [ ] UI teacher hiển thị breakdown với progress bar + band description + AI reasoning + override
- [ ] Old records vẫn hiển thị được (fallback raw JSON)
- [ ] `GroqGradingServiceImpl.recalculateQuizResult()` tính đúng overall band → display score
