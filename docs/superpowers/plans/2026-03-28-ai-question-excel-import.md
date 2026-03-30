# AI Generate Question & Excel Import Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement AI-generated question creation (OpenAI) and Excel template/import workflow for Expert Question Bank, including preview-select-save flow and inline error correction.

**Architecture:** Extend existing Question Bank flow with two new bounded feature slices: (1) AI generation pipeline that returns preview-only DTOs before persistence, and (2) Excel import pipeline with deterministic template schema, parse-time validation, and transactional persistence. Keep existing entities/services intact, adding thin dedicated services and DTOs to isolate new logic.

**Tech Stack:** Spring Boot 3.3.4, Java 17, Spring MVC/JPA/Validation, Thymeleaf, OpenAI HTTP integration, Apache POI (XLSX), JUnit/Spring test.

---

## File Structure Map

### Existing files to modify
- `NovaLMS/DoAn/pom.xml`
  - Add Apache POI dependency and HTTP client dependency for OpenAI integration.
- `NovaLMS/DoAn/src/main/java/com/example/DoAn/controller/ExpertQuestionController.java`
  - Add AI generate/import and Excel template/parse/import endpoints.
- `NovaLMS/DoAn/src/main/resources/templates/expert/question-bank.html`
  - Add buttons, AI panel, import panel mount points, JS handlers.
- `NovaLMS/DoAn/src/main/java/com/example/DoAn/model/Question.java`
  - Add `createdMethod` field (`MANUAL|AI_GENERATED|EXCEL_IMPORTED`) and persistence mapping.

### New backend files
- `NovaLMS/DoAn/src/main/java/com/example/DoAn/dto/request/AIGenerateRequestDTO.java`
- `NovaLMS/DoAn/src/main/java/com/example/DoAn/dto/request/AIImportRequestDTO.java`
- `NovaLMS/DoAn/src/main/java/com/example/DoAn/dto/request/ExcelImportRequestDTO.java`
- `NovaLMS/DoAn/src/main/java/com/example/DoAn/dto/response/AIGenerateResponseDTO.java`
- `NovaLMS/DoAn/src/main/java/com/example/DoAn/dto/response/ExcelParseResultDTO.java`
- `NovaLMS/DoAn/src/main/java/com/example/DoAn/service/AIQuestionService.java`
- `NovaLMS/DoAn/src/main/java/com/example/DoAn/service/ExcelQuestionImportService.java`
- `NovaLMS/DoAn/src/main/java/com/example/DoAn/service/impl/AIQuestionServiceImpl.java`
- `NovaLMS/DoAn/src/main/java/com/example/DoAn/service/impl/ExcelQuestionImportServiceImpl.java`
- `NovaLMS/DoAn/src/main/java/com/example/DoAn/util/AIQuestionPromptBuilder.java`
- `NovaLMS/DoAn/src/main/java/com/example/DoAn/util/ExcelTemplateGenerator.java`
- `NovaLMS/DoAn/src/main/java/com/example/DoAn/util/RateLimitWindowStore.java`

### Repository usage (existing)
- `NovaLMS/DoAn/src/main/java/com/example/DoAn/repository/QuestionRepository.java`
- `NovaLMS/DoAn/src/main/java/com/example/DoAn/repository/ModuleRepository.java` (or equivalent existing module repo)
- `NovaLMS/DoAn/src/main/java/com/example/DoAn/repository/LessonRepository.java` (or equivalent existing lesson repo)
- `NovaLMS/DoAn/src/main/java/com/example/DoAn/repository/UserRepository.java`

### Test files (new)
- `NovaLMS/DoAn/src/test/java/com/example/DoAn/service/AIQuestionServiceImplTest.java`
- `NovaLMS/DoAn/src/test/java/com/example/DoAn/service/ExcelQuestionImportServiceImplTest.java`
- `NovaLMS/DoAn/src/test/java/com/example/DoAn/controller/ExpertQuestionControllerAITest.java`
- `NovaLMS/DoAn/src/test/java/com/example/DoAn/controller/ExpertQuestionControllerExcelTest.java`
- `NovaLMS/DoAn/src/test/java/com/example/DoAn/util/ExcelTemplateGeneratorTest.java`

