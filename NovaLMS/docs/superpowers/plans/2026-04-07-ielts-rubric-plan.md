# IELTS Rubric Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Grade WRITING/SPEAKING answers using IELTS 9-band scale with structured rubric breakdown displayed as progress bars on both student and teacher UI.

**Architecture:** Backend changes to GroqClient (prompts + response parsing) and GroqGradingServiceImpl (score computation). Frontend changes to two Thymeleaf templates (student quiz-result + teacher grading-detail). DB schema unchanged — only JSON structure inside `aiRubricJson` changes.

**Tech Stack:** Spring Boot 3, Java 17, Groq API (LLaMA 3.3), Thymeleaf + vanilla JS, Bootstrap 5.

**Spec:** `docs/superpowers/specs/2026-04-07-ielts-rubric-design.md`

---

## Chunk 1: Backend — DTO + GroqClient (prompts + parsing)

**Files:**
- Modify: `DoAn/src/main/java/com/example/DoAn/dto/GradingResponse.java`
- Modify: `DoAn/src/main/java/com/example/DoAn/service/GroqClient.java`

---

### Task 1: Create RubricCriterion DTO and update GradingResponse

**Files:**
- Modify: `DoAn/src/main/java/com/example/DoAn/dto/GradingResponse.java`

- [ ] **Step 1: Write the new GradingResponse structure**

```java
// Thay toàn bộ file GradingResponse.java bằng:

package com.example.DoAn.dto;

import lombok.*;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradingResponse {

    /** Overall IELTS band (0–9, có 0.5), ví dụ: 6.875 */
    private double overallBand;

    /** Điểm hiển thị (thang 10 hoặc 20), ví dụ: 17.19/20 */
    private double displayScore;

    /** Điểm tối đa (10 hoặc 20) */
    private double maxScore;

    /** Nhận xét tổng bằng tiếng Việt */
    private String feedback;

    /** VD: "Good User (7.0)" */
    private String overallBandDescriptor;

    /**
     * Map của 4 tiêu chí.
     * Key WRITING: task_achievement, lexical_resource, grammatical_range, coherence_cohesion
     * Key SPEAKING: fluency_cohesion, lexical_resource, grammatical_range, pronunciation
     */
    private Map<String, RubricCriterion> rubric;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RubricCriterion {
        /** Điểm đạt được (0–9, có 0.5) */
        private double score;
        /** Luôn = 9 */
        private double max;
        /** VD: "Band 7.0" hoặc "7.5" */
        private String bandLabel;
        /** Mô tả band đạt được (tiếng Anh, trích từ IELTS official) */
        private String bandDescription;
        /** Giải thích ngắn bằng tiếng Việt tại sao đạt mức này */
        private String aiReasoning;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add DoAn/src/main/java/com/example/DoAn/dto/GradingResponse.java
git commit -m "feat(GradingResponse): add RubricCriterion DTO + IELTS 9-band fields

Adds overallBand, displayScore, overallBandDescriptor, and
Map<String, RubricCriterion> replacing old Map<String, Integer>

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
"
```

---

### Task 2: Rewrite GroqClient — buildSystemPrompt với 9-band descriptors

**Files:**
- Modify: `DoAn/src/main/java/com/example/DoAn/service/GroqClient.java`

- [ ] **Step 1: Thay thế hàm `buildRubricJson()` bằng hàm mới với đầy đủ 9-band descriptors**

Tìm hàm `buildRubricJson()` (line ~161) trong GroqClient.java, thay bằng:

