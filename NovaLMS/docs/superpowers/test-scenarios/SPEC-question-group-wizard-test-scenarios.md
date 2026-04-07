# SPEC — Expert QuestionGroup Wizard: Test Scenarios
**Date:** 2026-04-04
**Feature:** Expert tạo QuestionGroup với Wizard 4 bước (AI/Excel/Manual)
**Spec:** `docs/superpowers/specs/2026-04-04-question-group-wizard-design.md`
**Plan:** `docs/superpowers/plans/2026-04-04-question-group-wizard-plan.md`

---

## Test Structure

```bash
src/test/java/com/example/DoAn/
├── service/impl/
│   ├── WizardValidationServiceTest.java              # Validation logic unit tests
│   └── QuestionGroupWizardServiceImplTest.java     # Orchestrator unit tests
├── controller/
│   └── ExpertQuestionGroupWizardControllerTest.java  # API endpoint tests
└── integration/
    └── QuestionGroupWizardFlowTest.java            # End-to-end wizard flows
```

---

## 1. WizardValidationService — Group-Level Tests

### TC-VAL-GRP-001: PASSAGE_REQUIRED when mode=PASSAGE_BASED and passage is null
**Unit Test** | `WizardValidationServiceTest.java`

```
Setup:    step1: mode=PASSAGE_BASED, passageContent=null
Action:    validate(step1, [validQuestion])
Assertion:
  - isClean == false
  - groupErrors contains error with code="PASSAGE_REQUIRED"
```

### TC-VAL-GRP-002: PASSAGE_TOO_SHORT when passage < 10 chars
**Unit Test** | `WizardValidationServiceTest.java`

```
Setup:    step1: mode=PASSAGE_BASED, passageContent="short"
Action:    validate(step1, [validQuestion])
Assertion:
  - isClean == false
  - groupErrors contains code="PASSAGE_TOO_SHORT"
```

### TC-VAL-GRP-003: PASSAGE_TOO_LONG when passage > 5000 chars
**Unit Test** | `WizardValidationServiceTest.java`

```
Setup:    step1: mode=PASSAGE_BASED, passageContent="x".repeat(5001)
Action:    validate(step1, [validQuestion])
Assertion:
  - isClean == false
  - groupErrors contains code="PASSAGE_TOO_LONG"
```

### TC-VAL-GRP-004: PASSAGE_BASED with valid passage passes
**Unit Test** | `WizardValidationServiceTest.java`

```
Setup:    step1: mode=PASSAGE_BASED, passageContent="A".repeat(100), skill=B1, topic=...
Action:    validate(step1, [validQuestion])
Assertion:
  - No PASSAGE errors in groupErrors
```

### TC-VAL-GRP-005: TOPIC_BASED does not require passage
**Unit Test** | `WizardValidationServiceTest.java`

```
Setup:    step1: mode=TOPIC_BASED, passageContent=null
Action:    validate(step1, [validQuestion])
Assertion:
  - No PASSAGE errors
```

### TC-VAL-GRP-006: INVALID_AUDIO_URL when audio URL has wrong extension
**Unit Test** | `WizardValidationService.java`

```
Setup:    step1: audioUrl="https://example.com/file.pdf"
Action:    validate(step1, [validQuestion])
Assertion:
  - groupErrors contains code="INVALID_AUDIO_URL"
```

### TC-VAL-GRP-007: VALID audio URL passes (.mp3)
**Unit Test** | `WizardValidationServiceTest.java`

```
Setup:    step1: audioUrl="https://cloudinary.com/audio.mp3"
Action:    validate(step1, [validQuestion])
Assertion: No INVALID_AUDIO_URL error
```

### TC-VAL-GRP-008: VALID audio URL with query params passes
**Unit Test** | `WizardValidationServiceTest.java`

```
Setup:    step1: audioUrl="https://cloudinary.com/audio.mp3?token=abc123"
Action:    validate(step1, [validQuestion])
Assertion: No INVALID_AUDIO_URL error (regex handles ?.*)
```

### TC-VAL-GRP-009: INVALID_IMAGE_URL when URL has wrong extension
**Unit Test** | `WizardValidationServiceTest.java`

```
Setup:    step1: imageUrl="https://example.com/image.gif"
Action:    validate(step1, [validQuestion])
Assertion:
  - groupErrors contains code="INVALID_IMAGE_URL"
```

