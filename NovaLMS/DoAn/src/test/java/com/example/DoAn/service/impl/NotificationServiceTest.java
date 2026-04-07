package com.example.DoAn.service.impl;

import com.example.DoAn.model.Notification;
import com.example.DoAn.repository.NotificationRepository;
import com.example.DoAn.service.INotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    private INotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationServiceImpl(notificationRepository);
    }

    // TC-NOTIF-001: send() calls save() with correct fields
    @Test
    void TC_NOTIF_001_sendCorrectFields() {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

        service.send(42L, "TEST_TYPE", "Test Title", "Test Message", "/test/link");

        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertEquals(42L, saved.getUserId());
        assertEquals("TEST_TYPE", saved.getType());
        assertEquals("Test Title", saved.getTitle());
        assertEquals("Test Message", saved.getMessage());
        assertEquals("/test/link", saved.getLink());
        assertFalse(saved.getIsRead());
        assertNotNull(saved.getCreatedAt());
    }

    // TC-NOTIF-002: sendQuestionApproved() sets type=QUESTION_APPROVED, Vietnamese content
    @Test
    void TC_NOTIF_002_sendQuestionApproved() {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

        service.sendQuestionApproved(1L, "What is IELTS?", 10L);

        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertEquals("QUESTION_APPROVED", saved.getType());
        assertEquals("Câu hỏi của bạn đã được phê duyệt", saved.getTitle());
        assertTrue(saved.getMessage().contains("đã được Expert phê duyệt"));
        assertEquals("/teacher/my-questions", saved.getLink());
        assertEquals(1L, saved.getUserId());
    }

    // TC-NOTIF-003: sendQuestionRejected() with reviewNote includes note in message
    @Test
    void TC_NOTIF_003_sendQuestionRejectedWithNote() {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

        service.sendQuestionRejected(1L, "Sample question content", "Too difficult for B1 level");

        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertEquals("QUESTION_REJECTED", saved.getType());
        assertEquals("Câu hỏi của bạn bị từ chối", saved.getTitle());
        assertTrue(saved.getMessage().contains("Too difficult for B1 level"));
    }

    // TC-NOTIF-004: sendQuestionRejected() with null reviewNote does NOT crash
    @Test
    void TC_NOTIF_004_sendQuestionRejectedNullNote() {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

        assertDoesNotThrow(() -> service.sendQuestionRejected(1L, "Sample content", null));

        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertFalse(saved.getMessage().contains("Lý do: null"));
    }

    // TC-NOTIF-005: getInbox() delegates to findByUserIdOrderByCreatedAtDesc
    @Test
    void TC_NOTIF_005_getInbox() {
        Page<Notification> mockPage = new PageImpl<>(List.of());
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(5L), any())).thenReturn(mockPage);

        service.getInbox(5L, PageRequest.of(0, 20));

        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(eq(5L), any());
    }

    // TC-NOTIF-006: getUnreadCount() delegates to countByUserIdAndIsReadFalse
    @Test
    void TC_NOTIF_006_getUnreadCount() {
        when(notificationRepository.countByUserIdAndIsReadFalse(7L)).thenReturn(3L);

        long count = service.getUnreadCount(7L);

        assertEquals(3L, count);
        verify(notificationRepository).countByUserIdAndIsReadFalse(7L);
    }

    // TC-NOTIF-007: getTopUnread() delegates to findTop5ByUserIdAndIsReadFalseOrderByCreatedAtDesc
    @Test
    void TC_NOTIF_007_getTopUnread() {
        List<Notification> mockList = List.of();
        when(notificationRepository.findTop5ByUserIdAndIsReadFalseOrderByCreatedAtDesc(9L)).thenReturn(mockList);

        service.getTopUnread(9L);

        verify(notificationRepository).findTop5ByUserIdAndIsReadFalseOrderByCreatedAtDesc(9L);
    }

    // TC-NOTIF-008: markAsRead() calls markAsRead(id)
    @Test
    void TC_NOTIF_008_markAsRead() {
        service.markAsRead(55L);
        verify(notificationRepository).markAsRead(55L);
    }

    // TC-NOTIF-009: markAllAsRead() calls markAllAsRead(userId)
    @Test
    void TC_NOTIF_009_markAllAsRead() {
        service.markAllAsRead(12L);
        verify(notificationRepository).markAllAsRead(12L);
    }

    // TC-NOTIF-010: sendQuestionApproved() truncates content >80 chars with "..."
    @Test
    void TC_NOTIF_010_truncatesLongContent() {
        String longContent = "A".repeat(120); // 120 chars > 80
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

        service.sendQuestionApproved(1L, longContent, null);

        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertTrue(saved.getMessage().contains("..."));
        assertTrue(saved.getMessage().length() < 500); // reasonable length
    }
}
