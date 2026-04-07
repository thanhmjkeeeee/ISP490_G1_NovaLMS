# AI Question Group + Enhanced Excel Import Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Cập nhật AI Generate và Excel Import để hỗ trợ Question Group (Passage + câu hỏi con), nâng cấp metadata cho single-question imports.

**Architecture:**
- Backend: Thêm DTO mới, thêm method trong AIQuestionService và ExcelImportService, thêm endpoint trong ExpertQuestionController.
- Frontend: Thêm tab "Bộ câu hỏi" trong AI modal và lựa chọn `QUESTION_GROUP` trong Import modal của `question-bank.html`.

**Tech Stack:** Spring Boot 3, Java 17, Thymeleaf, Apache POI, OkHttp, Groq API

---

## Chunk 1: Backend - DTOs & AIQuestionService (AI Generate Group)

### Task 1.1: Tạo AIGenerateGroupRequestDTO

**Files:**
- Create: `DoAn/src/main/java/com/example/DoAn/dto/request/AIGenerateGroupRequestDTO.java`

- [ ] **Step 1: Tạo file**

```java
package com.example.DoAn.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIGenerateGroupRequestDTO {

    private String topic;

    private Integer moduleId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 20, message = "Quantity cannot exceed 20")
    private Integer quantity;

    private String skill;

    private String cefrLevel;

    private java.util.List<String> questionTypes;

    public boolean hasTopic() {
        return topic != null && !topic.isBlank();
    }

    public boolean hasModuleId() {
        return moduleId != null;
    }

    public boolean isValid() {
        return hasTopic() || hasModuleId();
    }
}
```

---

### Task 1.2: Tạo AIGenerateGroupResponseDTO

**Files:**
- Create: `DoAn/src/main/java/com/example/DoAn/dto/response/AIGenerateGroupResponseDTO.java`

- [ ] **Step 1: Tạo file**

```java
package com.example.DoAn.dto.response;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIGenerateGroupResponseDTO {

    private String passage;
    private String audioUrl;
    private String imageUrl;
    private String skill;
    private String cefrLevel;
    private String topic;
    private String explanation;
    private List<AIGenerateResponseDTO.QuestionDTO> questions;
    private String warning;
}
```

---

### Task 1.3: Tạo AIImportGroupRequestDTO

**Files:**
- Create: `DoAn/src/main/java/com/example/DoAn/dto/request/AIImportGroupRequestDTO.java`

- [ ] **Step 1: Tạo file**

```java
package com.example.DoAn.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIImportGroupRequestDTO {

    @NotNull(message = "Passage content is required")
    private String passage;

    private String audioUrl;
    private String imageUrl;
    private String skill;
    private String cefrLevel;
    private String topic;
    private String explanation;
    private String status = "DRAFT";

    @NotNull(message = "Questions list is required")
    private List<AIImportRequestDTO.AIQuestionDTO> questions;
}
```

---

### Task 1.4: Thêm method buildGroupPrompt trong AIQuestionPromptBuilder

**Files:**
- Modify: `DoAn/src/main/java/com/example/DoAn/util/AIQuestionPromptBuilder.java`

Tìm file, thêm method sau vào class:

- [ ] **Step 1: Thêm method vào AIQuestionPromptBuilder**

```java
/**
 * Builds prompt for generating a passage-based question group.
 * Groq returns flat JSON: { passage, audioUrl, imageUrl, skill, cefrLevel, topic,
 *                             explanation, questions: [{content, questionType, options, ...}] }
 */
public String buildGroupPrompt(String topic, String skill, String cefrLevel,
                                int questionCount, List<String> questionTypes) {
    StringBuilder sb = new StringBuilder();
    sb.append("You are an expert English language test designer.\n\n");
    sb.append("Generate a passage-based question group in JSON format. ");
    sb.append("The passage must be in English and should be interesting, educational, and appropriate for the CEFR level specified.\n\n");
    sb.append("Return ONLY a valid JSON object with this exact structure (no markdown, no explanation):\n");
    sb.append("{\n");
    sb.append("  \"passage\": \"<English passage text>\",\n");
    sb.append("  \"audioUrl\": <null or a sample audio URL>,\n");
    sb.append("  \"imageUrl\": <null or a sample image URL>,\n");
    sb.append("  \"skill\": \"<READING or LISTENING>\",\n");
    sb.append("  \"cefrLevel\": \"<A1 or A2 or B1 or B2 or C1 or C2>\",\n");
    sb.append("  \"topic\": \"<topic name>\",\n");
    sb.append("  \"explanation\": \"<brief explanation of the passage>\",\n");
    sb.append("  \"questions\": [\n");
    sb.append("    {\n");
    sb.append("      \"content\": \"<question text in English>\",\n");
    sb.append("      \"questionType\": \"<MULTIPLE_CHOICE_SINGLE or MULTIPLE_CHOICE_MULTI or FILL_IN_BLANK or MATCHING>\",\n");
    sb.append("      \"options\": [{ \"title\": \"<option text>\", \"correct\": <true or false> }, ...],\n");
    sb.append("      \"correctAnswer\": <null or answer text for FILL_IN_BLANK>,\n");
    sb.append("      \"matchLeft\": <null or [\"left1\",\"left2\"]>,\n");
    sb.append("      \"matchRight\": <null or [\"right1\",\"right2\"]>,\n");
    sb.append("      \"correctPairs\": <null or [1,2] where numbers are 1-based indices into matchRight>,\n");
    sb.append("      \"cefrLevel\": \"<same as group or different>\",\n");
    sb.append("      \"topic\": \"<same as group or more specific>\",\n");
    sb.append("      \"explanation\": \"<explanation for this specific question>\"\n");
    sb.append("    }\n");
    sb.append("  ]\n");
    sb.append("}\n\n");

    // Rules
    sb.append("Rules:\n");
    sb.append("- The passage must be between 150-400 words.\n");
    sb.append("- Generate exactly ").append(questionCount).append(" questions.\n");
    sb.append("- Question types allowed: ").append(
            questionTypes != null && !questionTypes.isEmpty()
                ? String.join(", ", questionTypes)
                : "MULTIPLE_CHOICE_SINGLE, MULTIPLE_CHOICE_MULTI, FILL_IN_BLANK, MATCHING")
      .append(".\n");
    sb.append("- MULTIPLE_CHOICE_SINGLE must have exactly 1 correct answer and at least 3 distractors.\n");
    sb.append("- MULTIPLE_CHOICE_MULTI must have 2 or more correct answers (but not all).\n");
    sb.append("- FILL_IN_BLANK must have a correctAnswer field.\n");
    sb.append("- MATCHING must have matchLeft, matchRight (same size, 3-5 items each), and correctPairs (1-based indices).\n");
    sb.append("- CEFR level: ").append(cefrLevel != null ? cefrLevel : "B1").append(".\n");
    sb.append("- All content must be in English.\n");
    sb.append("- Return ONLY the JSON object, no markdown fences, no commentary.\n");
    sb.append("- Topic: ").append(topic != null ? topic : "General English").append(".\n");

    return sb.toString();
}
```

---

### Task 1.5: Thêm generateGroup() trong AIQuestionServiceImpl

