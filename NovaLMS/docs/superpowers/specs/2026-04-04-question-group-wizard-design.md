# Expert QuestionGroup Wizard — Design Specification

> **Date:** 2026-04-04
> **Status:** Draft
> **Author:** AI-assisted design (brainstorming workflow)

---

## 1. Overview

**What:** A single-page multi-step wizard at `/expert/questions/wizard` enabling Expert users to create `QuestionGroup` (question set) entities via AI generation, Excel import, or manual entry — with full validation before save.

**Why:** Currently, creating question sets requires navigating between separate AI generation, Excel import, and manual question pages with no unified workflow. This wizard consolidates all three sources into one guided experience.

**Users:** Expert role (`ROLE_EXPERT`)

---

## 2. Architecture

### 2.1 High-Level Flow

```
GET /expert/questions/wizard
      │
      ▼
┌─────────────────────────────────────────────────────────┐
│  Step 1 — Group Setup                                  │
│  Mode (passage/topic), Skill, CEFR, Topic,             │
│  Passage, Audio URL, Image URL                          │
│  POST /api/v1/expert/questions/wizard/step1           │
└─────────────────────────────────────────────────────────┘
      │
      ▼
┌─────────────────────────────────────────────────────────┐
│  Step 2 — Question Source                              │
│  AI Generate  OR  Excel Import  OR  Manual Add         │
│  POST /api/v1/expert/questions/wizard/step2           │
└─────────────────────────────────────────────────────────┘
      │
      ▼
┌─────────────────────────────────────────────────────────┐
│  Step 3 — Preview & Validate                           │
│  Show all questions, highlight errors                  │
│  POST /api/v1/expert/questions/wizard/validate        │
└─────────────────────────────────────────────────────────┘
      │
      │ only valid (errors == 0)
      ▼
┌─────────────────────────────────────────────────────────┐
│  Step 4 — Save & Publish                               │
│  Save as DRAFT or PUBLISHED (EXPERT_BANK / PRIVATE)   │
│  POST /api/v1/expert/questions/wizard/save             │
│  → Redirect to /expert/questions                       │
└─────────────────────────────────────────────────────────┘
```

### 2.2 Session State

All wizard state is stored in `HttpSession` under keys:

| Session Key | Data |
|---|---|
| `wizard_step1` | `WizardStep1DTO` |
| `wizard_step2_ai` | `List<QuestionDTO>` from AI |
| `wizard_step2_excel` | `ExcelParseResultDTO` from Excel |
| `wizard_step2_manual` | `List<ManualQuestionDTO>` from manual add |
| `wizard_validation` | `WizardValidationResultDTO` |

### 2.3 New Components

| Type | Name | Location |
|---|---|---|
| Controller | `ExpertQuestionGroupWizardController` | `controller/` |
| Service Interface | `IQuestionGroupWizardService` | `service/` |
| Service Impl | `QuestionGroupWizardServiceImpl` | `service/impl/` |
| Validation Service | `WizardValidationService` | `service/impl/` |
| Step1 DTO | `WizardStep1DTO` | `dto/request/` |
| Step2 DTO | `WizardStep2DTO` | `dto/request/` |
| Save DTO | `WizardSaveDTO` | `dto/request/` |
| Validation Result DTO | `WizardValidationResultDTO` | `dto/response/` |
| Step Data Response DTO | `WizardStepDataDTO` | `dto/response/` |
| Thymeleaf Template | `question-group-wizard.html` | `templates/expert/` |
| JavaScript | `question-group-wizard.js` | `static/assets/expert/js/` |

### 2.4 Reused Components (no changes)

- `AIQuestionService` — AI generation
- `ExcelQuestionImportService` — Excel parsing
- `IExpertQuestionService` — **extended** with `saveQuestionGroup(group, questions, email)`
- `AIQuestionPromptBuilder` — prompt building
- `GroqClient` — Groq API call

---

## 3. API Endpoints

### 3.1 Page Controller

```
GET /expert/questions/wizard
  → Renders question-group-wizard.html
  → Clears any stale wizard session on first load
```

