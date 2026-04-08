# Design: Advanced Prompt Configuration cho AI Question Generation

**Project:** NovaLMS — AI Question Generation
**Date:** 2026-04-08
**Status:** Approved

---

## 1. Mục tiêu

Mở rộng hệ thống sinh câu hỏi AI hiện tại bằng chế độ **ADVANCED mode**, cho phép giáo viên tạo câu hỏi nâng cao với:

- **Bloom's Taxonomy** ở mức cao: Analyze, Evaluate, Create
- **Kỹ năng nâng cao**: Ưu tiên WRITING và SPEAKING
- **Ngữ pháp/ngôn ngữ nâng cao**: Idioms, collocations, complex structures, discourse markers, paraphrasing
- **Level-aware**: Prompt tự điều chỉnh theo CEFR của học viên (A1–C2)

---

## 2. Đối tượng sử dụng

Giáo viên đang sử dụng hệ thống NovaLMS, có quyền tạo quiz cho học viên thuộc mọi mức CEFR (A1–C2) và mọi độ tuổi.

---

## 3. Kiến trúc

### 3.1. Enum PromptMode

```java
public enum PromptMode {
    NORMAL,   // giữ nguyên prompt hiện tại
    ADVANCED  // prompt nâng cao theo Bloom's Taxonomy
}
```

### 3.2. Thêm field vào AIGenerateRequestDTO

```java
// Thêm vào AIGenerateRequestDTO
private String mode = "NORMAL"; // "NORMAL" | "ADVANCED"
```

### 3.3. Thêm field vào AIGenerateGroupRequestDTO (tương tự)

```java
private String mode = "NORMAL";
```

### 3.4. Cấu trúc thư mục config

```
src/main/resources/config/
  └── ai-prompt-advanced.yaml
```

> File `ai-prompt-normal.yaml` không cần tạo mới — giữ nguyên prompt string trong code như hiện tại.

---

## 4. Cấu hình YAML — ai-prompt-advanced.yaml

Cấu hình chia theo **3 CEFR bucket**:

| Bucket | CEFR Levels | Bloom Focus |
|--------|-------------|-------------|
| `beginner` | A1, A2 | REMEMBER, UNDERSTAND, APPLY |
| `intermediate` | B1, B2 | + ANALYZE, EVALUATE |
| `advanced` | C1, C2 | ANALYZE, EVALUATE, CREATE |

```yaml
advanced:
  beginner:
    # A1, A2
    bloom_levels:
      - REMEMBER
      - UNDERSTAND
      - APPLY
    bloom_instruction: |
      Use these verbs as guidance when framing questions:
      - REMEMBER: list, name, identify, recall
      - UNDERSTAND: describe, explain, summarize, classify
      - APPLY: use, demonstrate, solve, show
    grammar_focus:
      - "Present Simple / Past Simple / Future Simple"
      - "Subject-verb agreement"
      - "Basic comparatives (–er / more –)"
      - "Common collocations: make progress, take a break, go home"
      - "Basic connectors: and, but, because, so"
    question_types_ratio:
      MULTIPLE_CHOICE_SINGLE: 0.35
      FILL_IN_BLANK: 0.30
      WRITING: 0.20
      SPEAKING: 0.15
    skills: [ "READING", "LISTENING", "WRITING" ]
    lexical_complexity: "basic-to-intermediate"
    writing_constraint: "50–80 words per response"
    speaking_constraint: "Short responses, 2–4 sentences per turn"

  intermediate:
    # B1, B2
    bloom_levels:
      - REMEMBER
      - UNDERSTAND
      - APPLY
      - ANALYZE
    bloom_instruction: |
      Use these verbs as guidance when framing questions:
      - REMEMBER: list, name, identify, recall
      - UNDERSTAND: describe, explain, summarize, classify
      - APPLY: use, demonstrate, solve, show, implement
      - ANALYZE: compare, contrast, categorize, distinguish, examine
    grammar_focus:
      - "Modal verbs: could, would, might, should"
      - "First / Second conditionals"
      - "Passive voice"
      - "Reported speech (past tense shift)"
      - "Idioms B1: 'break the ice', 'cost an arm and a leg', 'hit the nail on the head'"
      - "Collocations: conduct research, take advantage of, make a decision"
      - "Connectors: although, however, therefore, moreover"
    question_types_ratio:
      MULTIPLE_CHOICE_SINGLE: 0.20
      MULTIPLE_CHOICE_MULTI: 0.15
      FILL_IN_BLANK: 0.15
      MATCHING: 0.10
      WRITING: 0.25
      SPEAKING: 0.15
    skills: [ "READING", "LISTENING", "WRITING", "SPEAKING" ]
    lexical_complexity: "intermediate"
    writing_constraint: "80–150 words per response"
    speaking_constraint: "Moderate responses, 4–8 sentences per turn"

  advanced:
    # C1, C2
    bloom_levels:
      - ANALYZE
      - EVALUATE
      - CREATE
    bloom_instruction: |
      Use these verbs as guidance when framing questions:
      - ANALYZE: examine, investigate, break down, differentiate, infer
      - EVALUATE: assess, justify, critique, argue, recommend
      - CREATE: design, formulate, develop, construct, propose
    grammar_focus:
      - "Complex sentences: relative clauses, participial clauses, cleft sentences"
      - "Subjunctive mood / wish + past perfect"
      - "Advanced passive and cleft constructions"
      - "Advanced discourse markers: nevertheless, subsequently, notwithstanding, insofar as"
      - "Idioms C1/C2: 'on the heels of', 'at a snail's pace', 'champion a cause'"
      - "Paraphrasing & lexical substitution"
      - "Register variation: formal / informal / academic"
      - "Advanced collocations: exert influence, yield results, pose a challenge"
    question_types_ratio:
      MULTIPLE_CHOICE_MULTI: 0.10
      FILL_IN_BLANK: 0.05
      WRITING: 0.50
      SPEAKING: 0.35
    skills: [ "WRITING", "SPEAKING" ]
    lexical_complexity: "advanced"
    writing_constraint: "150–300 words per response, formal register preferred"
    speaking_constraint: "Extended responses, justify opinions, discuss abstract topics"
```

