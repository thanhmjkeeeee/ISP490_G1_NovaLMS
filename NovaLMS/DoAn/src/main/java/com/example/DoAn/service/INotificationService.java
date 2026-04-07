package com.example.DoAn.service;

import com.example.DoAn.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface INotificationService {

    void send(Long userId, String type, String title, String message, String link);

    void sendQuestionApproved(Long teacherUserId, String questionContentPreview, Long quizId);

    void sendQuestionRejected(Long teacherUserId, String questionContentPreview, String reviewNote);

    Page<Notification> getInbox(Long userId, Pageable pageable);

    long getUnreadCount(Long userId);

    List<Notification> getTopUnread(Long userId);

    void markAsRead(Long notificationId);

    void markAllAsRead(Long userId);
}