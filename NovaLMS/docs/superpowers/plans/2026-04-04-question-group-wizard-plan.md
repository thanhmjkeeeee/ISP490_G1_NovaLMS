# QuestionGroup Wizard — Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a single-page 4-step wizard at `/expert/questions/wizard` enabling Expert users to create QuestionGroup entities via AI generation, Excel import, or manual entry, with full session-based state management and block-on-error validation.

**Architecture:** Single dedicated controller (`ExpertQuestionGroupWizardController`) + `QuestionGroupWizardService` orchestrator + `WizardValidationService` + Thymeleaf template + JS. Reuses existing `AIQuestionService`, `ExcelQuestionImportService`, and extends `IExpertQuestionService`.

**Tech Stack:** Spring Boot 3.3.4, Java 17, Thymeleaf, Jakarta Validation, Apache POI, Groq API

---

## Chunk 1: DTOs — Wizard Request/Response DTOs

**Files to create:**

| File | Purpose |
|---|---|
| `dto/request/WizardStep1DTO.java` | Step 1 payload |
| `dto/request/WizardStep2DTO.java` | Step 2 payload (AI/Excel/Manual source) |
| `dto/request/WizardSaveDTO.java` | Step 4 save payload |
| `dto/request/ManualQuestionDTO.java` | Inline manual question within Step 2 |
| `dto/response/WizardValidationResultDTO.java` | Validation result for Step 3 |
| `dto/response/ValidationErrorDTO.java` | Individual error/warning item |
| `dto/response/ValidatedQuestionDTO.java` | Validated question with its errors attached |

**Files to reference (read-only):**
- `dto/request/QuestionRequestDTO.java` — for `AnswerOptionDTO` pattern
- `dto/request/QuestionGroupRequestDTO.java` — for existing DTO pattern
- `dto/response/ResponseData.java` — for response wrapper

---

### 1.1 WizardStep1DTO.java

```java
package com.example.DoAn.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WizardStep1DTO {

    @NotNull(message = "Mode is required")
    private String mode; // "PASSAGE_BASED" or "TOPIC_BASED"

    @NotBlank(message = "Skill is required")
    private String skill; // "LISTENING", "READING", "WRITING", "SPEAKING"

    @NotBlank(message = "CEFR level is required")
    private String cefrLevel; // "A1", "A2", "B1", "B2", "C1", "C2"

    @NotBlank(message = "Topic is required")
    @Size(min = 2, max = 200, message = "Topic must be 2-200 characters")
    private String topic;

    private List<@Size(max = 50) String> tags; // max 10 items

    @Size(min = 10, max = 5000, message = "Passage must be 10-5000 characters")
    private String passageContent; // required when mode == PASSAGE_BASED

    @Size(max = 500, message = "Audio URL too long")
    private String audioUrl;

    @Size(max = 500, message = "Image URL too long")
    private String imageUrl;
}
```

---

### 1.2 ManualQuestionDTO.java

```java
package com.example.DoAn.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManualQuestionDTO {

    @NotBlank(message = "Question content is required")
    @Size(min = 10, max = 2000, message = "Content must be 10-2000 characters")
    private String content;

    @NotBlank(message = "Question type is required")
    private String questionType; // "MULTIPLE_CHOICE_SINGLE", "MULTIPLE_CHOICE_MULTI", "FILL_IN_BLANK", "MATCHING", "WRITING", "SPEAKING"

    private String explanation;

    // For MC / FILL_IN_BLANK / MATCHING
    private List<AnswerOptionInput> options;

    // For MATCHING pairs: each entry is "leftIndex:rightIndex" e.g. "0:2"
    private List<String> matchingPairs;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AnswerOptionInput {
        private String title;
        private Boolean correct; // true for correct answer(s)
        private String matchTarget; // non-null for left items in MATCHING type
    }
}
```

---

### 1.3 WizardStep2DTO.java

```java
package com.example.DoAn.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WizardStep2DTO {

    @NotBlank(message = "Source type is required")
    private String sourceType; // "AI_GENERATE", "EXCEL_IMPORT", "MANUAL"

    // AI_GENERATE fields
    @Size(min = 2, max = 200)
    private String aiTopic;

    @Min(1) @Max(50)
    private Integer aiQuantity;

    private List<String> aiQuestionTypes; // e.g. ["MULTIPLE_CHOICE_SINGLE", "FILL_IN_BLANK"]

    // EXCEL_IMPORT fields
    private MultipartFile excelFile;
    private String excelQuestionType; // the question type for the uploaded Excel

    // MANUAL fields
    private List<ManualQuestionDTO> manualQuestions;
}
```

---

### 1.4 WizardSaveDTO.java

```java
package com.example.DoAn.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WizardSaveDTO {

    @NotNull(message = "Status is required")
    private String status; // "DRAFT" or "PUBLISHED"

    @NotBlank(message = "Source is required")
    private String source; // "EXPERT_BANK" or "TEACHER_PRIVATE"
}
```

---

### 1.5 ValidationErrorDTO.java

```java
package com.example.DoAn.dto.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationErrorDTO {
    private int questionIndex; // -1 for group-level errors
    private String field;     // "passageContent", "cefrLevel", etc.
    private String code;       // "PASSAGE_TOO_SHORT", "CEFR_MISMATCH", etc.
    private String message;    // human-readable Vietnamese message
    private String severity;   // "ERROR" or "WARNING"
}
```

---

### 1.6 ValidatedQuestionDTO.java

```java
package com.example.DoAn.dto.response;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidatedQuestionDTO {
    private int index;
    private String content;
    private String questionType;
    private String skill;
    private String cefrLevel;
    private String explanation;
    private List<OptionDTO> options;
    private List<String> matchingPairs;
    private List<ValidationErrorDTO> errors;  // errors specific to this question
    private List<ValidationErrorDTO> warnings;
    private boolean isValid; // true if errors.isEmpty()

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OptionDTO {
        private String title;
        private Boolean correct;
        private String matchTarget;
    }
}
```

---

### 1.7 WizardValidationResultDTO.java

```java
package com.example.DoAn.dto.response;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WizardValidationResultDTO {
    private List<ValidatedQuestionDTO> questions;
    private List<ValidationErrorDTO> groupErrors;
    private List<ValidationErrorDTO> groupWarnings;
    private boolean isClean;      // true if groupErrors.isEmpty() AND all questions.isValid
    private int totalQuestions;
    private int errorCount;
    private int warningCount;
}
```

---

## Chunk 2: WizardValidationService — Validation Engine

**Files to create:**
- `service/impl/WizardValidationService.java` — all validation rules

