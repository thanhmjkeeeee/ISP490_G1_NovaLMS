# AI Advanced Prompt Implementation Plan

**Goal:** Thêm chế độ ADVANCED mode vào AI question generation, sinh câu hỏi nâng cao theo Bloom's Taxonomy (Analyze/Evaluate/Create), ưu tiên WRITING/SPEAKING, với ngữ pháp/ngôn ngữ nâng cao.

**Architecture:** Dùng enum PromptMode (NORMAL / ADVANCED) trong DTO. AIQuestionPromptBuilder thêm 2 method mới cho ADVANCED, load config từ YAML theo CEFR bucket (beginner/intermediate/advanced). AIQuestionServiceImpl đọc mode, gọi builder phù hợp.

**Tech Stack:** Java 17, Spring Boot, OkHttp (Groq API), SnakeYAML, Groq Llama model

---

## Chunk 1: YAML Config

### Task 1: Create ai-prompt-advanced.yaml

**Files:**
- Create: `NovaLMS/DoAn/src/main/resources/config/ai-prompt-advanced.yaml`

---

- [ ] **Step 1: Tạo file config**

```yaml
advanced:
  beginner:
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

## Chunk 2: DTO Changes

### Task 2: Add mode field to AIGenerateRequestDTO

**Files:**
- Modify: `NovaLMS/DoAn/src/main/java/com/example/DoAn/dto/request/AIGenerateRequestDTO.java`

Thêm field vào class (sau các field hiện có):
```java
private String mode = "NORMAL"; // "NORMAL" | "ADVANCED"
```

### Task 3: Add mode field to AIGenerateGroupRequestDTO

**Files:**
- Modify: `NovaLMS/DoAn/src/main/java/com/example/DoAn/dto/request/AIGenerateGroupRequestDTO.java`

Thêm field tương tự:
```java
private String mode = "NORMAL";
```

---

## Chunk 3: AIQuestionPromptBuilder — Advanced Methods

### Task 4: Add YAML config class + advanced builder methods

**Files:**
- Modify: `NovaLMS/DoAn/src/main/java/com/example/DoAn/util/AIQuestionPromptBuilder.java`

Thêm vào cuối class hiện tại:

```java
private static final String ADVANCED_CONFIG_PATH = "config/ai-prompt-advanced.yaml";
private Map<String, Object> cachedAdvancedConfig;

private Map<String, Object> loadAdvancedConfig() {
    if (cachedAdvancedConfig != null) return cachedAdvancedConfig;
    try {
        InputStream is = getClass().getClassLoader().getResourceAsStream(ADVANCED_CONFIG_PATH);
        if (is == null) return null;
        cachedAdvancedConfig = new Yaml().load(is);
        is.close();
        return cachedAdvancedConfig;
    } catch (Exception e) {
        log.warn("Failed to load advanced config: {}", e.getMessage());
        return null;
    }
}

private String getBucket(String cefr) {
    return switch (cefr == null ? "B1" : cefr.toUpperCase()) {
        case "A1", "A2" -> "beginner";
        case "B1", "B2" -> "intermediate";
        case "C1", "C2" -> "advanced";
        default          -> "intermediate";
    };
}

@SuppressWarnings("unchecked")
private Map<String, Object> getBucketConfig(String cefr) {
    Map<String, Object> config = loadAdvancedConfig();
    if (config == null) return getFallbackBucketConfig(getBucket(cefr));
    Map<String, Object> advanced = (Map<String, Object>) config.get("advanced");
    if (advanced == null) return getFallbackBucketConfig(getBucket(cefr));
    String bucket = getBucket(cefr);
    Map<String, Object> bucketConfig = (Map<String, Object>) advanced.get(bucket);
    if (bucketConfig == null) return getFallbackBucketConfig(bucket);
    return bucketConfig;
}