### 3.2 Wizard API (REST)

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/expert/questions/wizard/step1` | Validate & save Step 1 data to session |
| `POST` | `/api/v1/expert/questions/wizard/step2` | Process question source (AI/Excel/Manual), store to session |
| `POST` | `/api/v1/expert/questions/wizard/validate` | Run full validation on session data |
| `POST` | `/api/v1/expert/questions/wizard/save` | Persist group + questions, clear session |
| `GET` | `/api/v1/expert/questions/wizard/step-data` | Return current session data (for refresh recovery) |
| `GET` | `/api/v1/expert/questions/wizard/abandon` | Clear session, redirect to question bank |

---

## 4. DTO Specifications

### WizardStep1DTO
```java
@NotNull private QuestionGroupMode mode;           // PASSAGE_BASED, TOPIC_BASED
@NotNull private Skill skill;                      // LISTENING, READING, WRITING, SPEAKING
@NotNull private CefrLevel cefrLevel;              // A1, A2, B1, B2, C1, C2
@NotBlank @Size(min = 2, max = 200) private String topic;
private List<@Size(max = 50) String> tags;         // max 10
@Size(min = 10, max = 5000) private String passageContent; // required if PASSAGE_BASED
private String audioUrl;                           // optional
private String imageUrl;                           // optional
```

### WizardStep2DTO
```java
@NotNull private QuestionSourceType sourceType;    // AI_GENERATE, EXCEL_IMPORT, MANUAL

// AI_GENERATE
private String aiTopic;
@Min(1) @Max(50) private Integer aiQuantity;
private List<QuestionType> aiQuestionTypes;

// EXCEL_IMPORT
private MultipartFile excelFile;