**Files to reference (read-only):**
- `dto/request/WizardStep1DTO.java`
- `dto/response/WizardValidationResultDTO.java`
- `dto/response/ValidatedQuestionDTO.java`
- `dto/response/ValidationErrorDTO.java`

**Session access:** `HttpSession` passed as parameter to `validate(HttpSession session)`.

**Valid skill-to-type mapping:**
```
LISTENING  → MULTIPLE_CHOICE_SINGLE, MULTIPLE_CHOICE_MULTI, FILL_IN_BLANK, MATCHING
READING    → MULTIPLE_CHOICE_SINGLE, MULTIPLE_CHOICE_MULTI, FILL_IN_BLANK, MATCHING
WRITING    → WRITING
SPEAKING   → SPEAKING
```

**Media URL regex patterns:**
```
Audio: ^https?://.*\.(mp3|wav|ogg|m4a)(\?.*)?$
Image: ^https?://.*\.(jpg|jpeg|png|webp)(\?.*)?$
```

---

### 2.1 WizardValidationService.java (core logic)

```java
package com.example.DoAn.service.impl;

import com.example.DoAn.dto.request.ManualQuestionDTO;
import com.example.DoAn.dto.request.WizardStep1DTO;
import com.example.DoAn.dto.response.*;
import com.example.DoAn.dto.response.ValidatedQuestionDTO.OptionDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WizardValidationService {

    private static final Pattern AUDIO_PATTERN = Pattern.compile(
            "^https?://.*\\.(mp3|wav|ogg|m4a)(\\?.*)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern IMAGE_PATTERN = Pattern.compile(
            "^https?://.*\\.(jpg|jpeg|png|webp)(\\?.*)?$", Pattern.CASE_INSENSITIVE);

    private static final Set<String> VALID_SKILLS = Set.of(
            "LISTENING", "READING", "WRITING", "SPEAKING");
    private static final Set<String> VALID_CEFR = Set.of(
            "A1", "A2", "B1", "B2", "C1", "C2");

    private static final Map<String, Set<String>> SKILL_TYPE_MAP = Map.of(
            "LISTENING", Set.of("MULTIPLE_CHOICE_SINGLE", "MULTIPLE_CHOICE_MULTI", "FILL_IN_BLANK", "MATCHING"),
            "READING",   Set.of("MULTIPLE_CHOICE_SINGLE", "MULTIPLE_CHOICE_MULTI", "FILL_IN_BLANK", "MATCHING"),
            "WRITING",   Set.of("WRITING"),
            "SPEAKING",  Set.of("SPEAKING")
    );

    public WizardValidationResultDTO validate(
            WizardStep1DTO step1,
            List<ValidatedQuestionDTO> questions) {

        List<ValidationErrorDTO> groupErrors = new ArrayList<>();
        List<ValidationErrorDTO> groupWarnings = new ArrayList<>();

        // ─── Group-level validation ───
        if ("PASSAGE_BASED".equals(step1.getMode())) {
            if (step1.getPassageContent() == null || step1.getPassageContent().isBlank()) {
                groupErrors.add(error(-1, "passageContent", "PASSAGE_REQUIRED",
                        "Passage content is required for passage-based groups."));
            } else if (step1.getPassageContent().length() < 10) {
                groupErrors.add(error(-1, "passageContent", "PASSAGE_TOO_SHORT",
                        "Passage must be at least 10 characters."));
            } else if (step1.getPassageContent().length() > 5000) {
                groupErrors.add(error(-1, "passageContent", "PASSAGE_TOO_LONG",
                        "Passage must not exceed 5000 characters."));
            }
        }

        if (!VALID_SKILLS.contains(step1.getSkill())) {
            groupErrors.add(error(-1, "skill", "INVALID_SKILL",
                    "Invalid skill value: " + step1.getSkill()));
        }

        if (!VALID_CEFR.contains(step1.getCefrLevel())) {
            groupErrors.add(error(-1, "cefrLevel", "INVALID_CEFR",
                    "Invalid CEFR level: " + step1.getCefrLevel()));
        }

        if (step1.getTags() != null && step1.getTags().size() > 10) {
            groupErrors.add(error(-1, "tags", "TOO_MANY_TAGS",
                    "Maximum 10 tags allowed."));
        }

        if (step1.getAudioUrl() != null && !step1.getAudioUrl().isBlank()
                && !AUDIO_PATTERN.matcher(step1.getAudioUrl()).matches()) {
            groupErrors.add(error(-1, "audioUrl", "INVALID_AUDIO_URL",
                    "Audio URL must end with .mp3, .wav, .ogg, or .m4a"));
        }

        if (step1.getImageUrl() != null && !step1.getImageUrl().isBlank()
                && !IMAGE_PATTERN.matcher(step1.getImageUrl()).matches()) {
            groupErrors.add(error(-1, "imageUrl", "INVALID_IMAGE_URL",
                    "Image URL must end with .jpg, .jpeg, .png, or .webp"));
        }

        if (questions == null || questions.isEmpty()) {
            groupErrors.add(error(-1, "questions", "NO_QUESTIONS",
                    "At least one question is required."));
        } else if (questions.size() > 100) {
            groupErrors.add(error(-1, "questions", "TOO_MANY_QUESTIONS",
                    "Maximum 100 questions allowed per group."));
        }

        // ─── Per-question validation ───
        List<ValidatedQuestionDTO> validatedQuestions = new ArrayList<>();
        int idx = 0;
        for (ValidatedQuestionDTO q : questions) {
            validatedQuestions.add(validateQuestion(q, step1, idx));
            idx++;
        }

        int totalErrors = groupErrors.size()
                + validatedQuestions.stream().mapToInt(q -> q.getErrors().size()).sum();

        return WizardValidationResultDTO.builder()
                .questions(validatedQuestions)
                .groupErrors(groupErrors)
                .groupWarnings(groupWarnings)
                .isClean(totalErrors == 0)
                .totalQuestions(questions != null ? questions.size() : 0)
                .errorCount(totalErrors)
                .warningCount(
                        groupWarnings.size()
                        + validatedQuestions.stream().mapToInt(q -> q.getWarnings().size()).sum())
                .build();
    }

    private ValidatedQuestionDTO validateQuestion(
            ValidatedQuestionDTO q, WizardStep1DTO step1, int idx) {

        List<ValidationErrorDTO> errors = new ArrayList<>();
        List<ValidationErrorDTO> warnings = new ArrayList<>();

        // Skill mismatch
        if (!step1.getSkill().equalsIgnoreCase(q.getSkill())) {
            errors.add(error(idx, "skill", "SKILL_MISMATCH",
                    "Question skill '" + q.getSkill() + "' does not match group skill '" + step1.getSkill() + "'"));
        }

        // CEFR mismatch
        if (!step1.getCefrLevel().equalsIgnoreCase(q.getCefrLevel())) {
            errors.add(error(idx, "cefrLevel", "CEFR_MISMATCH",
                    "Question CEFR '" + q.getCefrLevel() + "' does not match group CEFR '" + step1.getCefrLevel() + "'"));
        }

        // Invalid type for skill
        Set<String> allowedTypes = SKILL_TYPE_MAP.getOrDefault(step1.getSkill().toUpperCase(), Set.of());
        if (!allowedTypes.contains(q.getQuestionType())) {
            errors.add(error(idx, "questionType", "INVALID_TYPE_FOR_SKILL",
                    "Question type '" + q.getQuestionType() + "' is not allowed for skill '" + step1.getSkill() + "'"));
        }

        // Content length
        String content = q.getContent() != null ? q.getContent().trim() : "";
        if (content.isEmpty() || content.length() < 10) {
            errors.add(error(idx, "content", "CONTENT_TOO_SHORT",
                    "Question content must be at least 10 characters."));
        } else if (content.length() > 2000) {
            errors.add(error(idx, "content", "CONTENT_TOO_LONG",
                    "Question content must not exceed 2000 characters."));
        }

        // Explanation missing — WARNING only
        if (q.getExplanation() == null || q.getExplanation().isBlank()) {
            warnings.add(error(idx, "explanation", "EXPLANATION_MISSING",
                    "Explanation is recommended for better learner feedback."));
        }

        // MC / FILL / MATCHING — validate options
        String qt = q.getQuestionType();
        if (!"WRITING".equals(qt) && !"SPEAKING".equals(qt)) {
            if (q.getOptions() == null || q.getOptions().isEmpty()) {
                errors.add(error(idx, "options", "NO_OPTIONS",
                        "Options are required for this question type."));
            } else {
                validateOptions(q, errors, warnings, idx);
            }
        }

        // Tags missing — WARNING only
        if (q.getTags() == null || q.getTags().isEmpty()) {
            warnings.add(error(idx, "tags", "TAGS_MISSING",
                    "Adding tags is recommended for organization."));
        }

        return ValidatedQuestionDTO.builder()
                .index(idx)
                .content(q.getContent())
                .questionType(q.getQuestionType())
                .skill(q.getSkill())
                .cefrLevel(q.getCefrLevel())
                .explanation(q.getExplanation())
                .options(q.getOptions())
                .matchingPairs(q.getMatchingPairs())
                .errors(errors)
                .warnings(warnings)
                .isValid(errors.isEmpty())
                .build();
    }

    private void validateOptions(ValidatedQuestionDTO q, List<ValidationErrorDTO> errors,
                                  List<ValidationErrorDTO> warnings, int idx) {
        List<OptionDTO> opts = q.getOptions();

        if ("MATCHING".equals(q.getQuestionType())) {
            // Split into left (has matchTarget) and right (no matchTarget)
            List<OptionDTO> lefts = opts.stream()
                    .filter(o -> o.getMatchTarget() != null && !o.getMatchTarget().isBlank()).toList();
            List<OptionDTO> rights = opts.stream()
                    .filter(o -> o.getMatchTarget() == null || o.getMatchTarget().isBlank()).toList();

            if (lefts.size() != rights.size()) {
                errors.add(error(idx, "options", "MATCHING_COUNT_MISMATCH",
                        "Matching left and right items must have the same count."));
            }
            for (OptionDTO o : lefts) {
                if (o.getTitle() == null || o.getTitle().length() > 300) {
                    errors.add(error(idx, "options", "LEFT_ITEM_TOO_LONG",
                            "Left item '" + o.getTitle() + "' exceeds 300 characters."));
                }
            }
            for (OptionDTO o : rights) {
                if (o.getTitle() == null || o.getTitle().length() > 300) {
                    errors.add(error(idx, "options", "RIGHT_ITEM_TOO_LONG",
                            "Right item '" + o.getTitle() + "' exceeds 300 characters."));
                }
            }
        } else {
            // MC or FILL_IN_BLANK
            if (opts.size() < 2) {
                errors.add(error(idx, "options", "TOO_FEW_OPTIONS",
                        "At least 2 options are required."));
            }

            long correctCount = opts.stream().filter(o -> Boolean.TRUE.equals(o.getCorrect())).count();
            if (correctCount == 0) {
                errors.add(error(idx, "options", "NO_CORRECT_ANSWER",
                        "At least one correct answer is required."));
            }
            if ("MULTIPLE_CHOICE_MULTI".equals(q.getQuestionType()) && correctCount == opts.size()) {
                errors.add(error(idx, "options", "ALL_OPTIONS_CORRECT",
                        "Not all options can be correct for multi-select questions."));
            }

            // Check for duplicates
            Set<String> titles = new HashSet<>();
            for (OptionDTO o : opts) {
                String t = o.getTitle() != null ? o.getTitle().trim() : "";
                if (t.isEmpty()) {
                    errors.add(error(idx, "options", "OPTION_EMPTY",
                            "Option title cannot be empty."));
                } else if (t.length() > 500) {
                    errors.add(error(idx, "options", "OPTION_TOO_LONG",
                            "Option title exceeds 500 characters: '" + t.substring(0, Math.min(50, t.length())) + "...'"));
                }
                if (!titles.add(t.toLowerCase())) {
                    errors.add(error(idx, "options", "OPTION_DUPLICATE",
                            "Duplicate option title: '" + t + "'"));
                }
            }
        }
    }

    private ValidationErrorDTO error(int idx, String field, String code, String message) {
        return ValidationErrorDTO.builder()
                .questionIndex(idx)
                .field(field)
                .code(code)
                .message(message)
                .severity("ERROR".equals(code) ? "ERROR" : "WARNING")
                .build();
    }
}
```