### TC-VAL-GRP-010: VALID image URL passes (.png, .jpg, .webp)
**Unit Test** | `WizardValidationServiceTest.java`

```
Setup:    step1: imageUrl="https://cloudinary.com/image.webp"
Action:    validate(step1, [validQuestion])
Assertion: No INVALID_IMAGE_URL error
```

### TC-VAL-GRP-011: NO_QUESTIONS when question list is empty
**Unit Test** | `WizardValidationServiceTest.java`

```
Setup:    step1: valid, questions=null
Action:    validate(step1, null)
Assertion:
  - isClean == false
  - groupErrors contains code="NO_QUESTIONS"
```

### TC-VAL-GRP-012: NO_QUESTIONS when question list is empty array
**Unit Test** | `WizardValidationServiceTest.java`

```
Setup:    step1: valid, questions=[]
Action:    validate(step1, [])
Assertion:
  - isClean == false
  - groupErrors contains code="NO_QUESTIONS"
```

### TC-VAL-GRP-013: TOO_MANY_QUESTIONS when > 100 questions
**Unit Test** | `WizardValidationServiceTest.java`

```
Setup:    step1: valid, questions=[validQuestion repeated 101 times]
Action:    validate(step1, questions)
Assertion:
  - isClean == false
  - groupErrors contains code="TOO_MANY_QUESTIONS"
```

### TC-VAL-GRP-014: 100 questions is valid
**Unit Test** | `WizardValidationServiceTest.java`

```
Setup:    step1: valid, questions=[validQuestion repeated 100 times]
Action:    validate(step1, questions)
Assertion: No TOO_MANY_QUESTIONS error
```

### TC-VAL-GRP-015: TOO_MANY_TAGS when > 10 tags
**Unit Test** | `WizardValidationServiceTest.java`

```
Setup:    step1: tags=[1,2,3,4,5,6,7,8,9,10,11]
Action:    validate(step1, [validQuestion])
Assertion:
  - isClean == false
  - groupErrors contains code="TOO_MANY_TAGS"
```

### TC-VAL-GRP-016: INVALID_SKILL when skill not in VALID_SKILLS
**Unit Test** | `WizardValidationServiceTest.java`

```
Setup:    step1: skill="INVALID"
Action:    validate(step1, [validQuestion])
Assertion:
  - isClean == false
  - groupErrors contains code="INVALID_SKILL"
```

### TC-VAL-GRP-017: INVALID_CEFR when CEFR not in A1-C2
**Unit Test** | `WizardValidationServiceTest.java`

```
Setup:    step1: cefrLevel="B3"
Action:    validate(step1, [validQuestion])
Assertion:
  - isClean == false
  - groupErrors contains code="INVALID_CEFR"
```

---

## 2. WizardValidationService — Question-Level Tests

### TC-VAL-Q-001: SKILL_MISMATCH when question.skill != group.skill
**Unit Test** | `WizardValidationServiceTest.java`

```
Setup:    step1: skill=READING, cefrLevel=B1
          question: skill=WRITING, questionType=WRITING, content=valid...
Action:    validate(step1, [question])
Assertion:
  - isClean == false
  - question.errors contains SKILL_MISMATCH
```

### TC-VAL-Q-002: SKILL_MISMATCH is case-insensitive
**Unit Test** | `WizardValidationServiceTest.java`

```
Setup:    step1: skill="reading" (lowercase)
          question: skill="READING" (uppercase)
Action:    validate(step1, [question])
Assertion: No SKILL_MISMATCH error (equalsIgnoreCase used)
```

### TC-VAL-Q-003: CEFR_MISMATCH when question.cefrLevel != group.cefrLevel
**Unit Test** | `WizardValidationServiceTest.java`

```
Setup:    step1: skill=READING, cefrLevel=B1
          question: skill=READING, cefrLevel=C1
Action:    validate(step1, [question])
Assertion:
  - question.errors contains CEFR_MISMATCH
```

### TC-VAL-Q-004: INVALID_TYPE_FOR_SKILL — WRITING type for LISTENING skill
**Unit Test** | `WizardValidationServiceTest.java`

```
Setup:    step1: skill=LISTENING
          question: questionType=WRITING (not in allowed set for LISTENING)
Action:    validate(step1, [question])
Assertion:
  - question.errors contains INVALID_TYPE_FOR_SKILL
```

