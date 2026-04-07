package com.example.DoAn.service.impl;

import com.example.DoAn.dto.request.AssignmentQuestionRequestDTO;
import com.example.DoAn.dto.request.QuizRequestDTO;
import com.example.DoAn.dto.response.AssignmentPreviewDTO;
import com.example.DoAn.dto.response.SkillSectionSummaryDTO;
import com.example.DoAn.exception.InvalidDataException;
import com.example.DoAn.exception.ResourceNotFoundException;
import com.example.DoAn.model.*;
import com.example.DoAn.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpertAssignmentServiceTest {

    @Mock private QuizRepository quizRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private QuizQuestionRepository quizQuestionRepository;
    @Mock private UserRepository userRepository;

    private ObjectMapper objectMapper;
    private ExpertAssignmentServiceImpl service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new ExpertAssignmentServiceImpl(
                quizRepository, questionRepository, quizQuestionRepository, userRepository, objectMapper);
    }

    // ─── Helper builders ───────────────────────────────────────────────────────

    private Setting expertRole() {
        Setting s = new Setting();
        s.setSettingId(203);
        s.setName("EXPERT");
        return s;
    }

    private Setting teacherRole() {
        Setting s = new Setting();
        s.setSettingId(204);
        s.setName("TEACHER");
        return s;
    }

    private User expertUser() {
        User u = new User();
        u.setUserId(1);
        u.setEmail("expert@nova.com");
        u.setRole(expertRole());
        return u;
    }

    private User teacherUser() {
        User u = new User();
        u.setUserId(2);
        u.setEmail("teacher@nova.com");
        u.setRole(teacherRole());
        return u;
    }

    private Quiz draftQuiz() {
        Quiz q = new Quiz();
        q.setQuizId(10);
        q.setStatus("DRAFT");
        q.setQuizCategory("COURSE_ASSIGNMENT");
        q.setUser(expertUser());
        q.setIsSequential(true);
        return q;
    }

    private QuizRequestDTO validDTO() {
        return QuizRequestDTO.builder()
                .title("Midterm Exam")
                .quizCategory("COURSE_ASSIGNMENT")
                .passScore(BigDecimal.valueOf(70))
                .maxAttempts(2)
                .build();
    }

    // ─── createAssignment tests ──────────────────────────────────────────────

    // TC-SVC-001: createAssignment(COURSE_ASSIGNMENT) → DRAFT, sequential, skillOrder
    @Test
    void TC_SVC_001_createCourseAssignment() throws JsonProcessingException {
        when(userRepository.findByEmail("expert@nova.com")).thenReturn(Optional.of(expertUser()));
        when(quizRepository.save(any(Quiz.class))).thenAnswer(inv -> {
            Quiz q = inv.getArgument(0);
            q.setQuizId(1);
            return q;
        });

        Quiz result = service.createAssignment(validDTO(), "expert@nova.com");

        assertEquals("DRAFT", result.getStatus());
        assertTrue(result.getIsSequential());
        assertNotNull(result.getSkillOrder());
        assertTrue(result.getSkillOrder().contains("LISTENING"));
        assertTrue(result.getSkillOrder().contains("WRITING"));
        assertFalse(result.getIsOpen());
        verify(quizRepository).save(any(Quiz.class));
    }

    // TC-SVC-002: createAssignment(MODULE_ASSIGNMENT) → quizCategory=MODULE_ASSIGNMENT, sequential
    @Test
    void TC_SVC_002_createModuleAssignment() throws JsonProcessingException {
        when(userRepository.findByEmail("expert@nova.com")).thenReturn(Optional.of(expertUser()));
        QuizRequestDTO dto = QuizRequestDTO.builder()
                .title("Module Quiz")
                .quizCategory("MODULE_ASSIGNMENT")
                .build();
        when(quizRepository.save(any(Quiz.class))).thenAnswer(inv -> {
            Quiz q = inv.getArgument(0);
            q.setQuizId(1);
            return q;
        });

        Quiz result = service.createAssignment(dto, "expert@nova.com");

        assertEquals("MODULE_ASSIGNMENT", result.getQuizCategory());
        assertTrue(result.getIsSequential());
    }

    // TC-SVC-003: Non-EXPERT → throws InvalidDataException
    @Test
    void TC_SVC_003_nonExpertThrows() {
        when(userRepository.findByEmail("teacher@nova.com")).thenReturn(Optional.of(teacherUser()));

        InvalidDataException ex = assertThrows(InvalidDataException.class,
                () -> service.createAssignment(validDTO(), "teacher@nova.com"));
        assertTrue(ex.getMessage().contains("Only experts can create assignments"));
    }

    // TC-SVC-004: Invalid category (LESSON_QUIZ) → throws InvalidDataException
    @Test
    void TC_SVC_004_invalidCategoryThrows() {
        when(userRepository.findByEmail("expert@nova.com")).thenReturn(Optional.of(expertUser()));
        QuizRequestDTO dto = QuizRequestDTO.builder()
                .title("Bad Quiz")
                .quizCategory("LESSON_QUIZ")
                .build();

        InvalidDataException ex = assertThrows(InvalidDataException.class,
                () -> service.createAssignment(dto, "expert@nova.com"));
        assertTrue(ex.getMessage().contains("Invalid category"));
    }

    // TC-SVC-005: createAssignment with timeLimitPerSkill → field stored as JSON
    @Test
    void TC_SVC_005_timeLimitPerSkillStoredAsJson() throws JsonProcessingException {
        when(userRepository.findByEmail("expert@nova.com")).thenReturn(Optional.of(expertUser()));
        Map<String, Integer> limits = new LinkedHashMap<>();
        limits.put("SPEAKING", 2);
        limits.put("WRITING", 30);
        QuizRequestDTO dto = QuizRequestDTO.builder()
                .title("Timed Exam")
                .quizCategory("COURSE_ASSIGNMENT")
                .timeLimitPerSkill(limits)
                .build();
        ArgumentCaptor<Quiz> captor = ArgumentCaptor.forClass(Quiz.class);
        when(quizRepository.save(any(Quiz.class))).thenAnswer(inv -> {
            Quiz q = inv.getArgument(0);
            q.setQuizId(1);
            return q;
        });

        Quiz result = service.createAssignment(dto, "expert@nova.com");

        verify(quizRepository).save(captor.capture());
        assertNotNull(captor.getValue().getTimeLimitPerSkill());
        assertTrue(captor.getValue().getTimeLimitPerSkill().contains("SPEAKING"));
        assertTrue(captor.getValue().getTimeLimitPerSkill().contains("2"));
    }

    // TC-SVC-006: getSkillSummaries returns correct counts per skill
    @Test
    void TC_SVC_006_getSkillSummaries() {
        when(quizRepository.existsById(10)).thenReturn(true);
        when(quizQuestionRepository.countByQuizIdAndSkill(10, "LISTENING")).thenReturn(5L);
        when(quizQuestionRepository.countByQuizIdAndSkill(10, "READING")).thenReturn(3L);
        when(quizQuestionRepository.countByQuizIdAndSkill(10, "SPEAKING")).thenReturn(0L);
        when(quizQuestionRepository.countByQuizIdAndSkill(10, "WRITING")).thenReturn(7L);

        Map<String, SkillSectionSummaryDTO> result = service.getSkillSummaries(10);

        assertEquals(5L, result.get("LISTENING").getQuestionCount());
        assertEquals(3L, result.get("READING").getQuestionCount());
        assertEquals(0L, result.get("SPEAKING").getQuestionCount());
        assertEquals(7L, result.get("WRITING").getQuestionCount());
    }

    // TC-SVC-007: addQuestionsToSection adds questions sequentially
    @Test
    void TC_SVC_007_addQuestionsToSectionSequential() {
        Quiz quiz = draftQuiz();
        when(quizRepository.findById(10)).thenReturn(Optional.of(quiz));
        when(quizQuestionRepository.findByQuizQuizIdAndSkill(10, "LISTENING")).thenReturn(List.of());
        Question q1 = new Question();
        q1.setQuestionId(101);
        Question q2 = new Question();
        q2.setQuestionId(102);
        Question q3 = new Question();
        q3.setQuestionId(103);
        when(questionRepository.findById(101)).thenReturn(Optional.of(q1));
        when(questionRepository.findById(102)).thenReturn(Optional.of(q2));
        when(questionRepository.findById(103)).thenReturn(Optional.of(q3));
        AssignmentQuestionRequestDTO dto = new AssignmentQuestionRequestDTO();
        dto.setSkill("LISTENING");
        dto.setQuestionIds(Arrays.asList(101, 102, 103));

        service.addQuestionsToSection(10, dto, "expert@nova.com");

        verify(quizQuestionRepository, times(3)).save(any(QuizQuestion.class));
    }

    // TC-SVC-008: addQuestionsToSection on non-sequential quiz → throws
    @Test
    void TC_SVC_008_addQuestionsNonSequentialThrows() {
        Quiz quiz = new Quiz();
        quiz.setQuizId(10);
        quiz.setIsSequential(false);
        when(quizRepository.findById(10)).thenReturn(Optional.of(quiz));

        AssignmentQuestionRequestDTO dto = new AssignmentQuestionRequestDTO();
        dto.setSkill("LISTENING");
        dto.setQuestionIds(List.of(1));

        InvalidDataException ex = assertThrows(InvalidDataException.class,
                () -> service.addQuestionsToSection(10, dto, "expert@nova.com"));
        assertTrue(ex.getMessage().contains("does not support section-based"));
    }

    // TC-SVC-009: addQuestionsToSection with invalid skill → throws
    @Test
    void TC_SVC_009_addQuestionsInvalidSkillThrows() {
        Quiz quiz = draftQuiz();
        when(quizRepository.findById(10)).thenReturn(Optional.of(quiz));

        AssignmentQuestionRequestDTO dto = new AssignmentQuestionRequestDTO();
        dto.setSkill("INVALID_SKILL");
        dto.setQuestionIds(List.of(1));

        InvalidDataException ex = assertThrows(InvalidDataException.class,
                () -> service.addQuestionsToSection(10, dto, "expert@nova.com"));
        assertTrue(ex.getMessage().contains("Invalid skill"));
    }

    // TC-SVC-010: publishAssignment with all 4 skills → PUBLISHED, isOpen=false
    @Test
    void TC_SVC_010_publishAssignmentAllSkills() {
        Quiz quiz = draftQuiz();
        when(quizRepository.findById(10)).thenReturn(Optional.of(quiz));
        when(quizRepository.existsById(10)).thenReturn(true);
        when(quizQuestionRepository.countByQuizIdAndSkill(10, "LISTENING")).thenReturn(5L);
        when(quizQuestionRepository.countByQuizIdAndSkill(10, "READING")).thenReturn(3L);
        when(quizQuestionRepository.countByQuizIdAndSkill(10, "SPEAKING")).thenReturn(2L);
        when(quizQuestionRepository.countByQuizIdAndSkill(10, "WRITING")).thenReturn(4L);
        when(quizRepository.save(any(Quiz.class))).thenReturn(quiz);

        service.publishAssignment(10);

        ArgumentCaptor<Quiz> captor = ArgumentCaptor.forClass(Quiz.class);
        verify(quizRepository).save(captor.capture());
        assertEquals("PUBLISHED", captor.getValue().getStatus());
        assertFalse(captor.getValue().getIsOpen());
    }

    // TC-SVC-011: publishAssignment missing WRITING → throws
    @Test
    void TC_SVC_011_publishAssignmentMissingSkillsThrows() {
        Quiz quiz = draftQuiz();
        when(quizRepository.findById(10)).thenReturn(Optional.of(quiz));
        when(quizQuestionRepository.countByQuizIdAndSkill(anyInt(), eq("LISTENING"))).thenReturn(5L);
        when(quizQuestionRepository.countByQuizIdAndSkill(anyInt(), eq("READING"))).thenReturn(3L);
        when(quizQuestionRepository.countByQuizIdAndSkill(anyInt(), eq("SPEAKING"))).thenReturn(2L);
        when(quizQuestionRepository.countByQuizIdAndSkill(anyInt(), eq("WRITING"))).thenReturn(0L);
        when(quizRepository.existsById(10)).thenReturn(true);

        InvalidDataException ex = assertThrows(InvalidDataException.class,
                () -> service.publishAssignment(10));
        assertTrue(ex.getMessage().contains("WRITING"));
    }

    // TC-SVC-012: publishAssignment on non-DRAFT → throws
    @Test
    void TC_SVC_012_publishAssignmentNonDraftThrows() {
        Quiz quiz = draftQuiz();
        quiz.setStatus("PUBLISHED");
        when(quizRepository.findById(10)).thenReturn(Optional.of(quiz));
        when(quizRepository.existsById(10)).thenReturn(true);

        InvalidDataException ex = assertThrows(InvalidDataException.class,
                () -> service.publishAssignment(10));
        assertTrue(ex.getMessage().contains("Only DRAFT"));
    }

    // TC-SVC-013: getAssignments filters by expertEmail and quizCategory
    @Test
    void TC_SVC_013_getAssignmentsFilters() {
        Quiz courseAssignment = draftQuiz();
        courseAssignment.setQuizCategory("COURSE_ASSIGNMENT");
        courseAssignment.setUser(expertUser());
        Quiz moduleAssignment = new Quiz();
        moduleAssignment.setQuizId(20);
        moduleAssignment.setQuizCategory("MODULE_ASSIGNMENT");
        moduleAssignment.setUser(expertUser());
        Quiz entryTest = new Quiz();
        entryTest.setQuizId(30);
        entryTest.setQuizCategory("ENTRY_TEST");
        entryTest.setUser(expertUser());
        when(quizRepository.findAll()).thenReturn(List.of(courseAssignment, moduleAssignment, entryTest));

        List<Quiz> result = service.getAssignments("expert@nova.com");

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(q ->
                "COURSE_ASSIGNMENT".equals(q.getQuizCategory()) ||
                "MODULE_ASSIGNMENT".equals(q.getQuizCategory())));
    }

    // TC-SVC-014: getPreview returns missingSkills list correctly
    @Test
    void TC_SVC_014_getPreviewMissingSkills() {
        Quiz quiz = draftQuiz();
        when(quizRepository.findById(10)).thenReturn(Optional.of(quiz));
        when(quizRepository.existsById(10)).thenReturn(true);
        when(quizQuestionRepository.countByQuizIdAndSkill(10, "LISTENING")).thenReturn(5L);
        when(quizQuestionRepository.countByQuizIdAndSkill(10, "READING")).thenReturn(0L);
        when(quizQuestionRepository.countByQuizIdAndSkill(10, "SPEAKING")).thenReturn(0L);
        when(quizQuestionRepository.countByQuizIdAndSkill(10, "WRITING")).thenReturn(4L);

        AssignmentPreviewDTO result = service.getPreview(10);

        assertTrue(result.getMissingSkills().contains("READING"));
        assertTrue(result.getMissingSkills().contains("SPEAKING"));
        assertFalse(result.getMissingSkills().contains("LISTENING"));
    }

    // TC-EDGE-004: createAssignment timeLimitPerSkill stored as valid JSON (deserializable)
    @Test
    void TC_EDGE_004_timeLimitStoredAsDeserializableJson() throws JsonProcessingException {
        when(userRepository.findByEmail("expert@nova.com")).thenReturn(Optional.of(expertUser()));
        Map<String, Integer> limits = Map.of("SPEAKING", 5, "WRITING", 15);
        QuizRequestDTO dto = QuizRequestDTO.builder()
                .title("Test")
                .quizCategory("COURSE_ASSIGNMENT")
                .timeLimitPerSkill(limits)
                .build();
        when(quizRepository.save(any(Quiz.class))).thenAnswer(inv -> {
            Quiz q = inv.getArgument(0);
            q.setQuizId(1);
            return q;
        });

        service.createAssignment(dto, "expert@nova.com");

        ArgumentCaptor<Quiz> captor = ArgumentCaptor.forClass(Quiz.class);
        verify(quizRepository).save(captor.capture());
        Quiz saved = captor.getValue();
        assertNotNull(saved.getTimeLimitPerSkill());
        // Verify it can be deserialized
        Map<String, Integer> parsed = objectMapper.readValue(saved.getTimeLimitPerSkill(),
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Integer>>() {});
        assertEquals(5, parsed.get("SPEAKING"));
        assertEquals(15, parsed.get("WRITING"));
    }

    // TC-EDGE-005: createAssignment always sets skillOrder to 4 skills in fixed order
    @Test
    void TC_EDGE_005_skillOrderFixedSequence() throws JsonProcessingException {
        when(userRepository.findByEmail("expert@nova.com")).thenReturn(Optional.of(expertUser()));
        when(quizRepository.save(any(Quiz.class))).thenAnswer(inv -> {
            Quiz q = inv.getArgument(0);
            q.setQuizId(1);
            return q;
        });

        Quiz result = service.createAssignment(validDTO(), "expert@nova.com");

        assertNotNull(result.getSkillOrder());
        // Should contain all 4 skills in order
        assertTrue(result.getSkillOrder().indexOf("LISTENING") < result.getSkillOrder().indexOf("READING"));
        assertTrue(result.getSkillOrder().indexOf("READING") < result.getSkillOrder().indexOf("SPEAKING"));
        assertTrue(result.getSkillOrder().indexOf("SPEAKING") < result.getSkillOrder().indexOf("WRITING"));
    }
}