---

## Chunk 3: QuestionGroupWizardService — Orchestrator

**Files to create:**
- `service/IQuestionGroupWizardService.java` — interface
- `service/impl/QuestionGroupWizardServiceImpl.java` — implementation

**Files to reference (read-only):**
- `service/IExpertQuestionService.java`
- `service/AIQuestionService.java`
- `service/ExcelQuestionImportService.java`
- `service/impl/WizardValidationService.java`
- `dto/request/WizardStep1DTO.java`
- `dto/request/WizardStep2DTO.java`
- `dto/request/WizardSaveDTO.java`
- `dto/response/WizardValidationResultDTO.java`
- `dto/response/ValidatedQuestionDTO.java`
- `dto/request/QuestionGroupRequestDTO.java`
- `dto/request/QuestionRequestDTO.java`
- `dto/request/AIImportRequestDTO.java`
- `model/QuestionGroup.java`

**Session keys (string constants):**
```java
static final String SESSION_STEP1 = "wizard_step1";
static final String SESSION_QUESTIONS = "wizard_step2_questions";
static final String SESSION_VALIDATION = "wizard_validation_result";
```

### 3.1 IQuestionGroupWizardService.java

```java
package com.example.DoAn.service;

import com.example.DoAn.dto.request.*;
import com.example.DoAn.dto.response.*;

public interface IQuestionGroupWizardService {
    void saveStep1(HttpSession session, WizardStep1DTO dto);
    WizardStep2ResultDTO processStep2(HttpSession session, WizardStep2DTO dto, String email);
    WizardValidationResultDTO validate(HttpSession session);
    Integer saveWizard(HttpSession session, WizardSaveDTO dto, String email);
    WizardStep1DTO getStep1(HttpSession session);
    List<ValidatedQuestionDTO> getQuestions(HttpSession session);
    WizardValidationResultDTO getValidationResult(HttpSession session);
    void abandon(HttpSession session);
}
```