// Fallback inline config khi YAML load fail
private Map<String, Object> getFallbackBucketConfig(String bucket) {
    return switch (bucket) {
        case "beginner" -> Map.of(
            "bloom_instruction", "Use REMEMBER/UNDERSTAND/APPLY verbs.",
            "grammar_focus", List.of("Present/Past Simple", "Basic collocations"),
            "question_types_ratio", Map.of("MULTIPLE_CHOICE_SINGLE", 0.35, "FILL_IN_BLANK", 0.30, "WRITING", 0.20, "SPEAKING", 0.15),
            "skills", List.of("READING", "LISTENING", "WRITING"),
            "writing_constraint", "50–80 words",
            "speaking_constraint", "2–4 sentences"
        );
        case "intermediate" -> Map.of(
            "bloom_instruction", "Use ANALYZE verbs. Include conditionals, idioms.",
            "grammar_focus", List.of("Conditionals", "Passive voice", "Idioms B1"),
            "question_types_ratio", Map.of("MULTIPLE_CHOICE_SINGLE", 0.20, "MULTIPLE_CHOICE_MULTI", 0.15, "FILL_IN_BLANK", 0.15, "MATCHING", 0.10, "WRITING", 0.25, "SPEAKING", 0.15),
            "skills", List.of("READING", "LISTENING", "WRITING", "SPEAKING"),
            "writing_constraint", "80–150 words",
            "speaking_constraint", "4–8 sentences"
        );
        default -> Map.of(
            "bloom_instruction", "Use ANALYZE/EVALUATE/CREATE verbs. Formal register.",
            "grammar_focus", List.of("Subjunctive", "Complex structures", "Discourse markers"),
            "question_types_ratio", Map.of("MULTIPLE_CHOICE_MULTI", 0.10, "FILL_IN_BLANK", 0.05, "WRITING", 0.50, "SPEAKING", 0.35),
            "skills", List.of("WRITING", "SPEAKING"),
            "writing_constraint", "150–300 words, formal register",
            "speaking_constraint", "Extended responses, justify opinions"
        );
    };
}

public String buildAdvancedContextPrompt(String moduleName, String lessonSummary,
                                         int quantity, List<String> questionTypes,
                                         String cefrLevel) {
    String types = buildTypesClause(questionTypes);
    String truncated = lessonSummary.length() > 8000 ? lessonSummary.substring(0, 8000) : lessonSummary;
    String bucket = getBucket(cefrLevel);
    Map<String, Object> cfg = getBucketConfig(cefrLevel);

    String bloomInstruction = cfg.get("bloom_instruction").toString();
    @SuppressWarnings("unchecked")
    List<String> grammarFocus = (List<String>) cfg.get("grammar_focus");
    @SuppressWarnings("unchecked")
    List<String> skills = (List<String>) cfg.get("skills");
    String writingConstraint = cfg.get("writing_constraint").toString();
    String speakingConstraint = cfg.get("speaking_constraint").toString();

    return """
        You are a professional English teacher specializing in advanced question design for CEFR level %s.
        The module "%s" has the following lesson content:
        %s

        IMPORTANT: ALL question content, answer options, and explanations MUST be in ENGLISH only. No Vietnamese.

        ## ADVANCED MODE REQUIREMENTS

        ### Bloom's Taxonomy Focus
        %s

        ### Grammar & Language Focus
        Generate questions that demonstrate mastery of:
        %s

        ### Skills Distribution
        Prioritize these skills (use this distribution as guidance):
        %s

        ### Question Requirements
        - Generate exactly %d questions following these Bloom levels and grammar focus.
        - Question types: %s
        - CEFR level: %s
        - Skills to mix: %s
        - WRITING questions: %s
        - SPEAKING questions: %s

        Every question must have: content, questionType, skill, cefrLevel, topic, explanation (can be null).

        Question type specifics:
        - MULTIPLE_CHOICE_SINGLE: 4 options (ENGLISH text), exactly 1 correct = true
        - MULTIPLE_CHOICE_MULTI: 4 options, 2-3 correct = true (but not all)
        - FILL_IN_BLANK: content contains "___", correctAnswer required
        - MATCHING: matchLeft (3-5 English words), matchRight (3-5 meanings), correctPairs (1-based indices)
        - WRITING, SPEAKING: no options needed, embed word count target in content

        IMPORTANT: Every "title" field must contain real ENGLISH text, not null, empty, or just a number.
        Do NOT generate questions that only test recall or simple comprehension. Focus on analysis, evaluation, or creation.

        Return ONLY a JSON array, no other text:
        [
          {
            "content": "...",
            "questionType": "...",
            "skill": "...",
            "cefrLevel": "%s",
            "topic": "%s",
            "explanation": "...",
            "options": [...],
            "correctAnswer": "...",
            "matchLeft": [...],
            "matchRight": [...],
            "correctPairs": [...]
          }
        ]
        """.formatted(
            cefrLevel, moduleName, truncated,
            bloomInstruction,
            String.join("\n        - ", grammarFocus),
            String.join(", ", skills),
            quantity, types, cefrLevel,
            String.join(", ", skills),
            writingConstraint,
            speakingConstraint,
            cefrLevel, moduleName
        );
}