### TC-VAL-Q-005: INVALID_TYPE_FOR_SKILL — SINGULAR type for LISTENING
**Unit Test** | `WizardValidationServiceTest.java`

```
Setup:    step1: skill=LISTENING
          question: questionType=SPEAKING
Action:    validate(step1, [question])
Assertion: question.errors contains INVALID_TYPE_FOR_SKILL
```

### TC-VAL-Q-006: CONTENT_TOO_SHORT when content < 10 chars
**Unit Test** | `WizardValidationServiceTest.java`

```
Setup:    step1: valid
          question: content="short", questionType=READING
Action:    validate(step1, [question])
Assertion: question.errors contains CONTENT_TOO_SHORT
```

### TC-VAL-Q-007: CONTENT_TOO_LONG when content > 2000 chars
**Unit Test** | `WizardValidationServiceTest.java`

```
Setup:    question: content="x".repeat(2001)
Action:    validate(step1, [question])
Assertion: question.errors contains CONTENT_TOO_LONG
```

### TC-VAL-Q-008: EXPLANATION_MISSING is WARNING only
**Unit Test** | `WizardValidationServiceTest.java`

```
Setup:    step1: valid
          question: content valid, explanation=null, questionType=READING
Action:    validate(step1, [question])
Assertion:
  - question.errors is EMPTY (no blocking error)
  - question.warnings contains EXPLANATION_MISSING
  - question.isValid == true (errors empty)
```

### TC-VAL-Q-009: TAGS_MISSING is WARNING only
**Unit Test** | `WizardValidationServiceTest.java`

```
Setup:    question: tags=null
Action:    validate(step1, [question])
Assertion:
  - question.errors is EMPTY
  - question.warnings contains TAGS_MISSING
```

### TC-VAL-Q-010: isValid = true when no errors (only warnings)
**Unit Test** | `WizardValidationServiceTest.java`

```
Setup:    question: explanation=null, tags=null, but all else valid
Action:    validate(step1, [question])
Assertion:
  - question.isValid == true
  - isClean == false (because warnings count toward warningCount)
```

### TC-VAL-Q-011: Multiple errors on same question
**Unit Test** | `WizardValidationServiceTest.java`

```
Setup:    question: skill mismatch AND content too short AND wrong type
Action:    validate(step1, [question])
Assertion:
  - question.errors contains 3 items
  - question.errors[0].questionIndex == question.errors[1].questionIndex
```

---

## 3. WizardValidationService — MC Option Tests

### TC-VAL-MC-001: TOO_FEW_OPTIONS when only 1 option
**Unit Test** | `WizardValidationServiceTest.java`

```
Setup:    question: questionType=MULTIPLE_CHOICE_SINGLE
          options: [{title: "Only option", correct: true}]
Action:    validate(step1, [question])
Assertion: question.errors contains TOO_FEW_OPTIONS
```

### TC-VAL-MC-002: NO_CORRECT_ANSWER when no correct option
**Unit Test** | `WizardValidationServiceTest.java`

```
Setup:    question: MULTIPLE_CHOICE_SINGLE
          options: [{title: "A", correct: false}, {title: "B", correct: false}]
Action:    validate(step1, [question])
Assertion: question.errors contains NO_CORRECT_ANSWER
```

### TC-VAL-MC-003: ALL_OPTIONS_CORRECT for MULTIPLE_CHOICE_MULTI
**Unit Test** | `WizardValidationServiceTest.java`

```
Setup:    question: MULTIPLE_CHOICE_MULTI
          options: 3 options, all correct=true
Action:    validate(step1, [question])
Assertion: question.errors contains ALL_OPTIONS_CORRECT
```

### TC-VAL-MC-004: MULTIPLE_CHOICE_MULTI with correct subset passes
**Unit Test** | `WizardValidationServiceTest.java`

```
Setup:    question: MULTIPLE_CHOICE_MULTI
          options: [{title: "A", correct: true}, {title: "B", correct: false}, {title: "C", correct: true}]
          (2 correct out of 3 — valid multi-select)
Action:    validate(step1, [question])
Assertion: No errors
```

### TC-VAL-MC-005: OPTION_DUPLICATE — same title lowercase
**Unit Test** | `WizardValidationServiceTest.java`