**Files:**
- Modify: `DoAn/src/main/java/com/example/DoAn/service/impl/AIQuestionServiceImpl.java`

Thêm method sau vào class `AIQuestionServiceImpl`:

- [ ] **Step 1: Thêm import**

```java
import com.example.DoAn.dto.request.AIGenerateGroupRequestDTO;
import com.example.DoAn.dto.response.AIGenerateGroupResponseDTO;
```

- [ ] **Step 2: Thêm method generateGroup()**

```java
@Override
public AIGenerateGroupResponseDTO generateGroup(AIGenerateGroupRequestDTO request, String userEmail) {
    if (!rateLimitStore.isAllowed(userEmail)) {
        throw new RateLimitExceededException(
                "Bạn đã vượt giới hạn 10 yêu cầu/phút. Vui lòng chờ một chút.");
    }

    String topic = request.hasTopic() ? request.getTopic()
            : (request.hasModuleId() ? "Module-based content" : "");
    String skill = request.getSkill() != null ? request.getSkill() : "READING";
    String cefr = request.getCefrLevel() != null ? request.getCefrLevel() : "B1";
    int qty = request.getQuantity() != null ? request.getQuantity() : 5;

    String prompt = promptBuilder.buildGroupPrompt(topic, skill, cefr, qty, request.getQuestionTypes());
    String rawJson = callGroq(prompt);

    AIGenerateGroupResponseDTO.GroupResponseBuilder builder = AIGenerateGroupResponseDTO.builder()
            .skill(skill)
            .cefrLevel(cefr)
            .topic(topic);

    // Parse flat JSON
    try {
        Map<String, Object> parsed = objectMapper.readValue(rawJson.trim(), new TypeReference<>() {});
        if (parsed.get("passage") != null) builder.passage((String) parsed.get("passage"));
        if (parsed.get("audioUrl") != null) builder.audioUrl((String) parsed.get("audioUrl"));
        if (parsed.get("imageUrl") != null) builder.imageUrl((String) parsed.get("imageUrl"));
        if (parsed.get("cefrLevel") != null) builder.cefrLevel((String) parsed.get("cefrLevel"));
        if (parsed.get("topic") != null) builder.topic((String) parsed.get("topic"));
        if (parsed.get("explanation") != null) builder.explanation((String) parsed.get("explanation"));
        if (parsed.get("skill") != null) builder.skill((String) parsed.get("skill"));

        List<AIGenerateResponseDTO.QuestionDTO> questions = new ArrayList<>();
        List<?> rawQuestions = (List<?>) parsed.get("questions");
        if (rawQuestions != null) {
            int validCount = 0;
            for (Object raw : rawQuestions) {
                if (raw instanceof Map<?, ?> m) {
                    AIGenerateResponseDTO.QuestionDTO dto = toQuestionDTO(new HashMap<>((Map<String, Object>) m));
                    if (dto != null && isValid(dto)) {
                        // Override with group-level skill/cefr if not set
                        if (dto.getSkill() == null) dto.setSkill(skill);
                        if (dto.getCefrLevel() == null) dto.setCefrLevel(cefr);
                        if (dto.getTopic() == null) dto.setTopic(topic);
                        questions.add(dto);
                        validCount++;
                    }
                }
            }
            if (questions.size() < qty) {
                builder.warning(String.format("AI chỉ sinh được %d/%d câu hỏi.", questions.size(), qty));
            }
        }
        builder.questions(questions);
    } catch (Exception e) {
        log.error("Failed to parse group JSON: {}", e.getMessage());
        // Try to extract just passage and questions even if partial
        builder.questions(new ArrayList<>());
        builder.warning("AI trả về định dạng không hoàn chỉnh. Vui lòng thử lại.");
    }

    return builder.build();
}
```

- [ ] **Step 3: Thêm interface method trong AIQuestionService.java**

Tìm interface `AIQuestionService.java` và thêm:

```java
AIGenerateGroupResponseDTO generateGroup(AIGenerateGroupRequestDTO request, String userEmail);
```

---

## Chunk 2: Backend - ExpertQuestionController (AI Group Endpoints)

### Task 2.1: Thêm AI Group endpoints trong ExpertQuestionController

**Files:**
- Modify: `DoAn/src/main/java/com/example/DoAn/controller/ExpertQuestionController.java`

- [ ] **Step 1: Thêm import**

```java
import com.example.DoAn.dto.request.AIGenerateGroupRequestDTO;
import com.example.DoAn.dto.request.AIImportGroupRequestDTO;
import com.example.DoAn.dto.response.AIGenerateGroupResponseDTO;
```

- [ ] **Step 2: Thêm 2 endpoint mới sau @PostMapping("/ai/import")**

```java
@Operation(summary = "Generate a passage-based question group using AI")
@PostMapping("/ai/generate/group")
public ResponseData<AIGenerateGroupResponseDTO> generateQuestionGroup(
        @Valid @RequestBody AIGenerateGroupRequestDTO request, Principal principal) {
    if (!request.isValid()) {
        return ResponseData.error(400, "Phải nhập topic hoặc chọn module.");
    }
    AIGenerateGroupResponseDTO result = aiQuestionService.generateGroup(request, getEmail(principal));
    return ResponseData.success("Sinh bộ câu hỏi thành công", result);
}

@Operation(summary = "Save selected AI-generated question group to DB")
@PostMapping("/ai/import/group")
public ResponseData<Void> importAIQuestionGroup(
        @Valid @RequestBody AIImportGroupRequestDTO request, Principal principal) {
    int saved = questionService.saveAIQuestionGroup(request, getEmail(principal));
    return new ResponseData<>(HttpStatus.CREATED.value(),
            "Đã lưu bộ câu hỏi với " + saved + " câu con.", null);
}
```

---

## Chunk 3: Backend - ExpertQuestionServiceImpl (saveAIQuestionGroup)

### Task 3.1: Thêm saveAIQuestionGroup trong ExpertQuestionServiceImpl

**Files:**
- Modify: `DoAn/src/main/java/com/example/DoAn/service/impl/ExpertQuestionServiceImpl.java`

- [ ] **Step 1: Thêm import**

```java
import com.example.DoAn.dto.request.AIImportGroupRequestDTO;
```

- [ ] **Step 2: Thêm method saveAIQuestionGroup() sau saveAIQuestions()**