```java
private String buildRubricJson(String questionType) {
    if ("WRITING".equals(questionType)) {
        return """
            {
              "task_achievement": {
                "max": 9,
                "bands": {
                  "0": "Content is irrelevant or does not communicate the message",
                  "1": "Barely communicates with occasional comprehensible sections",
                  "2": "Partially addresses the task; content may be largely irrelevant",
                  "3": "Task not fully achieved; coverage is inadequate",
                  "4": "Presents a position but development is insufficient or repetitive",
                  "5": "Presents some relevant ideas but development is limited",
                  "6": "Addresses the task adequately; relevant ideas with some development",
                  "7": "Covers all requirements; well-developed with clear progression",
                  "8": "Satisfies all requirements; very good development and cohesion",
                  "9": "Fully addresses all requirements with complete clarity and precision"
                }
              },
              "lexical_resource": {
                "max": 9,
                "bands": {
                  "0": "No appropriate lexical resource",
                  "1": "Rarely used appropriate vocabulary; comprehension is severely limited",
                  "2": "Limited vocabulary; frequent errors of word choice",
                  "3": "Limited vocabulary; inaccuracies impede meaning",
                  "4": "Adequate vocabulary; some inaccuracies but meaning largely clear",
                  "5": "Sufficient range; some vocabulary errors but meaning clear",
                  "6": "Wide enough vocabulary; occasional lexical errors",
                  "7": "Wide range; minor errors; effective communication",
                  "8": "Wide range; very few lexical errors; communicates flexibly",
                  "9": "Full flexibility and precision; sophisticated vocabulary"
                }
              },
              "grammatical_range": {
                "max": 9,
                "bands": {
                  "0": "No grammatical structures",
                  "1": "Rarely produces grammatical structures",
                  "2": "Few句 structures; accuracy only in simplest forms",
                  "3": "Limited control; errors impede communication",
                  "4": "Some accuracy; limited range of structures",
                  "5": "Fair range; frequent errors but clear communication",
                  "6": "Good range; reasonable accuracy; complex structures attempted",
                  "7": "Wide range; good accuracy; minor errors",
                  "8": "Wide range; very good accuracy; rare errors",
                  "9": "Full range; high accuracy; sophisticated structures"
                }
              },
              "coherence_cohesion": {
                "max": 9,
                "bands": {
                  "0": "No organization or cohesion",
                  "1": "Unconnected ideas; minimal cohesion",
                  "2": "Lacks cohesion; organization unclear",
                  "3": "Cohesion inadequate; organization hard to follow",
                  "4": "Cohesion developed; organization sometimes unclear",
                  "5": "Uses some cohesive devices; organization generally clear",
                  "6": "Logically organized; appropriate cohesive devices",
                  "7": "Well-organized; clear progression; effective cohesion",
                  "8": "Very well organized; seamless cohesion",
                  "9": "Fluent and sophisticated; perfect cohesion and progression"
                }
              }
            }
            """;
    } else {
        // SPEAKING
        return """
            {
              "fluency_cohesion": {
                "max": 9,
                "bands": {
                  "0": "Cannot communicate",
                  "1": "Difficult to produce connected speech",
                  "2": "Long pauses; hesitant; communication is stilted",
                  "3": "Usually hesitant; some connected speech",
                  "4": "Shows hesitation; limited connected speech",
                  "5": "Able to sustain speech; some hesitation",
                  "6": "Speaks at reasonable speed; occasional repetition",
                  "7": "Speaks fluently with occasional self-correction",
                  "8": "Speaks fluently with rare hesitation or repetition",
                  "9": "Speaks with complete fluency like a native speaker"
                }
              },
              "lexical_resource": {
                "max": 9,
                "bands": {
                  "0": "No appropriate lexical resource",
                  "1": "Barely communicates; no evidence of lexical control",
                  "2": "Limited vocabulary; frequent word-searching",
                  "3": "Limited vocabulary; comprehension often breaks down",
                  "4": "Limited range; occasional word-finding difficulty",
                  "5": "Sufficient range; some word choice errors",
                  "6": "Wide enough vocabulary; occasional errors",
                  "7": "Wide range; minor lexical gaps; communicates effectively",
                  "8": "Wide range; very few lexical errors",
                  "9": "Full lexical sophistication; precise word choice"
                }
              },
              "grammatical_range": {
                "max": 9,
                "bands": {
                  "0": "No grammatical structures",
                  "1": "Rarely produces grammatical structures",
                  "2": "Few structures; frequent errors",
                  "3": "Limited control; frequent errors impede meaning",
                  "4": "Some accuracy in simple sentences; complex forms rare",
                  "5": "Fair range; frequent grammatical errors",
                  "6": "Good range; reasonable accuracy; complex structures attempted",
                  "7": "Wide range; good accuracy; minor errors",
                  "8": "Wide range; very good accuracy; rare errors",
                  "9": "Full range; sophisticated structures; high accuracy"
                }
              },
              "pronunciation": {
                "max": 9,
                "bands": {
                  "0": "No intelligible pronunciation",
                  "1": "Barely intelligible; constant breakdowns",
                  "2": "Severe pronunciation difficulties; frequent breakdowns",
                  "3": "Heavy accent; frequent mispronunciation; meaning obscured",
                  "4": "Pronunciation errors require listener effort",
                  "5": "Acceptable but with occasional mispronunciation",
                  "6": "Generally intelligible; some errors but clear",
                  "7": "Clear and intelligible; minor pronunciation slips",
                  "8": "Very clear; rare pronunciation errors",
                  "9": "Equivalent to an educated native speaker"
                }
              }
            }
            """;
    }
}
```