```
Setup:    question: options: [{title: "Paris", correct: true}, {title: "paris", correct: false}]
Action:    validate(step1, [question])
Assertion: question.errors contains OPTION_DUPLICATE
```

### TC-VAL-MC-006: OPTION_EMPTY — option with empty title
**Unit Test** | `WizardValidationServiceTest.java`

```
Setup:    options: [{title: "", correct: true}, {title: "B", correct: false}]
Action:    validate(step1, [question])
Assertion: question.errors contains OPTION_EMPTY
```

### TC-VAL-MC-007: OPTION_TOO_LONG when title > 500 chars
**Unit Test** | `WizardValidationServiceTest.java`

```
Setup:    options: [{title: "A".repeat(501), correct: true}, {title: "B", correct: false}]
Action:    validate(step1, [question])
Assertion: question.errors contains OPTION_TOO_LONG
```

### TC-VAL-MC-008: SINGULAR question with no options (not MC/FILL/MATCH) — no error
**Unit Test** | `WizardValidationServiceTest.java`

```
Setup:    question: questionType=SPEAKING or WRITING, options=null
Action:    validate(step1, [question])
Assertion: No options-related errors (options optional for SPEAKING/WRITING)
```

### TC-VAL-MC-009: 2 valid options for SINGLE choice
**Unit Test** | `WizardValidationServiceTest.java`

```
Setup:    options: [{title: "A", correct: true}, {title: "B", correct: false}]
          questionType=MULTIPLE_CHOICE_SINGLE
Action:    validate(step1, [question])
Assertion: No errors
```

---

## 4. WizardValidationService — MATCHING Tests

### TC-VAL-MATCH-001: MATCHING_COUNT_MISMATCH — unequal left/right
**Unit Test** | `WizardValidationServiceTest.java`

```
Setup:    questionType=MATCHING
          lefts: ["A", "B"], rights: ["a"]  (2 lefts, 1 right)
Action:    validate(step1, [question])
Assertion: question.errors contains MATCHING_COUNT_MISMATCH
```

### TC-VAL-MATCH-002: LEFT_ITEM_TOO_LONG — > 300 chars
**Unit Test** | `WizardValidationServiceTest.java`

```
Setup:    lefts: ["A".repeat(301)]
Action:    validate(step1, [question])
Assertion: question.errors contains LEFT_ITEM_TOO_LONG
```

### TC-VAL-MATCH-003: RIGHT_ITEM_TOO_LONG — > 300 chars
**Unit Test** | `WizardValidationServiceTest.java`

```
Setup:    rights: ["B".repeat(301)]
Action:    validate(step1, [question])
Assertion: question.errors contains RIGHT_ITEM_TOO_LONG
```

### TC-VAL-MATCH-004: Valid matching — equal counts, all under 300
**Unit Test** | `WizardValidationServiceTest.java`

```
Setup:    lefts: 3 items (all < 300 chars), rights: 3 items (all < 300 chars)
Action:    validate(step1, [question])
Assertion: No MATCHING errors
```

---

## 5. QuestionGroupWizardService — Step Orchestration Tests

### TC-WIZ-SVC-001: Step 1 saves to session
**Unit Test** | `QuestionGroupWizardServiceImplTest.java`

```
Setup:    Mock HttpSession
Action:    wizardService.saveStep1(session, step1DTO)
Assertion:
  - session.setAttribute("wizard_step1", step1DTO) called
  - session.removeAttribute("wizard_validation_result") called
```

### TC-WIZ-SVC-002: Step 2 AI_GENERATE processes successfully
**Unit Test** | `QuestionGroupWizardServiceImplTest.java`

```
Setup:    Mock aiQuestionService
          step1: skill=READING, cefrLevel=B1
          step2: sourceType=AI_GENERATE, aiQuantity=5, aiQuestionTypes=["READING"]
          aiQuestionService.generate() returns 5 questions
Action:    wizardService.processStep2(session, step2DTO, expertEmail)
Assertion:
  - session.setAttribute("wizard_step2_questions", [...5 questions...]) called
  - Returns WizardStep2ResultDTO with totalQuestions=5
```

### TC-WIZ-SVC-003: Step 2 AI_GENERATE handles rate limit
**Unit Test** | `QuestionGroupWizardServiceImplTest.java`