```java
@Override
@Transactional
public int saveAIQuestionGroup(AIImportGroupRequestDTO request, String email) {
    User expert = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyên gia."));

    // Validate
    if (request.getQuestions() == null || request.getQuestions().isEmpty()) {
        throw new InvalidDataException("Bộ câu hỏi phải có ít nhất 1 câu hỏi con.");
    }

    // Create QuestionGroup
    QuestionGroup group = QuestionGroup.builder()
            .groupContent(request.getPassage())
            .audioUrl(request.getAudioUrl())
            .imageUrl(request.getImageUrl())
            .skill(request.getSkill() != null ? request.getSkill().toUpperCase() : "READING")
            .cefrLevel(request.getCefrLevel() != null ? request.getCefrLevel().toUpperCase() : "B1")
            .topic(request.getTopic())
            .explanation(request.getExplanation())
            .status(request.getStatus() != null ? request.getStatus() : "DRAFT")
            .user(expert)
            .build();
    questionGroupRepository.save(group);

    int saved = 0;
    for (AIImportRequestDTO.AIQuestionDTO qdto : request.getQuestions()) {
        Question question = Question.builder()
                .questionGroup(group)
                .user(expert)
                .content(qdto.getContent())
                .questionType(qdto.getQuestionType() != null ? qdto.getQuestionType() : "MULTIPLE_CHOICE_SINGLE")
                .skill(qdto.getSkill() != null ? qdto.getSkill().toUpperCase()
                        : (group.getSkill() != null ? group.getSkill() : "READING"))
                .cefrLevel(qdto.getCefrLevel() != null ? qdto.getCefrLevel().toUpperCase()
                        : (group.getCefrLevel() != null ? group.getCefrLevel() : "B1"))
                .topic(qdto.getTopic() != null ? qdto.getTopic() : group.getTopic())
                .explanation(qdto.getExplanation())
                .status("PUBLISHED")
                .source("EXPERT_BANK")
                .createdMethod("AI_GENERATED")
                .build();
        questionRepository.save(question);

        // Save options
        if (qdto.getOptions() != null && !qdto.getOptions().isEmpty()) {
            int idx = 0;
            for (AIImportRequestDTO.AIOptionDTO opt : qdto.getOptions()) {
                AnswerOption ao = AnswerOption.builder()
                        .question(question)
                        .title(opt.getTitle())
                        .correctAnswer(Boolean.TRUE.equals(opt.getCorrect()))
                        .orderIndex(idx++)
                        .build();
                answerOptionRepository.save(ao);
            }
        } else if (qdto.getMatchLeft() != null && qdto.getMatchRight() != null
                && qdto.getCorrectPairs() != null) {
            List<String> left = qdto.getMatchLeft();
            List<String> right = qdto.getMatchRight();
            List<Integer> pairs = qdto.getCorrectPairs();
            for (int i = 0; i < left.size(); i++) {
                int rightIdx = pairs.get(i) - 1;
                answerOptionRepository.save(AnswerOption.builder()
                        .question(question)
                        .title(left.get(i))
                        .correctAnswer(false)
                        .orderIndex(i)
                        .matchTarget(right.get(rightIdx))
                        .build());
                answerOptionRepository.save(AnswerOption.builder()
                        .question(question)
                        .title(right.get(rightIdx))
                        .correctAnswer(false)
                        .orderIndex(left.size() + i)
                        .build());
            }
        }
        saved++;
    }
    return saved;
}
```

- [ ] **Step 3: Thêm interface method trong IExpertQuestionService.java**

```java
int saveAIQuestionGroup(AIImportGroupRequestDTO request, String email);
```

---

## Chunk 4: Backend - Excel Import Question Group

### Task 4.1: Tạo ExcelQuestionGroupDTO

**Files:**
- Create: `DoAn/src/main/java/com/example/DoAn/dto/request/ExcelQuestionGroupDTO.java`

- [ ] **Step 1: Tạo file**

```java
package com.example.DoAn.dto.request;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExcelQuestionGroupDTO {

    private String passage;
    private String skill = "READING";
    private String cefrLevel = "B1";
    private String topic;
    private String audioUrl;
    private String imageUrl;
    private String explanation;
    private List<ExcelImportRequestDTO.ExcelQuestionDTO> questions;
}
```

---

### Task 4.2: Tạo ExcelImportGroupRequestDTO

**Files:**
- Create: `DoAn/src/main/java/com/example/DoAn/dto/request/ExcelImportGroupRequestDTO.java`

- [ ] **Step 1: Tạo file**

```java
package com.example.DoAn.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExcelImportGroupRequestDTO {

    @NotNull
    private ExcelQuestionGroupDTO group;
}
```

---

### Task 4.3: Tạo ExcelParseGroupResultDTO

**Files:**
- Create: `DoAn/src/main/java/com/example/DoAn/dto/response/ExcelParseGroupResultDTO.java`

- [ ] **Step 1: Tạo file**

```java
package com.example.DoAn.dto.response;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExcelParseGroupResultDTO {

    private List<ValidGroupRowDTO> valid;
    private List<ErrorRowDTO> errors;
    private int totalRows;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ValidGroupRowDTO {
        private int rowIndex;
        private ExcelQuestionGroupDTO group;
        private List<ExcelParseResultDTO.ValidRowDTO> questions;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ErrorRowDTO {
        private int rowIndex;
        private String message;
        private java.util.Map<String, String> rawData;
    }
}
```

---

### Task 4.4: Thêm method parseGroupFile và importQuestionGroups trong ExcelQuestionImportServiceImpl

**Files:**
- Modify: `DoAn/src/main/java/com/example/DoAn/service/impl/ExcelQuestionImportServiceImpl.java`

- [ ] **Step 1: Thêm import**

```java
import com.example.DoAn.dto.request.AIImportGroupRequestDTO;
import com.example.DoAn.dto.request.ExcelQuestionGroupDTO;
import com.example.DoAn.dto.request.ExcelImportGroupRequestDTO;
import com.example.DoAn.dto.response.ExcelParseGroupResultDTO;
import com.example.DoAn.model.QuestionGroup;
import com.example.DoAn.repository.QuestionGroupRepository;
import com.example.DoAn.util.AIQuestionPromptBuilder;
```

- [ ] **Step 2: Thêm field vào class**

```java
private final QuestionGroupRepository questionGroupRepository;
```

- [ ] **Step 3: Thêm method parseGroupFile()**