public String buildAdvancedQuickPrompt(String topic, int quantity,
                                       List<String> questionTypes,
                                       String cefrLevel) {
    String types = buildTypesClause(questionTypes);
    String bucket = getBucket(cefrLevel);
    Map<String, Object> cfg = getBucketConfig(cefrLevel);

    String bloomInstruction = cfg.get("bloom_instruction").toString();
    @SuppressWarnings("unchecked")
    List<String> grammarFocus = (List<String>) cfg.get("grammar_focus");
    @SuppressWarnings("unchecked")
    List<String> skills = (List<String>) cfg.get("skills");
    String writingConstraint = cfg.get("writing_constraint").toString();
    String speakingConstraint = cfg.get("speaking_constraint").toString();

    return """
        You are a professional English teacher specializing in advanced question design for CEFR level %s.
        Generate exactly %d advanced English questions about the topic "%s".

        IMPORTANT: ALL question content, answer options, and explanations MUST be in ENGLISH only. No Vietnamese.

        ## ADVANCED MODE REQUIREMENTS

        ### Bloom's Taxonomy Focus
        %s

        ### Grammar & Language Focus
        Generate questions that demonstrate mastery of:
        %s

        ### Skills Distribution
        Prioritize these skills:
        %s

        ### Question Requirements
        - Generate exactly %d questions following these Bloom levels and grammar focus.
        - Question types: %s
        - CEFR level: %s
        - WRITING questions: %s
        - SPEAKING questions: %s

        Every question must have: content, questionType, skill, cefrLevel, topic, explanation (can be null).

        Question type specifics:
        - MULTIPLE_CHOICE_SINGLE: 4 options (ENGLISH text), exactly 1 correct = true
        - MULTIPLE_CHOICE_MULTI: 4 options, 2-3 correct = true (but not all)
        - FILL_IN_BLANK: content contains "___", correctAnswer required
        - MATCHING: matchLeft (3-5 English words), matchRight (3-5 meanings), correctPairs (1-based indices)
        - WRITING, SPEAKING: no options needed

        IMPORTANT: Every "title" field must contain real ENGLISH text, not null, empty, or just a number.
        Do NOT generate questions that only test recall or simple comprehension. Focus on analysis, evaluation, or creation.

        Return ONLY a JSON array, no other text:
        [
          {
            "content": "...",
            "questionType": "...",
            "skill": "...",
            "cefrLevel": "%s",
            "topic": "%s",
            "explanation": "...",
            "options": [...],
            "correctAnswer": "...",
            "matchLeft": [...],
            "matchRight": [...],
            "correctPairs": [...]
          }
        ]
        """.formatted(
            cefrLevel, quantity, topic,
            bloomInstruction,
            String.join("\n        - ", grammarFocus),
            String.join(", ", skills),
            quantity, types, cefrLevel,
            writingConstraint,
            speakingConstraint,
            cefrLevel, topic
        );
}
```

**Thêm import** (ở đầu file):
```java
import org.yaml.snakeyaml.Yaml;
import java.io.InputStream;
import java.util.Map;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
```

**Thêm annotation** trên class:
```java
@Slf4j
public class AIQuestionPromptBuilder {
```

---

## Chunk 4: Wire Mode into AIQuestionServiceImpl

### Task 5: Modify generate() to use ADVANCED mode

**Files:**
- Modify: `NovaLMS/DoAn/src/main/java/com/example/DoAn/service/impl/AIQuestionServiceImpl.java`

Trong method `generate()`, thay thế phần `buildPrompt()`:

**Trước (tìm trong method generate):**
```java
String prompt = buildPrompt(request);
```

**Thay bằng:**
```java
String prompt;
String mode = (request.getMode() != null && "ADVANCED".equalsIgnoreCase(request.getMode()))
        ? "ADVANCED" : "NORMAL";

if ("ADVANCED".equals(mode)) {
    String cefr = request.getCefrLevel() != null ? request.getCefrLevel() : "B1";
    if (request.hasModuleId()) {
        Optional<Module> moduleOpt = moduleRepository.findById(request.getModuleId());
        if (moduleOpt.isEmpty()) {
            throw new IllegalArgumentException("Module không tồn tại: " + request.getModuleId());
        }
        Module module = moduleOpt.get();
        String lessonSummary = fetchLessonSummary(module.getModuleId());
        prompt = promptBuilder.buildAdvancedContextPrompt(
                (String) module.getModuleName(), (String) lessonSummary,
                (int) request.getQuantity().intValue(),
                (java.util.List<String>) request.getQuestionTypes(),
                cefr);
    } else {
        prompt = promptBuilder.buildAdvancedQuickPrompt(
                (String) request.getTopic(),
                (int) request.getQuantity().intValue(),
                (java.util.List<String>) request.getQuestionTypes(),
                cefr);
    }
} else {
    prompt = buildPrompt(request);
}
```

### Task 6: Modify generateGroup() to use ADVANCED mode

**Files:**
- Modify: `NovaLMS/DoAn/src/main/java/com/example/DoAn/service/impl/AIQuestionServiceImpl.java`

Trong method `generateGroup()`, thay thế phần prompt building:

**Tìm block hiện tại:**
```java
String topic = request.hasTopic() ? request.getTopic()
        : (request.hasModuleId() ? "Module-based content" : "");
String skill = request.getSkill() != null ? request.getSkill() : "READING";
String cefr = request.getCefrLevel() != null ? request.getCefrLevel() : "B1";
int qty = request.getQuantity() != null ? request.getQuantity() : 5;

String prompt = promptBuilder.buildGroupPrompt(topic, skill, cefr, qty, request.getQuestionTypes());
```

**Thay bằng:**
```java
String topic = request.hasTopic() ? request.getTopic()
        : (request.hasModuleId() ? "Module-based content" : "");
String skill = request.getSkill() != null ? request.getSkill() : "READING";
String cefr = request.getCefrLevel() != null ? request.getCefrLevel() : "B1";
int qty = request.getQuantity() != null ? request.getQuantity() : 5;
boolean isAdvanced = request.getMode() != null && "ADVANCED".equalsIgnoreCase(request.getMode());

String prompt;
if (isAdvanced) {
    prompt = promptBuilder.buildAdvancedQuickPrompt(topic, qty, request.getQuestionTypes(), cefr);
} else {
    prompt = promptBuilder.buildGroupPrompt(topic, skill, cefr, qty, request.getQuestionTypes());
}
```

---

## Success Criteria

1. `AIGenerateRequestDTO.mode = "ADVANCED"` → gọi `buildAdvancedContextPrompt` hoặc `buildAdvancedQuickPrompt`
2. `AIGenerateRequestDTO.mode = null` hoặc `"NORMAL"` → hành vi không đổi (backward compatible)
3. YAML load fail → dùng fallback inline config, không crash
4. Câu hỏi ADVANCED chứa Bloom verbs phù hợp với CEFR bucket
5. WRITING/SPEAKING questions ở C1-C2 có word count target 150–300 words