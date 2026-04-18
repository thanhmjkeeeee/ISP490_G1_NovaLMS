package com.example.DoAn.service.impl;

import com.example.DoAn.dto.request.*;
import com.example.DoAn.dto.response.*;
import com.example.DoAn.dto.response.ValidatedQuestionDTO.OptionDTO;
import com.example.DoAn.exception.InvalidDataException;
import com.example.DoAn.service.*;
import com.example.DoAn.util.ExcelTemplateGenerator;
import com.example.DoAn.util.RateLimitWindowStore;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    private final RateLimitWindowStore rateLimitStore;

    private static final String KEY_STEP1 = "wizard_step1";
    private static final String KEY_QUESTIONS = "wizard_step2_questions";
    private static final String KEY_VALIDATION = "wizard_validation_result";

    // ── Step 1 ────────────────────────────────────────────────────────────────

    @Override
    public void saveStep1(HttpSession session, WizardStep1DTO dto) {
        session.setAttribute(KEY_STEP1, dto);
        session.removeAttribute(KEY_VALIDATION);
        log.debug("[Wizard] Step1 saved, session={}", session.getId());
    }

    @Override
    public WizardStep1DTO getStep1(HttpSession session) {
        return (WizardStep1DTO) session.getAttribute(KEY_STEP1);
    }

    // ── Step 2 ────────────────────────────────────────────────────────────────

    @Override
    public WizardStep2ResultDTO processStep2(HttpSession session, WizardStep2DTO dto, String email) {
        WizardStep1DTO step1 = getStep1(session);
        if (step1 == null) {
            throw new InvalidDataException("Chưa hoàn thành Step 1. Vui lòng bắt đầu lại.");
        }

        List<ValidatedQuestionDTO> newQuestions;
        List<ValidationErrorDTO> errors = new ArrayList<>();

        String sourceType = dto.getSourceType();
        switch (sourceType != null ? sourceType : "MANUAL") {
            case "AI_GENERATE" -> {
                List<ValidationErrorDTO> aiErrors = new ArrayList<>();
                newQuestions = processAiGenerate(step1, dto, email, aiErrors);
                errors.addAll(aiErrors);
            }
            case "EXCEL_IMPORT" -> {
                List<ValidationErrorDTO> excelErrors = new ArrayList<>();
                newQuestions = processExcelImport(dto, excelErrors);
                errors.addAll(excelErrors);
            }
            case "MANUAL" -> newQuestions = processManual(dto);
            default -> throw new InvalidDataException("Nguồn câu hỏi không hợp lệ: " + sourceType);
        }

        // Merge with existing session questions (don't overwrite)
        @SuppressWarnings("unchecked")
        List<ValidatedQuestionDTO> existing = (List<ValidatedQuestionDTO>)
                Optional.ofNullable(session.getAttribute(KEY_QUESTIONS)).orElse(new ArrayList<>());
        List<ValidatedQuestionDTO> merged = new ArrayList<>(existing);
        merged.addAll(newQuestions);
        session.setAttribute(KEY_QUESTIONS, merged);
        session.removeAttribute(KEY_VALIDATION);

        log.info("[Wizard] Step2 processed: source={}, new={}, total={}",
                sourceType, newQuestions.size(), merged.size());

        return WizardStep2ResultDTO.builder()
                .questions(merged)
                .errors(errors)
                .totalQuestions(merged.size())
                .errorCount(errors.size())
                .sourceType(sourceType)
                .build();
    }

    private List<ValidatedQuestionDTO> processAiGenerate(
            WizardStep1DTO step1, WizardStep2DTO dto, String email, List<ValidationErrorDTO> errors) {

        if (!rateLimitStore.isAllowed(email)) {
            errors.add(ValidationErrorDTO.builder()
                    .questionIndex(-1).field("ai").code("RATE_LIMITED")
                    .message("Bạn đã vượt giới hạn 10 yêu cầu AI/phút. Vui lòng chờ một lát.")
                    .severity("ERROR").build());
            return List.of();
        }

        if (dto.getAiTopic() == null || dto.getAiTopic().isBlank()) {
            errors.add(ValidationErrorDTO.builder()
                    .questionIndex(-1).field("aiTopic").code("TOPIC_REQUIRED")
                    .message("AI topic is required for generation.").severity("ERROR").build());
            return List.of();
        }

        try {
            AIGenerateRequestDTO request = AIGenerateRequestDTO.builder()
                    .topic(dto.getAiTopic())
                    .quantity(dto.getAiQuantity() != null ? dto.getAiQuantity() : 5)
                    .skill(step1.getSkill())
                    .cefrLevel(step1.getCefrLevel())
                    .questionTypes(dto.getAiQuestionTypes())
                    .mode(dto.getAiMode() != null ? dto.getAiMode() : "NORMAL")
                    .build();

            AIGenerateResponseDTO response = aiQuestionService.generate(request, email);

            if (response.getWarning() != null && !response.getWarning().isBlank()) {
                log.warn("[Wizard] AI warning: {}", response.getWarning());
            }

            List<ValidatedQuestionDTO> result = new ArrayList<>();
            if (response.getQuestions() != null) {
                for (AIGenerateResponseDTO.QuestionDTO q : response.getQuestions()) {
                    result.add(toValidatedQuestion(q));
                }
            }
            return result;

        } catch (Exception e) {
            log.error("[Wizard] AI generation failed", e);
            errors.add(ValidationErrorDTO.builder()
                    .questionIndex(-1).field("ai").code("AI_GENERATION_FAILED")
                    .message("AI generation failed: " + e.getMessage()).severity("ERROR").build());
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
            String questionType = dto.getExcelQuestionType();
            ExcelTemplateGenerator.requireFilenameMatchesQuestionType(questionType, fn);
            ExcelParseResultDTO parseResult =
                    excelQuestionImportService.parseFile(file, questionType);

            List<ValidatedQuestionDTO> result = new ArrayList<>();
            int idx = 0;
            if (parseResult.getValid() != null) {
                for (ExcelParseResultDTO.ValidRowDTO row : parseResult.getValid()) {
                    result.add(toValidatedQuestion(row, idx++));
                }
            }

            if (parseResult.getErrors() != null) {
                for (ExcelParseResultDTO.ErrorRowDTO err : parseResult.getErrors()) {
                    errors.add(ValidationErrorDTO.builder()
                            .questionIndex(err.getRowIndex())
                            .field("excelRow").code("EXCEL_ROW_ERROR")
                            .message(err.getMessage()).severity("ERROR").build());
                }
            }
            return result;

        } catch (Exception e) {
            log.error("[Wizard] Excel parse failed", e);
            errors.add(ValidationErrorDTO.builder()
                    .questionIndex(-1).field("excelFile").code("EXCEL_PARSE_FAILED")
                    .message("Failed to parse Excel file: " + e.getMessage()).severity("ERROR").build());
            return List.of();
        }
    }

    private List<ValidatedQuestionDTO> processManual(WizardStep2DTO dto) {
        List<ManualQuestionDTO> manual = dto.getManualQuestions();
        if (manual == null || manual.isEmpty()) {
            return List.of();
        }

        List<ValidatedQuestionDTO> result = new ArrayList<>();
        for (ManualQuestionDTO m : manual) {
            List<OptionDTO> opts = new ArrayList<>();
            if (m.getOptions() != null) {
                for (ManualQuestionDTO.AnswerOptionInput ai : m.getOptions()) {
                    opts.add(OptionDTO.builder()
                            .title(ai.getTitle())
                            .correct(ai.getCorrect())
                            .matchTarget(ai.getMatchTarget())
                            .build());
                }
            }

            // Build matching pairs from left/right indexing if provided
            List<String> matchingPairs = null;
            if ("MATCHING".equals(m.getQuestionType()) && m.getMatchingPairs() != null) {
                matchingPairs = m.getMatchingPairs();
            }

            result.add(ValidatedQuestionDTO.builder()
                    .content(m.getContent())
                    .questionType(m.getQuestionType())
                    .explanation(m.getExplanation())
                    .options(opts)
                    .matchingPairs(matchingPairs)
                    .errors(List.of())
                    .warnings(List.of())
                    .isValid(true)
                    .build());
        }
        return result;
    }

    // ── Step 3 ────────────────────────────────────────────────────────────────

    @Override
    public WizardValidationResultDTO validate(HttpSession session) {
        WizardStep1DTO step1 = getStep1(session);
        if (step1 == null) {
            throw new InvalidDataException("Chưa hoàn thành Step 1. Vui lòng bắt đầu lại.");
        }
        List<ValidatedQuestionDTO> questions = getQuestions(session);
        WizardValidationResultDTO result = validationService.validate(step1, questions);
        session.setAttribute(KEY_VALIDATION, result);
        return result;
    }

    @Override
    public WizardValidationResultDTO getValidationResult(HttpSession session) {
        return (WizardValidationResultDTO) session.getAttribute(KEY_VALIDATION);
    }

    // ── Step 4 ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Integer saveWizard(HttpSession session, WizardSaveDTO dto, String email) {
        WizardStep1DTO step1 = getStep1(session);
        if (step1 == null) {
            throw new InvalidDataException("Chưa hoàn thành Step 1. Vui lòng bắt đầu lại.");
        }

        // Re-validate before saving (block on errors)
        WizardValidationResultDTO validation = validate(session);
        if (!validation.isClean()) {
            List<String> errorMsgs = validation.getGroupErrors().stream()
                    .map(ValidationErrorDTO::getMessage)
                    .collect(Collectors.toList());
            validation.getQuestions().stream()
                    .flatMap(q -> q.getErrors().stream())
                    .forEach(e -> errorMsgs.add("Q" + e.getCode() + ": " + e.getMessage()));
            throw new InvalidDataException("Dữ liệu không hợp lệ: " + String.join("; ", errorMsgs));
        }

        List<ValidatedQuestionDTO> questions = getQuestions(session);
        String skill = step1.getSkill();
        String cefr = step1.getCefrLevel();

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

        QuestionGroupResponseDTO saved =
                expertQuestionService.createQuestionGroup(groupRequest, email);

        abandon(session);
        log.info("[Wizard] QuestionGroup saved: groupId={}, questions={}",
                saved.getGroupId(), questions.size());

        return saved.getGroupId();
    }

    private QuestionRequestDTO validatedToQuestionRequest(
            ValidatedQuestionDTO q, String skill, String cefr) {
        List<QuestionRequestDTO.AnswerOptionDTO> opts = null;
        if (q.getOptions() != null) {
            opts = q.getOptions().stream()
                    .map(o -> QuestionRequestDTO.AnswerOptionDTO.builder()
                            .title(o.getTitle())
                            .correct(o.getCorrect())
                            .matchTarget(o.getMatchTarget())
                            .build())
                    .collect(Collectors.toList());
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

    // ── Session helpers ──────────────────────────────────────────────────────

    @Override
    public List<ValidatedQuestionDTO> getQuestions(HttpSession session) {
        @SuppressWarnings("unchecked")
        List<ValidatedQuestionDTO> list = (List<ValidatedQuestionDTO>)
                session.getAttribute(KEY_QUESTIONS);
        return list != null ? list : List.of();
    }

    @Override
    public void abandon(HttpSession session) {
        session.removeAttribute(KEY_STEP1);
        session.removeAttribute(KEY_QUESTIONS);
        session.removeAttribute(KEY_VALIDATION);
    }

    // ── Mapping helpers ──────────────────────────────────────────────────────

    private ValidatedQuestionDTO toValidatedQuestion(AIGenerateResponseDTO.QuestionDTO q) {
        List<OptionDTO> opts = new ArrayList<>();
        List<String> matchingPairs = null;

        if ("MATCHING".equals(q.getQuestionType())
                && q.getMatchLeft() != null && q.getMatchRight() != null && q.getCorrectPairs() != null) {
            // Build left (with matchTarget) + right options, and matchingPairs list
            matchingPairs = new ArrayList<>();
            for (int i = 0; i < q.getMatchLeft().size(); i++) {
                int rightIdx = q.getCorrectPairs().get(i) - 1;
                String rightTitle = q.getMatchRight().get(rightIdx);
                opts.add(OptionDTO.builder()
                        .title(q.getMatchLeft().get(i)).correct(false)
                        .matchTarget(rightTitle).build());
                opts.add(OptionDTO.builder()
                        .title(rightTitle).correct(false)
                        .matchTarget(null).build());
                matchingPairs.add(q.getMatchLeft().get(i) + ":" + rightTitle);
            }
        } else if (q.getOptions() != null) {
            for (AIGenerateResponseDTO.OptionDTO ao : q.getOptions()) {
                opts.add(OptionDTO.builder()
                        .title(ao.getTitle())
                        .correct(ao.getCorrect())
                        .matchTarget(null)
                        .build());
            }
        }

        return ValidatedQuestionDTO.builder()
                .content(q.getContent())
                .questionType(q.getQuestionType())
                .skill(q.getSkill())
                .cefrLevel(q.getCefrLevel())
                .explanation(q.getExplanation())
                .options(opts)
                .matchingPairs(matchingPairs)
                .errors(List.of())
                .warnings(List.of())
                .isValid(true)
                .build();
    }

    private ValidatedQuestionDTO toValidatedQuestion(ExcelParseResultDTO.ValidRowDTO row, int idx) {
        List<OptionDTO> opts = new ArrayList<>();
        List<String> matchingPairs = null;

        if ("MATCHING".equals(row.getQuestionType())
                && row.getMatchLeft() != null && row.getMatchRight() != null && row.getCorrectPairs() != null) {
            matchingPairs = new ArrayList<>();
            for (int i = 0; i < row.getMatchLeft().size(); i++) {
                int rightIdx = row.getCorrectPairs().get(i) - 1;
                String rightTitle = row.getMatchRight().get(rightIdx);
                opts.add(OptionDTO.builder()
                        .title(row.getMatchLeft().get(i)).correct(false)
                        .matchTarget(rightTitle).build());
                opts.add(OptionDTO.builder()
                        .title(rightTitle).correct(false)
                        .matchTarget(null).build());
                matchingPairs.add(row.getMatchLeft().get(i) + ":" + rightTitle);
            }
        } else if (row.getOptions() != null) {
            for (ExcelParseResultDTO.OptionDTO o : row.getOptions()) {
                opts.add(OptionDTO.builder()
                        .title(o.getTitle())
                        .correct(o.getCorrect())
                        .matchTarget(null)
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
                .matchingPairs(matchingPairs)
                .errors(List.of())
                .warnings(List.of())
                .isValid(true)
                .build();
    }
}