- [ ] **Step 2: Thay thế `buildSystemPrompt()` để yêu cầu AI trả về structured JSON với band justification**

Tìm hàm `buildSystemPrompt()` (line ~184), thay bằng:

```java
private String buildSystemPrompt(String questionType, String rubricJson) {
    if ("WRITING".equals(questionType)) {
        return String.format("""
            Bạn là giáo viên tiếng Anh chuyên IELTS, chấm bài WRITING theo thang điểm IELTS 9-band.
            Rubric (mỗi tiêu chí 0-9 điểm):
            %s

            Hãy đọc câu trả lời và CHẤM theo rubric trên.
            Trả về DUY NHẤT JSON, không có markdown hay text nào khác:
            {
              "overallBand": <(task_achievement + lexical_resource + grammatical_range + coherence_cohesion) / 4>,
              "displayScore": <overallBand * 2.5>,
              "maxScore": 10,
              "feedback": "<nhận xét tổng 2-3 câu bằng tiếng Việt, gợi ý cải thiện>",
              "overallBandDescriptor": "<VD: 'Good User (7.0)'>",
              "rubric": {
                "task_achievement": {
                  "score": <điểm 0-9>,
                  "max": 9,
                  "bandLabel": "<VD: 'Band 7.0' hoặc '7.5'>",
                  "bandDescription": "<lấy mô tả band tương ứng từ rubric ở trên>",
                  "aiReasoning": "<giải thích 1-2 câu bằng tiếng Việt tại sao đạt mức này>"
                },
                "lexical_resource": { "score": <>, "max": 9, "bandLabel": "...", "bandDescription": "...", "aiReasoning": "..." },
                "grammatical_range": { "score": <>, "max": 9, "bandLabel": "...", "bandDescription": "...", "aiReasoning": "..." },
                "coherence_cohesion": { "score": <>, "max": 9, "bandLabel": "...", "bandDescription": "...", "aiReasoning": "..." }
              }
            }
            """, rubricJson);
    } else {
        return String.format("""
            Bạn là giáo viên tiếng Anh chuyên IELTS, chấm bài SPEAKING theo thang điểm IELTS 9-band.
            Rubric (mỗi tiêu chí 0-9 điểm):
            %s

            Hãy nghe/nhìn câu trả lời và CHẤM theo rubric trên.
            Trả về DUY NHẤT JSON, không có markdown hay text nào khác:
            {
              "overallBand": <(fluency_cohesion + lexical_resource + grammatical_range + pronunciation) / 4>,
              "displayScore": <overallBand * 2.5>,
              "maxScore": 10,
              "feedback": "<nhận xét tổng 2-3 câu bằng tiếng Việt, gợi ý cải thiện>",
              "overallBandDescriptor": "<VD: 'Good User (7.0)'>",
              "rubric": {
                "fluency_cohesion": { "score": <>, "max": 9, "bandLabel": "...", "bandDescription": "...", "aiReasoning": "..." },
                "lexical_resource": { "score": <>, "max": 9, "bandLabel": "...", "bandDescription": "...", "aiReasoning": "..." },
                "grammatical_range": { "score": <>, "max": 9, "bandLabel": "...", "bandDescription": "...", "aiReasoning": "..." },
                "pronunciation": { "score": <>, "max": 9, "bandLabel": "...", "bandDescription": "...", "aiReasoning": "..." }
              }
            }
            """, rubricJson);
    }
}
```