```java
@Override
public ExcelParseGroupResultDTO parseGroupFile(MultipartFile file) throws Exception {
    List<ExcelParseGroupResultDTO.ValidGroupRowDTO> valid = new ArrayList<>();
    List<ExcelParseGroupResultDTO.ErrorRowDTO> errors = new ArrayList<>();

    try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
        Sheet sheet = wb.getSheetAt(0);

        // Row 0 = group header, Row 1 = group data, Row 2 = blank
        // Row 3 = child header, Row 4+ = child questions
        Row groupHeaderRow = sheet.getRow(0);
        Row groupDataRow = sheet.getRow(1);
        Row childHeaderRow = sheet.getRow(3);

        if (groupHeaderRow == null || groupDataRow == null) {
            throw new IllegalArgumentException("File Excel không đúng format. Cần có row 1 (group header) và row 2 (group data).");
        }

        // Parse group metadata
        String passage = trim(getCell(groupDataRow, 0));
        if (passage == null || passage.isBlank()) {
            errors.add(ExcelParseGroupResultDTO.ErrorRowDTO.builder()
                    .rowIndex(2)
                    .message("Passage không được trống.")
                    .rawData(java.util.Collections.singletonMap("passage", passage))
                    .build());
            return ExcelParseGroupResultDTO.builder().valid(valid).errors(errors).totalRows(0).build();
        }

        String skill = trim(getCell(groupDataRow, 1));
        String cefr = trim(getCell(groupDataRow, 2));
        String topic = trim(getCell(groupDataRow, 3));
        String audioUrl = trim(getCell(groupDataRow, 4));
        String imageUrl = trim(getCell(groupDataRow, 5));
        String explanation = trim(getCell(groupDataRow, 6));

        // Validate skill/cefr
        if (skill != null && !promptBuilder.isValidSkill(skill)) {
            errors.add(ExcelParseGroupResultDTO.ErrorRowDTO.builder()
                    .rowIndex(2).message("Skill '" + skill + "' không hợp lệ.")
                    .rawData(java.util.Collections.singletonMap("skill", skill)).build());
        }
        if (cefr != null && !promptBuilder.isValidCefr(cefr)) {
            errors.add(ExcelParseGroupResultDTO.ErrorRowDTO.builder()
                    .rowIndex(2).message("CEFR '" + cefr + "' không hợp lệ (A1-C2).")
                    .rawData(java.util.Collections.singletonMap("cefr", cefr)).build());
        }

        // Parse child questions (row 4 onwards)
        List<ExcelParseResultDTO.ValidRowDTO> childQuestions = new ArrayList<>();
        for (int i = 4; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isEmptyRow(row)) continue;

            int rowIdx = i + 1;
            String content = trim(getCell(row, 0));
            if (content == null || content.isBlank()) continue;

            try {
                // Detect question type from column 1
                String typeStr = trim(getCell(row, 1));
                String questionType = detectQuestionType(typeStr, "MULTIPLE_CHOICE_SINGLE");
                String childSkill = trim(getCell(row, 10));
                String childCefr = trim(getCell(row, 11));
                String childTopic = trim(getCell(row, 12));
                String childExplanation = trim(getCell(row, 13));

                if (childSkill != null && !promptBuilder.isValidSkill(childSkill)) {
                    throw new ValidationException("Skill '" + childSkill + "' không hợp lệ.");
                }
                if (childCefr != null && !promptBuilder.isValidCefr(childCefr)) {
                    throw new ValidationException("CEFR '" + childCefr + "' không hợp lệ.");
                }

                ExcelParseResultDTO.ValidRowDTO.ValidRowDTOBuilder childBuilder = ExcelParseResultDTO.ValidRowDTO.builder()
                        .rowIndex(rowIdx)
                        .content(content)
                        .questionType(questionType)
                        .skill(childSkill != null ? childSkill.toUpperCase() : (skill != null ? skill.toUpperCase() : "READING"))
                        .cefrLevel(childCefr != null ? childCefr.toUpperCase() : (cefr != null ? cefr.toUpperCase() : "B1"))
                        .topic(childTopic != null ? childTopic : topic)
                        .explanation(childExplanation != null ? childExplanation : explanation)
                        .selected(true);

                switch (questionType) {
                    case "MULTIPLE_CHOICE_SINGLE", "MULTIPLE_CHOICE_MULTI" -> {
                        String optA = trim(getCell(row, 2));
                        String optB = trim(getCell(row, 3));
                        String optC = trim(getCell(row, 4));
                        String optD = trim(getCell(row, 5));
                        String correct = trim(getCell(row, 6));
                        if ((optA == null && optB == null && optC == null && optD == null) ||
                                (correct == null || correct.isBlank())) {
                            throw new ValidationException("Thiếu đáp án hoặc đáp án đúng.");
                        }
                        List<ExcelParseResultDTO.OptionDTO> opts = new ArrayList<>();
                        int idx = 0;
                        for (String label : List.of("A", "B", "C", "D")) {
                            String val = idx == 0 ? optA : idx == 1 ? optB : idx == 2 ? optC : optD;
                            if (val != null && !val.isBlank()) {
                                opts.add(ExcelParseResultDTO.OptionDTO.builder()
                                        .title(val).correct(correct.toUpperCase().contains(label.toUpperCase())).build());
                            }
                            idx++;
                        }
                        childBuilder.options(opts).correctAnswer(correct);
                    }
                    case "FILL_IN_BLANK" -> {
                        String answer = trim(getCell(row, 6));
                        if (answer == null || answer.isBlank()) {
                            throw new ValidationException("Thiếu đáp án cho FILL_IN_BLANK.");
                        }
                        childBuilder.correctAnswer(answer);
                    }
                    case "MATCHING" -> {
                        String leftStr = trim(getCell(row, 7));
                        String rightStr = trim(getCell(row, 8));
                        String pairsStr = trim(getCell(row, 9));
                        if (leftStr == null || rightStr == null || pairsStr == null) {
                            throw new ValidationException("Thiếu cột ghép nối cho MATCHING.");
                        }
                        List<String> left = split(leftStr, "|");
                        List<String> right = split(rightStr, "|");
                        List<Integer> pairs = parsePairs(pairsStr);
                        if (left.size() != right.size()) {
                            throw new ValidationException("Số cặp ghép không khớp.");
                        }
                        if (pairs.size() != left.size()) {
                            throw new ValidationException("CorrectPairs phải có đúng " + left.size() + " số.");
                        }
                        childBuilder.matchLeft(left).matchRight(right).correctPairs(pairs);
                    }
                }
                childQuestions.add(childBuilder.build());
            } catch (ValidationException ve) {
                Map<String, String> raw = new LinkedHashMap<>();
                for (int c = 0; c < 14; c++) raw.put("col" + c, getCell(row, c));
                errors.add(ExcelParseGroupResultDTO.ErrorRowDTO.builder()
                        .rowIndex(rowIdx).message(ve.getMessage()).rawData(raw).build());
            }
        }

        if (childQuestions.isEmpty() && errors.isEmpty()) {
            errors.add(ExcelParseGroupResultDTO.ErrorRowDTO.builder()
                    .rowIndex(5).message("Không tìm thấy câu hỏi con nào trong file.")
                    .rawData(java.util.Collections.singletonMap("passage", passage)).build());
        }

        if (!errors.isEmpty() && childQuestions.isEmpty()) {
            // Group-level error only, no valid groups
        } else if (!childQuestions.isEmpty()) {
            valid.add(ExcelParseGroupResultDTO.ValidGroupRowDTO.builder()
                    .rowIndex(2)
                    .group(ExcelQuestionGroupDTO.builder()
                            .passage(passage)
                            .skill(skill != null ? skill.toUpperCase() : "READING")
                            .cefrLevel(cefr != null ? cefr.toUpperCase() : "B1")
                            .topic(topic)
                            .audioUrl(audioUrl)
                            .imageUrl(imageUrl)
                            .explanation(explanation)
                            .questions(childQuestions.stream()
                                    .map(q -> ExcelImportRequestDTO.ExcelQuestionDTO.builder()
                                            .content(q.getContent())
                                            .questionType(q.getQuestionType())
                                            .skill(q.getSkill())
                                            .cefrLevel(q.getCefrLevel())
                                            .topic(q.getTopic())
                                            .explanation(q.getExplanation())
                                            .options(q.getOptions() != null ? q.getOptions().stream()
                                                    .map(o -> ExcelImportRequestDTO.OptionDTO.builder()
                                                            .title(o.getTitle())
                                                            .correct(o.getCorrect())
                                                            .build())
                                                    .toList() : null)
                                            .correctAnswer(q.getCorrectAnswer())
                                            .matchLeft(q.getMatchLeft())
                                            .matchRight(q.getMatchRight())
                                            .correctPairs(q.getCorrectPairs())
                                            .build())
                                    .toList())
                            .build())
                    .questions(childQuestions)
                    .build());
        }
    }

    return ExcelParseGroupResultDTO.builder()
            .valid(valid)
            .errors(errors)
            .totalRows(valid.size() + errors.size())
            .build();
}
```