### 3.2 WizardStep2ResultDTO.java (response DTO for Step 2)

```java
package com.example.DoAn.dto.response;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WizardStep2ResultDTO {
    private List<ValidatedQuestionDTO> questions;
    private List<ValidationErrorDTO> errors;    // parse/AI errors (non-blocking)
    private int totalQuestions;
    private int errorCount;
    private String sourceType;
}
```

### 3.3 QuestionGroupWizardServiceImpl.java (key methods)

```java
package com.example.DoAn.service.impl;

import com.example.DoAn.dto.request.*;
import com.example.DoAn.dto.response.*;
import com.example.DoAn.dto.response.ValidatedQuestionDTO.OptionDTO;
import com.example.DoAn.service.*;
import com.example.DoAn.service.impl.WizardValidationService;
import com.example.DoAn.util.RateLimitWindowStore;
import com.example.DoAn.exception.InvalidDataException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionGroupWizardServiceImpl implements IQuestionGroupWizardService {

    private final AIQuestionService aiQuestionService;
    private final ExcelQuestionImportService excelQuestionImportService;
    private final IExpertQuestionService expertQuestionService;
    private final WizardValidationService validationService;

    static final String SESSION_STEP1 = "wizard_step1";
    static final String SESSION_QUESTIONS = "wizard_step2_questions";
    static final String SESSION_VALIDATION = "wizard_validation_result";

    // ─── Step 1 ───────────────────────────────────────────
    @Override
    public void saveStep1(HttpSession session, WizardStep1DTO dto) {
        session.setAttribute(SESSION_STEP1, dto);
        session.removeAttribute(SESSION_VALIDATION);
    }

    @Override
    public WizardStep1DTO getStep1(HttpSession session) {
        return (WizardStep1DTO) session.getAttribute(SESSION_STEP1);
    }

    // ─── Step 2 ────────────────────────────────────────────
    @Override
    public WizardStep2ResultDTO processStep2(HttpSession session, WizardStep2DTO dto, String email) {
        WizardStep1DTO step1 = getStep1(session);
        if (step1 == null) {
            throw new InvalidDataException("Step 1 data not found. Please restart the wizard.");
        }

        List<ValidatedQuestionDTO> questions = new ArrayList<>();
        List<ValidationErrorDTO> errors = new ArrayList<>();

        switch (dto.getSourceType()) {
            case "AI_GENERATE" -> {
                var result = processAiGenerate(dto, email, errors);
                questions.addAll(result);
            }
            case "EXCEL_IMPORT" -> {
                var result = processExcelImport(dto, errors);
                questions.addAll(result);
            }
            case "MANUAL" -> {
                var result = processManual(dto, errors);
                questions.addAll(result);
            }
            default -> throw new InvalidDataException("Unknown source type: " + dto.getSourceType());
        }

        // Merge with existing questions (don't overwrite on repeated Step 2 calls)
        List<ValidatedQuestionDTO> existing = getQuestions(session);
        List<ValidatedQuestionDTO> merged = new ArrayList<>(existing);
        merged.addAll(questions);

        session.setAttribute(SESSION_QUESTIONS, merged);
        session.removeAttribute(SESSION_VALIDATION);

        return WizardStep2ResultDTO.builder()
                .questions(merged)
                .errors(errors)
                .totalQuestions(merged.size())
                .errorCount(errors.size())
                .sourceType(dto.getSourceType())
                .build();
    }

    private List<ValidatedQuestionDTO> processAiGenerate(
            WizardStep2DTO dto, String email, List<ValidationErrorDTO> errors) {

        // Rate limit check
        if (!RateLimitWindowStore.tryAcquire(email)) {
            errors.add(ValidationErrorDTO.builder()
                    .questionIndex(-1).field("rateLimit").code("RATE_LIMITED")
                    .message("Bạn đã vượt giới hạn 10 yêu cầu AI/phút. Vui lòng chờ một lát.")
                    .severity("ERROR").build());
            return List.of();
        }

        try {
            // Build AI request
            var aiRequest = AIGenerateRequestDTO.builder()
                    .topic(dto.getAiTopic())
                    .quantity(dto.getAiQuantity() != null ? dto.getAiQuantity() : 5)
                    .skill(getStep1Skill())
                    .cefrLevel(getStep1Cefr())
                    .questionTypes(dto.getAiQuestionTypes())
                    .build();

            var aiResponse = aiQuestionService.generate(aiRequest);

            return aiResponse.getQuestions().stream()
                    .map(qdto -> toValidatedQuestion(qdto.getContent(), qdto.getQuestionType(),
                            qdto.getSkill(), qdto.getCefrLevel(), qdto.getExplanation(),
                            qdto.getOptions(), qdto.getMatchLeft(), qdto.getMatchRight(),
                            qdto.getCorrectPairs(), null))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("AI generation failed", e);
            errors.add(ValidationErrorDTO.builder()
                    .questionIndex(-1).field("ai").code("AI_GENERATION_FAILED")
                    .message("AI generation failed: " + e.getMessage())
                    .severity("ERROR").build());
            return List.of();
        }
    }

    private List<ValidatedQuestionDTO> processExcelImport(
            WizardStep2DTO dto, List<ValidationErrorDTO> errors) {

        MultipartFile file = dto.getExcelFile();
        if (file == null || file.isEmpty()) {
            errors.add(ValidationErrorDTO.builder()
                    .questionIndex(-1).field("excelFile").code("FILE_REQUIRED")
                    .message("Please upload an Excel file.").severity("ERROR").build());
            return List.of();
        }

        String fn = file.getOriginalFilename();
        if (fn == null || !fn.toLowerCase().endsWith(".xlsx")) {
            errors.add(ValidationErrorDTO.builder()
                    .questionIndex(-1).field("excelFile").code("INVALID_FILE_TYPE")
                    .message("Only .xlsx files are supported.").severity("ERROR").build());
            return List.of();
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            errors.add(ValidationErrorDTO.builder()
                    .questionIndex(-1).field("excelFile").code("FILE_TOO_LARGE")
                    .message("File size must not exceed 5MB.").severity("ERROR").build());
            return List.of();
        }

        try {
            var parseResult = excelQuestionImportService.parseFile(file, dto.getExcelQuestionType());
            List<ValidatedQuestionDTO> questions = new ArrayList<>();
            int idx = 0;
            for (var row : parseResult.getValidRows()) {
                questions.add(excelRowToValidatedQuestion(row, idx++));
            }
            // Add parse errors as non-blocking errors
            for (var err : parseResult.getErrorRows()) {
                errors.add(ValidationErrorDTO.builder()
                        .questionIndex(err.getRowNumber())
                        .field("excelRow")
                        .code("EXCEL_ROW_ERROR")
                        .message(err.getError())
                        .severity("ERROR").build());
            }
            return questions;
        } catch (Exception e) {
            log.error("Excel parse failed", e);
            errors.add(ValidationErrorDTO.builder()
                    .questionIndex(-1).field("excelFile").code("EXCEL_PARSE_FAILED")
                    .message("Failed to parse Excel file: " + e.getMessage())
                    .severity("ERROR").build());
            return List.of();
        }
    }

    private List<ValidatedQuestionDTO> processManual(
            WizardStep2DTO dto, List<ValidationErrorDTO> errors) {

        if (dto.getManualQuestions() == null || dto.getManualQuestions().isEmpty()) {
            return List.of();
        }

        List<ValidatedQuestionDTO> questions = new ArrayList<>();
        int idx = 0;
        for (ManualQuestionDTO mq : dto.getManualQuestions()) {
            List<OptionDTO> opts = new ArrayList<>();
            if (mq.getOptions() != null) {
                for (ManualQuestionDTO.AnswerOptionInput ai : mq.getOptions()) {
                    opts.add(OptionDTO.builder()
                            .title(ai.getTitle())
                            .correct(ai.getCorrect())
                            .matchTarget(ai.getMatchTarget())
                            .build());
                }
            }
            questions.add(toValidatedQuestion(
                    mq.getContent(), mq.getQuestionType(),
                    null, null, mq.getExplanation(),
                    opts, null, null, null, mq.getMatchingPairs()));
            idx++;
        }
        return questions;
    }

    // ─── Step 3 ────────────────────────────────────────────
    @Override
    public WizardValidationResultDTO validate(HttpSession session) {
        WizardStep1DTO step1 = getStep1(session);
        if (step1 == null) {
            throw new InvalidDataException("Step 1 data not found. Please restart the wizard.");
        }
        List<ValidatedQuestionDTO> questions = getQuestions(session);
        WizardValidationResultDTO result = validationService.validate(step1, questions);
        session.setAttribute(SESSION_VALIDATION, result);
        return result;
    }

    @Override
    public WizardValidationResultDTO getValidationResult(HttpSession session) {
        return (WizardValidationResultDTO) session.getAttribute(SESSION_VALIDATION);
    }

    // ─── Step 4 ────────────────────────────────────────────
    @Override
    @Transactional
    public Integer saveWizard(HttpSession session, WizardSaveDTO dto, String email) {
        WizardStep1DTO step1 = getStep1(session);
        if (step1 == null) {
            throw new InvalidDataException("Step 1 data not found. Please restart the wizard.");
        }

        // Re-validate before saving (block on errors)
        WizardValidationResultDTO validation = validate(session);
        if (!validation.isClean()) {
            throw new InvalidDataException("Cannot save: there are validation errors. Please fix them before saving.");
        }

        List<ValidatedQuestionDTO> questions = getQuestions(session);
        String skill = step1.getSkill();
        String cefr = step1.getCefrLevel();

        // Build QuestionGroupRequestDTO
        QuestionGroupRequestDTO groupRequest = QuestionGroupRequestDTO.builder()
                .groupContent("TOPIC_BASED".equals(step1.getMode()) ? null : step1.getPassageContent())
                .audioUrl(step1.getAudioUrl())
                .imageUrl(step1.getImageUrl())
                .skill(skill)
                .cefrLevel(cefr)
                .topic(step1.getTopic())
                .explanation(null)
                .status(dto.getStatus())
                .questions(questions.stream()
                        .map(q -> validatedToQuestionRequest(q, skill, cefr))
                        .collect(Collectors.toList()))
                .build();

        QuestionGroupResponseDTO saved = expertQuestionService.createQuestionGroup(groupRequest, email);

        // Clear session
        clearSession(session);

        return saved.getGroupId();
    }

    private QuestionRequestDTO validatedToQuestionRequest(
            ValidatedQuestionDTO q, String skill, String cefr) {
        List<QuestionRequestDTO.AnswerOptionDTO> opts = new ArrayList<>();
        if (q.getOptions() != null) {
            for (OptionDTO o : q.getOptions()) {
                opts.add(QuestionRequestDTO.AnswerOptionDTO.builder()
                        .title(o.getTitle())
                        .correct(o.getCorrect())
                        .matchTarget(o.getMatchTarget())
                        .build());
            }
        }
        return QuestionRequestDTO.builder()
                .content(q.getContent())
                .questionType(q.getQuestionType())
                .skill(skill)
                .cefrLevel(cefr)
                .topic(null)
                .explanation(q.getExplanation())
                .status("PUBLISHED")
                .options(opts)
                .build();
    }

    @Override
    public List<ValidatedQuestionDTO> getQuestions(HttpSession session) {
        Object obj = session.getAttribute(SESSION_QUESTIONS);
        if (obj instanceof List) {
            @SuppressWarnings("unchecked")
            List<ValidatedQuestionDTO> list = (List<ValidatedQuestionDTO>) obj;
            return list;
        }
        return new ArrayList<>();
    }

    @Override
    public void abandon(HttpSession session) {
        clearSession(session);
    }

    private void clearSession(HttpSession session) {
        session.removeAttribute(SESSION_STEP1);
        session.removeAttribute(SESSION_QUESTIONS);
        session.removeAttribute(SESSION_VALIDATION);
    }

    // ─── Helpers ───────────────────────────────────────────
    private String getStep1Skill() {
        // Accessed via ThreadLocal or passed from processStep2
        return lastSkill;
    }

    // Simple ThreadLocal for skill during a request
    private static final ThreadLocal<String> lastSkill = new ThreadLocal<>();
    private static final ThreadLocal<String> lastCefr = new ThreadLocal<>();

    private void setLastSkillAndCefr(String skill, String cefr) {
        lastSkill.set(skill);
        lastCefr.set(cefr);
    }

    private ValidatedQuestionDTO toValidatedQuestion(
            String content, String qt, String skill, String cefr, String explanation,
            List<AIGenerateResponseDTO.QuestionDTO.AIOptionDTO> aiOpts,
            List<String> matchLeft, List<String> matchRight, List<Integer> correctPairs,
            List<String> matchingPairs) {

        List<OptionDTO> opts = new ArrayList<>();
        if (aiOpts != null) {
            for (var ao : aiOpts) {
                opts.add(OptionDTO.builder()
                        .title(ao.getTitle())
                        .correct(ao.getCorrect())
                        .matchTarget(null)
                        .build());
            }
        }
        if (matchLeft != null && matchRight != null && correctPairs != null) {
            for (int i = 0; i < matchLeft.size(); i++) {
                int rightIdx = correctPairs.get(i) - 1;
                opts.add(OptionDTO.builder()
                        .title(matchLeft.get(i))
                        .correct(false)
                        .matchTarget(matchRight.get(rightIdx))
                        .build());
                opts.add(OptionDTO.builder()
                        .title(matchRight.get(rightIdx))
                        .correct(false)
                        .matchTarget(null)
                        .build());
            }
        }

        return ValidatedQuestionDTO.builder()
                .content(content)
                .questionType(qt)
                .skill(skill)
                .cefrLevel(cefr)
                .explanation(explanation)
                .options(opts)
                .matchingPairs(matchingPairs)
                .errors(List.of())
                .warnings(List.of())
                .isValid(true)
                .build();
    }

    private ValidatedQuestionDTO excelRowToValidatedQuestion(
            ExcelParseResultDTO.ValidRowDTO row, int idx) {
        List<OptionDTO> opts = new ArrayList<>();
        if (row.getOptions() != null) {
            for (var o : row.getOptions()) {
                opts.add(OptionDTO.builder()
                        .title(o.getTitle())
                        .correct(o.getCorrect())
                        .matchTarget(o.getMatchTarget())
                        .build());
            }
        }
        return ValidatedQuestionDTO.builder()
                .content(row.getContent())
                .questionType(row.getQuestionType())
                .skill(row.getSkill())
                .cefrLevel(row.getCefrLevel())
                .explanation(row.getExplanation())
                .options(opts)
                .matchingPairs(row.getMatchingPairs())
                .errors(List.of())
                .warnings(List.of())
                .isValid(true)
                .build();
    }

    // Import DTOs needed
    import com.example.DoAn.dto.request.AIGenerateRequestDTO;
    import com.example.DoAn.dto.response.AIGenerateResponseDTO;
    import com.example.DoAn.dto.response.ExcelParseResultDTO;
    import com.example.DoAn.model.QuestionGroup;
    import org.springframework.transaction.annotation.Transactional;
}
```

