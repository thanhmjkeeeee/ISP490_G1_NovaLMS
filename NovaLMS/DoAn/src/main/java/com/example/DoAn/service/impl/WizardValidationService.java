package com.example.DoAn.service.impl;

import com.example.DoAn.dto.request.WizardStep1DTO;
import com.example.DoAn.dto.response.*;
import com.example.DoAn.dto.response.ValidatedQuestionDTO.OptionDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

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
        if (questions != null) {
            for (ValidatedQuestionDTO q : questions) {
                validatedQuestions.add(validateQuestion(q, step1, idx));
                idx++;
            }
        }

        int totalErrors = groupErrors.size()
                + validatedQuestions.stream().mapToInt(q -> q.getErrors().size()).sum();

        int totalWarnings = groupWarnings.size()
                + validatedQuestions.stream().mapToInt(q -> q.getWarnings().size()).sum();

        return WizardValidationResultDTO.builder()
                .questions(validatedQuestions)
                .groupErrors(groupErrors)
                .groupWarnings(groupWarnings)
                .isClean(totalErrors == 0 && totalWarnings == 0)
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
        if (q.getSkill() != null && !step1.getSkill().equalsIgnoreCase(q.getSkill())) {
            errors.add(error(idx, "skill", "SKILL_MISMATCH",
                    "Question skill '" + q.getSkill() + "' does not match group skill '" + step1.getSkill() + "'"));
        }

        // CEFR mismatch
        if (q.getCefrLevel() != null && !step1.getCefrLevel().equalsIgnoreCase(q.getCefrLevel())) {
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
        if (qt != null && !"WRITING".equals(qt) && !"SPEAKING".equals(qt)) {
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
                .tags(q.getTags())
                .errors(errors)
                .warnings(warnings)
                .isValid(errors.isEmpty())
                .build();
    }

    private void validateOptions(ValidatedQuestionDTO q, List<ValidationErrorDTO> errors,
                                  List<ValidationErrorDTO> warnings, int idx) {
        List<OptionDTO> opts = q.getOptions();

        if ("MATCHING".equals(q.getQuestionType())) {
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
                            "Left item exceeds 300 characters."));
                }
            }
            for (OptionDTO o : rights) {
                if (o.getTitle() == null || o.getTitle().length() > 300) {
                    errors.add(error(idx, "options", "RIGHT_ITEM_TOO_LONG",
                            "Right item exceeds 300 characters."));
                }
            }
        } else {
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

            Set<String> titles = new HashSet<>();
            for (OptionDTO o : opts) {
                String t = o.getTitle() != null ? o.getTitle().trim() : "";
                if (t.isEmpty()) {
                    errors.add(error(idx, "options", "OPTION_EMPTY",
                            "Option title cannot be empty."));
                } else if (t.length() > 500) {
                    errors.add(error(idx, "options", "OPTION_TOO_LONG",
                            "Option title exceeds 500 characters."));
                }
                if (!titles.add(t.toLowerCase())) {
                    errors.add(error(idx, "options", "OPTION_DUPLICATE",
                            "Duplicate option title: '" + t + "'"));
                }
            }
        }
    }

    private ValidationErrorDTO error(int idx, String field, String code, String message) {
        String sev = "ERROR".equals(code) ? "ERROR" : "WARNING";
        return ValidationErrorDTO.builder()
                .questionIndex(idx)
                .field(field)
                .code(code)
                .message(message)
                .severity(sev)
                .build();
    }
}