```
Setup:    Mock rateLimitWindowStore → returns false (blocked)
Action:    processStep2(session, step2DTO with AI_GENERATE, expertEmail)
Assertion:
  - Returns WizardStep2ResultDTO with errorCount > 0
  - Error code = "RATE_LIMITED"
  - No questions returned
```

### TC-WIZ-SVC-004: Step 2 EXCEL_IMPORT with valid .xlsx
**Unit Test** | `QuestionGroupWizardServiceImplTest.java`

```
Setup:    Mock excelQuestionImportService
          step2: excelFile = valid .xlsx MultipartFile
          excelQuestionImportService.parseFile() returns 10 valid rows
Action:    processStep2(session, step2DTO, expertEmail)
Assertion:
  - Returns totalQuestions=10
  - session updated with questions
```

### TC-WIZ-SVC-005: Step 2 EXCEL_IMPORT rejects non-xlsx file
**Unit Test** | `QuestionGroupWizardServiceImplTest.java`

```
Setup:    step2: excelFile = "data.csv"
Action:    processStep2(session, step2DTO, expertEmail)
Assertion:
  - Returns error with code="INVALID_FILE_TYPE"
  - No questions added
```

### TC-WIZ-SVC-006: Step 2 EXCEL_IMPORT rejects file > 5MB
**Unit Test** | `QuestionGroupWizardServiceImplTest.java`

```
Setup:    step2: excelFile with size = 6MB
Action:    processStep2(session, step2DTO, expertEmail)
Assertion:
  - Returns error with code="FILE_TOO_LARGE"
```

### TC-WIZ-SVC-007: Step 2 MANUAL processes inline questions
**Unit Test** | `QuestionGroupWizardServiceImplTest.java`

```
Setup:    step2: sourceType=MANUAL, manualQuestions=[q1, q2]
Action:    processStep2(session, step2DTO, expertEmail)
Assertion:
  - Returns totalQuestions=2
  - session updated
```

### TC-WIZ-SVC-008: Step 2 merges with existing questions (not overwrite)
**Unit Test** | `QuestionGroupWizardServiceImplTest.java`

```
Setup:    Session already has 3 questions
          New step2 adds 2 more (AI_GENERATE)
Action:    processStep2(session, step2DTO, expertEmail)
Assertion:
  - Session has 5 total (3 existing + 2 new)
  - Not overwritten
```

### TC-WIZ-SVC-009: Validate step calls WizardValidationService
**Unit Test** | `QuestionGroupWizardServiceImplTest.java`

```
Setup:    Mock WizardValidationService
          step1 in session, questions in session
Action:    wizardService.validate(session)
Assertion:
  - validationService.validate(step1, questions) called
  - Result stored in session
  - Result returned
```

### TC-WIZ-SVC-010: Save with errors blocked (isClean=false)
**Unit Test** | `QuestionGroupWizardServiceImplTest.java`

```
Setup:    Mock validate() → returns isClean=false
          step1 in session, questions in session
Action:    saveWizard(session, saveDTO, expertEmail) → expect exception
Assertion: Throws InvalidDataException "validation errors. Please fix them"
```

### TC-WIZ-SVC-011: Save with isClean=true persists question group
**Unit Test** | `QuestionGroupWizardServiceImplTest.java`

```
Setup:    Mock validate → isClean=true
          Mock expertQuestionService.createQuestionGroup()
Action:    saveWizard(session, saveDTO, expertEmail)
Assertion:
  - expertQuestionService.createQuestionGroup() called
  - session cleared
  - Returns groupId
```

### TC-WIZ-SVC-012: Abandon clears session
**Unit Test** | `QuestionGroupWizardServiceImplTest.java`

```
Setup:    Session has step1, questions, validation stored
Action:    abandon(session)
Assertion:
  - session.removeAttribute("wizard_step1") called
  - session.removeAttribute("wizard_step2_questions") called
  - session.removeAttribute("wizard_validation_result") called
```

### TC-WIZ-SVC-013: getStep1 retrieves from session
**Unit Test** | `QuestionGroupWizardServiceImplTest.java`

```
Setup:    session.getAttribute("wizard_step1") returns step1DTO
Action:    getStep1(session)
Assertion: Returns step1DTO
```

### TC-WIZ-SVC-014: getQuestions retrieves from session
**Unit Test** | `QuestionGroupWizardServiceImplTest.java`