---

## 5. Phương thức mới trong AIQuestionPromptBuilder

### 5.1. Thêm method

```java
public String buildAdvancedContextPrompt(String moduleName, String lessonSummary,
                                         int quantity, List<String> questionTypes,
                                         String cefrLevel);

public String buildAdvancedQuickPrompt(String topic, int quantity,
                                       List<String> questionTypes,
                                       String cefrLevel);
```

### 5.2. Logic chọn bucket

```java
private String getBucket(String cefr) {
    return switch (cefr == null ? "B1" : cefr.toUpperCase()) {
        case "A1", "A2" -> "beginner";
        case "B1", "B2" -> "intermediate";
        case "C1", "C2" -> "advanced";
        default          -> "intermediate"; // fallback
    };
}
```

### 5.3. Cấu trúc prompt ADVANCED

```
System role:
"You are a professional English teacher. Generate [quantity] advanced questions
for CEFR level [cefrLevel]..."

Sections trong prompt:
1. Role + CEFR level
2. Bloom instruction (từ YAML)
3. Grammar focus list (từ YAML)
4. Skill mix (từ YAML)
5. Question type distribution (từ YAML)
6. WRITING/SPEAKING constraints (từ YAML)
7. Output format (JSON — giữ nguyên từ prompt NORMAL)
8. Hard rules: English only, no null titles, etc.
```

---

## 6. Thay đổi trong AIQuestionServiceImpl

```java
@Override
public AIGenerateResponseDTO generate(AIGenerateRequestDTO request, String userEmail) {
    // existing rate limit check...
    String mode = "NORMAL".equalsIgnoreCase(request.getMode()) ? "NORMAL" : "ADVANCED";

    String prompt;
    if ("ADVANCED".equals(mode)) {
        String cefr = request.getCefrLevel() != null ? request.getCefrLevel() : "B1";
        prompt = promptBuilder.buildAdvancedContextPrompt(
            moduleName, lessonSummary, quantity, types, cefr);
    } else {
        prompt = buildPrompt(request); // existing method
    }

    String rawJson = callGroq(prompt);
    List<QuestionDTO> questions = parseQuestions(rawJson, request.getQuantity());
    // ...
}
```

Tương tự cho `generateGroup()`.

---

## 7. API backward compatibility

- **Field `mode` mới:** Optional, default `"NORMAL"`
- **Tất cả API cũ** không có field `mode` → vẫn hoạt động bình thường
- **Nếu `mode` = giá trị không hợp lệ:** ignore, dùng `NORMAL`

---

## 8. Error handling & Fallback

| Scenario | Behavior |
|----------|----------|
| YAML load fail | Dùng hardcoded ADVANCED prompt (inline, ít detail) |
| CEFR không hợp lệ | Fallback B1 bucket |
| AI trả về rỗng / lỗi parse | Giữ nguyên error message hiện tại |
| Mode = "ADVANCED" + quantity > 50 | Vẫn áp dụng max=50 từ validation |

---

## 9. Files cần tạo / sửa

| File | Action |
|------|--------|
| `src/main/resources/config/ai-prompt-advanced.yaml` | **Create** |
| `src/main/java/.../dto/request/AIGenerateRequestDTO.java` | Edit — thêm field `mode` |
| `src/main/java/.../dto/request/AIGenerateGroupRequestDTO.java` | Edit — thêm field `mode` |
| `src/main/java/.../util/AIQuestionPromptBuilder.java` | Edit — thêm method `buildAdvanced*Prompt()` + YAML loader |
| `src/main/java/.../service/impl/AIQuestionServiceImpl.java` | Edit — đọc `mode`, gọi builder phù hợp |

---

## 10. Testing

### Unit tests
- `AIQuestionPromptBuilderTest`:
  - `testAdvancedPromptContainsBloomVerbs()` — verify prompt ADVANCED chứa Bloom verbs đúng bucket
  - `testAdvancedPromptGrammarFocusForC1()` — verify grammar_focus C1 chứa keywords: subjunctive, discourse markers
  - `testNormalPromptUnchanged()` — verify prompt NORMAL giữ nguyên
  - `testBucketSelection()` — A1→beginner, B2→intermediate, C2→advanced

### Integration tests
- Gọi Groq với ADVANCED prompt → parse JSON → `isValid()` pass
- So sánh `buildContextPrompt(topic, qty, types)` vs `buildAdvancedContextPrompt(topic, qty, types)` — output khác nhau rõ rệt

---

## 11. Success criteria

1. Khi `mode = "ADVANCED"`, prompt sinh ra chứa Bloom verbs ở mức phù hợp với CEFR
2. Câu hỏi WRITING/SPEAKING ở C1-C2 yêu cầu字数 phù hợp (150–300 words)
3. Câu hỏi ở A1-A2 **không** chứa idioms hoặc discourse markers nâng cao
4. API cũ không có `mode` vẫn trả về kết quả như trước
5. YAML config được load thành công, không crash khi file missing