- [ ] **Step 4: Thêm helper detectQuestionType() và importQuestionGroups()**

Thêm vào cuối class, trước `private static class ValidationException`:

```java
private String detectQuestionType(String val, String fallback) {
    if (val == null || val.isBlank()) return fallback;
    return switch (val.toUpperCase().replace(" ", "_").replace("-", "_")) {
        case "MULTIPLE_CHOICE_SINGLE", "MC_SINGLE", "MC" -> "MULTIPLE_CHOICE_SINGLE";
        case "MULTIPLE_CHOICE_MULTI", "MC_MULTI" -> "MULTIPLE_CHOICE_MULTI";
        case "FILL_IN_BLANK", "FILL", "FIB" -> "FILL_IN_BLANK";
        case "MATCHING", "MATCH" -> "MATCHING";
        case "WRITING" -> "WRITING";
        case "SPEAKING" -> "SPEAKING";
        default -> fallback;
    };
}

@Override
@Transactional
public int importQuestionGroups(ExcelImportGroupRequestDTO request, String userEmail) throws Exception {
    User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new IllegalArgumentException("User không tồn tại: " + userEmail));

    ExcelQuestionGroupDTO groupDto = request.getGroup();
    if (groupDto.getQuestions() == null || groupDto.getQuestions().isEmpty()) {
        throw new IllegalArgumentException("Bộ câu hỏi phải có ít nhất 1 câu hỏi con.");
    }

    QuestionGroup group = QuestionGroup.builder()
            .groupContent(groupDto.getPassage())
            .audioUrl(groupDto.getAudioUrl())
            .imageUrl(groupDto.getImageUrl())
            .skill(groupDto.getSkill() != null ? groupDto.getSkill().toUpperCase() : "READING")
            .cefrLevel(groupDto.getCefrLevel() != null ? groupDto.getCefrLevel().toUpperCase() : "B1")
            .topic(groupDto.getTopic())
            .explanation(groupDto.getExplanation())
            .status("DRAFT")
            .source("EXPERT_BANK")
            .createdMethod("EXCEL_IMPORTED")
            .user(user)
            .build();
    questionGroupRepository.save(group);

    int saved = 0;
    for (ExcelImportRequestDTO.ExcelQuestionDTO qdto : groupDto.getQuestions()) {
        Question question = Question.builder()
                .questionGroup(group)
                .user(user)
                .content(qdto.getContent())
                .questionType(qdto.getQuestionType() != null ? qdto.getQuestionType() : "MULTIPLE_CHOICE_SINGLE")
                .skill(qdto.getSkill() != null ? qdto.getSkill().toUpperCase() : group.getSkill())
                .cefrLevel(qdto.getCefrLevel() != null ? qdto.getCefrLevel().toUpperCase() : group.getCefrLevel())
                .topic(qdto.getTopic() != null ? qdto.getTopic() : group.getTopic())
                .explanation(qdto.getExplanation())
                .status("DRAFT")
                .source("EXPERT_BANK")
                .createdMethod("EXCEL_IMPORTED")
                .build();
        questionRepository.save(question);

        if (qdto.getOptions() != null && !qdto.getOptions().isEmpty()) {
            int idx = 0;
            for (ExcelImportRequestDTO.OptionDTO opt : qdto.getOptions()) {
                answerOptionRepository.save(AnswerOption.builder()
                        .question(question)
                        .title(opt.getTitle())
                        .correctAnswer(opt.getCorrect())
                        .orderIndex(idx++)
                        .build());
            }
        } else if (qdto.getMatchLeft() != null && qdto.getMatchRight() != null
                && qdto.getCorrectPairs() != null) {
            List<String> left = qdto.getMatchLeft();
            List<String> right = qdto.getMatchRight();
            List<Integer> pairs = qdto.getCorrectPairs();
            for (int i = 0; i < left.size(); i++) {
                int rightIdx = pairs.get(i) - 1;
                answerOptionRepository.save(AnswerOption.builder()
                        .question(question).title(left.get(i)).correctAnswer(false)
                        .orderIndex(i).matchTarget(right.get(rightIdx)).build());
                answerOptionRepository.save(AnswerOption.builder()
                        .question(question).title(right.get(rightIdx)).correctAnswer(false)
                        .orderIndex(left.size() + i).build());
            }
        }
        saved++;
    }
    return saved;
}
```

- [ ] **Step 5: Cập nhật interface ExcelQuestionImportService.java**

Thêm 2 method:

```java
ExcelParseGroupResultDTO parseGroupFile(MultipartFile file) throws Exception;
int importQuestionGroups(ExcelImportGroupRequestDTO request, String userEmail) throws Exception;
```

---

### Task 4.5: Thêm template generator cho Question Group

**Files:**
- Modify: `DoAn/src/main/java/com/example/DoAn/util/ExcelTemplateGenerator.java`

- [ ] **Step 1: Thêm import**

```java
import com.example.DoAn.dto.request.ExcelImportGroupRequestDTO;
```

- [ ] **Step 2: Thêm method generateGroupTemplate()**

```java
public byte[] generateGroupTemplate() throws IOException {
    try (Workbook wb = new XSSFWorkbook()) {

        // Sheet 1: Group Info
        Sheet groupSheet = wb.createSheet("Group Info");
        Row h0 = groupSheet.createRow(0);
        h0.createCell(0).setCellValue("PASSAGE");
        h0.createCell(1).setCellValue("SKILL");
        h0.createCell(2).setCellValue("CEFR");
        h0.createCell(3).setCellValue("TOPIC");
        h0.createCell(4).setCellValue("AUDIO_URL");
        h0.createCell(5).setCellValue("IMAGE_URL");
        h0.createCell(6).setCellValue("EXPLANATION");

        Row d1 = groupSheet.createRow(1);
        d1.createCell(0).setCellValue("Enter the passage text here (English, 150-400 words)...");
        d1.createCell(1).setCellValue("READING");
        d1.createCell(2).setCellValue("B1");
        d1.createCell(3).setCellValue("Education");
        d1.createCell(6).setCellValue("Brief explanation of the passage...");

        Row blank = groupSheet.createRow(2);
        blank.createCell(0).setCellValue("--- Child questions start from row 5 below ---");

        // Sheet 2: Child Questions
        Sheet childSheet = wb.createSheet("Child Questions");
        Row ch = childSheet.createRow(0);
        String[] headers = {"CONTENT", "TYPE", "OPTA", "OPTB", "OPTC", "OPTD", "CORRECT/ANSWER",
                "MATCH_LEFT(pipe|separated)", "MATCH_RIGHT(pipe|separated)", "PAIRS(comma,sep)",
                "SUB_SKILL", "SUB_CEFR", "SUB_TOPIC", "SUB_EXPLANATION"};
        for (int i = 0; i < headers.length; i++) ch.createCell(i).setCellValue(headers[i]);

        Row ex1 = childSheet.createRow(1);
        ex1.createCell(0).setCellValue("What is the main idea of the passage?");
        ex1.createCell(1).setCellValue("MULTIPLE_CHOICE_SINGLE");
        ex1.createCell(2).setCellValue("Option A text");
        ex1.createCell(3).setCellValue("Option B text");
        ex1.createCell(4).setCellValue("Option C text");
        ex1.createCell(5).setCellValue("Option D text");
        ex1.createCell(6).setCellValue("A");

        Row ex2 = childSheet.createRow(2);
        ex2.createCell(0).setCellValue("Complete: The author believes that ____.");
        ex2.createCell(1).setCellValue("FILL_IN_BLANK");
        ex2.createCell(6).setCellValue("education is key");

        Row ex3 = childSheet.createRow(3);
        ex3.createCell(0).setCellValue("Match the words with their meanings.");
        ex3.createCell(1).setCellValue("MATCHING");
        ex3.createCell(7).setCellValue("abundant|evidence|climate");
        ex3.createCell(8).setCellValue("plentiful|proof|weather");
        ex3.createCell(9).setCellValue("1,2,3");

        // Column widths
        groupSheet.setColumnWidth(0, 10000);
        childSheet.setColumnWidth(0, 6000);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            wb.write(baos);
            return baos.toByteArray();
        }
    }
}
```

