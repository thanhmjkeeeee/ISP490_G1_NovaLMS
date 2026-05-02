package com.example.DoAn.service;

import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.model.*;
import com.example.DoAn.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TeacherClassSessionServiceTest {

    @Mock
    private ClassSessionRepository classSessionRepository;
    @Mock
    private QuizRepository quizRepository;
    @Mock
    private ClazzRepository clazzRepository;
    @Mock
    private SessionQuizRepository sessionQuizRepository;
    @Mock
    private TeacherQuizService teacherQuizService;
    @Mock
    private EntityManager entityManager;
    @Mock
    private TypedQuery<Integer> typedQuery;

    @InjectMocks
    private TeacherClassSessionService service;

    private User teacher;
    private Clazz clazz;
    private ClassSession session;
    private Quiz quiz;
    private SessionQuiz sessionQuiz;

    private final String EMAIL = "teacher@test.com";
    private final Integer TEACHER_ID = 1;
    private final Integer SESSION_ID = 100;
    private final Integer CLASS_ID = 10;
    private final Integer QUIZ_ID = 99;

    @BeforeEach
    void setUp() {
        teacher = new User();
        teacher.setUserId(TEACHER_ID);

        clazz = new Clazz();
        clazz.setClassId(CLASS_ID);
        clazz.setTeacher(teacher);

        session = new ClassSession();
        session.setSessionId(SESSION_ID);
        session.setClazz(clazz);
        // Thiết lập thời gian an toàn (Dư dả 5 tiếng để pass điều kiện remaining time)
        session.setSessionDate(LocalDateTime.now());
        String safeEndTime = LocalDateTime.now().plusHours(5).format(DateTimeFormatter.ofPattern("HH:mm"));
        session.setEndTime(safeEndTime);

        quiz = new Quiz();
        quiz.setQuizId(QUIZ_ID);
        quiz.setTitle("Mock Quiz");
        quiz.setStatus("PUBLISHED");
        quiz.setTimeLimitMinutes(10);

        sessionQuiz = new SessionQuiz();
        sessionQuiz.setId(500);
        sessionQuiz.setSession(session);
        sessionQuiz.setQuiz(quiz);
        sessionQuiz.setIsOpen(false);

        // Fix lỗi Inject EntityManager bị null do @PersistenceContext
        ReflectionTestUtils.setField(service, "entityManager", entityManager);
    }

    // Helper: Giả lập Teacher luôn hợp lệ
    private void mockAuthSuccess() {
        when(entityManager.createQuery(anyString(), eq(Integer.class))).thenReturn(typedQuery);
        when(typedQuery.setParameter(eq("email"), anyString())).thenReturn(typedQuery);
        when(typedQuery.setMaxResults(1)).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(List.of(TEACHER_ID));

        when(classSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(clazzRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
    }

    @Test
    void TC01_AuthFailed_Returns401() {
        when(entityManager.createQuery(anyString(), eq(Integer.class))).thenReturn(typedQuery);
        when(typedQuery.setParameter(eq("email"), anyString())).thenReturn(typedQuery);
        when(typedQuery.setMaxResults(1)).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(Collections.emptyList());

        ResponseData<Map<String, Object>> res = service.toggleQuizOpenInSession(EMAIL, SESSION_ID, QUIZ_ID, null);

        assertEquals(401, res.getStatus());
        assertEquals("Không tìm thấy buổi học hoặc không có quyền", res.getMessage());
    }

    @Test
    void TC02_SessionQuizNotFound_Returns404() {
        mockAuthSuccess();
        when(sessionQuizRepository.findBySessionSessionIdAndQuizQuizId(SESSION_ID, QUIZ_ID))
                .thenReturn(Optional.empty());

        ResponseData<Map<String, Object>> res = service.toggleQuizOpenInSession(EMAIL, SESSION_ID, QUIZ_ID, null);

        assertEquals(404, res.getStatus());
        assertEquals("Quiz không tồn tại trong buổi học này", res.getMessage());
    }

    @Test
    void TC03_OpenQuiz_TimeLimitLessThanOrEqualToZero_Returns400() {
        mockAuthSuccess();

        // FIX: Đặt thời gian mặc định của Quiz về 0 để lách qua bước validate "nhỏ hơn
        // cấu hình tối thiểu"
        quiz.setTimeLimitMinutes(0);

        when(sessionQuizRepository.findBySessionSessionIdAndQuizQuizId(SESSION_ID, QUIZ_ID))
                .thenReturn(Optional.of(sessionQuiz)); // Trạng thái đang đóng -> Mở

        // Input thời gian = 0 (Giá trị biên)
        ResponseData<Map<String, Object>> res = service.toggleQuizOpenInSession(EMAIL, SESSION_ID, QUIZ_ID, 0);

        assertEquals(400, res.getStatus());
        assertEquals("Thời gian làm bài phải lớn hơn 0", res.getMessage());

        // Dựa đúng theo code thực tế: sessionQuizRepository.save() ĐÃ ĐƯỢC CHẠY trước
        // khi return 400
        verify(sessionQuizRepository, times(1)).save(sessionQuiz);
        verify(quizRepository, never()).save(any());
    }

    @Test
    void TC04_OpenQuiz_TimeLimitLessThanRequired_Returns400() {
        mockAuthSuccess();
        quiz.setTimeLimitPerSkill("{\"READING\": 30}"); // Bắt buộc tối thiểu 30 phút
        when(sessionQuizRepository.findBySessionSessionIdAndQuizQuizId(SESSION_ID, QUIZ_ID))
                .thenReturn(Optional.of(sessionQuiz));

        ResponseData<Map<String, Object>> res = service.toggleQuizOpenInSession(EMAIL, SESSION_ID, QUIZ_ID, 15);

        assertEquals(400, res.getStatus());
        assertTrue(res.getMessage().contains("không được nhỏ hơn cấu hình tối thiểu"));
        verify(sessionQuizRepository, never()).save(any()); // Bị chặn sớm nên chưa lưu
    }

    @Test
    void TC05_OpenQuiz_NotEnoughRemainingTime_Returns400() {
        mockAuthSuccess();
        // Sửa thời gian kết thúc buổi học chỉ còn 5 phút nữa
        String closeEndTime = LocalDateTime.now().plusMinutes(5).format(DateTimeFormatter.ofPattern("HH:mm"));
        session.setEndTime(closeEndTime);

        when(sessionQuizRepository.findBySessionSessionIdAndQuizQuizId(SESSION_ID, QUIZ_ID))
                .thenReturn(Optional.of(sessionQuiz));

        // Giáo viên set quiz limit là 60 phút
        ResponseData<Map<String, Object>> res = service.toggleQuizOpenInSession(EMAIL, SESSION_ID, QUIZ_ID, 60);

        assertEquals(400, res.getStatus());
        assertTrue(res.getMessage().contains("không đủ để thực hiện bài Quiz này"));
        verify(sessionQuizRepository, never()).save(any());
    }

    @Test
    void TC06_OpenQuiz_PublishDraftFailed_ReturnsError() {
        mockAuthSuccess();
        quiz.setStatus("DRAFT");
        when(sessionQuizRepository.findBySessionSessionIdAndQuizQuizId(SESSION_ID, QUIZ_ID))
                .thenReturn(Optional.of(sessionQuiz));

        when(teacherQuizService.publishQuiz(QUIZ_ID, EMAIL))
                .thenReturn(ResponseData.error(400, "Lỗi do thiếu câu hỏi"));

        ResponseData<Map<String, Object>> res = service.toggleQuizOpenInSession(EMAIL, SESSION_ID, QUIZ_ID, 45);

        assertEquals(400, res.getStatus());
        assertEquals("Lỗi do thiếu câu hỏi", res.getMessage());
        verify(sessionQuizRepository, never()).save(any());
    }

    @Test
    void TC07_OpenQuiz_Success_WithNewTimeLimit() {
        mockAuthSuccess();
        when(sessionQuizRepository.findBySessionSessionIdAndQuizQuizId(SESSION_ID, QUIZ_ID))
                .thenReturn(Optional.of(sessionQuiz));

        // Giáo viên truyền thời gian làm bài mới là 45 phút
        ResponseData<Map<String, Object>> res = service.toggleQuizOpenInSession(EMAIL, SESSION_ID, QUIZ_ID, 45);

        assertEquals(200, res.getStatus());
        assertEquals("Quiz đã được mở!", res.getMessage());
        assertTrue((Boolean) res.getData().get("isOpen"));
        assertEquals(45, quiz.getTimeLimitMinutes());

        verify(sessionQuizRepository).save(sessionQuiz);
        verify(quizRepository).save(quiz); // Quiz repo được lưu để update time
    }

    @Test
    void TC08_OpenQuiz_Success_WithNullTimeLimit() {
        mockAuthSuccess();
        when(sessionQuizRepository.findBySessionSessionIdAndQuizQuizId(SESSION_ID, QUIZ_ID))
                .thenReturn(Optional.of(sessionQuiz));

        // Giáo viên MỞ bài nhưng KHÔNG truyền thời gian (thời gian = null)
        ResponseData<Map<String, Object>> res = service.toggleQuizOpenInSession(EMAIL, SESSION_ID, QUIZ_ID, null);

        assertEquals(200, res.getStatus());
        assertEquals("Quiz đã được mở!", res.getMessage());
        assertTrue((Boolean) res.getData().get("isOpen"));

        verify(sessionQuizRepository).save(sessionQuiz);
        verify(quizRepository, never()).save(any()); // Quiz time không đổi nên không lưu Quiz repo
    }

    @Test
    void TC09_CloseQuiz_Success() {
        mockAuthSuccess();
        sessionQuiz.setIsOpen(true); // Trạng thái đang Mở -> Hàm này sẽ thực hiện ĐÓNG
        when(sessionQuizRepository.findBySessionSessionIdAndQuizQuizId(SESSION_ID, QUIZ_ID))
                .thenReturn(Optional.of(sessionQuiz));

        ResponseData<Map<String, Object>> res = service.toggleQuizOpenInSession(EMAIL, SESSION_ID, QUIZ_ID, null);

        assertEquals(200, res.getStatus());
        assertEquals("Quiz đã được đóng!", res.getMessage());
        assertFalse((Boolean) res.getData().get("isOpen"));

        verify(sessionQuizRepository).save(sessionQuiz);
        verify(quizRepository, never()).save(quiz); // Đóng quiz không update time limit
    }

    @Test
    void TC10_ExceptionHandling_Returns500() {
        when(entityManager.createQuery(anyString(), eq(Integer.class)))
                .thenThrow(new RuntimeException("Database Connection Timeout"));

        ResponseData<Map<String, Object>> res = service.toggleQuizOpenInSession(EMAIL, SESSION_ID, QUIZ_ID, null);

        assertEquals(500, res.getStatus());
        assertEquals("Database Connection Timeout", res.getMessage());
    }
}