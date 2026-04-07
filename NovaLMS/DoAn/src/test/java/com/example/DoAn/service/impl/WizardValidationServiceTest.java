package com.example.DoAn.service.impl;

import com.example.DoAn.dto.request.WizardStep1DTO;
import com.example.DoAn.dto.response.ValidatedQuestionDTO;
import com.example.DoAn.dto.response.ValidationErrorDTO;
import com.example.DoAn.dto.response.WizardValidationResultDTO;
import com.example.DoAn.dto.response.ValidatedQuestionDTO.OptionDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WizardValidationServiceTest {

    private WizardValidationService service;

    @BeforeEach
    void setUp() {
        service = new WizardValidationService();
    }

    // ─── Helper builders ───────────────────────────────────────────────────────

    private ValidatedQuestionDTO validQuestion(String skill, String cefr, String type) {
        return ValidatedQuestionDTO.builder()
                .content("This is a valid question with enough characters")
                .questionType(type)
                .skill(skill)
                .cefrLevel(cefr)
                .explanation("Explanation here")
                .options(List.of(
                        OptionDTO.builder().title("Option A").correct(true).build(),
                        OptionDTO.builder().title("Option B").correct(false).build()
                ))
                .tags(List.of("tag1"))
                .errors(List.of())
                .warnings(List.of())
                .isValid(true)
                .build();
    }

    private WizardStep1DTO validStep1(String skill, String cefr) {
        WizardStep1DTO dto = new WizardStep1DTO();
        dto.setSkill(skill);
        dto.setCefrLevel(cefr);
        dto.setMode("TOPIC_BASED");
        dto.setTopic("General topic");
        return dto;
    }

    private void assertError(List<ValidationErrorDTO> errors, String code) {
        assertTrue(errors.stream().anyMatch(e -> e.getCode().equals(code)),
                "Expected error code: " + code + " but got: " + errors);
    }

    private void assertNoError(List<ValidationErrorDTO> errors, String code) {
        assertFalse(errors.stream().anyMatch(e -> e.getCode().equals(code)),
                "Should not have error: " + code + " in " + errors);
    }

    // ─── Group validation tests ───────────────────────────────────────────────

    // TC-VAL-GRP-001: PASSAGE_REQUIRED — mode=PASSAGE_BASED, passageContent=null
    @Test
    void TC_VAL_GRP_001_passageRequired_nullPassage() {
        WizardStep1DTO step1 = new WizardStep1DTO();
        step1.setMode("PASSAGE_BASED");
        step1.setSkill("READING");
        step1.setCefrLevel("B1");
        step1.setTopic("Test");
        step1.setPassageContent(null);

        WizardValidationResultDTO result = service.validate(step1, List.of(validQuestion("READING", "B1", "MULTIPLE_CHOICE_SINGLE")));

        assertFalse(result.isClean());
        assertTrue(result.getGroupErrors().stream().anyMatch(e -> e.getCode().equals("PASSAGE_REQUIRED")));
    }

    // TC-VAL-GRP-002: PASSAGE_TOO_SHORT — passageContent="short"
    @Test
    void TC_VAL_GRP_002_passageTooShort() {
        WizardStep1DTO step1 = new WizardStep1DTO();
        step1.setMode("PASSAGE_BASED");
        step1.setSkill("READING");
        step1.setCefrLevel("B1");
        step1.setTopic("Test");
        step1.setPassageContent("short");

        WizardValidationResultDTO result = service.validate(step1, List.of(validQuestion("READING", "B1", "MULTIPLE_CHOICE_SINGLE")));

        assertFalse(result.isClean());
        assertTrue(result.getGroupErrors().stream().anyMatch(e -> e.getCode().equals("PASSAGE_TOO_SHORT")));
    }

    // TC-VAL-GRP-003: PASSAGE_TOO_LONG — passageContent="x".repeat(5001)
    @Test
    void TC_VAL_GRP_003_passageTooLong() {
        WizardStep1DTO step1 = new WizardStep1DTO();
        step1.setMode("PASSAGE_BASED");
        step1.setSkill("READING");
        step1.setCefrLevel("B1");
        step1.setTopic("Test");
        step1.setPassageContent("x".repeat(5001));

        WizardValidationResultDTO result = service.validate(step1, List.of(validQuestion("READING", "B1", "MULTIPLE_CHOICE_SINGLE")));

        assertFalse(result.isClean());
        assertTrue(result.getGroupErrors().stream().anyMatch(e -> e.getCode().equals("PASSAGE_TOO_LONG")));
    }

    // TC-VAL-GRP-004: PASSAGE_BASED with 100-char passage — no PASSAGE errors
    @Test
    void TC_VAL_GRP_004_passageValid() {
        WizardStep1DTO step1 = new WizardStep1DTO();
        step1.setMode("PASSAGE_BASED");
        step1.setSkill("READING");
        step1.setCefrLevel("B1");
        step1.setTopic("Test");
        step1.setPassageContent("A".repeat(100));

        WizardValidationResultDTO result = service.validate(step1, List.of(validQuestion("READING", "B1", "MULTIPLE_CHOICE_SINGLE")));

        assertNoError(result.getGroupErrors(), "PASSAGE_REQUIRED");
        assertNoError(result.getGroupErrors(), "PASSAGE_TOO_SHORT");
        assertNoError(result.getGroupErrors(), "PASSAGE_TOO_LONG");
    }

    // TC-VAL-GRP-005: TOPIC_BASED with null passage — no PASSAGE errors
    @Test
    void TC_VAL_GRP_005_topicBasedNullPassage() {
        WizardStep1DTO step1 = new WizardStep1DTO();
        step1.setMode("TOPIC_BASED");
        step1.setSkill("READING");
        step1.setCefrLevel("B1");
        step1.setTopic("Test");
        step1.setPassageContent(null);

        WizardValidationResultDTO result = service.validate(step1, List.of(validQuestion("READING", "B1", "MULTIPLE_CHOICE_SINGLE")));

        assertNoError(result.getGroupErrors(), "PASSAGE_REQUIRED");
        assertNoError(result.getGroupErrors(), "PASSAGE_TOO_SHORT");
        assertNoError(result.getGroupErrors(), "PASSAGE_TOO_LONG");
    }

    // TC-VAL-GRP-006: INVALID_AUDIO_URL — .pdf extension
    @Test
    void TC_VAL_GRP_006_invalidAudioUrl() {
        WizardStep1DTO step1 = validStep1("LISTENING", "B1");
        step1.setAudioUrl("https://example.com/file.pdf");

        WizardValidationResultDTO result = service.validate(step1, List.of(validQuestion("LISTENING", "B1", "MULTIPLE_CHOICE_SINGLE")));

        assertError(result.getGroupErrors(), "INVALID_AUDIO_URL");
    }

    // TC-VAL-GRP-007: VALID audio URL .mp3 — no error
    @Test
    void TC_VAL_GRP_007_validAudioMp3() {
        WizardStep1DTO step1 = validStep1("LISTENING", "B1");
        step1.setAudioUrl("https://example.com/audio.mp3");

        WizardValidationResultDTO result = service.validate(step1, List.of(validQuestion("LISTENING", "B1", "MULTIPLE_CHOICE_SINGLE")));

        assertNoError(result.getGroupErrors(), "INVALID_AUDIO_URL");
    }

    // TC-VAL-GRP-008: VALID audio URL with query params (.mp3?token=...)
    @Test
    void TC_VAL_GRP_008_validAudioWithQueryParams() {
        WizardStep1DTO step1 = validStep1("LISTENING", "B1");
        step1.setAudioUrl("https://example.com/audio.mp3?token=abc123");

        WizardValidationResultDTO result = service.validate(step1, List.of(validQuestion("LISTENING", "B1", "MULTIPLE_CHOICE_SINGLE")));

        assertNoError(result.getGroupErrors(), "INVALID_AUDIO_URL");
    }

    // TC-VAL-GRP-009: INVALID_IMAGE_URL — .gif extension
    @Test
    void TC_VAL_GRP_009_invalidImageUrl() {
        WizardStep1DTO step1 = validStep1("READING", "B1");
        step1.setImageUrl("https://example.com/image.gif");

        WizardValidationResultDTO result = service.validate(step1, List.of(validQuestion("READING", "B1", "MULTIPLE_CHOICE_SINGLE")));

        assertError(result.getGroupErrors(), "INVALID_IMAGE_URL");
    }

    // TC-VAL-GRP-010: VALID image URL .webp — no error
    @Test
    void TC_VAL_GRP_010_validImageWebp() {
        WizardStep1DTO step1 = validStep1("READING", "B1");
        step1.setImageUrl("https://example.com/image.webp");

        WizardValidationResultDTO result = service.validate(step1, List.of(validQuestion("READING", "B1", "MULTIPLE_CHOICE_SINGLE")));

        assertNoError(result.getGroupErrors(), "INVALID_IMAGE_URL");
    }

    // TC-VAL-GRP-011: NO_QUESTIONS — questions=null
    @Test
    void TC_VAL_GRP_011_noQuestionsNull() {
        WizardStep1DTO step1 = validStep1("READING", "B1");

        WizardValidationResultDTO result = service.validate(step1, null);

        assertFalse(result.isClean());
        assertError(result.getGroupErrors(), "NO_QUESTIONS");
    }

    // TC-VAL-GRP-012: NO_QUESTIONS — questions=[]
    @Test
    void TC_VAL_GRP_012_noQuestionsEmptyList() {
        WizardStep1DTO step1 = validStep1("READING", "B1");

        WizardValidationResultDTO result = service.validate(step1, List.of());

        assertFalse(result.isClean());
        assertError(result.getGroupErrors(), "NO_QUESTIONS");
    }

    // TC-VAL-GRP-013: TOO_MANY_QUESTIONS — 101 questions
    @Test
    void TC_VAL_GRP_013_tooManyQuestions() {
        WizardStep1DTO step1 = validStep1("READING", "B1");
        List<ValidatedQuestionDTO> questions = java.util.stream.IntStream.range(0, 101)
                .mapToObj(i -> validQuestion("READING", "B1", "MULTIPLE_CHOICE_SINGLE"))
                .toList();

        WizardValidationResultDTO result = service.validate(step1, questions);

        assertFalse(result.isClean());
        assertError(result.getGroupErrors(), "TOO_MANY_QUESTIONS");
    }

    // TC-VAL-GRP-014: 100 questions is valid — no error
    @Test
    void TC_VAL_GRP_014_100QuestionsValid() {
        WizardStep1DTO step1 = validStep1("READING", "B1");
        List<ValidatedQuestionDTO> questions = java.util.stream.IntStream.range(0, 100)
                .mapToObj(i -> validQuestion("READING", "B1", "MULTIPLE_CHOICE_SINGLE"))
                .toList();

        WizardValidationResultDTO result = service.validate(step1, questions);

        assertNoError(result.getGroupErrors(), "TOO_MANY_QUESTIONS");
    }

    // TC-VAL-GRP-015: TOO_MANY_TAGS — 11 tags
    @Test
    void TC_VAL_GRP_015_tooManyTags() {
        WizardStep1DTO step1 = validStep1("READING", "B1");
        step1.setTags(java.util.stream.IntStream.range(0, 11).mapToObj(i -> "tag" + i).toList());

        WizardValidationResultDTO result = service.validate(step1, List.of(validQuestion("READING", "B1", "MULTIPLE_CHOICE_SINGLE")));

        assertError(result.getGroupErrors(), "TOO_MANY_TAGS");
    }

    // TC-VAL-GRP-016: INVALID_SKILL — "INVALID"
    @Test
    void TC_VAL_GRP_016_invalidSkill() {
        WizardStep1DTO step1 = new WizardStep1DTO();
        step1.setSkill("INVALID");
        step1.setCefrLevel("B1");
        step1.setMode("TOPIC_BASED");
        step1.setTopic("Test");

        WizardValidationResultDTO result = service.validate(step1, List.of(validQuestion("INVALID", "B1", "MULTIPLE_CHOICE_SINGLE")));

        assertError(result.getGroupErrors(), "INVALID_SKILL");
    }

    // TC-VAL-GRP-017: INVALID_CEFR — "B3"
    @Test
    void TC_VAL_GRP_017_invalidCefr() {
        WizardStep1DTO step1 = new WizardStep1DTO();
        step1.setSkill("READING");
        step1.setCefrLevel("B3");
        step1.setMode("TOPIC_BASED");
        step1.setTopic("Test");

        WizardValidationResultDTO result = service.validate(step1, List.of(validQuestion("READING", "B3", "MULTIPLE_CHOICE_SINGLE")));

        assertError(result.getGroupErrors(), "INVALID_CEFR");
    }

    // ─── Question validation tests ───────────────────────────────────────────

    // TC-VAL-Q-001: SKILL_MISMATCH — group=READING, q=WRITING
    @Test
    void TC_VAL_Q_001_skillMismatch() {
        WizardStep1DTO step1 = validStep1("READING", "B1");
        ValidatedQuestionDTO q = validQuestion("WRITING", "B1", "WRITING");

        WizardValidationResultDTO result = service.validate(step1, List.of(q));

        assertFalse(result.isClean());
        assertTrue(result.getQuestions().get(0).getErrors().stream()
                .anyMatch(e -> e.getCode().equals("SKILL_MISMATCH")));
    }

    // TC-VAL-Q-002: SKILL_MISMATCH case-insensitive — no SKILL_MISMATCH
    @Test
    void TC_VAL_Q_002_skillMismatchCaseInsensitive() {
        WizardStep1DTO step1 = validStep1("READING", "B1");
        ValidatedQuestionDTO q = validQuestion("reading", "B1", "MULTIPLE_CHOICE_SINGLE");
        q.setSkill("reading"); // lowercase — should NOT trigger mismatch with "READING"

        WizardValidationResultDTO result = service.validate(step1, List.of(q));

        assertTrue(result.getQuestions().get(0).getErrors().stream()
                .noneMatch(e -> e.getCode().equals("SKILL_MISMATCH")));
    }

    // TC-VAL-Q-003: CEFR_MISMATCH — group=B1, q=C1
    @Test
    void TC_VAL_Q_003_cefrMismatch() {
        WizardStep1DTO step1 = validStep1("READING", "B1");
        ValidatedQuestionDTO q = validQuestion("READING", "C1", "MULTIPLE_CHOICE_SINGLE");
        q.setCefrLevel("C1");

        WizardValidationResultDTO result = service.validate(step1, List.of(q));

        assertTrue(result.getQuestions().get(0).getErrors().stream()
                .anyMatch(e -> e.getCode().equals("CEFR_MISMATCH")));
    }

    // TC-VAL-Q-004: INVALID_TYPE_FOR_SKILL — LISTENING+WRITING type
    @Test
    void TC_VAL_Q_004_invalidTypeForSkill_listeningPlusWriting() {
        WizardStep1DTO step1 = validStep1("LISTENING", "B1");
        ValidatedQuestionDTO q = validQuestion("LISTENING", "B1", "WRITING");

        WizardValidationResultDTO result = service.validate(step1, List.of(q));

        assertTrue(result.getQuestions().get(0).getErrors().stream()
                .anyMatch(e -> e.getCode().equals("INVALID_TYPE_FOR_SKILL")));
    }

    // TC-VAL-Q-005: INVALID_TYPE_FOR_SKILL — LISTENING+SPEAKING type
    @Test
    void TC_VAL_Q_005_invalidTypeForSkill_listeningPlusSpeaking() {
        WizardStep1DTO step1 = validStep1("LISTENING", "B1");
        ValidatedQuestionDTO q = validQuestion("LISTENING", "B1", "SPEAKING");

        WizardValidationResultDTO result = service.validate(step1, List.of(q));

        assertTrue(result.getQuestions().get(0).getErrors().stream()
                .anyMatch(e -> e.getCode().equals("INVALID_TYPE_FOR_SKILL")));
    }

    // TC-VAL-Q-006: CONTENT_TOO_SHORT — content="short"
    @Test
    void TC_VAL_Q_006_contentTooShort() {
        WizardStep1DTO step1 = validStep1("READING", "B1");
        ValidatedQuestionDTO q = validQuestion("READING", "B1", "MULTIPLE_CHOICE_SINGLE");
        q.setContent("short");

        WizardValidationResultDTO result = service.validate(step1, List.of(q));

        assertTrue(result.getQuestions().get(0).getErrors().stream()
                .anyMatch(e -> e.getCode().equals("CONTENT_TOO_SHORT")));
    }

    // TC-VAL-Q-007: CONTENT_TOO_LONG — 2001 chars
    @Test
    void TC_VAL_Q_007_contentTooLong() {
        WizardStep1DTO step1 = validStep1("READING", "B1");
        ValidatedQuestionDTO q = validQuestion("READING", "B1", "MULTIPLE_CHOICE_SINGLE");
        q.setContent("x".repeat(2001));

        WizardValidationResultDTO result = service.validate(step1, List.of(q));

        assertTrue(result.getQuestions().get(0).getErrors().stream()
                .anyMatch(e -> e.getCode().equals("CONTENT_TOO_LONG")));
    }

    // TC-VAL-Q-008: EXPLANATION_MISSING is WARNING only — errors empty, warnings has it
    @Test
    void TC_VAL_Q_008_explanationMissingIsWarning() {
        WizardStep1DTO step1 = validStep1("READING", "B1");
        ValidatedQuestionDTO q = validQuestion("READING", "B1", "MULTIPLE_CHOICE_SINGLE");
        q.setExplanation(null);

        WizardValidationResultDTO result = service.validate(step1, List.of(q));

        assertTrue(result.getQuestions().get(0).getErrors().isEmpty());
        assertTrue(result.getQuestions().get(0).getWarnings().stream()
                .anyMatch(e -> e.getCode().equals("EXPLANATION_MISSING")));
    }

    // TC-VAL-Q-009: TAGS_MISSING is WARNING only — errors empty, warnings has it
    @Test
    void TC_VAL_Q_009_tagsMissingIsWarning() {
        WizardStep1DTO step1 = validStep1("READING", "B1");
        ValidatedQuestionDTO q = validQuestion("READING", "B1", "MULTIPLE_CHOICE_SINGLE");
        q.setTags(null);

        WizardValidationResultDTO result = service.validate(step1, List.of(q));

        assertTrue(result.getQuestions().get(0).getErrors().isEmpty());
        assertTrue(result.getQuestions().get(0).getWarnings().stream()
                .anyMatch(e -> e.getCode().equals("TAGS_MISSING")));
    }

    // TC-VAL-Q-010: isValid=true when only warnings (no errors) — isClean=false (warningCount>0)
    @Test
    void TC_VAL_Q_010_onlyWarningsStillValid() {
        WizardStep1DTO step1 = validStep1("READING", "B1");
        ValidatedQuestionDTO q = validQuestion("READING", "B1", "MULTIPLE_CHOICE_SINGLE");
        q.setExplanation(null); // triggers warning
        q.setTags(null);         // triggers warning

        WizardValidationResultDTO result = service.validate(step1, List.of(q));

        assertTrue(result.getQuestions().get(0).isValid()); // no errors
        assertFalse(result.isClean());                      // has warnings
        assertTrue(result.getWarningCount() > 0);
    }

    // TC-VAL-Q-011: Multiple errors on same question — 3 errors all with same questionIndex
    @Test
    void TC_VAL_Q_011_multipleErrorsSameQuestionIndex() {
        WizardStep1DTO step1 = validStep1("READING", "B1");
        ValidatedQuestionDTO q = ValidatedQuestionDTO.builder()
                .content("short")                          // CONTENT_TOO_SHORT
                .questionType("WRITING")                   // wrong type for READING
                .skill("WRITING")                          // SKILL_MISMATCH
                .cefrLevel("B1")
                .options(List.of(
                        OptionDTO.builder().title("A").correct(true).build(),
                        OptionDTO.builder().title("B").correct(false).build()
                ))
                .errors(List.of())
                .warnings(List.of())
                .isValid(true)
                .build();

        WizardValidationResultDTO result = service.validate(step1, List.of(q));

        List<ValidationErrorDTO> errs = result.getQuestions().get(0).getErrors();
        assertEquals(3, errs.size());
        assertTrue(errs.stream().allMatch(e -> e.getQuestionIndex() == 0));
    }

    // ─── MC/Option validation tests ───────────────────────────────────────────

    // TC-VAL-MC-001: TOO_FEW_OPTIONS — 1 option
    @Test
    void TC_VAL_MC_001_tooFewOptions() {
        WizardStep1DTO step1 = validStep1("READING", "B1");
        ValidatedQuestionDTO q = validQuestion("READING", "B1", "MULTIPLE_CHOICE_SINGLE");
        q.setOptions(List.of(OptionDTO.builder().title("Only one").correct(true).build()));

        WizardValidationResultDTO result = service.validate(step1, List.of(q));

        assertTrue(result.getQuestions().get(0).getErrors().stream()
                .anyMatch(e -> e.getCode().equals("TOO_FEW_OPTIONS")));
    }

    // TC-VAL-MC-002: NO_CORRECT_ANSWER — all correct=false
    @Test
    void TC_VAL_MC_002_noCorrectAnswer() {
        WizardStep1DTO step1 = validStep1("READING", "B1");
        ValidatedQuestionDTO q = validQuestion("READING", "B1", "MULTIPLE_CHOICE_SINGLE");
        q.setOptions(List.of(
                OptionDTO.builder().title("Option A").correct(false).build(),
                OptionDTO.builder().title("Option B").correct(false).build()
        ));

        WizardValidationResultDTO result = service.validate(step1, List.of(q));

        assertTrue(result.getQuestions().get(0).getErrors().stream()
                .anyMatch(e -> e.getCode().equals("NO_CORRECT_ANSWER")));
    }

    // TC-VAL-MC-003: ALL_OPTIONS_CORRECT — multi with all correct
    @Test
    void TC_VAL_MC_003_allOptionsCorrect() {
        WizardStep1DTO step1 = validStep1("READING", "B1");
        ValidatedQuestionDTO q = validQuestion("READING", "B1", "MULTIPLE_CHOICE_MULTI");
        q.setOptions(List.of(
                OptionDTO.builder().title("Option A").correct(true).build(),
                OptionDTO.builder().title("Option B").correct(true).build()
        ));

        WizardValidationResultDTO result = service.validate(step1, List.of(q));

        assertTrue(result.getQuestions().get(0).getErrors().stream()
                .anyMatch(e -> e.getCode().equals("ALL_OPTIONS_CORRECT")));
    }

    // TC-VAL-MC-004: MULTIPLE_CHOICE_MULTI with 2/3 correct — no errors
    @Test
    void TC_VAL_MC_004_multiChoiceMultiPartialCorrect() {
        WizardStep1DTO step1 = validStep1("READING", "B1");
        ValidatedQuestionDTO q = validQuestion("READING", "B1", "MULTIPLE_CHOICE_MULTI");
        q.setOptions(List.of(
                OptionDTO.builder().title("Option A").correct(true).build(),
                OptionDTO.builder().title("Option B").correct(true).build(),
                OptionDTO.builder().title("Option C").correct(false).build()
        ));

        WizardValidationResultDTO result = service.validate(step1, List.of(q));

        assertTrue(result.getQuestions().get(0).getErrors().isEmpty());
    }

    // TC-VAL-MC-005: OPTION_DUPLICATE — "Paris"/"paris"
    @Test
    void TC_VAL_MC_005_duplicateOptionCaseInsensitive() {
        WizardStep1DTO step1 = validStep1("READING", "B1");
        ValidatedQuestionDTO q = validQuestion("READING", "B1", "MULTIPLE_CHOICE_SINGLE");
        q.setOptions(List.of(
                OptionDTO.builder().title("Paris").correct(true).build(),
                OptionDTO.builder().title("paris").correct(false).build()
        ));

        WizardValidationResultDTO result = service.validate(step1, List.of(q));

        assertTrue(result.getQuestions().get(0).getErrors().stream()
                .anyMatch(e -> e.getCode().equals("OPTION_DUPLICATE")));
    }

    // TC-VAL-MC-006: OPTION_EMPTY — "" title
    @Test
    void TC_VAL_MC_006_emptyOptionTitle() {
        WizardStep1DTO step1 = validStep1("READING", "B1");
        ValidatedQuestionDTO q = validQuestion("READING", "B1", "MULTIPLE_CHOICE_SINGLE");
        q.setOptions(List.of(
                OptionDTO.builder().title("Option A").correct(true).build(),
                OptionDTO.builder().title("").correct(false).build()
        ));

        WizardValidationResultDTO result = service.validate(step1, List.of(q));

        assertTrue(result.getQuestions().get(0).getErrors().stream()
                .anyMatch(e -> e.getCode().equals("OPTION_EMPTY")));
    }

    // TC-VAL-MC-007: OPTION_TOO_LONG — 501 chars
    @Test
    void TC_VAL_MC_007_optionTooLong() {
        WizardStep1DTO step1 = validStep1("READING", "B1");
        ValidatedQuestionDTO q = validQuestion("READING", "B1", "MULTIPLE_CHOICE_SINGLE");
        q.setOptions(List.of(
                OptionDTO.builder().title("A".repeat(501)).correct(true).build(),
                OptionDTO.builder().title("B").correct(false).build()
        ));

        WizardValidationResultDTO result = service.validate(step1, List.of(q));

        assertTrue(result.getQuestions().get(0).getErrors().stream()
                .anyMatch(e -> e.getCode().equals("OPTION_TOO_LONG")));
    }

    // TC-VAL-MC-008: SPEAKING question with null options — no option errors
    @Test
    void TC_VAL_MC_008_speakingNullOptions() {
        WizardStep1DTO step1 = validStep1("SPEAKING", "B1");
        ValidatedQuestionDTO q = validQuestion("SPEAKING", "B1", "SPEAKING");
        q.setOptions(null);

        WizardValidationResultDTO result = service.validate(step1, List.of(q));

        assertTrue(result.getQuestions().get(0).getErrors().stream()
                .noneMatch(e -> e.getCode().equals("TOO_FEW_OPTIONS")));
        assertTrue(result.getQuestions().get(0).getErrors().stream()
                .noneMatch(e -> e.getCode().equals("NO_OPTIONS")));
    }

    // TC-VAL-MC-009: 2 valid options for SINGLE choice — no errors
    @Test
    void TC_VAL_MC_009_twoOptionsSingleChoiceValid() {
        WizardStep1DTO step1 = validStep1("READING", "B1");
        ValidatedQuestionDTO q = validQuestion("READING", "B1", "MULTIPLE_CHOICE_SINGLE");
        q.setOptions(List.of(
                OptionDTO.builder().title("Option A").correct(true).build(),
                OptionDTO.builder().title("Option B").correct(false).build()
        ));

        WizardValidationResultDTO result = service.validate(step1, List.of(q));

        assertTrue(result.getQuestions().get(0).getErrors().isEmpty());
    }

    // ─── Matching validation tests ───────────────────────────────────────────

    // TC-VAL-MATCH-001: MATCHING_COUNT_MISMATCH — 2 lefts, 1 right
    @Test
    void TC_VAL_MATCH_001_countMismatch() {
        WizardStep1DTO step1 = validStep1("READING", "B1");
        ValidatedQuestionDTO q = ValidatedQuestionDTO.builder()
                .content("Match the words")
                .questionType("MATCHING")
                .skill("READING")
                .cefrLevel("B1")
                .options(List.of(
                        OptionDTO.builder().title("Apple").correct(true).matchTarget("fruit").build(),
                        OptionDTO.builder().title("Banana").correct(true).matchTarget("fruit").build(),
                        OptionDTO.builder().title("Carrot").correct(false).build() // right side — only 1
                ))
                .tags(List.of("match"))
                .errors(List.of())
                .warnings(List.of())
                .isValid(true)
                .build();

        WizardValidationResultDTO result = service.validate(step1, List.of(q));

        assertTrue(result.getQuestions().get(0).getErrors().stream()
                .anyMatch(e -> e.getCode().equals("MATCHING_COUNT_MISMATCH")));
    }

    // TC-VAL-MATCH-002: LEFT_ITEM_TOO_LONG — 301 chars
    @Test
    void TC_VAL_MATCH_002_leftItemTooLong() {
        WizardStep1DTO step1 = validStep1("READING", "B1");
        ValidatedQuestionDTO q = ValidatedQuestionDTO.builder()
                .content("Match the words")
                .questionType("MATCHING")
                .skill("READING")
                .cefrLevel("B1")
                .options(List.of(
                        OptionDTO.builder().title("A".repeat(301)).correct(true).matchTarget("right").build(),
                        OptionDTO.builder().title("B").correct(true).matchTarget("right").build(),
                        OptionDTO.builder().title("X").correct(false).build(),
                        OptionDTO.builder().title("Y").correct(false).build()
                ))
                .tags(List.of("match"))
                .errors(List.of())
                .warnings(List.of())
                .isValid(true)
                .build();

        WizardValidationResultDTO result = service.validate(step1, List.of(q));

        assertTrue(result.getQuestions().get(0).getErrors().stream()
                .anyMatch(e -> e.getCode().equals("LEFT_ITEM_TOO_LONG")));
    }

    // TC-VAL-MATCH-003: RIGHT_ITEM_TOO_LONG — 301 chars
    @Test
    void TC_VAL_MATCH_003_rightItemTooLong() {
        WizardStep1DTO step1 = validStep1("READING", "B1");
        ValidatedQuestionDTO q = ValidatedQuestionDTO.builder()
                .content("Match the words")
                .questionType("MATCHING")
                .skill("READING")
                .cefrLevel("B1")
                .options(List.of(
                        OptionDTO.builder().title("Left1").correct(true).matchTarget("right").build(),
                        OptionDTO.builder().title("Left2").correct(true).matchTarget("right2").build(),
                        // Right items: no matchTarget; put 301-char string in title to trigger RIGHT_ITEM_TOO_LONG
                        OptionDTO.builder().title("B".repeat(301)).correct(false).build(),
                        OptionDTO.builder().title("X").correct(false).build()
                ))
                .tags(List.of("match"))
                .errors(List.of())
                .warnings(List.of())
                .isValid(true)
                .build();

        WizardValidationResultDTO result = service.validate(step1, List.of(q));

        assertTrue(result.getQuestions().get(0).getErrors().stream()
                .anyMatch(e -> e.getCode().equals("RIGHT_ITEM_TOO_LONG")));
    }

    // TC-VAL-MATCH-004: Valid matching (3 pairs, all <300 chars) — no MATCHING errors
    @Test
    void TC_VAL_MATCH_004_validMatching() {
        WizardStep1DTO step1 = validStep1("READING", "B1");
        ValidatedQuestionDTO q = ValidatedQuestionDTO.builder()
                .content("Match the words")
                .questionType("MATCHING")
                .skill("READING")
                .cefrLevel("B1")
                .options(List.of(
                        OptionDTO.builder().title("Apple").correct(true).matchTarget("fruit").build(),
                        OptionDTO.builder().title("Banana").correct(true).matchTarget("yellow").build(),
                        OptionDTO.builder().title("Carrot").correct(true).matchTarget("vegetable").build(),
                        OptionDTO.builder().title("X").correct(false).build(),
                        OptionDTO.builder().title("Y").correct(false).build(),
                        OptionDTO.builder().title("Z").correct(false).build()
                ))
                .tags(List.of("match"))
                .errors(List.of())
                .warnings(List.of())
                .isValid(true)
                .build();

        WizardValidationResultDTO result = service.validate(step1, List.of(q));

        assertTrue(result.getQuestions().get(0).getErrors().stream()
                .noneMatch(e -> e.getCode().equals("MATCHING_COUNT_MISMATCH")));
        assertTrue(result.getQuestions().get(0).getErrors().stream()
                .noneMatch(e -> e.getCode().equals("LEFT_ITEM_TOO_LONG")));
        assertTrue(result.getQuestions().get(0).getErrors().stream()
                .noneMatch(e -> e.getCode().equals("RIGHT_ITEM_TOO_LONG")));
    }
}
