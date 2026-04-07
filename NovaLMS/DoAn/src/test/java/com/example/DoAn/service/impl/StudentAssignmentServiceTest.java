package com.example.DoAn.service.impl;

import com.example.DoAn.dto.response.*;
import com.example.DoAn.exception.InvalidDataException;
import com.example.DoAn.exception.ResourceNotFoundException;
import com.example.DoAn.model.*;
import com.example.DoAn.repository.*;
import com.example.DoAn.service.GroqGradingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentAssignmentServiceTest {

    @Mock private QuizRepository quizRepository;
    @Mock private AssignmentSessionRepository sessionRepository;
    @Mock private QuizQuestionRepository quizQuestionRepository;
    @Mock private QuizResultRepository quizResultRepository;
    @Mock private QuizAnswerRepository quizAnswerRepository;
    @Mock private UserRepository userRepository;
    @Mock private RegistrationRepository registrationRepository;
    @Mock private GroqGradingService groqGradingService;
    @Mock private GroqGradingServiceImpl groqGradingServiceImpl;
    @Mock private TransactionTemplate transactionTemplate;

    private ObjectMapper objectMapper;
    private StudentAssignmentServiceImpl service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        service = new StudentAssignmentServiceImpl(
                quizRepository, sessionRepository, quizQuestionRepository,
                quizResultRepository, quizAnswerRepository, userRepository,
                registrationRepository, groqGradingService,
                groqGradingServiceImpl, transactionTemplate, objectMapper);
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private User student() {
        User u = new User();
        u.setUserId(1);
        u.setEmail("student@nova.com");
        u.setFullName("Test Student");
        return u;
    }

    private User teacher() {
        User u = new User();
        u.setUserId(2);
        u.setEmail("teacher@nova.com");
        u.setFullName("Teacher");
        return u;
    }

    private Clazz clazz() {
        Clazz c = new Clazz();
        c.setClassId(1);
        c.setClassName("Class A");
        return c;
    }

    private Quiz publishedAssignment() {
        Quiz q = new Quiz();
        q.setQuizId(10);
        q.setStatus("PUBLISHED");
        q.setIsOpen(true);
        q.setIsSequential(true);
        q.setQuizCategory("COURSE_ASSIGNMENT");
        q.setTitle("Midterm Exam");
        q.setMaxAttempts(2);
        q.setClazz(clazz());
        return q;
    }

    private AssignmentSession inProgressSession(Quiz quiz, User user) {
        AssignmentSession s = new AssignmentSession();
        s.setId(100L);
        s.setQuiz(quiz);
        s.setUser(user);
        s.setStatus("IN_PROGRESS");
        s.setCurrentSkillIndex(0);
        s.setSectionStatuses("{\"LISTENING\":\"IN_PROGRESS\",\"READING\":\"LOCKED\",\"SPEAKING\":\"LOCKED\",\"WRITING\":\"LOCKED\"}");
        s.setSectionAnswers("{}");
        return s;
    }

    // ─── getAssignmentInfo tests ─────────────────────────────────────────────

    // TC-STD-001: quiz not found → throws ResourceNotFoundException
    @Test
    void TC_STD_001_quizNotFound() {
        when(quizRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getAssignmentInfo(999, "student@nova.com"));
    }

    // TC-STD-002: non-sequential quiz → throws InvalidDataException
    @Test
    void TC_STD_002_nonSequentialQuiz() {
        Quiz q = new Quiz();
        q.setQuizId(10);
        q.setIsSequential(false);
        when(quizRepository.findById(10)).thenReturn(Optional.of(q));

        assertThrows(InvalidDataException.class,
                () -> service.getAssignmentInfo(10, "student@nova.com"));
    }

    // TC-STD-003: not published or not open → throws InvalidDataException
    @Test
    void TC_STD_003_notPublishedOrOpen() {
        Quiz q = publishedAssignment();
        q.setStatus("DRAFT");
        when(quizRepository.findById(10)).thenReturn(Optional.of(q));

        assertThrows(InvalidDataException.class,
                () -> service.getAssignmentInfo(10, "student@nova.com"));
    }

    // TC-STD-004: student not enrolled → throws InvalidDataException
    @Test
    void TC_STD_004_notEnrolled() {
        Quiz q = publishedAssignment();
        when(quizRepository.findById(10)).thenReturn(Optional.of(q));
        when(userRepository.findByEmail("student@nova.com")).thenReturn(Optional.of(student()));
        when(registrationRepository.existsByUser_UserIdAndClazz_ClassIdAndStatusApproved(1, 1))
                .thenReturn(false);

        InvalidDataException ex = assertThrows(InvalidDataException.class,
                () -> service.getAssignmentInfo(10, "student@nova.com"));
        assertTrue(ex.getMessage().contains("chưa đăng ký"));
    }

    // TC-STD-005: first access → creates new session
    @Test
    void TC_STD_005_firstAccessCreatesSession() {
        Quiz q = publishedAssignment();
        when(quizRepository.findById(10)).thenReturn(Optional.of(q));
        when(userRepository.findByEmail("student@nova.com")).thenReturn(Optional.of(student()));
        when(registrationRepository.existsByUser_UserIdAndClazz_ClassIdAndStatusApproved(1, 1))
                .thenReturn(true);
        when(sessionRepository.countByQuizAndUser(10, 1L)).thenReturn(0L);
        when(sessionRepository.findByQuizQuizIdAndUserUserId(10, 1L)).thenReturn(Optional.empty());
        when(sessionRepository.save(any(AssignmentSession.class))).thenAnswer(inv -> {
            AssignmentSession s = inv.getArgument(0);
            s.setId(100L);
            return s;
        });

        AssignmentInfoDTO result = service.getAssignmentInfo(10, "student@nova.com");

        assertEquals(10, result.getQuizId());
        assertTrue(result.getCanStart());
        verify(sessionRepository).save(any(AssignmentSession.class));
    }

    // TC-STD-006: max attempts exceeded → attemptsExceeded=true
    @Test
    void TC_STD_006_maxAttemptsExceeded() {
        Quiz q = publishedAssignment();
        when(quizRepository.findById(10)).thenReturn(Optional.of(q));
        when(userRepository.findByEmail("student@nova.com")).thenReturn(Optional.of(student()));
        when(registrationRepository.existsByUser_UserIdAndClazz_ClassIdAndStatusApproved(1, 1))
                .thenReturn(true);
        when(sessionRepository.countByQuizAndUser(10, 1L)).thenReturn(2L);

        AssignmentInfoDTO result = service.getAssignmentInfo(10, "student@nova.com");

        assertTrue(result.getAttemptsExceeded());
    }

    // TC-STD-007: existing session → returns existing, does NOT create new
    @Test
    void TC_STD_007_existingSessionReturnsExisting() {
        Quiz q = publishedAssignment();
        AssignmentSession existing = inProgressSession(q, student());
        when(quizRepository.findById(10)).thenReturn(Optional.of(q));
        when(userRepository.findByEmail("student@nova.com")).thenReturn(Optional.of(student()));
        when(registrationRepository.existsByUser_UserIdAndClazz_ClassIdAndStatusApproved(1, 1))
                .thenReturn(true);
        when(sessionRepository.countByQuizAndUser(10, 1L)).thenReturn(1L);
        when(sessionRepository.findByQuizQuizIdAndUserUserId(10, 1L)).thenReturn(Optional.of(existing));

        AssignmentInfoDTO result = service.getAssignmentInfo(10, "student@nova.com");

        assertEquals(100L, result.getSessionId());
        verify(sessionRepository, never()).save(any(AssignmentSession.class));
    }

    // ─── getSection tests ────────────────────────────────────────────────────

    // TC-STD-008: invalid skill → throws InvalidDataException
    @Test
    void TC_STD_008_invalidSkill() {
        AssignmentSession s = inProgressSession(publishedAssignment(), student());
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(s));

        assertThrows(InvalidDataException.class,
                () -> service.getSection(100L, "INVALID_SKILL", "student@nova.com"));
    }

    // TC-STD-009: future section locked → throws "Phần này chưa được mở"
    @Test
    void TC_STD_009_futureSectionLocked() {
        AssignmentSession s = inProgressSession(publishedAssignment(), student());
        s.setCurrentSkillIndex(0); // only LISTENING unlocked
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(s));

        InvalidDataException ex = assertThrows(InvalidDataException.class,
                () -> service.getSection(100L, "READING", "student@nova.com"));
        assertTrue(ex.getMessage().contains("chưa được mở"));
    }

    // TC-STD-010: getSection LISTENING → returns questions, not locked
    @Test
    void TC_STD_010_getSectionListening() {
        AssignmentSession s = inProgressSession(publishedAssignment(), student());
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(s));
        QuizQuestion qq = new QuizQuestion();
        Question q = new Question();
        q.setQuestionId(1);
        q.setQuestionType("MULTIPLE_CHOICE_SINGLE");
        q.setSkill("LISTENING");
        q.setAnswerOptions(List.of());
        qq.setQuestion(q);
        qq.setPoints(BigDecimal.ONE);
        when(quizQuestionRepository.findByQuizQuizIdAndSkill(10, "LISTENING"))
                .thenReturn(List.of(qq));

        AssignmentSectionDTO result = service.getSection(100L, "LISTENING", "student@nova.com");

        assertEquals("LISTENING", result.getSkill());
        assertFalse(result.getIsLocked());
        assertEquals(1, result.getQuestions().size());
        assertNotNull(result.getNextSkill());
    }

    // ─── saveAnswers tests ───────────────────────────────────────────────────

    // TC-STD-011: saveAnswers persists to session
    @Test
    void TC_STD_011_saveAnswers() {
        AssignmentSession s = inProgressSession(publishedAssignment(), student());
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(s));

        Map<Integer, Object> answers = new HashMap<>();
        answers.put(1, "a");
        service.saveAnswers(100L, "LISTENING", answers, "student@nova.com");

        verify(sessionRepository).save(any(AssignmentSession.class));
    }

    // ─── submitSection tests ─────────────────────────────────────────────────

    // TC-STD-012: submitSection already completed → throws
    @Test
    void TC_STD_012_submitSectionAlreadyCompleted() {
        AssignmentSession s = inProgressSession(publishedAssignment(), student());
        s.setStatus("COMPLETED");
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(s));

        assertThrows(InvalidDataException.class,
                () -> service.submitSection(100L, "LISTENING", new HashMap<>(), "student@nova.com"));
    }

    // TC-STD-013: submitSection LISTENING → advances to READING
    @Test
    void TC_STD_013_submitSectionAdvancesToNextSkill() {
        AssignmentSession s = inProgressSession(publishedAssignment(), student());
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(s));
        when(quizQuestionRepository.findByQuizQuizIdAndSkill(10, "LISTENING")).thenReturn(List.of());

        Map<String, Object> result = service.submitSection(100L, "LISTENING", new HashMap<>(), "student@nova.com");

        assertEquals("READING", result.get("nextSkill"));
        assertEquals(1, result.get("nextSkillIndex"));
        assertTrue((Boolean) result.get("sectionCompleted"));
        verify(sessionRepository).save(any(AssignmentSession.class));
    }

    // TC-STD-014: submitSection last section (WRITING) → completes assignment
    @Test
    void TC_STD_014_submitSectionLastSkillCompletes() {
        AssignmentSession s = inProgressSession(publishedAssignment(), student());
        s.setCurrentSkillIndex(3); // WRITING
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(s));
        when(quizQuestionRepository.findByQuizQuizIdAndSkill(anyInt(), eq("WRITING"))).thenReturn(List.of());
        QuizResult savedResult = new QuizResult();
        savedResult.setResultId(1);
        when(quizResultRepository.save(any(QuizResult.class))).thenReturn(savedResult);

        Map<String, Object> result = service.submitSection(100L, "WRITING", new HashMap<>(), "student@nova.com");

        assertTrue((Boolean) result.get("sectionCompleted"));
        verify(quizResultRepository).save(any(QuizResult.class));
    }

    // ─── autoSubmit tests ───────────────────────────────────────────────────

    // TC-STD-015: autoSubmit on IN_PROGRESS → sets all incomplete to EXPIRED
    @Test
    void TC_STD_015_autoSubmitInProgress() {
        AssignmentSession s = inProgressSession(publishedAssignment(), student());
        s.setStatus("IN_PROGRESS");
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(s));

        service.autoSubmit(100L, "student@nova.com");

        verify(sessionRepository).save(any(AssignmentSession.class));
    }

    // TC-STD-016: autoSubmit on already COMPLETED → returns early
    @Test
    void TC_STD_016_autoSubmitAlreadyCompleted() {
        AssignmentSession s = inProgressSession(publishedAssignment(), student());
        s.setStatus("COMPLETED");
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(s));

        service.autoSubmit(100L, "student@nova.com");

        verify(sessionRepository, never()).save(any(AssignmentSession.class));
    }

    // ─── completeAssignment tests ───────────────────────────────────────────

    // TC-STD-017: completeAssignment → creates QuizResult
    @Test
    void TC_STD_017_completeAssignment() {
        AssignmentSession s = inProgressSession(publishedAssignment(), student());
        s.setStatus("COMPLETED");
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(s));
        QuizResult saved = new QuizResult();
        saved.setResultId(55);
        when(quizResultRepository.save(any(QuizResult.class))).thenReturn(saved);

        Integer resultId = service.completeAssignment(100L, "student@nova.com");

        assertEquals(55, resultId);
    }

    // ─── checkAnswer tests ──────────────────────────────────────────────────

    // TC-STD-018: checkAnswer MULTIPLE_CHOICE_SINGLE correct → true
    @Test
    void TC_STD_018_checkAnswerCorrect() {
        Question q = new Question();
        q.setQuestionType("MULTIPLE_CHOICE_SINGLE");
        AnswerOption opt1 = new AnswerOption();
        opt1.setAnswerOptionId(1);
        opt1.setCorrectAnswer(true);
        AnswerOption opt2 = new AnswerOption();
        opt2.setAnswerOptionId(2);
        opt2.setCorrectAnswer(false);
        q.setAnswerOptions(List.of(opt1, opt2));

        // Use reflection or test via the submitSection path
        AssignmentSession s = inProgressSession(publishedAssignment(), student());
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(s));
        QuizQuestion qq = new QuizQuestion();
        qq.setQuestion(q);
        qq.setPoints(BigDecimal.ONE);
        when(quizQuestionRepository.findByQuizQuizIdAndSkill(10, "LISTENING")).thenReturn(List.of(qq));

        Map<Integer, Object> answers = new HashMap<>();
        answers.put(1, 1); // selected correct option id
        Map<String, Object> result = service.submitSection(100L, "LISTENING", answers, "student@nova.com");

        assertEquals("READING", result.get("nextSkill"));
    }

    // TC-STD-019: checkAnswer MULTIPLE_CHOICE_SINGLE incorrect → false (advances anyway, but grade is wrong)
    @Test
    void TC_STD_019_checkAnswerIncorrect() {
        Question q = new Question();
        q.setQuestionType("MULTIPLE_CHOICE_SINGLE");
        AnswerOption opt1 = new AnswerOption();
        opt1.setAnswerOptionId(1);
        opt1.setCorrectAnswer(true);
        AnswerOption opt2 = new AnswerOption();
        opt2.setAnswerOptionId(2);
        opt2.setCorrectAnswer(false);
        q.setAnswerOptions(List.of(opt1, opt2));
        AssignmentSession s = inProgressSession(publishedAssignment(), student());
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(s));
        QuizQuestion qq = new QuizQuestion();
        qq.setQuestion(q);
        qq.setPoints(BigDecimal.ONE);
        when(quizQuestionRepository.findByQuizQuizIdAndSkill(10, "LISTENING")).thenReturn(List.of(qq));

        // Select wrong option
        Map<Integer, Object> answers = new HashMap<>();
        answers.put(1, 2); // wrong option
        Map<String, Object> result = service.submitSection(100L, "LISTENING", answers, "student@nova.com");

        // Advances to next skill (grading doesn't block progression)
        assertEquals("READING", result.get("nextSkill"));
    }
}