**Note on ThreadLocal:** The `lastSkill`/`lastCefr` ThreadLocal above is a simplified approach. A cleaner implementation passes `WizardStep1DTO` directly into `processAiGenerate` as a parameter. Update `processStep2` to call `setLastSkillAndCefr(step1.getSkill(), step1.getCefrLevel())` before calling `processAiGenerate`, and update `processAiGenerate` to accept `WizardStep1DTO` instead of using ThreadLocal.

---

## Chunk 4: REST API Endpoints

**Files to create:**
- `controller/api/ExpertQuestionGroupWizardApiController.java` — all wizard API endpoints

**Files to reference (read-only):**
- `controller/api/ExpertQuestionController.java` — for endpoint pattern
- `dto/response/ResponseData.java` — for response wrapper
- `service/IQuestionGroupWizardService.java`

### 4.1 ExpertQuestionGroupWizardApiController.java

```java
package com.example.DoAn.controller.api;

import com.example.DoAn.dto.request.*;
import com.example.DoAn.dto.response.*;
import com.example.DoAn.service.IQuestionGroupWizardService;
import com.example.DoAn.util.JwtUtil;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/expert/questions/wizard")
@RequiredArgsConstructor
public class ExpertQuestionGroupWizardApiController {

    private final IQuestionGroupWizardService wizardService;

    // POST /api/v1/expert/questions/wizard/step1
    @PostMapping("/step1")
    public ResponseEntity<ResponseData<Void>> submitStep1(
            @Valid @RequestBody WizardStep1DTO dto, HttpSession session) {
        wizardService.saveStep1(session, dto);
        return ResponseEntity.ok(ResponseData.ok(null, "Step 1 saved"));
    }

    // POST /api/v1/expert/questions/wizard/step2
    @PostMapping("/step2")
    public ResponseEntity<ResponseData<WizardStep2ResultDTO>> submitStep2(
            @Valid @ModelAttribute WizardStep2DTO dto, HttpSession session,
            Authentication auth) {
        String email = auth.getName();
        WizardStep2ResultDTO result = wizardService.processStep2(session, dto, email);
        return ResponseEntity.ok(ResponseData.ok(result, "Step 2 processed"));
    }

    // POST /api/v1/expert/questions/wizard/validate
    @PostMapping("/validate")
    public ResponseEntity<ResponseData<WizardValidationResultDTO>> validate(HttpSession session) {
        WizardValidationResultDTO result = wizardService.validate(session);
        return ResponseEntity.ok(ResponseData.ok(result, "Validation complete"));
    }

    // POST /api/v1/expert/questions/wizard/save
    @PostMapping("/save")
    public ResponseEntity<ResponseData<Integer>> save(
            @Valid @RequestBody WizardSaveDTO dto, HttpSession session,
            Authentication auth) {
        String email = auth.getName();
        Integer groupId = wizardService.saveWizard(session, dto, email);
        return ResponseEntity.ok(ResponseData.ok(groupId, "Question group saved"));
    }

    // GET /api/v1/expert/questions/wizard/step-data
    @GetMapping("/step-data")
    public ResponseEntity<ResponseData<WizardStepDataDTO>> getStepData(HttpSession session) {
        WizardStep1DTO step1 = wizardService.getStep1(session);
        List<ValidatedQuestionDTO> questions = wizardService.getQuestions(session);
        WizardValidationResultDTO validation = wizardService.getValidationResult(session);
        WizardStepDataDTO data = WizardStepDataDTO.builder()
                .step1(step1)
                .questions(questions)
                .validationResult(validation)
                .build();
        return ResponseEntity.ok(ResponseData.ok(data, "Step data retrieved"));
    }

    // GET /api/v1/expert/questions/wizard/abandon
    @GetMapping("/abandon")
    public ResponseEntity<ResponseEntity<?>> abandon(HttpSession session) {
        wizardService.abandon(session);
        return ResponseEntity.ok(ResponseData.ok(null, "Wizard abandoned"));
    }

    // Helper DTO for step-data response
    @lombok.Data @lombok.NoArgsConstructor @lombok.AllArgsConstructor @lombok.Builder
    public static class WizardStepDataDTO {
        private WizardStep1DTO step1;
        private List<ValidatedQuestionDTO> questions;
        private WizardValidationResultDTO validationResult;
    }
}
```

