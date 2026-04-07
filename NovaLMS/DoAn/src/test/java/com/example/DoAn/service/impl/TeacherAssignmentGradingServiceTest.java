package com.example.DoAn.service.impl;

import com.example.DoAn.dto.request.AssignmentGradingRequestDTO;
import com.example.DoAn.dto.response.AssignmentGradingDetailDTO;
import com.example.DoAn.dto.response.AssignmentGradingQueueDTO;
import com.example.DoAn.exception.ResourceNotFoundException;
import com.example.DoAn.model.*;
import com.example.DoAn.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherAssignmentGradingServiceTest {

    @Mock private QuizResultRepository quizResultRepository;
    @Mock private QuizAnswerRepository quizAnswerRepository;
    @Mock private QuizQuestionRepository quizQuestionRepository;

    private ObjectMapper objectMapper;
    private TeacherAssignmentGradingServiceImpl service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new TeacherAssignmentGradingServiceImpl(
                quizResultRepository, quizAnswerRepository, quizQuestionRepository, objectMapper);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private QuizResult makeResult(Integer id, Quiz quiz, User user, Boolean passed) {
        QuizResult r = new QuizResult();
        r.setResultId(id);
        r.setQuiz(quiz);
        r.setUser(user);
        r.setPassed(passed);
        r.setScore(0);
        r.setSubmittedAt(LocalDateTime.now());
        return r;
    }

    private Quiz makeQuiz() {
        Quiz q = new Quiz();
        q.setQuizId(10);
        q.setTitle("Midterm");
        q.setQuizCategory("COURSE_ASSIGNMENT");
        Clazz c = new Clazz();
        c.setClassId(1);
        c.setClassName("Class A");
        q.setClazz(c);
        return q;
    }

    private User teacher() {
        User u = new User();
        u.setUserId(2);
        u.setEmail("teacher@nova.com");
        u.setFullName("Teacher");
        return u;
    }

    private User student() {
        User u = new User();
        u.setUserId(1);
        u.setEmail("student@nova.com");
        u.setFullName("Student");
        return u;
    }

    private QuizAnswer makeAnswer(Integer id, QuizResult result, String skill,
                                   Boolean isCorrect, String aiScore, BigDecimal ptsAwarded) {
        Question q = new Question();
        q.setQuestionId(id);
        q.setSkill(skill);
        q.setQuestionType("MULTIPLE_CHOICE_SINGLE");
        QuizAnswer a = new QuizAnswer();
        a.setAnswerId(id * 10);
        a.setQuizResult(result);
        a.setQuestion(q);
        a.setIsCorrect(isCorrect);
        a.setAiScore(aiScore);
        a.setPointsAwarded(ptsAwarded);
        return a;
    }

    // ─── Queue tests ─────────────────────────────────────────────────────────

    // TC-QUEUE-001: getGradingQueue returns only assignment results (filter by isSequential)
    // Note: filtering by isSequential is done in the repository query itself
    @Test
    void TC_QUEUE_001_returnsAssignmentResults() {
        Quiz quiz = makeQuiz();
        QuizResult r = makeResult(1, quiz, student(), null);
        Page<QuizResult> page = new PageImpl<>(List.of(r));
        when(quizResultRepository.findAssignmentResultsForTeacher(any(), any(), any(), any()))
                .thenReturn(page);
        when(quizAnswerRepository.findByQuizResultResultId(1)).thenReturn(List.of());

        Page<AssignmentGradingQueueDTO> result = service.getGradingQueue(
                "teacher@nova.com", null, null, null, PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().get(0).getResultId());
    }

    // TC-QUEUE-002: Queue with 5 LISTENING auto-graded, 2 SPEAKING pending AI → correct status
    @Test
    void TC_QUEUE_002_correctSectionStatuses() {
        QuizResult r = makeResult(1, makeQuiz(), student(), null);
        List<QuizAnswer> answers = new ArrayList<>();
        for (int i = 1; i <= 5; i++) answers.add(makeAnswer(i, r, "LISTENING", true, null, BigDecimal.ONE));
        answers.add(makeAnswer(6, r, "SPEAKING", null, null, null)); // AI pending
        answers.add(makeAnswer(7, r, "SPEAKING", null, null, null));
        Page<QuizResult> page = new PageImpl<>(List.of(r));
        when(quizResultRepository.findAssignmentResultsForTeacher(any(), any(), any(), any()))
                .thenReturn(page);
        when(quizAnswerRepository.findByQuizResultResultId(1)).thenReturn(answers);

        Page<AssignmentGradingQueueDTO> result = service.getGradingQueue(
                "teacher@nova.com", null, null, null, PageRequest.of(0, 20));

        AssignmentGradingQueueDTO dto = result.getContent().get(0);
        assertEquals("AUTO", dto.getListening().getGradingStatus());
        assertEquals("AI_PENDING", dto.getSpeaking().getGradingStatus());
    }

    // TC-QUEUE-003: autoScore = LISTENING + READING scores
    @Test
    void TC_QUEUE_003_autoScoreSum() {
        QuizResult r = makeResult(1, makeQuiz(), student(), null);
        List<QuizAnswer> answers = List.of(
                makeAnswer(1, r, "LISTENING", true, null, BigDecimal.valueOf(5)),
                makeAnswer(2, r, "LISTENING", false, null, BigDecimal.ZERO),
                makeAnswer(3, r, "READING", true, null, BigDecimal.valueOf(3))
        );
        Page<QuizResult> page = new PageImpl<>(List.of(r));
        when(quizResultRepository.findAssignmentResultsForTeacher(any(), any(), any(), any()))
                .thenReturn(page);
        when(quizAnswerRepository.findByQuizResultResultId(1)).thenReturn(answers);

        Page<AssignmentGradingQueueDTO> result = service.getGradingQueue(
                "teacher@nova.com", null, null, null, PageRequest.of(0, 20));

        AssignmentGradingQueueDTO dto = result.getContent().get(0);
        assertEquals(BigDecimal.valueOf(8).setScale(0), dto.getAutoScore().setScale(0));
    }

    // TC-QUEUE-004: deriveOverallStatus all GRADED → "ALL_GRADED"
    @Test
    void TC_QUEUE_004_overallStatusAllGraded() {
        QuizResult r = makeResult(1, makeQuiz(), student(), null);
        List<QuizAnswer> answers = List.of(
                makeAnswer(1, r, "SPEAKING", null, null, BigDecimal.valueOf(7)),
                makeAnswer(2, r, "WRITING", null, null, BigDecimal.valueOf(8))
        );
        Page<QuizResult> page = new PageImpl<>(List.of(r));
        when(quizResultRepository.findAssignmentResultsForTeacher(any(), any(), any(), any()))
                .thenReturn(page);
        when(quizAnswerRepository.findByQuizResultResultId(1)).thenReturn(answers);

        Page<AssignmentGradingQueueDTO> result = service.getGradingQueue(
                "teacher@nova.com", null, null, null, PageRequest.of(0, 20));

        assertEquals("ALL_GRADED", result.getContent().get(0).getOverallStatus());
    }

    // TC-QUEUE-005: deriveOverallStatus PENDING_BOTH → "PENDING_BOTH"
    @Test
    void TC_QUEUE_005_overallStatusPendingBoth() {
        QuizResult r = makeResult(1, makeQuiz(), student(), null);
        List<QuizAnswer> answers = List.of(
                makeAnswer(1, r, "SPEAKING", null, null, null), // no pts, no AI
                makeAnswer(2, r, "WRITING", null, null, null)
        );
        Page<QuizResult> page = new PageImpl<>(List.of(r));
        when(quizResultRepository.findAssignmentResultsForTeacher(any(), any(), any(), any()))
                .thenReturn(page);
        when(quizAnswerRepository.findByQuizResultResultId(1)).thenReturn(answers);

        Page<AssignmentGradingQueueDTO> result = service.getGradingQueue(
                "teacher@nova.com", null, null, null, PageRequest.of(0, 20));

        assertEquals("PENDING_BOTH", result.getContent().get(0).getOverallStatus());
    }

    // ─── Grading tests ────────────────────────────────────────────────────────

    // TC-GRADE-001: gradeAssignment saves pointsAwarded per question
    @Test
    void TC_GRADE_001_savesPointsAwarded() {
        QuizResult r = makeResult(1, makeQuiz(), student(), null);
        r.setQuiz(makeQuiz());
        when(quizResultRepository.findById(1)).thenReturn(Optional.of(r));
        QuizAnswer answer = makeAnswer(5, r, "SPEAKING", null, null, null);
        when(quizAnswerRepository.findByQuizResultResultIdAndQuestionQuestionId(1, 5))
                .thenReturn(answer);
        AssignmentGradingRequestDTO.QuestionGradingItem item =
                new AssignmentGradingRequestDTO.QuestionGradingItem();
        item.setQuestionId(5);
        item.setPointsAwarded(BigDecimal.valueOf(8));
        item.setTeacherNote("Good work");
        AssignmentGradingRequestDTO request = new AssignmentGradingRequestDTO();
        request.setGradingItems(List.of(item));
        request.setSectionScores(Map.of("SPEAKING", BigDecimal.valueOf(8)));

        service.gradeAssignment(1, request, "teacher@nova.com");

        verify(quizAnswerRepository).save(any(QuizAnswer.class));
    }

    // TC-GRADE-002: gradeAssignment recalculates total score from sectionScores
    @Test
    void TC_GRADE_002_recalculatesTotalScore() {
        QuizResult r = makeResult(1, makeQuiz(), student(), null);
        when(quizResultRepository.findById(1)).thenReturn(Optional.of(r));
        AssignmentGradingRequestDTO request = new AssignmentGradingRequestDTO();
        request.setSectionScores(Map.of(
                "LISTENING", BigDecimal.valueOf(8),
                "READING", BigDecimal.valueOf(7),
                "SPEAKING", BigDecimal.valueOf(9),
                "WRITING", BigDecimal.valueOf(6)
        ));

        service.gradeAssignment(1, request, "teacher@nova.com");

        ArgumentCaptor<QuizResult> captor = ArgumentCaptor.forClass(QuizResult.class);
        verify(quizResultRepository).save(captor.capture());
        assertEquals(30, captor.getValue().getScore()); // 8+7+9+6
    }

    // TC-GRADE-003: gradeAssignment sets correctRate and passed/fail
    @Test
    void TC_GRADE_003_setsCorrectRateAndPassed() {
        Quiz quiz = makeQuiz();
        quiz.setPassScore(BigDecimal.valueOf(70));
        QuizResult r = makeResult(1, quiz, student(), null);
        QuizQuestion qq = new QuizQuestion();
        qq.setPoints(BigDecimal.valueOf(40)); // max total
        when(quizResultRepository.findById(1)).thenReturn(Optional.of(r));
        when(quizQuestionRepository.findByQuizQuizId(10)).thenReturn(List.of(qq));
        AssignmentGradingRequestDTO request = new AssignmentGradingRequestDTO();
        request.setSectionScores(Map.of("SPEAKING", BigDecimal.valueOf(30)));

        service.gradeAssignment(1, request, "teacher@nova.com");

        ArgumentCaptor<QuizResult> captor = ArgumentCaptor.forClass(QuizResult.class);
        verify(quizResultRepository).save(captor.capture());
        assertEquals(BigDecimal.valueOf(75.00).setScale(2),
                captor.getValue().getCorrectRate().setScale(2));
        assertTrue(captor.getValue().getPassed());
    }

    // TC-GRADE-004: getGradingDetail returns all 4 skill sections with questions
    @Test
    void TC_GRADE_004_all4SkillSections() {
        Quiz quiz = makeQuiz();
        QuizResult r = makeResult(1, quiz, student(), null);
        List<QuizAnswer> answers = List.of(
                makeAnswer(1, r, "LISTENING", true, null, BigDecimal.ONE),
                makeAnswer(2, r, "READING", true, null, BigDecimal.ONE),
                makeAnswer(3, r, "SPEAKING", null, "8/10", null),
                makeAnswer(4, r, "WRITING", null, "7/10", null)
        );
        when(quizResultRepository.findById(1)).thenReturn(Optional.of(r));
        when(quizAnswerRepository.findByQuizResultResultId(1)).thenReturn(answers);
        when(quizQuestionRepository.findByQuizQuizIdAndQuestionQuestionId(anyInt(), anyInt()))
                .thenReturn(Optional.of(new QuizQuestion()));

        AssignmentGradingDetailDTO result = service.getGradingDetail(1, "teacher@nova.com");

        assertEquals(4, result.getSections().size());
        assertTrue(result.getSections().stream()
                .anyMatch(s -> "LISTENING".equals(s.getSkill())));
        assertTrue(result.getSections().stream()
                .anyMatch(s -> "WRITING".equals(s.getSkill())));
    }

    // TC-GRADE-005: getGradingDetail returns AI scores and feedback for SPEAKING/WRITING
    @Test
    void TC_GRADE_005_aiScoresForSpeakingWriting() {
        QuizResult r = makeResult(1, makeQuiz(), student(), null);
        QuizAnswer spkAnswer = makeAnswer(1, r, "SPEAKING", null, "9/10", null);
        spkAnswer.setAiFeedback("Good pronunciation");
        QuizAnswer wrtAnswer = makeAnswer(2, r, "WRITING", null, "7/10", null);
        wrtAnswer.setAiFeedback("Well structured");
        when(quizResultRepository.findById(1)).thenReturn(Optional.of(r));
        when(quizAnswerRepository.findByQuizResultResultId(1))
                .thenReturn(List.of(spkAnswer, wrtAnswer));
        when(quizQuestionRepository.findByQuizQuizIdAndQuestionQuestionId(anyInt(), anyInt()))
                .thenReturn(Optional.of(new QuizQuestion()));

        AssignmentGradingDetailDTO result = service.getGradingDetail(1, "teacher@nova.com");

        var speaking = result.getSections().stream()
                .filter(s -> "SPEAKING".equals(s.getSkill())).findFirst().orElseThrow();
        assertEquals("9/10", speaking.getAiScore());
        assertEquals("Good pronunciation", speaking.getAiFeedback());
    }

    // TC-GRADE-006: buildSectionStatus SPEAKING with ptsAwarded → gradingStatus=GRADED
    @Test
    void TC_GRADE_006_speakingGraded() {
        QuizResult r = makeResult(1, makeQuiz(), student(), null);
        QuizAnswer a = makeAnswer(1, r, "SPEAKING", null, null, BigDecimal.valueOf(8));
        when(quizResultRepository.findById(1)).thenReturn(Optional.of(r));
        when(quizAnswerRepository.findByQuizResultResultId(1)).thenReturn(List.of(a));

        // Via getGradingDetail since buildSectionStatus is private
        AssignmentGradingDetailDTO dto = service.getGradingDetail(1, "teacher@nova.com");
        var speaking = dto.getSections().get(0);
        assertEquals("GRADED", speaking.getGradingStatus());
    }

    // TC-GRADE-007: buildSectionStatus SPEAKING with aiScore but no ptsAwarded → gradingStatus=AI_READY
    @Test
    void TC_GRADE_007_speakingAiReady() {
        QuizResult r = makeResult(1, makeQuiz(), student(), null);
        QuizAnswer a = makeAnswer(1, r, "SPEAKING", null, "8/10", null);
        when(quizResultRepository.findById(1)).thenReturn(Optional.of(r));
        when(quizAnswerRepository.findByQuizResultResultId(1)).thenReturn(List.of(a));

        AssignmentGradingDetailDTO dto = service.getGradingDetail(1, "teacher@nova.com");
        var speaking = dto.getSections().get(0);
        assertEquals("AI_READY", speaking.getGradingStatus());
    }

    // TC-GRADE-008: buildSectionStatus LISTENING auto-graded → gradingStatus=AUTO
    @Test
    void TC_GRADE_008_listeningAuto() {
        QuizResult r = makeResult(1, makeQuiz(), student(), null);
        QuizAnswer a = makeAnswer(1, r, "LISTENING", true, null, BigDecimal.ONE);
        when(quizResultRepository.findById(1)).thenReturn(Optional.of(r));
        when(quizAnswerRepository.findByQuizResultResultId(1)).thenReturn(List.of(a));

        AssignmentGradingDetailDTO dto = service.getGradingDetail(1, "teacher@nova.com");
        var listening = dto.getSections().get(0);
        assertEquals("AUTO", listening.getGradingStatus());
    }

    // ─── Edge cases ──────────────────────────────────────────────────────────

    // TC-GRADE-EDGE-001: gradeAssignment on non-existent resultId → throws
    @Test
    void TC_GRADE_EDGE_001_resultNotFound() {
        when(quizResultRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.gradeAssignment(999, new AssignmentGradingRequestDTO(), "teacher@nova.com"));
    }

    // TC-GRADE-EDGE-002: getGradingDetail on non-existent resultId → throws
    @Test
    void TC_GRADE_EDGE_002_detailNotFound() {
        when(quizResultRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getGradingDetail(999, "teacher@nova.com"));
    }
}