- [ ] **Step 3: Cập nhật method generate() trong ExcelTemplateGenerator**

Thêm case vào switch trong `generate()`:

```java
case "QUESTION_GROUP" -> generateGroupTemplate();
```

---

### Task 4.6: Thêm Excel Group endpoints trong ExpertQuestionController

**Files:**
- Modify: `DoAn/src/main/java/com/example/DoAn/controller/ExpertQuestionController.java`

- [ ] **Step 1: Thêm import**

```java
import com.example.DoAn.dto.request.ExcelImportGroupRequestDTO;
import com.example.DoAn.dto.response.ExcelParseGroupResultDTO;
```

- [ ] **Step 2: Thêm 3 endpoint mới sau excel/import endpoint**

```java
@Operation(summary = "Parse uploaded Excel file for Question Group")
@PostMapping(value = "/excel/parse-group", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseData<ExcelParseGroupResultDTO> parseGroupExcel(
        @RequestParam("file") MultipartFile file) {
    try {
        if (file.isEmpty()) return ResponseData.error(400, "Vui lòng chọn file Excel.");
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            return ResponseData.error(400, "Chỉ chấp nhận file .xlsx");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            return ResponseData.error(413, "File không được vượt quá 5MB.");
        }
        ExcelParseGroupResultDTO result = excelService.parseGroupFile(file);
        return ResponseData.success("Đã phân tích file", result);
    } catch (Exception e) {
        return ResponseData.error(500, "Lỗi khi đọc file: " + e.getMessage());
    }
}

@Operation(summary = "Import validated Question Groups from Excel")
@PostMapping("/excel/import-group")
public ResponseData<Void> importGroupExcel(
        @Valid @RequestBody ExcelImportGroupRequestDTO request, Principal principal) {
    try {
        int saved = excelService.importQuestionGroups(request, getEmail(principal));
        return new ResponseData<>(HttpStatus.CREATED.value(),
                "Đã import bộ câu hỏi với " + saved + " câu con.", null);
    } catch (Exception e) {
        return ResponseData.error(500, "Lỗi khi lưu: " + e.getMessage());
    }
}
```

---

## Chunk 5: Frontend - question-bank.html (AI Modal + Import Modal)

### Task 5.1: Thêm tab "Bộ câu hỏi" trong AI Modal

**Files:**
- Modify: `DoAn/src/main/resources/templates/expert/question-bank.html`

- [ ] **Step 1: Thêm tab navigation mới sau tabModule**

```html
<li class="nav-item"><button class="nav-link" data-bs-toggle="tab" data-bs-target="#tabGroup">Bộ câu hỏi</button></li>
```

- [ ] **Step 2: Thêm tab content mới sau tabModule content**

```html
<div class="tab-pane fade" id="tabGroup">
    <div class="row g-3">
        <div class="col-md-4">
            <label class="form-label fw-bold">Chủ đề <span class="text-danger">*</span></label>
            <input type="text" id="groupTopic" class="form-control" placeholder="VD: Climate Change, Technology...">
        </div>
        <div class="col-md-3">
            <label class="form-label fw-bold">Kỹ năng</label>
            <select id="groupSkill" class="form-select">
                <option value="READING" selected>Reading</option>
                <option value="LISTENING">Listening</option>
            </select>
        </div>
        <div class="col-md-2">
            <label class="form-label fw-bold">CEFR</label>
            <select id="groupCefr" class="form-select">
                <option value="A1">A1</option>
                <option value="A2">A2</option>
                <option value="B1" selected>B1</option>
                <option value="B2">B2</option>
                <option value="C1">C1</option>
                <option value="C2">C2</option>
            </select>
        </div>
        <div class="col-md-3">
            <label class="form-label fw-bold">Số câu hỏi con (2–20)</label>
            <input type="number" id="groupQty" class="form-control" min="2" max="20" value="5">
        </div>
    </div>
    <div class="text-end mt-3">
        <button class="btn btn-primary" onclick="generateAIGroup()">
            <i class="bi bi-lightning-charge me-1"></i> Sinh bộ câu hỏi
        </button>
    </div>
</div>
```

- [ ] **Step 3: Thêm preview container cho group sau aiPreview**

Tìm `<div id="aiPreview"...` và thêm bên dưới:

```html
<div id="groupPreview" class="mt-3" style="display:none;">
    <div class="d-flex justify-content-between align-items-center mb-2">
        <h6 class="m-0">Passage + Câu hỏi con</h6>
    </div>
    <div id="groupPreviewContent"></div>
    <div class="text-end mt-3">
        <button class="btn text-white" style="background-color: #136ad5;" onclick="importAIGroup()">
            <i class="bi bi-check-lg me-1"></i> Lưu bộ câu hỏi
        </button>
    </div>
</div>
```

---

### Task 5.2: Thêm JS cho AI Group generate/import

**Files:**
- Modify: `DoAn/src/main/resources/templates/expert/question-bank.html`

Thêm vào `<script>` section, sau `toggleAllAI()`:

- [ ] **Step 1: Thêm biến**

```javascript
let groupPreview = null;
```

- [ ] **Step 2: Thêm function generateAIGroup()**

```javascript
async function generateAIGroup() {
    const topic = document.getElementById('groupTopic').value.trim();
    const skill = document.getElementById('groupSkill').value;
    const cefr = document.getElementById('groupCefr').value;
    const qty = parseInt(document.getElementById('groupQty').value);

    if (!topic) { alert('Vui lòng nhập chủ đề.'); return; }
    if (!qty || qty < 2 || qty > 20) { alert('Số câu hỏi con phải từ 2-20.'); return; }

    const body = {
        topic: topic,
        quantity: qty,
        skill: skill,
        cefrLevel: cefr
    };

    document.getElementById('aiLoading').style.display = 'block';
    document.getElementById('aiPreview').style.display = 'none';
    document.getElementById('groupPreview').style.display = 'none';
    document.getElementById('aiWarning').style.display = 'none';

    try {
        const res = await fetch('/api/v1/expert/questions/ai/generate/group', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        const result = await res.json();
        if (!res.ok) { alert(result.message || 'Lỗi khi sinh bộ câu hỏi.'); return; }

        groupPreview = result.data;
        if (groupPreview.warning) {
            const w = document.getElementById('aiWarning');
            w.textContent = groupPreview.warning;
            w.style.display = 'block';
        }
        renderGroupPreview();
    } catch (e) { alert('Lỗi: ' + e.message); }
    finally { document.getElementById('aiLoading').style.display = 'none'; }
}
```