- [ ] **Step 3: Cập nhật `gradeWritingOrSpeaking()` để gọi đúng hàm mới**

Tìm `gradeWritingOrSpeaking()` trong GroqClient.java, thay phần build prompt:

```java
// THAY:
// String rubricJson = buildRubricJson(questionType, maxPoints);
// String systemPrompt = buildSystemPrompt(questionType, rubricJson, maxPoints);

// BẰNG:
String rubricJson = buildRubricJson(questionType);
String systemPrompt = buildSystemPrompt(questionType, rubricJson);
```

Và thay parsing ở cuối hàm:
```java
// THAY:
// Map<String, Integer> rubric = ... (old format)
// return GradingResponse.builder()
//     .totalScore(...).maxScore(...).feedback(...).rubric(rubric)
//     .build();

// BẰNG:
GradingResponse response = mapper.readValue(content, GradingResponse.class);
return response;
```

- [ ] **Step 4: Commit**

```bash
git add DoAn/src/main/java/com/example/DoAn/service/GroqClient.java
git commit -m "feat(GroqClient): rewrite prompts with IELTS 9-band descriptors

- buildRubricJson() now returns full 9-band descriptor per criterion
- buildSystemPrompt() instructs LLaMA to return structured JSON with
  overallBand, displayScore, bandLabel, bandDescription, aiReasoning
- gradeWritingOrSpeaking() parses new GradingResponse structure

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
"
```

---

## Chunk 2: Backend — GroqGradingServiceImpl (save + recalculate)

**Files:**
- Modify: `DoAn/src/main/java/com/example/DoAn/service/impl/GroqGradingServiceImpl.java`

---

### Task 3: Update doGradeQuizAnswer — save new format + recalculate

**Files:**
- Modify: `DoAn/src/main/java/com/example/DoAn/service/impl/GroqGradingServiceImpl.java`

Tìm hàm `doGradeQuizAnswer()` trong GroqGradingServiceImpl.java, thay phần save:

```java
// TÌM (khoảng line 270-276):
answer.setAiScore(grading.getTotalScore() + "/" + grading.getMaxScore());

// THAY BẰNG:
// aiScore hiển thị: "8.13/10" = displayScore/maxScore
String displayScoreStr = String.format("%.2f", grading.getDisplayScore());
answer.setAiScore(displayScoreStr + "/" + (int) grading.getMaxScore());
```

Và thay phần lưu rubric:
```java
// TÌM:
// answer.setAiRubricJson(objectMapper.writeValueAsString(grading.getRubric()));

// GIỮ NGUYÊN — grading.getRubric() đã là Map<String, RubricCriterion>
// ObjectMapper sẽ serialize đúng cấu trúc mới
answer.setAiRubricJson(objectMapper.writeValueAsString(grading.getRubric()));
```

**Thêm vào đầu class:**
```java
import com.example.DoAn.dto.GradingResponse.RubricCriterion;
import com.example.DoAn.dto.GradingResponse;
```

---

### Task 4: Update recalculateQuizResult() — compute from IELTS overallBand

**Files:**
- Modify: `DoAn/src/main/java/com/example/DoAn/service/impl/GroqGradingServiceImpl.java`

