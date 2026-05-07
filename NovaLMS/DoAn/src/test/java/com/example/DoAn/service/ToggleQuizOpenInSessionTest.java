package com.example.DoAn.service.impl;

import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.model.*;
import com.example.DoAn.repository.*;
import com.example.DoAn.service.EmailService;
import com.example.DoAn.service.INotificationService;
import com.example.DoAn.service.TeacherQuizService;
import com.example.DoAn.service.TeacherScheduleConflictService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * JUnit 5 Unit Tests for toggleQuizOpenInSession()
 * Test Matrix ID: toggleQuizOpenInSession_MATRIX
 * Total Test Cases: 6 (4 Abnormal + 2 Normal)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Test: toggleQuizOpenInSession()")
class ToggleQuizOpenInSessionTest {

    @Mock
    private ClassSessionRepository classSessionRepository;

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClazzRepository clazzRepository;

    @Mock
    private SessionQuizRepository sessionQuizRepository;

    @Mock
    private QuizQuestionRepository quizQuestionRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private ModuleRepository moduleRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private SessionLessonRepository sessionLessonRepository;

    @Mock
    private AssignmentSessionRepository assignmentSessionRepository;

    @Mock
    private UserLessonRepository userLessonRepository;

    @Mock
    private INotificationService notificationService;

    @Mock
    private EmailService emailService;

    @Mock
    private TeacherScheduleConflictService teacherScheduleConflictService;

    @Mock
    private TeacherQuizService teacherQuizService;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private com.example.DoAn.service.TeacherClassSessionService teacherClassSessionService;

    private User testTeacher;
    private Clazz testClazz;
    private Course testCourse;
    private ClassSession testSession;
    private Quiz testQuiz;
    private SessionQuiz testSessionQuiz;

    @BeforeEach
    void setUp() {
        // Build test teacher
        testTeacher = User.builder()
                .userId(1)
                .email("teacher@example.com")
                .fullName("Test Teacher")
                .build();

        // Build test course
        testCourse = Course.builder()
                .courseId(1)
                .title("English 101")
                .build();

        // Build test class
        testClazz = Clazz.builder()
                .classId(1)
                .className("Class 1")
                .course(testCourse)
                .teacher(testTeacher)
                .build();

        // Build test session
        testSession = ClassSession.builder()
                .sessionId(1)
                .sessionNumber(1)
                .sessionDate(LocalDate.now().plusDays(1))
                .startTime("09:00")
                .endTime("11:00")
                .clazz(testClazz)
                .build();

        // Build test quiz
        testQuiz = Quiz.builder()
                .quizId(1)
                .title("Test Quiz")
                .status("PUBLISHED")
                .timeLimitMinutes(60)
                .isOpen(false)
                .build();

        // Build test session_quiz
        testSessionQuiz = SessionQuiz.builder()
                .id(1)
                .session(testSession)
                .quiz(testQuiz)
                .isOpen(false)
                .orderIndex(1)
                .build();
    }