// MANUAL
private List<ManualQuestionDTO> manualQuestions;
```

### WizardSaveDTO
```java
@NotNull private QuestionGroupStatus status;      // DRAFT, PUBLISHED
@NotNull private QuestionSource source;           // EXPERT_BANK, TEACHER_PRIVATE
```

### WizardValidationResultDTO
```java
private List<ValidatedQuestionDTO> validQuestions;
private List<ValidationErrorDTO> errors;
private boolean isClean;                          // true if errors.isEmpty()
private int totalQuestions;
private int errorCount;
```

### ValidationErrorDTO
```java
private int questionIndex;                        // -1 for group-level errors
private String field;                             // "passageContent", "cefrLevel", etc.
private String code;                              // "PASSAGE_TOO_SHORT", "CEFR_MISMATCH", etc.
private String message;                           // human-readable
private String severity;                          // "ERROR" (blocks save)
```

---

## 5. Validation Rules (Step 3)

### Group-Level Validation
| Code | Condition | Severity |
|---|---|---|
| `PASSAGE_TOO_SHORT` | PASSAGE_BASED and passageContent < 10 chars | ERROR |
| `PASSAGE_TOO_LONG` | passageContent > 5000 chars | ERROR |
| `PASSAGE_REQUIRED` | PASSAGE_BASED and passageContent empty | ERROR |
| `INVALID_AUDIO_URL` | audioUrl not blank and not valid URL + extension in [.mp3,.wav,.ogg,.m4a] | ERROR |
| `INVALID_IMAGE_URL` | imageUrl not blank and not valid URL + extension in [.jpg,.jpeg,.png,.webp] | ERROR |
| `NO_QUESTIONS` | total questions == 0 | ERROR |
| `TOO_MANY_QUESTIONS` | total questions > 100 | ERROR |

### Question-Level Validation
| Code | Condition | Severity |
|---|---|---|
| `SKILL_MISMATCH` | question.skill != group.skill | ERROR |
| `CEFR_MISMATCH` | question.cefrLevel != group.cefrLevel | ERROR |
| `INVALID_TYPE_FOR_SKILL` | question type not compatible with group skill | ERROR |
| `CONTENT_TOO_SHORT` | question content < 10 chars | ERROR |
| `CONTENT_TOO_LONG` | question content > 2000 chars | ERROR |
| `EXPLANATION_MISSING` | explanation blank | WARNING |
| `TAGS_MISSING` | tags empty | WARNING |

### MC Option Validation
| Code | Condition | Severity |
|---|---|---|
| `TOO_FEW_OPTIONS` | options.size < 2 | ERROR |
| `NO_CORRECT_ANSWER` | no option with correctAnswer == true | ERROR |
| `ALL_OPTIONS_CORRECT` | multi-select and all options correct | ERROR |
| `OPTION_TOO_LONG` | any option title > 500 chars | ERROR |
| `OPTION_DUPLICATE` | duplicate option titles in same question | ERROR |

### Matching Validation
| Code | Condition | Severity |
|---|---|---|
| `MATCHING_COUNT_MISMATCH` | left.size != right.size | ERROR |
| `LEFT_ITEM_TOO_LONG` | any left item > 300 chars | ERROR |
| `RIGHT_ITEM_TOO_LONG` | any right item > 300 chars | ERROR |
| `MATCHING_PAIR_OUT_OF_RANGE` | pair leftIndex/rightIndex not in [0, count-1] | ERROR |

---

## 6. Step 2 — Source Processing Details

### AI Generate
1. Check rate limit: `RateLimitWindowStore` — 10 req/min/user → return error if exceeded
2. Call `AIQuestionService.generate(AIGenerateRequestDTO)` → `AIGenerateResponseDTO`
3. Store `List<QuestionDTO>` in session as `wizard_step2_ai`
4. Return `{ questions[], errors[] }` (partial results pattern)

### Excel Import
1. Validate file: `.xlsx` extension, ≤ 5MB
2. Call `ExcelQuestionImportService.parseFile(file, null)` → `ExcelParseResultDTO`
3. Store result in session as `wizard_step2_excel`
4. Return `{ questions[], errors[] }` (partial results pattern)

### Manual Add
1. Validate inline question data in `WizardStep2DTO.manualQuestions`
2. Store in session as `wizard_step2_manual`
3. Return immediate validation result

---

## 7. UI Specification

### 7.1 Page Layout
- Full-width Thymeleaf page, reusing `expert/layout.html` fragment (if exists) or standalone
- Horizontal step indicator: Step 1 → Step 2 → Step 3 → Step 4
- Active step highlighted; completed steps show checkmark
- "Leave Wizard" button always visible (triggers `beforeunload` + `/abandon`)

### 7.2 Step 1 — Group Setup
- Form fields as per `WizardStep1DTO`
- Toggle: "Passage-based" vs "Topic-based" (radio buttons)
- Passage textarea shown/hidden based on mode
- Skill + CEFR dropdowns
- Tags input (chip/tag input pattern — add/remove tags)
- Audio URL + Image URL text inputs

### 7.3 Step 2 — Question Source
- Three tab buttons: **AI Generate** | **Import Excel** | **Manual Add**
- **AI Generate tab:** topic input, quantity (1-50), question type checkboxes, Generate button
- **Import Excel tab:** drag-and-drop file upload, type selector dropdown, Upload button
- **Manual Add tab:** "Add Question" button opens inline form, list of added questions below with edit/delete
- "Continue to Preview" button (enabled after at least 1 question accumulated)
- Results list showing generated/imported questions with question type badge, skill badge, CEFR badge

### 7.4 Step 3 — Preview & Validate
- Full accordion/table listing all questions
- Validation errors shown as red banners per question
- Warnings shown as yellow banners per question
- "Re-validate" button (calls `/validate`)
- "Back to Step 2" to add more questions
- "Continue to Step 4" (enabled only when `isClean == true`)

### 7.5 Step 4 — Save & Publish
- Radio buttons: **Save as Draft** vs **Publish**
- Radio buttons: **Expert Bank** (shared) vs **Private** (personal)
- Summary panel: group name, skill, CEFR, question count, source breakdown
- "Save & Finish" button → POST `/save` → redirect to `/expert/questions`
- "Back to Step 3" always available

### 7.6 JavaScript Behavior
- `beforeunload`: warn user if session has data and wizard is incomplete
- AJAX calls for all step transitions (no full page reload between steps)
- Step indicator updates dynamically
- Error display with field highlighting
- File upload with drag-and-drop support

---

## 8. Error Handling

| Scenario | Behavior |
|---|---|
| Rate limit exceeded (AI) | Show error message, allow retry after wait time |
| Excel parse partial errors | Show valid rows + error rows, allow removing error rows |
| AI generation partial failure | Show valid questions + specific errors per failed item |
| Validation errors exist | Step 4 button disabled, errors listed clearly |
| Session lost / page refresh | Load existing step data via `/step-data`, show recovery message |
| Unauthorized access | 403 → redirect to `/expert/questions` |
| Save failure | Show error toast, stay on Step 4, do not clear session |

---

## 9. Security

- All endpoints require `ROLE_EXPERT` or `ROLE_ADMIN`
- Current user email extracted from `SecurityContext` for `createdBy` field
- File upload: `.xlsx` only, validated by extension + MIME type
- File size limit enforced at controller level: 5MB
- No XSS risk: all user input rendered via Thymeleaf (escaped by default)
- CSRF: handled by Spring Security (Thymeleaf CSRF token in forms)

---

## 10. Success Criteria

1. Expert can complete a full question group creation in ≤ 5 steps (wizard pages)
2. AI generation, Excel import, and manual entry all work in the same wizard session
3. Validation blocks Step 4 until all errors are resolved
4. Saved question groups appear immediately in the question bank
5. `beforeunload` warning prevents accidental data loss
6. Partial results shown for AI/Excel failures (not full block)