Tìm `recalculateQuizResult()` (line ~295), thay toàn bộ logic trong vòng for:

```java
// TÌM phần WRITING/SPEAKING trong recalculateQuizResult():
// if ("WRITING".equals(qType) || "SPEAKING".equals(qType)) {
//     if (a.getAiScore() != null) {
//         try {
//             String[] parts = a.getAiScore().split("/");
//             totalScore += Integer.parseInt(parts[0].trim());
//             maxScore += Integer.parseInt(parts[1].trim());
//         } catch (Exception ignored) {}
//     }
// }

// THAY BẰNG:
if ("WRITING".equals(qType) || "SPEAKING".equals(qType)) {
    if (a.getAiScore() != null && a.getAiRubricJson() != null) {
        try {
            // Parse new format: "8.13/10" hoặc fallback cũ "8/10"
            String scoreStr = a.getAiScore();
            if (scoreStr.contains("/")) {
                double scoreVal = Double.parseDouble(scoreStr.split("/")[0].trim());
                int maxVal = Integer.parseInt(scoreStr.split("/")[1].trim());
                totalScore += scoreVal;
                maxScore += maxVal;
            }
        } catch (Exception ignored) {}
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add DoAn/src/main/java/com/example/DoAn/service/impl/GroqGradingServiceImpl.java
git commit -m "feat(GroqGradingServiceImpl): save + recalculate with IELTS format

- doGradeQuizAnswer() saves aiScore as '8.13/10' format from grading.displayScore
- recalculateQuizResult() handles new 'score/max' format for WRITING/SPEAKING

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
"
```

---

## Chunk 3: Frontend — Student quiz-result.html

**Files:**
- Modify: `DoAn/src/main/resources/templates/student/quiz-result.html`

---

### Task 5: Thay raw JSON pre tag bằng structured rubric breakdown

**Files:**
- Modify: `DoAn/src/main/resources/templates/student/quiz-result.html`

- [ ] **Step 1: Thay thế phần hiển thị rubric trong quiz-result.html**

Tìm đoạn (khoảng line 143-151):

```html
<!-- TÌM: -->
<button class="btn btn-link btn-sm p-0 text-decoration-none" type="button"
        data-bs-toggle="collapse"
        th:attr="data-bs-target='#rubric-' + ${question.questionId}">
  <i class="bi bi-chevron-expand"></i> Xem chi tiết rubric
</button>
<div class="collapse mt-2" th:id="'rubric-' + ${question.questionId}">
  <pre class="small text-muted bg-light p-2 rounded" style="font-size:0.75rem; white-space:pre-wrap;"
       th:if="${question.aiRubricJson != null}" th:text="${question.aiRubricJson}"></pre>
</div>
```

**THAY BẰNG:**

```html
<!-- Rubric Breakdown (structured display) -->
<th:block th:if="${question.aiRubricJson != null}">
  <button class="btn btn-link btn-sm p-0 text-decoration-none" type="button"
          data-bs-toggle="collapse"
          th:attr="data-bs-target='#rubric-' + ${question.questionId}">
    <i class="bi bi-chevron-expand"></i> Xem chi tiết rubric (IELTS)
  </button>
  <div class="collapse mt-2" th:id="'rubric-' + ${question.questionId}">
    <!-- Rendered by JavaScript below -->
    <div class="rubric-breakdown"
         th:attr="data-rubric=${question.aiRubricJson},
                   data-qtype=${question.questionType},
                   data-is-teacher='false'">
    </div>
  </div>
</th:block>
```

- [ ] **Step 2: Thêm JavaScript render function vào cuối file (trong `<script>` block)**

Tìm `<script th:inline="javascript">` ở cuối file, thay toàn bộ nội dung script:

```javascript
(function() {
    // ── Band label color map ──────────────────────────────────────────────────
    function bandColor(score) {
        if (score <= 4)   return '#dc3545'; // red
        if (score <= 6)   return '#ffc107'; // yellow
        return '#198754';                    // green
    }

    function bandColorCss(score) {
        if (score <= 4)   return 'bg-danger';
        if (score <= 6)   return 'bg-warning text-dark';
        return 'bg-success';
    }

    // ── Criterion label i18n ─────────────────────────────────────────────────
    var CRITERION_LABELS = {
        // WRITING
        'task_achievement':   '📝 Task Achievement',
        'lexical_resource':  '📖 Lexical Resource',
        'grammatical_range': '🔤 Grammatical Range & Accuracy',
        'coherence_cohesion':'🔗 Coherence & Cohesion',
        // SPEAKING
        'fluency_cohesion':  '🎤 Fluency & Coherence',
        'pronunciation':     '🔊 Pronunciation'
    };

    // ── Main render ─────────────────────────────────────────────────────────
    function renderRubric(container, isTeacher) {
        var rubricJson = container.getAttribute('data-rubric');
        if (!rubricJson) return;

        try {
            var rubric = JSON.parse(rubricJson);
        } catch(e) {
            // Fallback: old format → show raw JSON
            container.innerHTML = '<pre class="small text-muted">' + rubricJson + '</pre>';
            return;
        }

        // Check if new format (has rubric.criterion.score) vs old (just numbers)
        var isNewFormat = rubric && typeof rubric === 'object' &&
            Object.values(rubric).some(function(v) { return v && typeof v === 'object' && v.score !== undefined; });

        if (!isNewFormat) {
            container.innerHTML = '<pre class="small text-muted">' + rubricJson + '</pre>';
            return;
        }

        var html = '<div class="table-responsive"><table class="table table-sm align-middle mb-0">';
        html += '<thead><tr class="small text-muted">' +
            '<th style="width:35%">Tiêu chí</th>' +
            '<th style="width:15%">Band</th>' +
            '<th style="width:40%">Progress</th>' +
            '<th style="width:10%">Điểm</th></tr></thead><tbody>';

        for (var key in rubric) {
            var c = rubric[key];
            if (!c || typeof c.score === 'undefined') continue;

            var label    = CRITERION_LABELS[key] || key;
            var pct      = Math.round((c.score / c.max) * 100);
            var color    = bandColor(c.score);
            var colorCls = bandColorCss(c.score);
            var bandDisp = c.bandLabel || (c.score % 1 === 0 ? String(c.score) : c.score.toFixed(1));

            html += '<tr>' +
                '<td><span class="small fw-semibold">' + label + '</span><br>' +
                '<span class="text-muted" style="font-size:0.7rem;font-style:italic">' +
                (c.bandDescription || '') + '</span></td>' +
                '<td><span class="badge ' + colorCls + '">' + bandDisp + '</span></td>' +
                '<td><div class="progress" style="height:10px">' +
                '<div class="progress-bar" role="progressbar" style="width:' + pct + '%;background-color:' + color + '"></div></div></td>' +
                '<td><span class="small fw-bold">' + c.score + '/' + c.max + '</span></td></tr>';
        }
        html += '</tbody></table></div>';

        // AI reasoning — teacher only
        if (isTeacher) {
            for (var key in rubric) {
                var c = rubric[key];
                if (!c || !c.aiReasoning) continue;
                var label = CRITERION_LABELS[key] || key;
                html += '<div class="mt-2 p-2 bg-light rounded small text-secondary">' +
                    '<strong>💡 ' + label + ':</strong> ' + c.aiReasoning + '</div>';
            }
        }

        container.innerHTML = html;
    }

    // ── Bootstrap auto-init ──────────────────────────────────────────────────
    document.querySelectorAll('.rubric-breakdown').forEach(function(el) {
        var isTeacher = el.getAttribute('data-is-teacher') === 'true';
        renderRubric(el, isTeacher);
    });
})();
```

- [ ] **Step 3: Commit**