**Important:** `@ModelAttribute` is needed for `WizardStep2DTO` because it contains `MultipartFile excelFile`. Move to a separate `@RequestPart` approach if needed for file upload alongside other fields, or use a dedicated multipart wrapper DTO.

---

## Chunk 5: View Controller + Thymeleaf Page

**Files to create:**
- `controller/ExpertQuestionGroupWizardController.java` — page controller
- `templates/expert/question-group-wizard.html` — wizard page
- `static/assets/expert/js/question-wizard.js` — client-side JS

**Files to reference (read-only):**
- `templates/expert/layout.html` (if exists)
- `templates/expert/dashboard.html` — for fragment/header patterns
- `templates/expert/question-bank.html` — for nav links

### 5.1 ExpertQuestionGroupWizardController.java

```java
package com.example.DoAn.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/expert/questions")
@RequiredArgsConstructor
public class ExpertQuestionGroupWizardController {

    private static final String SESSION_STEP1 = "wizard_step1";
    private static final String SESSION_QUESTIONS = "wizard_step2_questions";

    @GetMapping("/wizard")
    public String wizardPage(HttpSession session, Model model) {
        // Clear stale session data on fresh load
        session.removeAttribute(SESSION_STEP1);
        session.removeAttribute(SESSION_QUESTIONS);
        model.addAttribute("currentStep", 1);
        return "expert/question-group-wizard";
    }
}
```

