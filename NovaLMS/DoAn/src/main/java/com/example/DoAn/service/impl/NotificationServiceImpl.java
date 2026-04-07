package com.example.DoAn.service.impl;

import com.example.DoAn.model.Notification;
import com.example.DoAn.repository.NotificationRepository;
import com.example.DoAn.service.INotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements INotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public void send(Long userId, String type, String title, String message, String link) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setLink(link);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Override
    public void sendQuestionApproved(Long teacherUserId, String questionContentPreview, Long quizId) {
        String content = questionContentPreview != null && questionContentPreview.length() > 80
            ? questionContentPreview.substring(0, 80) + "..."
            : (questionContentPreview != null ? questionContentPreview : "");
        send(teacherUserId, "QUESTION_APPROVED",
            "Câu hỏi của bạn đã được phê duyệt",
            "Câu hỏi \"" + content + "\" đã được Expert phê duyệt và xuất bản.",
            "/teacher/my-questions");
    }

    @Override
    public void sendQuestionRejected(Long teacherUserId, String questionContentPreview, String reviewNote) {
        String content = questionContentPreview != null && questionContentPreview.length() > 80
            ? questionContentPreview.substring(0, 80) + "..."
            : (questionContentPreview != null ? questionContentPreview : "");
        String note = (reviewNote != null && !reviewNote.isBlank())
            ? " Lý do: " + reviewNote
            : "";
        send(teacherUserId, "QUESTION_REJECTED",
            "Câu hỏi của bạn bị từ chối",
            "Câu hỏi \"" + content + "\" đã bị từ chối." + note,
            "/teacher/my-questions");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Notification> getInbox(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getTopUnread(Long userId) {
        return notificationRepository.findTop5ByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    @Override
    public void markAsRead(Long notificationId) {
        notificationRepository.markAsRead(notificationId);
    }

    @Override
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }
}