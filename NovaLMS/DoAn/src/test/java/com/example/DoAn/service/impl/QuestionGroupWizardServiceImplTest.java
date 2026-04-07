package com.example.DoAn.service.impl;

import com.example.DoAn.dto.request.*;
import com.example.DoAn.dto.response.*;
import com.example.DoAn.dto.response.ValidatedQuestionDTO.OptionDTO;
import com.example.DoAn.exception.InvalidDataException;
import com.example.DoAn.service.*;
import com.example.DoAn.util.RateLimitWindowStore;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionGroupWizardServiceImplTest {

    @Mock private AIQuestionService aiQuestionService;
    @Mock private ExcelQuestionImportService excelQuestionImportService;
    @Mock private IExpertQuestionService expertQuestionService;
    @Mock private WizardValidationService validationService;
    @Mock private RateLimitWindowStore rateLimitStore;

    private QuestionGroupWizardServiceImpl service;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        service = new QuestionGroupWizardServiceImpl(
                aiQuestionService, excelQuestionImportService, expertQuestionService,
                validationService, rateLimitStore);
        session = new MockHttpSession();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private WizardStep1DTO makeStep1() {
        WizardStep1DTO dto = new WizardStep1DTO();
        dto.setSkill("READING");
        dto.setCefrLevel("B1");
        dto.setMode("TOPIC_BASED");
        dto.setTopic("General topic");
        return dto;
    }

    private void putStep1InSession() {
        session.setAttribute("wizard_step1", makeStep1());
    }

    private ValidatedQuestionDTO makeValidatedQ() {
        return ValidatedQuestionDTO.builder()
                .content("Sample question content here")
                .questionType("MULTIPLE_CHOICE_SINGLE")
                .skill("READING")
                .cefrLevel("B1")
                .explanation("This is the explanation")
                .options(List.of(
                        OptionDTO.builder().title("A").correct(true).build(),
                        OptionDTO.builder().title("B").correct(false).build()
                ))
                .tags(List.of("tag1"))
                .errors(List.of())
                .warnings(List.of())
                .isValid(true)
                .build();
    }

    // ─── saveStep1 / getStep1 tests ─────────────────────────────────────────

    // TC-WIZ-SVC-001: saveStep1() calls setAttribute and removeAttribute
    @Test
    void TC_WIZ_SVC_001_saveStep1() {
        WizardStep1DTO dto = makeStep1();

        service.saveStep1(session, dto);

        assertEquals(dto, session.getAttribute("wizard_step1"));
        assertNull(session.getAttribute("wizard_validation_result"));
    }

    // TC-WIZ-SVC-002: getStep1() retrieves from session
    @Test
    void TC_WIZ_SVC_002_getStep1() {
        WizardStep1DTO dto = makeStep1();
        session.setAttribute("wizard_step1", dto);

        WizardStep1DTO result = service.getStep1(session);

        assertEquals(dto, result);
    }

    // ─── processStep2 — AI_GENERATE ─────────────────────────────────────────

    // TC-WIZ-SVC-003: AI_GENERATE with rate limit allowed → questions returned
    @Test
    void TC_WIZ_SVC_003_aiGenerateAllowed() {
        putStep1InSession();
        when(rateLimitStore.isAllowed("expert@nova.com")).thenReturn(true);

        AIGenerateResponseDTO.QuestionDTO aiQ = AIGenerateResponseDTO.QuestionDTO.builder()
                .content("What is the main idea?")
                .questionType("MULTIPLE_CHOICE_SINGLE")
                .skill("READING")
                .cefrLevel("B1")
                .options(List.of(
                        AIGenerateResponseDTO.OptionDTO.builder().title("A").correct(true).build(),
                        AIGenerateResponseDTO.OptionDTO.builder().title("B").correct(false).build()
                ))
                .build();
        AIGenerateResponseDTO aiResponse = AIGenerateResponseDTO.builder()
                .questions(List.of(aiQ))
                .build();
        when(aiQuestionService.generate(any(), eq("expert@nova.com"))).thenReturn(aiResponse);

        WizardStep2DTO step2 = new WizardStep2DTO();
        step2.setSourceType("AI_GENERATE");
        step2.setAiTopic("Main idea");
        step2.setAiQuantity(5);

        WizardStep2ResultDTO result = service.processStep2(session, step2, "expert@nova.com");

        assertEquals(1, result.getTotalQuestions());
        assertEquals(1, result.getQuestions().size());
        assertEquals("AI_GENERATE", result.getSourceType());
    }

    // TC-WIZ-SVC-004: AI_GENERATE rate limited → returns RATE_LIMITED error
    @Test
    void TC_WIZ_SVC_004_aiRateLimited() {
        putStep1InSession();
        when(rateLimitStore.isAllowed("expert@nova.com")).thenReturn(false);

        WizardStep2DTO step2 = new WizardStep2DTO();
        step2.setSourceType("AI_GENERATE");
        step2.setAiTopic("Some topic");

        WizardStep2ResultDTO result = service.processStep2(session, step2, "expert@nova.com");

        assertEquals(1, result.getErrorCount());
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.getCode().equals("RATE_LIMITED")));
        assertTrue(result.getQuestions().isEmpty());
    }

    // TC-WIZ-SVC-005: AI_GENERATE missing topic → returns TOPIC_REQUIRED error
    @Test
    void TC_WIZ_SVC_005_aiMissingTopic() {
        putStep1InSession();
        when(rateLimitStore.isAllowed("expert@nova.com")).thenReturn(true);

        WizardStep2DTO step2 = new WizardStep2DTO();
        step2.setSourceType("AI_GENERATE");
        step2.setAiTopic(null); // missing

        WizardStep2ResultDTO result = service.processStep2(session, step2, "expert@nova.com");

        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.getCode().equals("TOPIC_REQUIRED")));
    }

    // ─── processStep2 — EXCEL_IMPORT ────────────────────────────────────────

    // TC-WIZ-SVC-006: EXCEL_IMPORT valid .xlsx → parses and returns questions
    @Test
    void TC_WIZ_SVC_006_excelImportValid() throws Exception {
        putStep1InSession();
        MockMultipartFile file = new MockMultipartFile(
                "file", "questions.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[1024]);

        ExcelParseResultDTO.ValidRowDTO row = ExcelParseResultDTO.ValidRowDTO.builder()
                .rowIndex(1)
                .content("Excel question content here")
                .questionType("MULTIPLE_CHOICE_SINGLE")
                .skill("READING")
                .cefrLevel("B1")
                .options(List.of(
                        ExcelParseResultDTO.OptionDTO.builder().title("A").correct(true).build(),
                        ExcelParseResultDTO.OptionDTO.builder().title("B").correct(false).build()
                ))
                .build();
        ExcelParseResultDTO parseResult = ExcelParseResultDTO.builder()
                .valid(List.of(row))
                .build();
        when(excelQuestionImportService.parseFile(any(), any())).thenReturn(parseResult);

        WizardStep2DTO step2 = new WizardStep2DTO();
        step2.setSourceType("EXCEL_IMPORT");
        step2.setExcelFile(file);

        WizardStep2ResultDTO result = service.processStep2(session, step2, "expert@nova.com");

        assertEquals(1, result.getTotalQuestions());
        assertFalse(result.getQuestions().isEmpty());
    }

    // TC-WIZ-SVC-007: EXCEL_IMPORT .csv file → returns INVALID_FILE_TYPE error
    @Test
    void TC_WIZ_SVC_007_excelCsvInvalid() throws Exception {
        putStep1InSession();
        MockMultipartFile file = new MockMultipartFile(
                "file", "questions.csv", "text/csv",
                "a,b,c".getBytes());

        WizardStep2DTO step2 = new WizardStep2DTO();
        step2.setSourceType("EXCEL_IMPORT");
        step2.setExcelFile(file);

        WizardStep2ResultDTO result = service.processStep2(session, step2, "expert@nova.com");

        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.getCode().equals("INVALID_FILE_TYPE")));
    }

    // TC-WIZ-SVC-008: EXCEL_IMPORT >5MB → returns FILE_TOO_LARGE error
    @Test
    void TC_WIZ_SVC_008_excelTooLarge() throws Exception {
        putStep1InSession();
        byte[] bigContent = new byte[6 * 1024 * 1024]; // 6MB
        MockMultipartFile file = new MockMultipartFile(
                "file", "questions.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                bigContent);

        WizardStep2DTO step2 = new WizardStep2DTO();
        step2.setSourceType("EXCEL_IMPORT");
        step2.setExcelFile(file);

        WizardStep2ResultDTO result = service.processStep2(session, step2, "expert@nova.com");

        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.getCode().equals("FILE_TOO_LARGE")));
    }

    // TC-WIZ-SVC-009: MANUAL → processes inline questions
    @Test
    void TC_WIZ_SVC_009_manual() {
        putStep1InSession();
        ManualQuestionDTO mq = new ManualQuestionDTO();
        mq.setContent("Manual question content here");
        mq.setQuestionType("MULTIPLE_CHOICE_SINGLE");
        mq.setExplanation("Explanation");
        List<ManualQuestionDTO.AnswerOptionInput> opts = List.of(
                ManualQuestionDTO.AnswerOptionInput.builder().title("A").correct(true).build(),
                ManualQuestionDTO.AnswerOptionInput.builder().title("B").correct(false).build()
        );
        mq.setOptions(opts);

        WizardStep2DTO step2 = new WizardStep2DTO();
        step2.setSourceType("MANUAL");
        step2.setManualQuestions(List.of(mq));

        WizardStep2ResultDTO result = service.processStep2(session, step2, "expert@nova.com");

        assertEquals(1, result.getTotalQuestions());
        assertEquals("Manual question content here", result.getQuestions().get(0).getContent());
    }

    // TC-WIZ-SVC-010: processStep2 merges with existing (not overwrite) → session accumulates
    @Test
    void TC_WIZ_SVC_010_mergeNotOverwrite() {
        putStep1InSession();
        session.setAttribute("wizard_step2_questions", new ArrayList<>(List.of(makeValidatedQ())));

        ManualQuestionDTO mq = new ManualQuestionDTO();
        mq.setContent("Second question");
        mq.setQuestionType("MULTIPLE_CHOICE_SINGLE");
        mq.setOptions(List.of(
                ManualQuestionDTO.AnswerOptionInput.builder().title("A").correct(true).build(),
                ManualQuestionDTO.AnswerOptionInput.builder().title("B").correct(false).build()
        ));
        WizardStep2DTO step2 = new WizardStep2DTO();
        step2.setSourceType("MANUAL");
        step2.setManualQuestions(List.of(mq));

        WizardStep2ResultDTO result = service.processStep2(session, step2, "expert@nova.com");

        assertEquals(2, result.getTotalQuestions()); // 1 existing + 1 new
    }

    // ─── validate ────────────────────────────────────────────────────────────

    // TC-WIZ-SVC-011: validate() calls validationService and stores in session
    @Test
    void TC_WIZ_SVC_011_validate() {
        putStep1InSession();
        session.setAttribute("wizard_step2_questions", new ArrayList<>(List.of(makeValidatedQ())));
        WizardValidationResultDTO mockResult = WizardValidationResultDTO.builder()
                .questions(List.of())
                .groupErrors(List.of())
                .groupWarnings(List.of())
                .isClean(true)
                .totalQuestions(1)
                .errorCount(0)
                .warningCount(0)
                .build();
        when(validationService.validate(any(), any())).thenReturn(mockResult);

        WizardValidationResultDTO result = service.validate(session);

        assertNotNull(session.getAttribute("wizard_validation_result"));
        assertTrue(result.isClean());
    }

    // TC-WIZ-SVC-012: saveWizard with isClean=false → throws InvalidDataException
    @Test
    void TC_WIZ_SVC_012_saveWithErrorsThrows() {
        putStep1InSession();
        session.setAttribute("wizard_step2_questions", new ArrayList<>(List.of(makeValidatedQ())));
        WizardValidationResultDTO dirtyResult = WizardValidationResultDTO.builder()
                .questions(List.of(ValidatedQuestionDTO.builder()
                        .index(0)
                        .content("short") // too short → CONTENT_TOO_SHORT
                        .questionType("MULTIPLE_CHOICE_SINGLE")
                        .skill("READING")
                        .cefrLevel("B1")
                        .errors(List.of(ValidationErrorDTO.builder()
                                .questionIndex(0).code("CONTENT_TOO_SHORT")
                                .message("Question too short").severity("ERROR").build()))
                        .warnings(List.of())
                        .isValid(false)
                        .build()))
                .groupErrors(List.of())
                .groupWarnings(List.of())
                .isClean(false)
                .totalQuestions(1)
                .errorCount(1)
                .warningCount(0)
                .build();
        when(validationService.validate(any(), any())).thenReturn(dirtyResult);

        assertThrows(InvalidDataException.class,
                () -> service.saveWizard(session, new WizardSaveDTO(), "expert@nova.com"));
    }

    // TC-WIZ-SVC-013: saveWizard with isClean=true → creates question group, clears session
    @Test
    void TC_WIZ_SVC_013_saveCleanWizard() {
        putStep1InSession();
        session.setAttribute("wizard_step2_questions", new ArrayList<>(List.of(makeValidatedQ())));
        WizardValidationResultDTO cleanResult = WizardValidationResultDTO.builder()
                .questions(List.of(makeValidatedQ()))
                .groupErrors(List.of())
                .groupWarnings(List.of())
                .isClean(true)
                .totalQuestions(1)
                .errorCount(0)
                .warningCount(0)
                .build();
        when(validationService.validate(any(), any())).thenReturn(cleanResult);
        QuestionGroupResponseDTO savedGroup = new QuestionGroupResponseDTO();
        savedGroup.setGroupId(42);
        when(expertQuestionService.createQuestionGroup(any(), eq("expert@nova.com")))
                .thenReturn(savedGroup);

        Integer groupId = service.saveWizard(session, new WizardSaveDTO(), "expert@nova.com");

        assertEquals(42, groupId);
        assertNull(session.getAttribute("wizard_step1"));
        assertNull(session.getAttribute("wizard_step2_questions"));
    }

    // TC-WIZ-SVC-014: abandon() removes all wizard session attributes
    @Test
    void TC_WIZ_SVC_014_abandon() {
        session.setAttribute("wizard_step1", makeStep1());
        session.setAttribute("wizard_step2_questions", new ArrayList<>(List.of(makeValidatedQ())));
        session.setAttribute("wizard_validation_result", WizardValidationResultDTO.builder().build());

        service.abandon(session);

        assertNull(session.getAttribute("wizard_step1"));
        assertNull(session.getAttribute("wizard_step2_questions"));
        assertNull(session.getAttribute("wizard_validation_result"));
    }
}