---

## Chunk 6: Security Config Update

**Files to modify:**
- `configuration/SecurityConfig.java` — add new wizard API paths (if needed beyond `/api/v1/expert/**`)

**Change:** No change needed. The existing rule `.requestMatchers("/api/v1/expert/**")` requires `ROLE_EXPERT`, and `/expert/**` page route also requires `ROLE_EXPERT`. Both are covered by the existing security config. The new endpoints fall under `/api/v1/expert/questions/wizard/**` which is already protected.

---

## Chunk 7: Integration Tests

**Files to create:**
- `src/test/java/com/example/DoAn/service/impl/WizardValidationServiceTest.java`
- `src/test/java/com/example/DoAn/service/impl/QuestionGroupWizardServiceImplTest.java`

**Files to reference (read-only):**
- `src/test/java/com/example/DoAn/` (existing test structure if any)

---

### 7.1 WizardValidationServiceTest.java

```java
package com.example.DoAn.service.impl;

import com.example.DoAn.dto.request.WizardStep1DTO;
import com.example.DoAn.dto.response.ValidatedQuestionDTO;
import com.example.DoAn.dto.response.WizardValidationResultDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class WizardValidationServiceTest {

    private WizardValidationService service;

    @BeforeEach
    void setUp() {
        service = new WizardValidationService();
    }

    @Test
    void testPassageRequiredWhenPassageBased() {
        WizardStep1DTO step1 = WizardStep1DTO.builder()
                .mode("PASSAGE_BASED")
                .passageContent(null)
                .skill("READING")
                .cefrLevel("B1")
                .topic("Environment")
                .build();

        WizardValidationResultDTO result = service.validate(step1, List.of(
                makeValidQuestion("READING", "B1", "MULTIPLE_CHOICE_SINGLE")
        ));

        assertFalse(result.isClean());
        assertTrue(result.getGroupErrors().stream()
                .anyMatch(e -> "PASSAGE_REQUIRED".equals(e.getCode())));
    }

    @Test
    void testSkillMismatchBlocksSave() {
        WizardStep1DTO step1 = WizardStep1DTO.builder()
                .mode("TOPIC_BASED")
                .skill("READING")
                .cefrLevel("B1")
                .topic("Environment")
                .build();

        // Question with WRITING skill while group is READING
        ValidatedQuestionDTO q = ValidatedQuestionDTO.builder()
                .content("Write an essay about climate change.")
                .questionType("WRITING")
                .skill("WRITING")  // mismatch
                .cefrLevel("B1")
                .options(List.of())
                .build();

        WizardValidationResultDTO result = service.validate(step1, List.of(q));

        assertFalse(result.isClean());
        assertTrue(result.getQuestions().get(0).getErrors().stream()
                .anyMatch(e -> "SKILL_MISMATCH".equals(e.getCode())));
    }

    @Test
    void testCeFrMismatchBlocksSave() {
        WizardStep1DTO step1 = WizardStep1DTO.builder()
                .mode("TOPIC_BASED")
                .skill("READING")
                .cefrLevel("B1")
                .topic("Technology")
                .build();

        ValidatedQuestionDTO q = makeValidQuestion("READING", "C1", "MULTIPLE_CHOICE_SINGLE");

        WizardValidationResultDTO result = service.validate(step1, List.of(q));

        assertFalse(result.isClean());
        assertTrue(result.getQuestions().get(0).getErrors().stream()
                .anyMatch(e -> "CEFR_MISMATCH".equals(e.getCode())));
    }

    @Test
    void testInvalidTypeForSkillBlocksSave() {
        WizardStep1DTO step1 = WizardStep1DTO.builder()
                .mode("TOPIC_BASED")
                .skill("READING")
                .cefrLevel("B1")
                .topic("Travel")
                .build();

        // WRITING type not allowed for READING skill
        ValidatedQuestionDTO q = makeValidQuestion("READING", "B1", "WRITING");

        WizardValidationResultDTO result = service.validate(step1, List.of(q));

        assertFalse(result.isClean());
        assertTrue(result.getQuestions().get(0).getErrors().stream()
                .anyMatch(e -> "INVALID_TYPE_FOR_SKILL".equals(e.getCode())));
    }

    @Test
    void testNoCorrectAnswerBlocksSave() {
        WizardStep1DTO step1 = WizardStep1DTO.builder()
                .mode("TOPIC_BASED")
                .skill("READING")
                .cefrLevel("B1")
                .topic("Health")
                .build();

        ValidatedQuestionDTO q = ValidatedQuestionDTO.builder()
                .content("What is the capital of France?")
                .questionType("MULTIPLE_CHOICE_SINGLE")
                .skill("READING")
                .cefrLevel("B1")
                .options(List.of(
                        option("Paris", false),
                        option("London", false),
                        option("Berlin", false)
                        // No correct answer
                ))
                .build();

        WizardValidationResultDTO result = service.validate(step1, List.of(q));

        assertFalse(result.isClean());
        assertTrue(result.getQuestions().get(0).getErrors().stream()
                .anyMatch(e -> "NO_CORRECT_ANSWER".equals(e.getCode())));
    }

    @Test
    void testMatchingCountMismatchBlocksSave() {
        WizardStep1DTO step1 = WizardStep1DTO.builder()
                .mode("TOPIC_BASED")
                .skill("READING")
                .cefrLevel("B1")
                .topic("Science")
                .build();

        ValidatedQuestionDTO q = ValidatedQuestionDTO.builder()
                .content("Match the words with their definitions.")
                .questionType("MATCHING")
                .skill("READING")
                .cefrLevel("B1")
                .options(List.of(
                        option("Abundant", false, "plentiful"),  // left
                        option("plentiful", false, null),       // right
                        option("Rare", false, "scarce"),         // left
                        // missing right item for "Rare" — only 1 right but 2 lefts
                        option("scarce", false, null)           // right (only 2 rights needed for 2 lefts, this is 1)
                ))
                .build();
        // Correct: 2 lefts (Abundant→plentiful, Rare→scarce), 2 rights (plentiful, scarce)
        // This test sets up mismatched counts

        WizardValidationResultDTO result = service.validate(step1, List.of(q));

        assertFalse(result.isClean());
    }

    @Test
    void testValidQuestionPasses() {
        WizardStep1DTO step1 = WizardStep1DTO.builder()
                .mode("TOPIC_BASED")
                .skill("READING")
                .cefrLevel("B1")
                .topic("Environment")
                .build();

        ValidatedQuestionDTO q = makeValidQuestion("READING", "B1", "MULTIPLE_CHOICE_SINGLE");

        WizardValidationResultDTO result = service.validate(step1, List.of(q));

        assertTrue(result.isClean());
        assertEquals(0, result.getErrorCount());
        assertEquals(1, result.getTotalQuestions());
    }

    @Test
    void testNoQuestionsBlocksSave() {
        WizardStep1DTO step1 = WizardStep1DTO.builder()
                .mode("TOPIC_BASED")
                .skill("READING")
                .cefrLevel("B1")
                .topic("Environment")
                .build();

        WizardValidationResultDTO result = service.validate(step1, List.of());

        assertFalse(result.isClean());
        assertTrue(result.getGroupErrors().stream()
                .anyMatch(e -> "NO_QUESTIONS".equals(e.getCode())));
    }

    @Test
    void testTooFewOptionsBlocksSave() {
        WizardStep1DTO step1 = WizardStep1DTO.builder()
                .mode("TOPIC_BASED")
                .skill("READING")
                .cefrLevel("B1")
                .topic("History")
                .build();

        ValidatedQuestionDTO q = ValidatedQuestionDTO.builder()
                .content("Which one is correct?")
                .questionType("MULTIPLE_CHOICE_SINGLE")
                .skill("READING")
                .cefrLevel("B1")
                .options(List.of(option("Only one option", true)))
                .build();

        WizardValidationResultDTO result = service.validate(step1, List.of(q));

        assertFalse(result.isClean());
        assertTrue(result.getQuestions().get(0).getErrors().stream()
                .anyMatch(e -> "TOO_FEW_OPTIONS".equals(e.getCode())));
    }

    // ─── Helper methods ───────────────────────────────────
    private ValidatedQuestionDTO makeValidQuestion(String skill, String cefr, String qt) {
        return ValidatedQuestionDTO.builder()
                .content("This is a valid question with enough content for testing purposes.")
                .questionType(qt)
                .skill(skill)
                .cefrLevel(cefr)
                .explanation("This is the explanation.")
                .tags(List.of("test", "sample"))
                .options(List.of(
                        ValidatedQuestionDTO.OptionDTO.builder().title("Option A").correct(true).build(),
                        ValidatedQuestionDTO.OptionDTO.builder().title("Option B").correct(false).build(),
                        ValidatedQuestionDTO.OptionDTO.builder().title("Option C").correct(false).build()
                ))
                .errors(List.of())
                .warnings(List.of())
                .isValid(true)
                .build();
    }

    private ValidatedQuestionDTO.OptionDTO option(String title, Boolean correct) {
        return option(title, correct, null);
    }

    private ValidatedQuestionDTO.OptionDTO option(String title, Boolean correct, String matchTarget) {
        return ValidatedQuestionDTO.OptionDTO.builder()
                .title(title).correct(correct).matchTarget(matchTarget).build();
    }
}
```