```
Setup:    session.getAttribute("wizard_step2_questions") returns List<ValidatedQuestionDTO>
Action:    getQuestions(session)
Assertion: Returns the list
```

---

## 6. API Controller Tests

### TC-API-001: POST /api/v1/expert/questions/wizard/step1 saves step1
**Integration Test** | `ExpertQuestionGroupWizardControllerTest.java`

```
Setup:    Auth as EXPERT
          Body: {mode: "TOPIC_BASED", skill: "READING", cefrLevel: "B1", topic: "Science"}
Action:    POST /api/v1/expert/questions/wizard/step1
Assertion:
  - HTTP 200 OK
  - Session stores step1 data
```

### TC-API-002: POST /api/v1/expert/questions/wizard/step2 — AI generate
**Integration Test** | `ExpertQuestionGroupWizardControllerTest.java`

```
Setup:    step1 in session, valid AI request
Action:    POST /api/v1/expert/questions/wizard/step2
          (multipart/form-data: sourceType=AI_GENERATE, aiTopic="Environment",
           aiQuantity=5, aiQuestionTypes=["READING"])
Assertion:
  - HTTP 200 OK
  - Returns WizardStep2ResultDTO with questions
```

### TC-API-003: POST /api/v1/expert/questions/wizard/step2 — Excel import
**Integration Test** | `ExpertQuestionGroupWizardControllerTest.java`

```
Setup:    step1 in session, valid .xlsx file
Action:    POST /api/v1/expert/questions/wizard/step2
          Body: sourceType=EXCEL_IMPORT, excelFile=<.xlsx>
Assertion:
  - HTTP 200 OK
  - Questions parsed from Excel
```

### TC-API-004: POST /api/v1/expert/questions/wizard/validate
**Integration Test** | `ExpertQuestionGroupWizardControllerTest.java`

```
Setup:    step1 and questions in session
Action:    POST /api/v1/expert/questions/wizard/validate
Assertion:
  - HTTP 200 OK
  - Returns WizardValidationResultDTO
  - isClean reflects validation result
```

### TC-API-005: POST /api/v1/expert/questions/wizard/save publishes
**Integration Test** | `ExpertQuestionGroupWizardControllerTest.java`

```
Setup:    Valid step1 + questions, no errors
Action:    POST /api/v1/expert/questions/wizard/save
          Body: {status: "PUBLISHED", source: "EXPERT_BANK"}
Assertion:
  - HTTP 200 OK
  - Returns groupId
  - QuestionGroup created in DB
```

### TC-API-006: POST /save without valid session returns error
**Integration Test** | `ExpertQuestionGroupWizardControllerTest.java`

```
Setup:    Session is empty (step1 not set)
Action:    POST /api/v1/expert/questions/wizard/save
Assertion:
  - HTTP 409 Conflict or 400 Bad Request
  - Error: "Step 1 data not found. Please restart the wizard."
```

### TC-API-007: GET /api/v1/expert/questions/wizard/step-data — recovery
**Integration Test** | `ExpertQuestionGroupWizardControllerTest.java`

```
Setup:    Session has step1 and questions stored
Action:    GET /api/v1/expert/questions/wizard/step-data
Assertion:
  - HTTP 200 OK
  - Returns WizardStepDataDTO with step1, questions, validation
```

### TC-API-008: GET /api/v1/expert/questions/wizard/abandon clears session
**Integration Test** | `ExpertQuestionGroupWizardControllerTest.java`

```
Setup:    Session has data
Action:    GET /api/v1/expert/questions/wizard/abandon
Assertion:
  - HTTP 200 OK
  - Session cleared
  - Redirects or returns success
```

### TC-API-009: TEACHER cannot use wizard
**Integration Test** | `ExpertQuestionGroupWizardControllerTest.java`

```
Setup:    Auth as TEACHER
Action:    POST /api/v1/expert/questions/wizard/step1
Assertion: HTTP 403 Forbidden
```

### TC-API-010: Student cannot access wizard
**Integration Test** | `ExpertQuestionGroupWizardControllerTest.java`

```
Setup:    Auth as STUDENT
Action:    POST /api/v1/expert/questions/wizard/step1
Assertion: HTTP 403 Forbidden
```

---

## 7. Full Wizard Flow Tests