- [ ] **Step 3: Thêm function renderGroupPreview()**

```javascript
function renderGroupPreview() {
    const container = document.getElementById('groupPreviewContent');
    const g = groupPreview;

    const qList = (g.questions || []).map((q, i) => `
        <div class="list-group-item mb-2" style="border-left: 3px solid #136ad5;">
            <div class="d-flex align-items-start gap-2 mb-1">
                <input type="checkbox" class="form-check-input mt-1" id="gqChk${i}" checked
                    onchange="toggleGroupQ(${i})">
                <div class="flex-grow-1">
                    <div class="d-flex gap-2 flex-wrap mb-1">
                        <span class="badge bg-secondary">${q.questionType || 'MC'}</span>
                        <span class="badge bg-primary">${q.cefrLevel || cefr}</span>
                    </div>
                    <p class="mb-1 small">${escapeHtml(q.content || '')}</p>
                    ${q.options && q.options.length > 0 ? `<ul class="mb-1 small">${q.options.map(o =>
                        `<li class="${o.correct ? 'text-success fw-bold' : ''}">${escapeHtml(o.title)} ${o.correct ? '✓' : ''}</li>`
                    ).join('')}</ul>` : ''}
                    ${q.correctAnswer ? `<p class="mb-0 small text-muted">Đáp án: ${escapeHtml(q.correctAnswer)}</p>` : ''}
                </div>
            </div>
        </div>
    `).join('');

    container.innerHTML = `
        <div class="card mb-3 border-primary">
            <div class="card-header bg-primary text-white small fw-bold">PASSAGE</div>
            <div class="card-body" style="max-height: 200px; overflow-y: auto;">
                <p class="mb-1 small">${escapeHtml(g.passage || '')}</p>
                <div class="mt-2 d-flex gap-2 flex-wrap">
                    <span class="badge bg-info text-dark">${g.skill || 'READING'}</span>
                    <span class="badge bg-primary">${g.cefrLevel || 'B1'}</span>
                    ${g.topic ? `<span class="badge bg-secondary">${escapeHtml(g.topic)}</span>` : ''}
                </div>
                ${g.explanation ? `<p class="mb-0 mt-2 small text-muted fst-italic">${escapeHtml(g.explanation)}</p>` : ''}
            </div>
        </div>
        <div class="d-flex justify-content-between align-items-center mb-2">
            <small class="text-muted">${(g.questions || []).length} câu hỏi con</small>
            <button class="btn btn-sm btn-outline-secondary" onclick="toggleAllGroupQ()">Chọn/Bỏ tất cả</button>
        </div>
        <div class="list-group">${qList}</div>
    `;

    document.getElementById('groupPreview').style.display = 'block';
    // Track selected indices
    window.groupSelectedSet = window.groupSelectedSet || new Set((g.questions || []).map((_, i) => i));
}
```

- [ ] **Step 4: Thêm helper functions**

```javascript
function toggleGroupQ(idx) {
    const chk = document.getElementById('gqChk' + idx);
    if (chk.checked) window.groupSelectedSet.add(idx);
    else window.groupSelectedSet.delete(idx);
}

function toggleAllGroupQ() {
    const allSelected = window.groupSelectedSet.size === (groupPreview.questions || []).length;
    window.groupSelectedSet = allSelected
        ? new Set()
        : new Set((groupPreview.questions || []).map((_, i) => i));
    (groupPreview.questions || []).forEach((_, i) => {
        const chk = document.getElementById('gqChk' + i);
        if (chk) chk.checked = !allSelected;
    });
}
```

- [ ] **Step 5: Thêm function importAIGroup()**

```javascript
async function importAIGroup() {
    if (!window.groupSelectedSet || window.groupSelectedSet.size === 0) {
        alert('Vui lòng chọn ít nhất 1 câu hỏi con.'); return;
    }
    const selected = (groupPreview.questions || []).filter((_, i) => window.groupSelectedSet.has(i));
    if (selected.length === 0) { alert('Vui lòng chọn ít nhất 1 câu hỏi con.'); return; }

    const body = {
        passage: groupPreview.passage,
        audioUrl: groupPreview.audioUrl || null,
        imageUrl: groupPreview.imageUrl || null,
        skill: groupPreview.skill || 'READING',
        cefrLevel: groupPreview.cefrLevel || 'B1',
        topic: groupPreview.topic || '',
        explanation: groupPreview.explanation || '',
        status: 'DRAFT',
        questions: selected
    };

    try {
        const res = await fetch('/api/v1/expert/questions/ai/import/group', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        const result = await res.json();
        if (!res.ok) { alert(result.message || 'Lỗi khi lưu.'); return; }
        alert(result.message || 'Đã lưu bộ câu hỏi.');
        aiModal.hide();
        loadQuestions();
    } catch (e) { alert('Lỗi: ' + e.message); }
}
```

---

### Task 5.3: Thêm QUESTION_GROUP vào Import Modal

**Files:**
- Modify: `DoAn/src/main/resources/templates/expert/question-bank.html`

- [ ] **Step 1: Thêm option vào importType select**

```html
<option value="QUESTION_GROUP">Bộ câu hỏi (Passage + Sub-questions)</option>
```

- [ ] **Step 2: Thêm hidden div cho Question Group preview**

Sau `<div id="importErrorRows" class="mb-3"></div>` thêm:

```html
<div id="importGroupPreview" class="mb-3" style="display:none;"></div>
```

- [ ] **Step 3: Thêm biến**

```javascript
let importGroupResult = null;
```

- [ ] **Step 4: Thêm function downloadGroupTemplate() và update uploadExcel()**

Thêm vào `uploadExcel()` — phát hiện `QUESTION_GROUP`:

```javascript
async function uploadExcel() {
    const type = document.getElementById('importType').value;
    const fileInput = document.getElementById('importFile');
    const file = fileInput.files[0];
    if (!type) { alert('Vui lòng chọn loại câu hỏi.'); return; }
    if (!file) { alert('Vui lòng chọn file Excel.'); return; }

    document.getElementById('importLoading').style.display = 'block';

    try {
        if (type === 'QUESTION_GROUP') {
            const formData = new FormData();
            formData.append('file', file);
            const res = await fetch('/api/v1/expert/questions/excel/parse-group', {
                method: 'POST',
                body: formData
            });
            const result = await res.json();
            if (!res.ok) { alert(result.message || 'Lỗi khi phân tích file.'); return; }
            importGroupResult = result.data;
            renderImportGroupPreview();
        } else {
            // existing code for single question types
            const formData = new FormData();
            formData.append('type', type);
            formData.append('file', file);
            const res = await fetch('/api/v1/expert/questions/excel/parse', {
                method: 'POST',
                body: formData
            });
            const result = await res.json();
            if (!res.ok) { alert(result.message || 'Lỗi khi phân tích file.'); return; }
            importParseResult = result.data;
            renderImportPreview();
        }
    } catch (e) { alert('Lỗi: ' + e.message); }
    finally { document.getElementById('importLoading').style.display = 'none'; }
}
```