---

## Chunk 1: Domain + Dependency Baseline

### Task 1: Add dependencies for Excel + HTTP

**Files:**
- Modify: `NovaLMS/DoAn/pom.xml`

- [ ] **Step 1: Write failing build expectation**

Expected compile blockers before changes:
- `org.apache.poi` classes unresolved
- HTTP client for OpenAI unresolved

- [ ] **Step 2: Add dependencies**

Add to `pom.xml`:
```xml
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

- [ ] **Step 3: Verify dependency resolution**

Run: `./mvnw -q -DskipTests compile`
Expected: `BUILD SUCCESS`

### Task 2: Extend Question model for created method

**Files:**
- Modify: `NovaLMS/DoAn/src/main/java/com/example/DoAn/model/Question.java`
- Test: `NovaLMS/DoAn/src/test/java/com/example/DoAn/model/QuestionModelTest.java` (create if absent)

- [ ] **Step 1: Write failing test for default created method**
```java
@Test
void prePersist_setsDefaultCreatedMethodManual_whenNull() {
    Question q = Question.builder().content("x").questionType("WRITING").skill("WRITING").cefrLevel("B1").build();
    q.onCreate();
    assertEquals("MANUAL", q.getCreatedMethod());
}
```

- [ ] **Step 2: Add field and defaulting logic**

Add to entity:
```java
@Column(name = "created_method", length = 20)
private String createdMethod;
```
Update `@PrePersist` to set `MANUAL` when null.

- [ ] **Step 3: Re-run test**

Run: `./mvnw -q -Dtest=QuestionModelTest test`
Expected: PASS

---

## Chunk 2: DTO Contracts + Validation

### Task 3: Add AI request/response DTOs

**Files:**
- Create: `dto/request/AIGenerateRequestDTO.java`
- Create: `dto/request/AIImportRequestDTO.java`
- Create: `dto/response/AIGenerateResponseDTO.java`
- Test: `src/test/java/.../AIDtoValidationTest.java`

- [ ] **Step 1: Write failing validation test**
```java
@Test
void aiGenerateRequest_requiresTopicOrModuleAndQuantityRange() {
    AIGenerateRequestDTO dto = new AIGenerateRequestDTO();
    dto.setQuantity(0);
    Set<ConstraintViolation<AIGenerateRequestDTO>> violations = validator.validate(dto);
    assertFalse(violations.isEmpty());
}
```

- [ ] **Step 2: Implement DTOs with bean validation**

Rules:
- `quantity`: `@Min(1) @Max(50)`
- custom method-level validation: at least one of `topic` or `moduleId`
- enum-like checks for optional `questionTypes`

- [ ] **Step 3: Re-run DTO tests**

Run: `./mvnw -q -Dtest=AIDtoValidationTest test`
Expected: PASS

### Task 4: Add Excel parse/import DTOs

**Files:**
- Create: `dto/request/ExcelImportRequestDTO.java`
- Create: `dto/response/ExcelParseResultDTO.java`
- Test: `src/test/java/.../ExcelDtoValidationTest.java`

- [ ] **Step 1: Write failing tests for parse result shape**
- [ ] **Step 2: Implement DTOs for `validRows`, `errorRows`, `rowIndex`, `message`, `rawData`**
- [ ] **Step 3: Run tests**

Run: `./mvnw -q -Dtest=ExcelDtoValidationTest test`
Expected: PASS

---

## Chunk 3: AI Generation Service (No Persistence)

### Task 5: Build prompt builder utility

**Files:**
- Create: `util/AIQuestionPromptBuilder.java`
- Test: `util/AIQuestionPromptBuilderTest.java`

- [ ] **Step 1: Write failing tests for quick vs context-aware prompt content**
- [ ] **Step 2: Implement prompt methods**
  - `buildQuickPrompt(topic, quantity, questionTypes)`
  - `buildContextPrompt(moduleName, lessonSummary, quantity, questionTypes)`
  - enforce JSON-only response instruction
- [ ] **Step 3: Run tests**

Run: `./mvnw -q -Dtest=AIQuestionPromptBuilderTest test`
Expected: PASS

### Task 6: Implement rate limit store

**Files:**
- Create: `util/RateLimitWindowStore.java`
- Test: `util/RateLimitWindowStoreTest.java`

- [ ] **Step 1: Write failing tests for 10/min per user**
- [ ] **Step 2: Implement `ConcurrentHashMap<String, Deque<Long>>` sliding window**
- [ ] **Step 3: Run tests**

Run: `./mvnw -q -Dtest=RateLimitWindowStoreTest test`
Expected: PASS

### Task 7: Implement AIQuestionService generate flow

**Files:**
- Create: `service/AIQuestionService.java`
- Create: `service/impl/AIQuestionServiceImpl.java`
- Modify: existing repositories imports as needed
- Test: `service/AIQuestionServiceImplTest.java`

- [ ] **Step 1: Write failing service tests**
  - quick mode uses topic
  - context mode fetches module + lessons
  - module without lessons falls back gracefully
  - malformed AI JSON triggers single retry then fails
  - returns warnings when fewer than requested

- [ ] **Step 2: Implement minimal service**
  - read module and lesson content (truncate 8000 chars)
  - call OpenAI `gpt-4o-mini` (fallback `gpt-4o`)
  - parse JSON response to DTO
  - validate each question with rules

- [ ] **Step 3: Run service tests**

Run: `./mvnw -q -Dtest=AIQuestionServiceImplTest test`
Expected: PASS

---

## Chunk 4: Excel Template + Parse Service

### Task 8: Implement Excel template generator

**Files:**
- Create: `util/ExcelTemplateGenerator.java`
- Test: `util/ExcelTemplateGeneratorTest.java`

- [ ] **Step 1: Write failing tests for each template type headers**
- [ ] **Step 2: Implement POI workbook generation for 5 question types**
- [ ] **Step 3: Run tests**

Run: `./mvnw -q -Dtest=ExcelTemplateGeneratorTest test`
Expected: PASS

### Task 9: Implement Excel parse and validation logic

**Files:**
- Create: `service/ExcelQuestionImportService.java`
- Create: `service/impl/ExcelQuestionImportServiceImpl.java`
- Test: `service/ExcelQuestionImportServiceImplTest.java`

- [ ] **Step 1: Write failing tests for row validations**
  - missing content
  - invalid CEFR
  - MC correct answer mismatch
  - MC multi-select all-correct invalid
  - matching item count mismatch
  - speaking audio url validation

- [ ] **Step 2: Implement parser**
  - one row = one question
  - return `validRows` + `errorRows`
  - preserve raw row data for inline UI correction

- [ ] **Step 3: Run tests**

Run: `./mvnw -q -Dtest=ExcelQuestionImportServiceImplTest test`
Expected: PASS

### Task 10: Implement transactional import persistence

**Files:**
- Modify: `service/impl/ExcelQuestionImportServiceImpl.java`
- Test: `service/ExcelQuestionImportTransactionalTest.java`

- [ ] **Step 1: Write failing test for rollback-on-any-failure**
- [ ] **Step 2: Add `@Transactional` save-all behavior**
- [ ] **Step 3: Run tests**

Run: `./mvnw -q -Dtest=ExcelQuestionImportTransactionalTest test`
Expected: PASS

---

## Chunk 5: Controller Endpoints

### Task 11: Add AI endpoints to ExpertQuestionController

**Files:**
- Modify: `controller/ExpertQuestionController.java`
- Test: `controller/ExpertQuestionControllerAITest.java`

- [ ] **Step 1: Write failing MockMvc tests**
  - `POST /api/v1/expert/questions/ai/generate`
  - `POST /api/v1/expert/questions/ai/import`
  - 401 when unauthenticated

- [ ] **Step 2: Implement endpoints**
  - call service
  - map warnings/messages
  - enforce expert auth and email extraction

- [ ] **Step 3: Run tests**

Run: `./mvnw -q -Dtest=ExpertQuestionControllerAITest test`
Expected: PASS

### Task 12: Add Excel endpoints to ExpertQuestionController

**Files:**
- Modify: `controller/ExpertQuestionController.java`
- Test: `controller/ExpertQuestionControllerExcelTest.java`

- [ ] **Step 1: Write failing MockMvc tests**
  - `GET /api/v1/expert/questions/excel/template`
  - `POST /api/v1/expert/questions/excel/parse`
  - `POST /api/v1/expert/questions/excel/import`

- [ ] **Step 2: Implement endpoints**
  - template download headers
  - multipart file parse
  - transactional import API

- [ ] **Step 3: Run tests**

Run: `./mvnw -q -Dtest=ExpertQuestionControllerExcelTest test`
Expected: PASS

---

## Chunk 6: Frontend Integration (Question Bank)

### Task 13: Add AI generate UI panel and JS flow

**Files:**
- Modify: `templates/expert/question-bank.html`
- (Optional create if preferred existing pattern): `templates/expert/fragments/ai-gen-panel.html`

- [ ] **Step 1: Write failing UI acceptance notes/manual checklist**
  - button visible
  - modal opens
  - quick/context tabs
  - preview and selection count

- [ ] **Step 2: Implement UI elements + JS**
  - add buttons
  - add modal panel
  - call generate endpoint
  - render question cards with checkbox
  - submit selected questions to import endpoint

- [ ] **Step 3: Manual verify in browser**

Run app: `./mvnw spring-boot:run`
Checklist expected:
- generate works in both modes
- unselected questions are dropped

### Task 14: Add Excel import UI panel and inline error correction

**Files:**
- Modify: `templates/expert/question-bank.html`
- (Optional fragment): `templates/expert/fragments/import-excel-panel.html`

- [ ] **Step 1: Write failing UI acceptance notes/manual checklist**
  - choose type and download template
  - upload xlsx
  - show valid rows + error rows
  - inline edit error rows and revalidate

- [ ] **Step 2: Implement UI + JS**
  - template download link with type
  - file upload via FormData
  - render error table with editable controls
  - import all valid/edited rows

- [ ] **Step 3: Manual verify in browser**

Expected:
- user can fix row errors directly and retry without re-uploading from scratch

---

## Chunk 7: Final Verification

### Task 15: Full regression + targeted scenarios

**Files:**
- No new files required

- [ ] **Step 1: Run full test suite**

Run: `./mvnw test`
Expected: `BUILD SUCCESS`

- [ ] **Step 2: Run compile/package smoke**

Run: `./mvnw -DskipTests package`
Expected: jar built successfully

- [ ] **Step 3: Manual end-to-end smoke**

Checklist:
- AI quick generate: topic + quantity -> preview -> select subset -> save
- AI context generate: module with lessons -> context-grounded output
- AI context fallback: module with no lesson still returns output + warning
- Excel MC template download -> fill -> parse -> inline fix -> import
- Excel Speaking with invalid URL -> parse error shown
- Excel import rollback test: induce one invalid row in import payload -> no rows saved

- [ ] **Step 4: Confirm no unintended behavior change**

Verify existing manual question create/edit/list still works.

---

## Suggested Execution Order

1. Chunk 1 → 2 → 3 (backend contracts first)
2. Chunk 4 (Excel backend)
3. Chunk 5 (controller APIs)
4. Chunk 6 (UI integration)
5. Chunk 7 (verification)

This order ensures APIs are stable before UI hookup and minimizes rework.

---

## Notes for Implementor

- Keep DRY: extract shared question validation into reusable validator helper.
- Keep YAGNI: avoid adding new tables unless existing model cannot support flow.
- Keep services focused: AI service should not own persistence; import endpoint handles save.
- Avoid giant controller methods; delegate to service layer.
- Preserve existing response envelope (`ResponseData`) style used in project.

---

Plan complete and saved to `docs/superpowers/plans/2026-03-28-ai-question-excel-import.md`. Ready to execute?