### TC-E2E-001: Full AI Generate → Validate → Save flow
**Integration Test** | `QuestionGroupWizardFlowTest.java`

```
Setup:    Expert authenticated

Flow:
  Step 1 → POST /api/v1/expert/questions/wizard/step1
    Body: {
      mode: "TOPIC_BASED",
      skill: "READING",
      cefrLevel: "B1",
      topic: "Climate Change",
      tags: ["environment", "global-warming"],
      passageContent: null
    }
    → HTTP 200

  Step 2 → POST /api/v1/expert/questions/wizard/step2
    Body: {
      sourceType: "AI_GENERATE",
      aiTopic: "Climate change effects",
      aiQuantity: 5,
      aiQuestionTypes: ["MULTIPLE_CHOICE_SINGLE", "FILL_IN_BLANK"]
    }
    → HTTP 200, 5 questions returned

  Validation → POST /api/v1/expert/questions/wizard/validate
    → HTTP 200, isClean=true

  Save → POST /api/v1/expert/questions/wizard/save
    Body: {status: "PUBLISHED", source: "EXPERT_BANK"}
    → HTTP 200, groupId returned

Assertion:
  - QuestionGroup created in DB
  - 5 questions created and linked to group
  - All questions have skill=READING, cefrLevel=B1
  - status=PUBLISHED, source=EXPERT_BANK
  - Session cleared after save
```

### TC-E2E-002: Excel import → partial errors → fix → save
**Integration Test** | `QuestionGroupWizardFlowTest.java`

```
Setup:    Excel file with 10 rows: 8 valid, 2 with parse errors

Flow:
  Step 1 → valid

  Step 2 → Excel upload
    → Returns 8 questions + 2 error rows

  Step 3 → Validate
    → isClean=true (only 8 questions, no validation errors)

  Save → HTTP 200

Assertion:
  - 8 questions saved to DB
  - 2 error rows discarded
```

### TC-E2E-003: Validation errors block Step 4
**Integration Test** | `QuestionGroupWizardFlowTest.java`

```
Setup:    Step 2: AI generate, some questions have SKILL_MISMATCH

Flow:
  Step 3 → Validate
    → isClean=false, errors present

  Step 4 (Save) → attempt
    → HTTP 409 Conflict or blocked

Assertion:
  - Save blocked until errors fixed
  - Error list returned clearly
```

### TC-E2E-004: Mixed sources (AI + Manual) in same wizard
**Integration Test** | `QuestionGroupWizardFlowTest.java`

```
Setup:    Expert uses both AI and manual in same session

Flow:
  Step 2a → AI Generate → 3 questions
  Step 2a → Manual Add → 2 more questions
    → Session has 5 total

  Validate → HTTP 200, isClean=true

  Save → HTTP 200

Assertion:
  - All 5 questions saved (3 AI + 2 manual)
  - Correctly attributed by source
```

### TC-E2E-005: Rate limit hit mid-session
**Integration Test** | `QuestionGroupWizardFlowTest.java`

```
Setup:    Expert hits 10 req/min limit

Flow:
  Step 2 → AI Generate → blocked (RATE_LIMITED)

Assertion:
  - Error shown: "Bạn đã vượt giới hạn 10 yêu cầu AI/phút"
  - Expert can wait and retry
  - Step 1 data preserved
```

### TC-E2E-006: beforeunload warning on incomplete session
**UI/Integration Test** | `QuestionGroupWizardFlowTest.java`

```
Setup:    Step 1 completed, step 2 partially done
          User has questions in session

Flow:
  Browser triggers beforeunload event

Assertion:
  - Browser shows warning dialog
  - Or: Expert clicks "Leave Wizard" explicitly
  - Session cleared via /abandon endpoint
```

### TC-E2E-007: Session recovery after page refresh
**Integration Test** | `QuestionGroupWizardFlowTest.java`

```
Setup:    User fills Step 1 + 2, then refreshes page

Flow:
  GET /api/v1/expert/questions/wizard/step-data
    → Returns existing step1 + questions

Assertion:
  - Recovery message shown: "Continuing where you left off"
  - User continues without data loss
```

---

## 8. Validation Error Code Coverage Matrix

