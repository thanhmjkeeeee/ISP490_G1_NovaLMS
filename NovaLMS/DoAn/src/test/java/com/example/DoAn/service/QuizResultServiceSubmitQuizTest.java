package com.example.DoAn.service;

import com.example.DoAn.model.*;
import com.example.DoAn.repository.*;
import com.example.DoAn.service.impl.QuizResultServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class QuizResultServiceSubmitQuizTest {

    @Mock private QuizRepository quizRepository;
    @Mock private QuizResultRepository quizResultRepository;
    @Mock private QuizAnswerRepository quizAnswerRepository;
    @Mock private UserRepository userRepository;
    @Mock private ObjectMapper objectMapper;
    @Mock private EmailService emailService;
    @Mock private INotificationService notificationService;
    @Mock private GroqGradingService groqGradingService;
    @Mock private LessonRepository lessonRepository;
    @Mock private LearningService learningService;
    @Mock private LessonQuizService lessonQuizService;
    @Mock private ClazzRepository clazzRepository;
    @Mock private SessionQuizRepository sessionQuizRepository;
    @Mock private QuizQuestionRepository quizQuestionRepository;
    @Mock private RegistrationRepository registrationRepository;

    @InjectMocks
    @Spy
    private QuizResultServiceImpl quizResultService;

    private User user;
    private Quiz quiz;
    private final String EMAIL = "teststudent@gmail.com";
    private final Integer QUIZ_ID = 1;

    @BeforeEach
    void setUp() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.initSynchronization();
        }

        user = new User();
        user.setUserId(1);
        user.setEmail(EMAIL);
        user.setFullName("Test Student");

        quiz = new Quiz();
        quiz.setQuizId(QUIZ_ID);
        quiz.setMaxAttempts(3);
        quiz.setQuizQuestions(new ArrayList<>());

        ReflectionTestUtils.setField(quizResultService, "lessonQuizService", lessonQuizService);
        ReflectionTestUtils.setField(quizResultService, "groqGradingService", groqGradingService);
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clear();
        }
    }

    @Test
    void TC01_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            quizResultService.submitQuiz(QUIZ_ID, EMAIL, null);
        });
        assertEquals("Không tìm thấy người dùng", exception.getMessage());
    }

    @Test
    void TC02_QuizNotFound_ThrowsException() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            quizResultService.submitQuiz(QUIZ_ID, EMAIL, null);
        });
        assertEquals("Không tìm thấy bài kiểm tra", exception.getMessage());
    }

    @Test
    void TC03_MaxAttemptsReached_ThrowsException() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(quiz));
        when(quizResultRepository.countByQuizQuizIdAndUserUserIdAndStatusNot(QUIZ_ID, user.getUserId(), "IN_PROGRESS"))
                .thenReturn(3L); // max is 3

        Exception exception = assertThrows(RuntimeException.class, () -> {
            quizResultService.submitQuiz(QUIZ_ID, EMAIL, null);
        });
        assertTrue(exception.getMessage().contains("Bạn đã hết lượt làm bài"));
    }

    @Test
    void TC04_AutoGraded_Passed_CompletesLesson() throws Exception {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        quiz.setPassScore(BigDecimal.valueOf(50));
        Lesson lesson = new Lesson();
        lesson.setLessonId(5);
        quiz.setLesson(lesson);
        when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(quiz));
        when(quizResultRepository.countByQuizQuizIdAndUserUserIdAndStatusNot(QUIZ_ID, user.getUserId(), "IN_PROGRESS"))
                .thenReturn(0L);

        doNothing().when(quizResultService).recalculateQuizResult(anyInt());

        Question q = new Question();
        q.setQuestionId(10);
        q.setQuestionType("MULTIPLE_CHOICE_SINGLE");
        AnswerOption opt1 = new AnswerOption();
        opt1.setAnswerOptionId(100);
        opt1.setCorrectAnswer(true);
        q.setAnswerOptions(List.of(opt1));

        QuizQuestion qq = new QuizQuestion();
        qq.setQuestion(q);
        qq.setPoints(BigDecimal.valueOf(10));
        quiz.getQuizQuestions().add(qq);

        QuizResult mockResult = new QuizResult();
        mockResult.setResultId(999);
        mockResult.setPassed(true);
        mockResult.setScore(10);

        when(quizResultRepository.save(any(QuizResult.class))).thenReturn(mockResult);
        when(quizResultRepository.findById(999)).thenReturn(Optional.of(mockResult));
        when(quizAnswerRepository.save(any(QuizAnswer.class))).thenAnswer(i -> i.getArgument(0));
        when(lessonRepository.findByQuizId(QUIZ_ID)).thenReturn(Optional.of(lesson));

        Map<Integer, Object> answers = new HashMap<>();
        answers.put(10, 100);

        Integer resultId = quizResultService.submitQuiz(QUIZ_ID, EMAIL, answers);

        assertEquals(999, resultId);
        verify(quizAnswerRepository, times(1)).save(argThat(ans -> Boolean.TRUE.equals(ans.getIsCorrect())));
        verify(emailService, times(1)).sendQuizResultEmail(eq(EMAIL), anyString(), anyString(), anyString(), anyString(), eq("Dat"));
        verify(learningService, times(1)).markLessonCompleted(5, EMAIL);
    }

    @Test
    void TC05_AutoGraded_Failed_UpdatesProgressOnly() throws Exception {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        quiz.setPassScore(BigDecimal.valueOf(50));
        Lesson lesson = new Lesson();
        lesson.setLessonId(5);
        quiz.setLesson(lesson);
        when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(quiz));
        when(quizResultRepository.countByQuizQuizIdAndUserUserIdAndStatusNot(QUIZ_ID, user.getUserId(), "IN_PROGRESS"))
                .thenReturn(0L);

        doNothing().when(quizResultService).recalculateQuizResult(anyInt());

        Question q = new Question();
        q.setQuestionId(11);
        q.setQuestionType("FILL_IN_BLANK");
        AnswerOption opt1 = new AnswerOption();
        opt1.setCorrectAnswer(true);
        opt1.setTitle("hello");
        q.setAnswerOptions(List.of(opt1));

        QuizQuestion qq = new QuizQuestion();
        qq.setQuestion(q);
        qq.setPoints(BigDecimal.valueOf(10));
        quiz.getQuizQuestions().add(qq);

        QuizResult mockResult = new QuizResult();
        mockResult.setResultId(888);
        mockResult.setPassed(false);
        mockResult.setScore(0);

        when(quizResultRepository.save(any(QuizResult.class))).thenReturn(mockResult);
        when(quizResultRepository.findById(888)).thenReturn(Optional.of(mockResult));
        when(quizAnswerRepository.save(any(QuizAnswer.class))).thenAnswer(i -> i.getArgument(0));

        Map<Integer, Object> answers = new HashMap<>();
        answers.put(11, "wrong_answer");

        Integer resultId = quizResultService.submitQuiz(QUIZ_ID, EMAIL, answers);

        assertEquals(888, resultId);
        verify(quizAnswerRepository, times(1)).save(argThat(ans -> Boolean.FALSE.equals(ans.getIsCorrect())));
        verify(emailService, times(1)).sendQuizResultEmail(eq(EMAIL), anyString(), anyString(), anyString(), anyString(), eq("Khong dat"));
        verify(learningService, never()).markLessonCompleted(anyInt(), anyString());
        verify(lessonQuizService, times(1)).updateProgressAfterSubmit(eq(5), eq(QUIZ_ID), eq(1), anyDouble(), eq(false));
    }

    @Test
    void TC06_ManualGraded_PendingAI_NoEmail() throws Exception {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        quiz.setPassScore(BigDecimal.valueOf(50));
        when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(quiz));
        when(quizResultRepository.countByQuizQuizIdAndUserUserIdAndStatusNot(QUIZ_ID, user.getUserId(), "IN_PROGRESS"))
                .thenReturn(0L);

        doNothing().when(quizResultService).recalculateQuizResult(anyInt());

        Question q = new Question();
        q.setQuestionId(20);
        q.setQuestionType("SPEAKING");
        
        QuizQuestion qq = new QuizQuestion();
        qq.setQuestion(q);
        qq.setPoints(BigDecimal.valueOf(5));
        quiz.getQuizQuestions().add(qq);

        QuizResult mockResult = new QuizResult();
        mockResult.setResultId(777);
        // Khi nộp bài tự luận, kết quả chưa thể xác định được ngay (passed = null)
        mockResult.setPassed(null);

        when(quizResultRepository.save(any(QuizResult.class))).thenReturn(mockResult);
        when(quizResultRepository.findById(777)).thenReturn(Optional.of(mockResult));
        when(quizAnswerRepository.save(any(QuizAnswer.class))).thenAnswer(i -> i.getArgument(0));
        when(objectMapper.writeValueAsString(any())).thenReturn("audio_url.mp3");

        Map<Integer, Object> answers = new HashMap<>();
        answers.put(20, "audio_url.mp3");

        Integer resultId = quizResultService.submitQuiz(QUIZ_ID, EMAIL, answers);

        assertEquals(777, resultId);
        verify(quizAnswerRepository, times(1)).save(argThat(ans -> 
            Boolean.TRUE.equals(ans.getPendingAiReview()) &&
            "PENDING".equals(ans.getAiGradingStatus()) &&
            ans.getIsCorrect() == null
        ));
        
        assertFalse(TransactionSynchronizationManager.getSynchronizations().isEmpty());
        // Không gửi email do bài chưa chấm xong (passed = null)
        verify(emailService, never()).sendQuizResultEmail(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void TC07_Matching_MalformedAnswer_HandledGracefully() throws Exception {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(quiz));
        when(quizResultRepository.countByQuizQuizIdAndUserUserIdAndStatusNot(QUIZ_ID, user.getUserId(), "IN_PROGRESS"))
                .thenReturn(0L);

        doNothing().when(quizResultService).recalculateQuizResult(anyInt());

        Question q = new Question();
        q.setQuestionId(30);
        q.setQuestionType("MATCHING");
        AnswerOption opt1 = new AnswerOption();
        opt1.setAnswerOptionId(100);
        opt1.setMatchTarget("target");
        q.setAnswerOptions(List.of(opt1));

        QuizQuestion qq = new QuizQuestion();
        qq.setQuestion(q);
        quiz.getQuizQuestions().add(qq);

        QuizResult mockResult = new QuizResult();
        mockResult.setResultId(666);
        when(quizResultRepository.save(any(QuizResult.class))).thenReturn(mockResult);
        when(quizResultRepository.findById(666)).thenReturn(Optional.of(mockResult));
        when(quizAnswerRepository.save(any(QuizAnswer.class))).thenAnswer(i -> i.getArgument(0));

        // Hàm ObjectMapper mock để ném exception khi parse chuỗi rác
        when(objectMapper.convertValue(any(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenThrow(new IllegalArgumentException("Malformed JSON"));

        Map<Integer, Object> answers = new HashMap<>();
        answers.put(30, "malformed_string_not_map");

        Integer resultId = quizResultService.submitQuiz(QUIZ_ID, EMAIL, answers);

        assertEquals(666, resultId);
        verify(quizAnswerRepository, times(1)).save(argThat(ans -> Boolean.FALSE.equals(ans.getIsCorrect())));
    }

    @Test
    void TC08_NoLesson_UnlimitedAttempts() throws Exception {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        
        quiz.setMaxAttempts(null); // Không giới hạn
        quiz.setLesson(null);      // Không thuộc bài học nào
        quiz.setPassScore(BigDecimal.valueOf(50));
        
        when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(quiz));

        doNothing().when(quizResultService).recalculateQuizResult(anyInt());

        QuizResult mockResult = new QuizResult();
        mockResult.setResultId(555);
        mockResult.setPassed(true);
        mockResult.setScore(100);

        when(quizResultRepository.save(any(QuizResult.class))).thenReturn(mockResult);
        when(quizResultRepository.findById(555)).thenReturn(Optional.of(mockResult));

        Map<Integer, Object> answers = new HashMap<>();

        Integer resultId = quizResultService.submitQuiz(QUIZ_ID, EMAIL, answers);

        assertEquals(555, resultId);
        // Đếm số lần làm bài ĐƯỢC GỌI 1 lần theo implementation hiện tại
        verify(quizResultRepository, times(1)).countByQuizQuizIdAndUserUserIdAndStatusNot(QUIZ_ID, user.getUserId(), "IN_PROGRESS");
        
        // Gửi email đậu bình thường
        verify(emailService, times(1)).sendQuizResultEmail(eq(EMAIL), anyString(), anyString(), anyString(), anyString(), eq("Dat"));
        
        // Không gọi các hàm liên quan đến Lesson
        verify(learningService, never()).markLessonCompleted(anyInt(), anyString());
        verify(lessonQuizService, never()).updateProgressAfterSubmit(anyInt(), anyInt(), anyInt(), anyDouble(), anyBoolean());
    }
}