- [ ] **Step 5: Thêm function renderImportGroupPreview()**

```javascript
function renderImportGroupPreview() {
    document.getElementById('importStep1').style.display = 'none';
    document.getElementById('importStep2').style.display = 'block';
    document.getElementById('importValidRows').style.display = 'none';
    document.getElementById('importErrorRows').style.display = 'none';
    document.getElementById('importGroupPreview').style.display = 'block';

    const r = importGroupResult;
    const validCount = r.valid ? r.valid.length : 0;
    const errorCount = r.errors ? r.errors.length : 0;

    const summary = document.getElementById('importSummary');
    summary.className = `alert ${errorCount > 0 ? 'alert-warning' : 'alert-success'}`;
    summary.innerHTML = `<strong>${validCount} bộ câu hỏi hợp lệ</strong>${errorCount > 0 ? ` | <strong class="text-danger">${errorCount} bộ bị lỗi</strong>` : ''}`;

    const container = document.getElementById('importGroupPreview');
    if (validCount > 0) {
        container.innerHTML = r.valid.map((g, gi) => `
            <div class="card mb-3 border-primary">
                <div class="card-header bg-primary text-white small fw-bold">
                    Bộ câu hỏi ${gi + 1} — ${g.group.passage ? escapeHtml(g.group.passage.substring(0, 80)) + '...' : ''}
                </div>
                <div class="card-body">
                    <div class="mb-2 d-flex gap-2 flex-wrap">
                        <span class="badge bg-info text-dark">${g.group.skill || 'READING'}</span>
                        <span class="badge bg-primary">${g.group.cefrLevel || 'B1'}</span>
                        ${g.group.topic ? `<span class="badge bg-secondary">${escapeHtml(g.group.topic)}</span>` : ''}
                        <span class="badge bg-success">${g.questions.length} câu hỏi con</span>
                    </div>
                    ${g.questions.map((q, qi) => `
                        <div class="border rounded p-2 mb-2 bg-light">
                            <div class="small fw-bold">${qi + 1}. ${escapeHtml(q.content || '')}</div>
                            <div class="small text-muted">Type: ${q.questionType} | CEFR: ${q.cefrLevel}</div>
                        </div>
                    `).join('')}
                </div>
            </div>
        `).join('');
    } else {
        container.innerHTML = '';
    }

    if (errorCount > 0) {
        const errEl = document.getElementById('importErrorRows');
        errEl.style.display = 'block';
        errEl.innerHTML = `<h6 class="text-danger"><i class="bi bi-exclamation-circle me-1"></i>Lỗi</h6>` +
            r.errors.map(e => `<div class="alert alert-danger py-2 small">Dòng ${e.rowIndex}: ${escapeHtml(e.message)}</div>`).join('');
    }
}
```

- [ ] **Step 6: Thêm function confirmGroupImport()**

```javascript
async function confirmGroupImport() {
    if (!importGroupResult || !importGroupResult.valid || importGroupResult.valid.length === 0) {
        alert('Không có bộ câu hỏi nào để import.'); return;
    }
    const body = { group: importGroupResult.valid[0].group };
    try {
        const res = await fetch('/api/v1/expert/questions/excel/import-group', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        const result = await res.json();
        if (!res.ok) { alert(result.message || 'Lỗi khi lưu.'); return; }
        alert(result.message || 'Import thành công.');
        importModal.hide();
        loadQuestions();
    } catch (e) { alert('Lỗi: ' + e.message); }
}
```

- [ ] **Step 7: Update confirmImport button onclick**

Sửa `confirmImport()` button trong Import Modal step 2 để gọi `confirmGroupImport()` khi type là QUESTION_GROUP, hoặc đơn giản thêm button mới. Tìm:

```html
<button class="btn text-white" style="background-color: #136ad5;" onclick="confirmImport()">
```

Thêm data attribute hoặc thêm 2 button:

```html
<button class="btn text-white" style="background-color: #136ad5;" onclick="confirmSingleImport()">
    <i class="bi bi-check-lg me-1"></i> Lưu câu hỏi đơn
</button>
<button class="btn btn-success" onclick="confirmGroupImport()">
    <i class="bi bi-check-lg me-1"></i> Lưu bộ câu hỏi
</button>
```

Đổi tên `confirmImport()` thành `confirmSingleImport()` trong JS.

- [ ] **Step 8: Update resetImport()**

```javascript
function resetImport() {
    document.getElementById('importStep1').style.display = 'block';
    document.getElementById('importStep2').style.display = 'none';
    document.getElementById('importGroupPreview').style.display = 'none';
    document.getElementById('importValidRows').style.display = 'block';
    document.getElementById('importErrorRows').style.display = 'block';
    importParseResult = null;
    importGroupResult = null;
}
```

---

## Chunk 6: Fix & Verify

### Task 6.1: Fix imports - kiểm tra Question model có đủ field

**Files:**
- Check: `DoAn/src/main/java/com/example/DoAn/model/Question.java`

- [ ] **Step 1: Verify Question entity có đủ fields**

Kiểm tra Question entity có: `source`, `createdMethod`, `status`, `questionGroup`, `user`. Nếu thiếu `source` hoặc `createdMethod`, thêm vào entity:

```java
@Column(name = "source")
private String source;  // EXPERT_BANK, TEACHER_PRIVATE

@Column(name = "created_method")
private String createdMethod;  // MANUAL, AI_GENERATED, EXCEL_IMPORTED
```

- [ ] **Step 2: Verify QuestionGroup entity có đủ fields**

Kiểm tra QuestionGroup có: `groupContent`, `audioUrl`, `imageUrl`, `skill`, `cefrLevel`, `topic`, `explanation`, `status`, `user`, `questions`.

- [ ] **Step 3: Verify AnswerOption entity có matchTarget field**

```java
@Column(name = "match_target")
private String matchTarget;
```

### Task 6.2: Compile check

- [ ] **Step 1: Run build**

```bash
cd NovaLMS/DoAn
./mvnw clean compile -q
```

Expected: BUILD SUCCESS

### Task 6.3: Test manual trên browser

- [ ] **Step 1:** Mở `http://localhost:8080/expert/question-bank`
- [ ] **Step 2:** Click "Sinh bằng AI" → tab "Bộ câu hỏi" → nhập topic "Technology in Education" → click "Sinh bộ câu hỏi"
- [ ] **Step 3:** Verify passage + câu hỏi con hiển thị đúng → click "Lưu bộ câu hỏi"
- [ ] **Step 4:** Verify redirect về question-bank với bộ câu hỏi mới trong danh sách
- [ ] **Step 5:** Test "Import Excel" → chọn "Bộ câu hỏi" → download template → fill → upload → preview → import