| Rule | Input | Expected Code | Severity | Test |
|---|---|---|---|---|
| Passage-based, no passage | `passageContent=null` | `PASSAGE_REQUIRED` | ERROR | TC-VAL-GRP-001 |
| Passage < 10 chars | `"short"` | `PASSAGE_TOO_SHORT` | ERROR | TC-VAL-GRP-002 |
| Passage > 5000 chars | `"x"*5001` | `PASSAGE_TOO_LONG` | ERROR | TC-VAL-GRP-003 |
| Audio wrong extension | `.pdf` | `INVALID_AUDIO_URL` | ERROR | TC-VAL-GRP-006 |
| Image wrong extension | `.gif` | `INVALID_IMAGE_URL` | ERROR | TC-VAL-GRP-009 |
| No questions | `[]` | `NO_QUESTIONS` | ERROR | TC-VAL-GRP-011 |
| > 100 questions | `101` questions | `TOO_MANY_QUESTIONS` | ERROR | TC-VAL-GRP-013 |
| > 10 tags | `11` tags | `TOO_MANY_TAGS` | ERROR | TC-VAL-GRP-015 |
| Invalid skill | `"INVALID"` | `INVALID_SKILL` | ERROR | TC-VAL-GRP-016 |
| Invalid CEFR | `"B3"` | `INVALID_CEFR` | ERROR | TC-VAL-GRP-017 |
| Skill mismatch | `group:READING, q:WRITING` | `SKILL_MISMATCH` | ERROR | TC-VAL-Q-001 |
| CEFR mismatch | `group:B1, q:C1` | `CEFR_MISMATCH` | ERROR | TC-VAL-Q-003 |
| Wrong type for skill | `LISTENING+WRITING` | `INVALID_TYPE_FOR_SKILL` | ERROR | TC-VAL-Q-004 |
| Content < 10 chars | `"short"` | `CONTENT_TOO_SHORT` | ERROR | TC-VAL-Q-006 |
| Content > 2000 chars | `"x"*2001` | `CONTENT_TOO_LONG` | ERROR | TC-VAL-Q-007 |
| Explanation missing | `null` | `EXPLANATION_MISSING` | WARNING | TC-VAL-Q-008 |
| Tags missing | `null` | `TAGS_MISSING` | WARNING | TC-VAL-Q-009 |
| < 2 options | `1` option | `TOO_FEW_OPTIONS` | ERROR | TC-VAL-MC-001 |
| No correct answer | `all correct=false` | `NO_CORRECT_ANSWER` | ERROR | TC-VAL-MC-002 |
| All options correct (multi) | `all correct=true` | `ALL_OPTIONS_CORRECT` | ERROR | TC-VAL-MC-003 |
| Duplicate option titles | `"Paris"/"paris"` | `OPTION_DUPLICATE` | ERROR | TC-VAL-MC-005 |
| Empty option title | `""` | `OPTION_EMPTY` | ERROR | TC-VAL-MC-006 |
| Option > 500 chars | `"x"*501` | `OPTION_TOO_LONG` | ERROR | TC-VAL-MC-007 |
| Matching unequal sides | `2 left, 1 right` | `MATCHING_COUNT_MISMATCH` | ERROR | TC-VAL-MATCH-001 |
| Left item > 300 chars | `"x"*301` | `LEFT_ITEM_TOO_LONG` | ERROR | TC-VAL-MATCH-002 |
| Right item > 300 chars | `"x"*301` | `RIGHT_ITEM_TOO_LONG` | ERROR | TC-VAL-MATCH-003 |
| Rate limit exceeded | 11th AI req | `RATE_LIMITED` | ERROR | TC-WIZ-SVC-003 |
| Non-xlsx file | `.csv` | `INVALID_FILE_TYPE` | ERROR | TC-WIZ-SVC-005 |
| File > 5MB | `6MB` | `FILE_TOO_LARGE` | ERROR | TC-WIZ-SVC-006 |

**Total: 27 validation rule tests**

---

## Test Fixtures

```java
private WizardStep1DTO createValidStep1(String mode) { ... }
private ValidatedQuestionDTO createValidQuestion(String skill, String cefr,
    String questionType) { ... }
private List<ValidatedQuestionDTO> createQuestionList(int count) { ... }
private void assertError(List<ValidationErrorDTO> errors, String code) { ... }
private void assertWarning(List<ValidationErrorDTO> warnings, String code) { ... }
private WizardStep2DTO createAiStep2(String topic, int qty) { ... }
```