---

## Chunk 8: GlobalExceptionHandler Update

**Files to modify:**
- `exception/GlobalExceptionHandler.java` — add handler for wizard-specific exceptions

**Add after existing handlers:**

```java
@ExceptionHandler(InvalidDataException.class)
public ResponseEntity<ResponseData<Void>> handleInvalidData(InvalidDataException ex) {
    return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ResponseData.error(ex.getMessage()));
}
```

---

## File Summary

### Create (new files)

| # | File |
|---|---|
| 1 | `dto/request/WizardStep1DTO.java` |
| 2 | `dto/request/ManualQuestionDTO.java` |
| 3 | `dto/request/WizardStep2DTO.java` |
| 4 | `dto/request/WizardSaveDTO.java` |
| 5 | `dto/response/ValidationErrorDTO.java` |
| 6 | `dto/response/ValidatedQuestionDTO.java` |
| 7 | `dto/response/WizardValidationResultDTO.java` |
| 8 | `dto/response/WizardStep2ResultDTO.java` |
| 9 | `service/IQuestionGroupWizardService.java` |
| 10 | `service/impl/WizardValidationService.java` |
| 11 | `service/impl/QuestionGroupWizardServiceImpl.java` |
| 12 | `controller/ExpertQuestionGroupWizardController.java` |
| 13 | `controller/api/ExpertQuestionGroupWizardApiController.java` |
| 14 | `templates/expert/question-group-wizard.html` |
| 15 | `static/assets/expert/js/question-wizard.js` |
| 16 | `src/test/java/com/example/DoAn/service/impl/WizardValidationServiceTest.java` |

### Modify (existing files)

| # | File | Change |
|---|---|---|
| 1 | `exception/GlobalExceptionHandler.java` | Add `InvalidDataException` handler |
| 2 | `configuration/SecurityConfig.java` | No change needed (already covers `/api/v1/expert/**`) |

---

## Task Order (for subagent execution)

```
Chunk 1 → Chunk 2 → Chunk 3 → Chunk 4 → Chunk 5 → Chunk 6 → Chunk 7 → Chunk 8
```

Recommended subagent dispatch: Chunks 1–4 (backend), then Chunks 5–8 (frontend + tests + integration).
