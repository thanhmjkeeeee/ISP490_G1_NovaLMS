package com.example.DoAn.controller;

import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.model.Notification;
import com.example.DoAn.service.INotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final INotificationService notificationService;

    @GetMapping
    public ResponseEntity<ResponseData<Page<Notification>>> getInbox(
            Authentication auth,
            @PageableDefault(size = 20) Pageable pageable) {
        Long userId = extractUserId(auth);
        if (userId == null) {
            return ResponseEntity.status(401).body(ResponseData.error(401, "Unauthorized"));
        }
        Page<Notification> inbox = notificationService.getInbox(userId, pageable);
        return ResponseEntity.ok(ResponseData.success(inbox));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ResponseData<Map<String, Long>>> getUnreadCount(Authentication auth) {
        Long userId = extractUserId(auth);
        if (userId == null) {
            return ResponseEntity.status(401).body(ResponseData.error(401, "Unauthorized"));
        }
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ResponseData.success(Map.of("count", count)));
    }

    @GetMapping("/top")
    public ResponseEntity<ResponseData<List<Notification>>> getTopUnread(Authentication auth) {
        Long userId = extractUserId(auth);
        if (userId == null) {
            return ResponseEntity.status(401).body(ResponseData.error(401, "Unauthorized"));
        }
        List<Notification> top = notificationService.getTopUnread(userId);
        return ResponseEntity.ok(ResponseData.success(top));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ResponseData<Boolean>> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ResponseData.success(true));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ResponseData<Boolean>> markAllAsRead(Authentication auth) {
        Long userId = extractUserId(auth);
        if (userId == null) {
            return ResponseEntity.status(401).body(ResponseData.error(401, "Unauthorized"));
        }
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ResponseData.success(true));
    }

    private Long extractUserId(Authentication auth) {
        if (auth == null) return null;
        Object principal = auth.getPrincipal();
        if (principal == null) return null;
        try {
            java.lang.reflect.Method method = principal.getClass().getMethod("getUserId");
            return (Long) method.invoke(principal);
        } catch (Exception e) {
            return null;
        }
    }
}