    // Helper method to mock teacher authentication
    private void mockTeacherAuth(String email) {
        when(entityManager.createQuery(anyString(), eq(Integer.class)))
                .thenAnswer(invocation -> {
                    var query = mock(jakarta.persistence.Query.class);
                    when(query.setParameter(anyString(), any())).thenReturn(query);
                    when(query.setMaxResults(anyInt())).thenReturn(query);
                    when(query.getResultList()).thenReturn(List.of(1));
                    return query;
                });
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST GROUP 1: T1 - Session not found (Abnormal)
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("T1: Session not found (Abnormal)")
    class T1_SessionNotFound {

        @Test
        @DisplayName("Should return error when session does not exist")
        void testSessionNotFound() {
            // Arrange
            mockTeacherAuth(testTeacher.getEmail());
            when(classSessionRepository.findById(999)).thenReturn(Optional.empty());

            // Act
            ResponseData<Map<String, Object>> result = teacherClassSessionService.toggleQuizOpenInSession(
                    testTeacher.getEmail(), 999, 1, null);

            // Assert
            assertNotNull(result);
            assertEquals(401, result.getStatus());
            assertTrue(result.getMessage().contains("Không tìm thấy buổi học"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST GROUP 2: T2 - Quiz not in session (Abnormal)
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("T2: Quiz not in session (Abnormal)")
    class T2_QuizNotInSession {

        @Test
        @DisplayName("Should return error when quiz is not in this session")
        void testQuizNotInSession() {
            // Arrange
            mockTeacherAuth(testTeacher.getEmail());
            when(classSessionRepository.findById(testSession.getSessionId()))
                    .thenReturn(Optional.of(testSession));
            when(sessionQuizRepository.findBySessionSessionIdAndQuizQuizId(testSession.getSessionId(), 999))
                    .thenReturn(Optional.empty());

            // Act
            ResponseData<Map<String, Object>> result = teacherClassSessionService.toggleQuizOpenInSession(
                    testTeacher.getEmail(), testSession.getSessionId(), 999, null);

            // Assert
            assertNotNull(result);
            assertEquals(404, result.getStatus());
            assertTrue(result.getMessage().contains("Quiz không tồn tại trong buổi học"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST GROUP 3: T3 - Teacher not authorized (Abnormal)
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("T3: Teacher not authorized (Abnormal)")
    class T3_NotAuthorized {

        @Test
        @DisplayName("Should return error when teacher is not authorized for this session")
        void testTeacherNotAuthorized() {
            // Arrange
            when(entityManager.createQuery(anyString(), eq(Integer.class)))
                    .thenAnswer(invocation -> {
                        var query = mock(jakarta.persistence.Query.class);
                        when(query.setParameter(anyString(), any())).thenReturn(query);
                        when(query.setMaxResults(anyInt())).thenReturn(query);
                        when(query.getResultList()).thenReturn(List.of(999)); // Different teacher ID
                        return query;
                    });

            // Act
            ResponseData<Map<String, Object>> result = teacherClassSessionService.toggleQuizOpenInSession(
                    "other@example.com", testSession.getSessionId(), testQuiz.getQuizId(), null);

            // Assert
            assertNotNull(result);
            assertEquals(401, result.getStatus());
            assertTrue(result.getMessage().contains("Không tìm thấy buổi học hoặc không có quyền"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST GROUP 4: T4 - Time limit too short (Abnormal)
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("T4: Time limit too short (Abnormal)")
    class T4_TimeLimitTooShort {

        @Test
        @DisplayName("Should return error when time limit is less than quiz minimum duration")
        void testTimeLimitTooShort() {
            // Arrange
            mockTeacherAuth(testTeacher.getEmail());
            when(classSessionRepository.findById(testSession.getSessionId()))
                    .thenReturn(Optional.of(testSession));
            when(sessionQuizRepository.findBySessionSessionIdAndQuizQuizId(testSession.getSessionId(), testQuiz.getQuizId()))
                    .thenReturn(Optional.of(testSessionQuiz));

            // Quiz requires 60 minutes but teacher sets only 30
            // Act
            ResponseData<Map<String, Object>> result = teacherClassSessionService.toggleQuizOpenInSession(
                    testTeacher.getEmail(), testSession.getSessionId(), testQuiz.getQuizId(), 30);

            // Assert
            assertNotNull(result);
            assertEquals(400, result.getStatus());
            assertTrue(result.getMessage().contains("Thời gian làm bài không được nhỏ hơn"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST GROUP 5: T5 - Successfully open quiz (Normal)
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("T5: Successfully open quiz (Normal)")
    class T5_SuccessfullyOpenQuiz {

        @Test
        @DisplayName("Should successfully open quiz when all conditions are met")
        void testSuccessfullyOpenQuiz() {
            // Arrange
            mockTeacherAuth(testTeacher.getEmail());
            when(classSessionRepository.findById(testSession.getSessionId()))
                    .thenReturn(Optional.of(testSession));
            when(sessionQuizRepository.findBySessionSessionIdAndQuizQuizId(testSession.getSessionId(), testQuiz.getQuizId()))
                    .thenReturn(Optional.of(testSessionQuiz));
            when(sessionQuizRepository.save(any(SessionQuiz.class))).thenReturn(testSessionQuiz);

            // Act
            ResponseData<Map<String, Object>> result = teacherClassSessionService.toggleQuizOpenInSession(
                    testTeacher.getEmail(), testSession.getSessionId(), testQuiz.getQuizId(), null);

            // Assert
            assertNotNull(result);
            assertEquals(200, result.getStatus());
            assertTrue(result.getMessage().contains("Quiz đã được mở"));
        }

        @Test
        @DisplayName("Should successfully open quiz with custom time limit")
        void testSuccessfullyOpenQuizWithCustomTimeLimit() {
            // Arrange
            mockTeacherAuth(testTeacher.getEmail());
            when(classSessionRepository.findById(testSession.getSessionId()))
                    .thenReturn(Optional.of(testSession));
            when(sessionQuizRepository.findBySessionSessionIdAndQuizQuizId(testSession.getSessionId(), testQuiz.getQuizId()))
                    .thenReturn(Optional.of(testSessionQuiz));
            when(sessionQuizRepository.save(any(SessionQuiz.class))).thenReturn(testSessionQuiz);
            when(quizRepository.save(any(Quiz.class))).thenReturn(testQuiz);

            // Act
            ResponseData<Map<String, Object>> result = teacherClassSessionService.toggleQuizOpenInSession(
                    testTeacher.getEmail(), testSession.getSessionId(), testQuiz.getQuizId(), 90);

            // Assert
            assertNotNull(result);
            assertEquals(200, result.getStatus());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST GROUP 6: T6 - Successfully close quiz (Normal)
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("T6: Successfully close quiz (Normal)")
    class T6_SuccessfullyCloseQuiz {

        @Test
        @DisplayName("Should successfully close quiz that is currently open")
        void testSuccessfullyCloseQuiz() {
            // Arrange - Quiz is currently open
            testSessionQuiz.setIsOpen(true);

            mockTeacherAuth(testTeacher.getEmail());
            when(classSessionRepository.findById(testSession.getSessionId()))
                    .thenReturn(Optional.of(testSession));
            when(sessionQuizRepository.findBySessionSessionIdAndQuizQuizId(testSession.getSessionId(), testQuiz.getQuizId()))
                    .thenReturn(Optional.of(testSessionQuiz));
            when(sessionQuizRepository.save(any(SessionQuiz.class))).thenReturn(testSessionQuiz);

            // Act
            ResponseData<Map<String, Object>> result = teacherClassSessionService.toggleQuizOpenInSession(
                    testTeacher.getEmail(), testSession.getSessionId(), testQuiz.getQuizId(), null);

            // Assert
            assertNotNull(result);
            assertEquals(200, result.getStatus());
            assertTrue(result.getMessage().contains("Quiz đã được đóng"));
        }
    }
}