```bash
git add DoAn/src/main/resources/templates/student/quiz-result.html
git commit -m "feat(quiz-result.html): render IELTS rubric as structured table + progress bars

Replaces raw <pre> JSON display with:
- Table breakdown: criterion | band badge | progress bar | score
- Progress bar color: red (1-4), yellow (4.5-6), green (6.5-9)
- Old JSON format falls back to <pre> display (backward compat)

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
"
```

---

## Chunk 4: Frontend — Teacher quiz-grading-detail.html

**Files:**
- Modify: `DoAn/src/main/resources/templates/teacher/quiz-grading-detail.html`

---

### Task 6: Update teacher grading detail — structured rubric + teacher mode

**Files:**
- Modify: `DoAn/src/main/resources/templates/teacher/quiz-grading-detail.html`

- [ ] **Step 1: Thay thế phần hiển thị rubric trong quiz-grading-detail.html**

Tìm đoạn (khoảng line 173-178):

```html
${q.aiRubricJson ? `
<details class="small">
    <summary class="text-decoration-none text-secondary cursor-pointer">Xem chi tiết rubric</summary>
    <pre class="mt-1 text-muted" style="font-size:0.75rem; white-space:pre-wrap;">${q.aiRubricJson}</pre>
</details>` : ''}
```

**THAY BẰNG:**

```html
${q.aiRubricJson ? `
<details class="small" ${q.aiGradingStatus !== 'PENDING' ? 'open' : ''}>
    <summary class="text-decoration-none text-secondary fw-semibold">📊 Chi tiết IELTS Rubric</summary>
    <div class="rubric-breakdown mt-2"
         data-rubric="${q.aiRubricJson}"
         data-qtype="${q.questionType}"
         data-is-teacher="true">
    </div>
</details>` : ''}
```

- [ ] **Step 2: Cập nhật display điểm AI (thêm overall band badge)**

Tìm đoạn hiển thị điểm (khoảng line 168-172):

```html
${q.aiScore ? `
<div class="mb-1">
    <span class="fs-5 fw-bold text-primary">${q.aiScore}</span>
    <span class="text-muted"> / ${q.points} điểm</span>
</div>` : ''}
```

**THAY BẰNG:**

```html
${q.aiScore ? `
<div class="mb-2">
    <div class="d-flex align-items-center gap-2">
        <span class="badge bg-primary fs-6">IELTS</span>
        <span class="fs-5 fw-bold text-primary">${q.aiScore}</span>
        <span class="text-muted">/ ${q.points} điểm</span>
    </div>
</div>` : ''}
```

- [ ] **Step 3: Commit**

```bash
git add DoAn/src/main/resources/templates/teacher/quiz-grading-detail.html
git commit -m "feat(quiz-grading-detail.html): render IELTS rubric table + teacher reasoning

- Structured rubric table with progress bars (teacher mode)
- Shows AI reasoning per criterion (isTeacher=true)
- Falls back to raw JSON for old records
- Added IELTS badge next to AI score display

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
"
```

---

## Chunk 5: Verification

- [ ] **Step 1: Compile check**

```bash
cd NovaLMS/DoAn
./mvnw compile -q 2>&1 | head -50
```

Expected: No errors. Classes compile successfully.

- [ ] **Step 2: Restart app and test grading flow**

1. Submit a quiz with 1 WRITING question
2. Wait ~30s for AI to grade
3. Open quiz result page → verify rubric displays as table with progress bars
4. Open teacher grading detail → verify rubric + AI reasoning visible

---

## Dependencies Between Chunks

```
Chunk 1 (GradingResponse + GroqClient)
  ↓
Chunk 2 (GroqGradingServiceImpl) ← depends on Chunk 1 (GradingResponse new structure)
  ↓
Chunk 3 (student quiz-result.html) ← depends on Chunk 1 (aiRubricJson new format)
  ↓
Chunk 4 (teacher quiz-grading-detail.html) ← depends on Chunk 1
  ↓
Chunk 5 (Verification)
```

**Execute chunks in order. Each chunk is independently testable.**
