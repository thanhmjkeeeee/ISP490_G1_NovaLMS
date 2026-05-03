package com.example.DoAn.service;

import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.model.*;
import com.example.DoAn.repository.ClassSessionRepository;
import com.example.DoAn.repository.RescheduleRequestRepository;
import com.example.DoAn.repository.UserRepository;
import com.example.DoAn.dto.request.RescheduleRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RescheduleServiceCreateRequestTest {

    @Mock
    private RescheduleRequestRepository rescheduleRequestRepository;
    @Mock
    private ClassSessionRepository classSessionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private INotificationService notificationService;

    @InjectMocks
    private RescheduleService rescheduleService;

    private User teacher;
    private ClassSession session;
    private Clazz clazz;
    private final String EMAIL = "teacher@gmail.com";
    private final Integer SESSION_ID = 1;

    @BeforeEach
    void setUp() {
        teacher = new User();
        teacher.setUserId(10);
        teacher.setEmail(EMAIL);
        teacher.setFullName("Test Teacher");

        clazz = new Clazz();
        clazz.setTeacher(teacher);
        clazz.setClassName("Java Web");

        session = new ClassSession();
        session.setSessionId(SESSION_ID);
        session.setClazz(clazz);
        // Default valid session in the future
        session.setSessionDate(LocalDateTime.now().plusDays(5));
        session.setStartTime("19:00");
    }

    @Test
    void TC01_UserNotFound_ByEmail_Returns401() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        ResponseData<Integer> response = rescheduleService.createRequest(SESSION_ID, "2026-05-01", "20:00", "Bận việc",
                EMAIL);

        assertEquals(401, response.getStatus());
        assertEquals("Người dùng không tồn tại", response.getMessage());
    }

    @Test
    void TC02_UserNotFound_ForUpdate_Returns401() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(teacher));
        when(userRepository.findByIdForUpdate(teacher.getUserId())).thenReturn(Optional.empty());

        ResponseData<Integer> response = rescheduleService.createRequest(SESSION_ID, "2026-05-01", "20:00", "Bận việc",
                EMAIL);

        assertEquals(401, response.getStatus());
        assertEquals("Người dùng không tồn tại", response.getMessage());
    }

    @Test
    void TC03_SessionNotFound_Returns404() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(teacher));
        when(userRepository.findByIdForUpdate(teacher.getUserId())).thenReturn(Optional.of(teacher));
        when(classSessionRepository.findByIdForUpdate(SESSION_ID)).thenReturn(Optional.empty());

        ResponseData<Integer> response = rescheduleService.createRequest(SESSION_ID, "2026-05-01", "20:00", "Bận việc",
                EMAIL);

        assertEquals(404, response.getStatus());
        assertEquals("Không tìm thấy buổi học", response.getMessage());
    }

    @Test
    void TC04_NotSessionOwner_Returns403() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(teacher));
        when(userRepository.findByIdForUpdate(teacher.getUserId())).thenReturn(Optional.of(teacher));

        User anotherTeacher = new User();
        anotherTeacher.setUserId(99);
        clazz.setTeacher(anotherTeacher);
        when(classSessionRepository.findByIdForUpdate(SESSION_ID)).thenReturn(Optional.of(session));

        ResponseData<Integer> response = rescheduleService.createRequest(SESSION_ID, "2026-05-01", "20:00", "Bận",
                EMAIL);

        assertEquals(403, response.getStatus());
        assertTrue(response.getMessage().contains("Bạn không có quyền gửi yêu cầu"));
    }

    @Test
    void TC05_PendingRequestAlreadyExists_Returns400() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(teacher));
        when(userRepository.findByIdForUpdate(teacher.getUserId())).thenReturn(Optional.of(teacher));
        when(classSessionRepository.findByIdForUpdate(SESSION_ID)).thenReturn(Optional.of(session));

        RescheduleRequest pendingReq = new RescheduleRequest();
        when(rescheduleRequestRepository.findPendingBySessionId(SESSION_ID)).thenReturn(Optional.of(pendingReq));

        ResponseData<Integer> response = rescheduleService.createRequest(SESSION_ID, "2026-05-01", "20:00", "Bận",
                EMAIL);

        assertEquals(400, response.getStatus());
        assertTrue(response.getMessage().contains("vui lòng chờ duyệt"));
    }

    @Test
    void TC06_InvalidDateFormat_Returns400() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(teacher));
        when(userRepository.findByIdForUpdate(teacher.getUserId())).thenReturn(Optional.of(teacher));
        when(classSessionRepository.findByIdForUpdate(SESSION_ID)).thenReturn(Optional.of(session));
        when(rescheduleRequestRepository.findPendingBySessionId(SESSION_ID)).thenReturn(Optional.empty());

        ResponseData<Integer> response = rescheduleService.createRequest(SESSION_ID, "28/04/2026", "20:00", "Bận",
                EMAIL);

        assertEquals(400, response.getStatus());
        assertTrue(response.getMessage().contains("Định dạng ngày không hợp lệ"));
    }

    @Test
    void TC07_OriginalSessionInPast_Returns400() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(teacher));
        when(userRepository.findByIdForUpdate(teacher.getUserId())).thenReturn(Optional.of(teacher));

        session.setSessionDate(LocalDateTime.now().minusDays(1));
        when(classSessionRepository.findByIdForUpdate(SESSION_ID)).thenReturn(Optional.of(session));
        when(rescheduleRequestRepository.findPendingBySessionId(SESSION_ID)).thenReturn(Optional.empty());

        String validFutureDate = LocalDate.now().plusDays(2).toString();
        ResponseData<Integer> response = rescheduleService.createRequest(SESSION_ID, validFutureDate, "20:00", "Bận",
                EMAIL);

        assertEquals(400, response.getStatus());
        assertEquals("Không thể đổi lịch cho buổi học đã diễn ra.", response.getMessage());
    }

    @Test
    void TC08_NewScheduleMatchesOldSchedule_Returns400() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(teacher));
        when(userRepository.findByIdForUpdate(teacher.getUserId())).thenReturn(Optional.of(teacher));
        when(classSessionRepository.findByIdForUpdate(SESSION_ID)).thenReturn(Optional.of(session));
        when(rescheduleRequestRepository.findPendingBySessionId(SESSION_ID)).thenReturn(Optional.empty());

        String sameDateStr = session.getSessionDate().toLocalDate().toString();
        String sameTimeStr = session.getStartTime();

        ResponseData<Integer> response = rescheduleService.createRequest(SESSION_ID, sameDateStr, sameTimeStr, "Bận",
                EMAIL);

        assertEquals(400, response.getStatus());
        assertEquals("Thời gian đổi lịch không được trùng với lịch hiện tại.", response.getMessage());
    }

    @Test
    void TC09_ConflictWithConfirmedSession_Returns400() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(teacher));
        when(userRepository.findByIdForUpdate(teacher.getUserId())).thenReturn(Optional.of(teacher));
        when(classSessionRepository.findByIdForUpdate(SESSION_ID)).thenReturn(Optional.of(session));
        when(rescheduleRequestRepository.findPendingBySessionId(SESSION_ID)).thenReturn(Optional.empty());

        String futureDate = LocalDate.now().plusDays(2).toString();
        when(classSessionRepository.countConflictsInDateRange(eq(teacher.getUserId()), any(), any(), eq("20:00"),
                eq(SESSION_ID)))
                .thenReturn(1L);

        ResponseData<Integer> response = rescheduleService.createRequest(SESSION_ID, futureDate, "20:00", "Bận", EMAIL);

        assertEquals(400, response.getStatus());
        assertTrue(response.getMessage().contains("Trùng lịch! Bạn đã có lịch dạy một lớp khác"));
    }

    @Test
    void TC10_ConflictWithPendingRequest_Returns400() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(teacher));
        when(userRepository.findByIdForUpdate(teacher.getUserId())).thenReturn(Optional.of(teacher));
        when(classSessionRepository.findByIdForUpdate(SESSION_ID)).thenReturn(Optional.of(session));
        when(rescheduleRequestRepository.findPendingBySessionId(SESSION_ID)).thenReturn(Optional.empty());

        String futureDate = LocalDate.now().plusDays(2).toString();
        when(classSessionRepository.countConflictsInDateRange(anyInt(), any(), any(), anyString(), anyInt()))
                .thenReturn(0L);
        when(rescheduleRequestRepository.existsByCreatedBy_UserIdAndNewDateAndNewStartTimeAndStatus(
                eq(teacher.getUserId()), any(), eq("20:00"), eq("PENDING"))).thenReturn(true);

        ResponseData<Integer> response = rescheduleService.createRequest(SESSION_ID, futureDate, "20:00", "Bận", EMAIL);

        assertEquals(400, response.getStatus());
        assertTrue(response.getMessage().contains("Trùng lịch! Bạn đang có một yêu cầu đổi lịch khác"));
    }

    @Test
    void TC11_RaceCondition_PendingRequestAppears_Returns400() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(teacher));
        when(userRepository.findByIdForUpdate(teacher.getUserId())).thenReturn(Optional.of(teacher));
        when(classSessionRepository.findByIdForUpdate(SESSION_ID)).thenReturn(Optional.of(session));

        when(rescheduleRequestRepository.findPendingBySessionId(SESSION_ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new RescheduleRequest()));

        String futureDate = LocalDate.now().plusDays(2).toString();
        when(classSessionRepository.countConflictsInDateRange(anyInt(), any(), any(), anyString(), anyInt()))
                .thenReturn(0L);
        when(rescheduleRequestRepository.existsByCreatedBy_UserIdAndNewDateAndNewStartTimeAndStatus(
                anyInt(), any(), anyString(), anyString())).thenReturn(false);

        ResponseData<Integer> response = rescheduleService.createRequest(SESSION_ID, futureDate, "20:00", "Bận", EMAIL);

        assertEquals(400, response.getStatus());
        assertTrue(response.getMessage().contains("vui lòng chờ duyệt"));
        verify(rescheduleRequestRepository, never()).save(any());
    }

    @Test
    void TC12_CreateRequestSuccess_NotifiesManagers_Returns200() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(teacher));
        when(userRepository.findByIdForUpdate(teacher.getUserId())).thenReturn(Optional.of(teacher));
        when(classSessionRepository.findByIdForUpdate(SESSION_ID)).thenReturn(Optional.of(session));
        when(rescheduleRequestRepository.findPendingBySessionId(SESSION_ID)).thenReturn(Optional.empty());

        String futureDate = LocalDate.now().plusDays(2).toString();
        when(classSessionRepository.countConflictsInDateRange(anyInt(), any(), any(), anyString(), anyInt()))
                .thenReturn(0L);
        when(rescheduleRequestRepository.existsByCreatedBy_UserIdAndNewDateAndNewStartTimeAndStatus(
                anyInt(), any(), anyString(), anyString())).thenReturn(false);

        User manager = new User();
        manager.setUserId(20);
        when(userRepository.findByRole_Value("ROLE_MANAGER")).thenReturn(List.of(manager));

        doAnswer(invocation -> {
            RescheduleRequest req = invocation.getArgument(0);
            req.setId(777);
            return req;
        }).when(rescheduleRequestRepository).save(any(RescheduleRequest.class));

        ResponseData<Integer> response = rescheduleService.createRequest(SESSION_ID, futureDate, "20:00", "Bận", EMAIL);

        assertEquals(200, response.getStatus());
        assertEquals(777, response.getData());
        verify(notificationService, times(1)).sendRescheduleRequestForManager(eq(20L), anyString(), anyString(),
                anyString());
    }

    @Test
    void TC13_CreateRequestSuccess_NotificationFails_DoesNotRollback() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(teacher));
        when(userRepository.findByIdForUpdate(teacher.getUserId())).thenReturn(Optional.of(teacher));
        when(classSessionRepository.findByIdForUpdate(SESSION_ID)).thenReturn(Optional.of(session));
        when(rescheduleRequestRepository.findPendingBySessionId(SESSION_ID)).thenReturn(Optional.empty());

        String futureDate = LocalDate.now().plusDays(2).toString();
        when(classSessionRepository.countConflictsInDateRange(anyInt(), any(), any(), anyString(), anyInt()))
                .thenReturn(0L);
        when(rescheduleRequestRepository.existsByCreatedBy_UserIdAndNewDateAndNewStartTimeAndStatus(
                anyInt(), any(), anyString(), anyString())).thenReturn(false);

        User manager = new User();
        manager.setUserId(20);
        when(userRepository.findByRole_Value("ROLE_MANAGER")).thenReturn(List.of(manager));

        doThrow(new RuntimeException("Notification failed"))
                .when(notificationService)
                .sendRescheduleRequestForManager(anyLong(), anyString(), anyString(), anyString());

        doAnswer(invocation -> {
            RescheduleRequest req = invocation.getArgument(0);
            req.setId(888);
            return req;
        }).when(rescheduleRequestRepository).save(any(RescheduleRequest.class));

        ResponseData<Integer> response = rescheduleService.createRequest(SESSION_ID, futureDate, "20:00", "Bận", EMAIL);

        assertEquals(200, response.getStatus());
        assertEquals(888, response.getData());
        verify(rescheduleRequestRepository, times(1)).save(any(RescheduleRequest.class));
    }
}
