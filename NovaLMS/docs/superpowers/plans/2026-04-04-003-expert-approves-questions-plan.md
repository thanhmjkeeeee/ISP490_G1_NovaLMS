# Plan 003 — Expert Approves Teacher Questions

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expert reviews and approves/rejects Teacher questions (PENDING_REVIEW), receives notifications, and sees a queue summary. Teacher sees their question statuses. Notifications are created when Expert acts.

**Architecture:** Reuse existing `ExpertReviewController` endpoints but extend with filters, review notes, and notifications. New `Notification` entity + service. New approval UI page. Teacher's "my questions" page.

**Tech Stack:** Spring Boot 3.3.4, Spring Data JPA, Thymeleaf.

---

## File Structure

```
src/main/java/com/example/DoAn/
├── model/
│   └── Notification.java               CREATE
├── repository/
│   └── NotificationRepository.java    CREATE
├── service/
│   ├── INotificationService.java      CREATE
│   └── impl/
│       └── NotificationServiceImpl.java CREATE
├── controller/
│   └── NotificationController.java    CREATE
└── views/
    └── templates/
        ├── expert/
        │   └── question-approval.html   CREATE
        └── teacher/
            └── my-questions.html       CREATE
```

---

## Chunk 1: Notification Entity & Repository

### Task 1: Create Notification entity

**Files:**
- Create: `src/main/java/com/example/DoAn/model/Notification.java`

- [ ] **Step 1: Write the entity**

```java
package com.example.DoAn.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String type; // QUESTION_APPROVED, QUESTION_REJECTED

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(length = 500)
    private String link; // e.g. "/teacher/my-questions"
}
```

### Task 2: Create NotificationRepository

**Files:**
- Create: `src/main/java/com/example/DoAn/repository/NotificationRepository.java`

- [ ] **Step 1: Write the repository**

```java
package com.example.DoAn.repository;

import com.example.DoAn.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserIdAndIsReadFalse(Long userId);

    List<Notification> findTop5ByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId")
    void markAllAsRead(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id")
    void markAsRead(@Param("id") Long id);
}
```

---

## Chunk 2: Notification Service

### Task 3: Create INotificationService interface

**Files:**
- Create: `src/main/java/com/example/DoAn/service/INotificationService.java`

- [ ] **Step 1: Write the interface**

```java
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
```

### Task 4: Create NotificationServiceImpl

**Files:**
- Create: `src/main/java/com/example/DoAn/service/impl/NotificationServiceImpl.java`

- [ ] **Step 1: Write the implementation**

```java
package com.example.DoAn.service.impl;

import com.example.DoAn.model.Notification;
import com.example.DoAn.repository.NotificationRepository;
import com.example.DoAn.service.INotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
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
    @Async
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
        String content = questionContentPreview.length() > 80
            ? questionContentPreview.substring(0, 80) + "..."
            : questionContentPreview;
        send(teacherUserId, "QUESTION_APPROVED",
            "Câu hỏi của bạn đã được phê duyệt",
            "Câu hỏi \"" + content + "\" đã được Expert phê duyệt và xuất bản.",
            "/teacher/my-questions");
    }

    @Override
    public void sendQuestionRejected(Long teacherUserId, String questionContentPreview, String reviewNote) {
        String content = questionContentPreview.length() > 80
            ? questionContentPreview.substring(0, 80) + "..."
            : questionContentPreview;
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
```

---

## Chunk 3: Notification API Controller

### Task 5: Create NotificationController

**Files:**
- Create: `src/main/java/com/example/DoAn/controller/NotificationController.java`

- [ ] **Step 1: Write the controller**

```java
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
import org.springframework.web.bind.annotation.*;

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
        Page<Notification> inbox = notificationService.getInbox(userId, pageable);
        return ResponseEntity.ok(ResponseData.success(inbox));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ResponseData<Map<String, Long>>> getUnreadCount(Authentication auth) {
        Long userId = extractUserId(auth);
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ResponseData.success(Map.of("count", count)));
    }

    @GetMapping("/top")
    public ResponseEntity<ResponseData<List<Notification>>> getTopUnread(Authentication auth) {
        Long userId = extractUserId(auth);
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
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ResponseData.success(true));
    }

    private Long extractUserId(Authentication auth) {
        // Get userId from user details
        // Assumes auth.getPrincipal() returns UserDetails with getId() method
        // Adapt based on actual implementation in SecurityConfig
        Object principal = auth.getPrincipal();
        try {
            var method = principal.getClass().getMethod("getId");
            return (Long) method.invoke(principal);
        } catch (Exception e) {
            throw new RuntimeException("Could not extract userId from authentication", e);
        }
    }
}
```

---

## Chunk 4: Extend ExpertReviewController with filters and review notes

### Task 6: Extend ExpertReviewController — add filters, review note, and notification

**Files:**
- Modify: `src/main/java/com/example/DoAn/controller/ExpertReviewController.java`

- [ ] **Step 1: Read the existing controller**

Run: Read `ExpertReviewController.java` completely.

- [ ] **Step 2: Add imports and new fields**

Add after existing imports:

```java
import com.example.DoAn.service.INotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
```

Add as a field:

```java
private final INotificationService notificationService;
```

- [ ] **Step 3: Extend GET /pending with filter params**

Find the existing `getPendingQuestions` method and replace it with:

```java
@GetMapping("/pending")
public ResponseEntity<ResponseData<Page<Question>>> getPendingQuestions(
        @RequestParam(required = false) String skill,
        @RequestParam(required = false) String cefrLevel,
        @RequestParam(required = false) String teacherEmail,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String questionType,
        @RequestParam(required = false) String fromDate,
        @RequestParam(required = false) String toDate,
        @PageableDefault(size = 20) Pageable pageable) {

    Page<Question> questions = expertQuestionService.getPendingQuestionsWithFilters(
        skill, cefrLevel, teacherEmail, keyword, questionType, fromDate, toDate, pageable);
    return ResponseData.success(questions);
}
```

- [ ] **Step 4: Extend POST approve with reviewNote**

Find the existing `approveQuestion` method and replace the body with:

```java
@PostMapping("/{id}/approve")
public ResponseEntity<ResponseData<Boolean>> approveQuestion(
        @PathVariable Long id,
        @RequestBody(required = false) Map<String, String> body,
        Authentication auth) {

    String reviewNote = (body != null) ? body.get("reviewNote") : null;
    expertQuestionService.approveQuestion(id, auth.getName(), reviewNote);
    return ResponseData.success(true);
}
```

- [ ] **Step 5: Extend POST reject with reviewNote + delete flag**

Find the existing `rejectQuestion` method and replace:

```java
@PostMapping("/{id}/reject")
public ResponseEntity<ResponseData<Boolean>> rejectQuestion(
        @PathVariable Long id,
        @RequestBody(required = false) Map<String, Object> body,
        Authentication auth) {

    String reviewNote = (body != null && body.get("reviewNote") != null)
        ? (String) body.get("reviewNote") : null;
    boolean delete = (body != null && body.get("delete") != null)
        ? (Boolean) body.get("delete") : false;
    expertQuestionService.rejectQuestion(id, auth.getName(), reviewNote, delete);
    return ResponseData.success(true);
}
```

### Task 7: Extend IExpertQuestionService — add new method signatures

**Files:**
- Modify: `src/main/java/com/example/DoAn/service/IExpertQuestionService.java`

- [ ] **Step 1: Read the interface**

Run: Read `IExpertQuestionService.java`.

- [ ] **Step 2: Add new method signatures**

Add after existing method signatures:

```java
Page<Question> getPendingQuestionsWithFilters(
    String skill, String cefrLevel, String teacherEmail, String keyword,
    String questionType, String fromDate, String toDate, Pageable pageable);

void approveQuestion(Long questionId, String expertEmail, String reviewNote);

void rejectQuestion(Long questionId, String expertEmail, String reviewNote, boolean delete);
```

### Task 8: Extend ExpertQuestionServiceImpl — implement new methods + fire notifications

**Files:**
- Modify: `src/main/java/com/example/DoAn/service/impl/ExpertQuestionServiceImpl.java`

- [ ] **Step 1: Read the service implementation**

Run: Read `ExpertQuestionServiceImpl.java`.

- [ ] **Step 2: Add imports**

Add:

```java
import com.example.DoAn.service.INotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
```

Add as a field:

```java
private final INotificationService notificationService;
```

- [ ] **Step 3: Add getPendingQuestionsWithFilters implementation**

Add after the existing `getPendingQuestions` method:

```java
@Override
public Page<Question> getPendingQuestionsWithFilters(
        String skill, String cefrLevel, String teacherEmail, String keyword,
        String questionType, String fromDate, String toDate, Pageable pageable) {

    Specification<Question> spec = (root, query, cb) -> {
        List<Predicate> preds = new ArrayList<>();

        preds.add(cb.equal(root.get("status"), "PENDING_REVIEW"));
        preds.add(cb.equal(root.get("source"), "TEACHER_PRIVATE"));

        if (StringUtils.hasText(skill)) {
            preds.add(cb.equal(root.get("skill"), skill));
        }
        if (StringUtils.hasText(cefrLevel)) {
            preds.add(cb.equal(root.get("cefrLevel"), cefrLevel));
        }
        if (StringUtils.hasText(teacherEmail)) {
            preds.add(cb.equal(root.get("user").get("email"), teacherEmail));
        }
        if (StringUtils.hasText(keyword)) {
            preds.add(cb.or(
                cb.like(root.get("content"), "%" + keyword + "%"),
                cb.like(root.get("topic"), "%" + keyword + "%")
            ));
        }
        if (StringUtils.hasText(questionType)) {
            preds.add(cb.equal(root.get("questionType"), questionType));
        }
        if (StringUtils.hasText(fromDate)) {
            LocalDateTime from = LocalDate.parse(fromDate).atStartOfDay();
            preds.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (StringUtils.hasText(toDate)) {
            LocalDateTime to = LocalDate.parse(toDate).atTime(23, 59, 59);
            preds.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
        }

        return cb.and(preds.toArray(new Predicate[0]));
    };

    return questionRepository.findAll(spec, pageable);
}
```

- [ ] **Step 4: Add approveQuestion implementation**

Add after existing approve logic:

```java
@Override
public void approveQuestion(Long questionId, String expertEmail, String reviewNote) {
    Question question = questionRepository.findById(questionId)
        .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));

    if (!"PENDING_REVIEW".equals(question.getStatus())) {
        throw new InvalidDataException("Question is not pending review");
    }
    if (!"TEACHER_PRIVATE".equals(question.getSource())) {
        throw new InvalidDataException("Only teacher-private questions can be approved here");
    }

    User expert = userRepository.findByEmail(expertEmail)
        .orElseThrow(() -> new ResourceNotFoundException("Expert not found"));

    question.setStatus("PUBLISHED");
    question.setReviewerId(expert.getUserId());
    question.setReviewedAt(LocalDateTime.now());
    question.setReviewNote(reviewNote);
    questionRepository.save(question);

    // Fire notification to teacher
    notificationService.sendQuestionApproved(
        question.getUser().getUserId(),
        question.getContent(),
        null
    );
}
```

- [ ] **Step 5: Add rejectQuestion implementation**

Add after existing reject logic:

```java
@Override
public void rejectQuestion(Long questionId, String expertEmail, String reviewNote, boolean delete) {
    Question question = questionRepository.findById(questionId)
        .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));

    if (!"PENDING_REVIEW".equals(question.getStatus())) {
        throw new InvalidDataException("Question is not pending review");
    }

    User expert = userRepository.findByEmail(expertEmail)
        .orElseThrow(() -> new ResourceNotFoundException("Expert not found"));

    question.setReviewerId(expert.getUserId());
    question.setReviewedAt(LocalDateTime.now());
    question.setReviewNote(reviewNote);

    if (delete) {
        questionRepository.delete(question);
    } else {
        question.setStatus("DRAFT");
        questionRepository.save(question);
    }

    // Fire notification to teacher
    notificationService.sendQuestionRejected(
        question.getUser().getUserId(),
        question.getContent(),
        reviewNote
    );
}
```

---

## Chunk 5: Expert Approval UI Page

### Task 9: Add view route for question approval

**Files:**
- Modify: `src/main/java/com/example/DoAn/controller/ExpertViewController.java`

- [ ] **Step 1: Read the controller**

Run: Read `ExpertViewController.java`.

- [ ] **Step 2: Add route**

Add after existing expert routes:

```java
@GetMapping("/expert/question-approval")
public String questionApprovalPage() {
    return "expert/question-approval";
}
```

### Task 10: Create expert/question-approval.html

**Files:**
- Create: `src/main/resources/templates/expert/question-approval.html`

- [ ] **Step 1: Write the template**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      th:replace="~{fragments/layout :: layout(~{::title}, ~{::#content})}">
<head>
    <title>Phê duyệt câu hỏi</title>
</head>
<body>
<div id="content" class="container-fluid mt-4">

    <h3>📋 Câu hỏi chờ phê duyệt</h3>

    <!-- Filters -->
    <div class="card mb-3">
        <div class="card-body">
            <form id="filterForm" class="row g-2">
                <div class="col-md-2">
                    <select name="skill" id="filterSkill" class="form-select">
                        <option value="">Kỹ năng</option>
                        <option value="LISTENING">LISTENING</option>
                        <option value="READING">READING</option>
                        <option value="WRITING">WRITING</option>
                        <option value="SPEAKING">SPEAKING</option>
                    </select>
                </div>
                <div class="col-md-2">
                    <select name="cefrLevel" id="filterCefr" class="form-select">
                        <option value="">CEFR</option>
                        <option value="A1">A1</option>
                        <option value="A2">A2</option>
                        <option value="B1">B1</option>
                        <option value="B2">B2</option>
                        <option value="C1">C1</option>
                        <option value="C2">C2</option>
                    </select>
                </div>
                <div class="col-md-2">
                    <select name="questionType" id="filterType" class="form-select">
                        <option value="">Loại câu hỏi</option>
                        <option value="MULTIPLE_CHOICE_SINGLE">Trắc nghiệm 1 đáp án</option>
                        <option value="MULTIPLE_CHOICE_MULTI">Trắc nghiệm nhiều đáp án</option>
                        <option value="FILL_IN_BLANK">Điền khuyết</option>
                        <option value="MATCHING">Nối cột</option>
                        <option value="WRITING">Viết</option>
                        <option value="SPEAKING">Nói</option>
                    </select>
                </div>
                <div class="col-md-3">
                    <input type="text" id="filterKeyword" class="form-control"
                           placeholder="Tìm kiếm nội dung...">
                </div>
                <div class="col-md-1">
                    <button type="button" class="btn btn-primary w-100" onclick="loadQueue(0)">Lọc</button>
                </div>
            </form>
        </div>
    </div>

    <!-- Queue -->
    <div id="queueContainer">
        <!-- Loaded via JS -->
    </div>

    <!-- Pagination -->
    <nav id="paginationNav" class="mt-3" style="display:none">
        <ul class="pagination justify-content-center" id="paginationList">
        </ul>
    </nav>

    <!-- Approval Modal -->
    <div class="modal fade" id="approveModal" tabindex="-1">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">Phê duyệt câu hỏi</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <p>Xác nhận phê duyệt câu hỏi này?</p>
                    <div class="mb-3">
                        <label class="form-label">Ghi chú (tùy chọn)</label>
                        <textarea id="approveNote" class="form-control" rows="2"
                                  maxlength="500" placeholder="Nhận xét cho giáo viên..."></textarea>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                    <button type="button" class="btn btn-success" id="confirmApproveBtn">✓ Phê duyệt</button>
                </div>
            </div>
        </div>
    </div>

    <!-- Reject Modal -->
    <div class="modal fade" id="rejectModal" tabindex="-1">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">Từ chối câu hỏi</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <div class="mb-2">
                        <label class="form-label">Lý do từ chối *</label>
                        <textarea id="rejectNote" class="form-control" rows="3"
                                  maxlength="500" required></textarea>
                    </div>
                    <div class="form-check">
                        <input class="form-check-input" type="checkbox" id="rejectDelete">
                        <label class="form-check-label" for="rejectDelete">
                            Xóa vĩnh viễn câu hỏi này
                        </label>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                    <button type="button" class="btn btn-danger" id="confirmRejectBtn">✗ Từ chối</button>
                </div>
            </div>
        </div>
    </div>

    <!-- Preview Modal -->
    <div class="modal fade" id="previewModal" tabindex="-1">
        <div class="modal-dialog modal-lg">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">Chi tiết câu hỏi</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body" id="previewBody">
                    <!-- Filled by JS -->
                </div>
            </div>
        </div>
    </div>
</div>

<script th:inline="javascript">
let currentPage = 0;
let currentQuestionId = null;
let approveModal, rejectModal, previewModal;

document.addEventListener('DOMContentLoaded', () => {
    approveModal = new bootstrap.Modal(document.getElementById('approveModal'));
    rejectModal = new bootstrap.Modal(document.getElementById('rejectModal'));
    previewModal = new bootstrap.Modal(document.getElementById('previewModal'));
    loadQueue(0);
});

async function loadQueue(page = 0) {
    currentPage = page;
    const skill = document.getElementById('filterSkill').value;
    const cefr = document.getElementById('filterCefr').value;
    const type = document.getElementById('filterType').value;
    const keyword = document.getElementById('filterKeyword').value;

    let url = `/api/v1/expert/question-review/pending?page=${page}&size=20`;
    if (skill) url += `&skill=${skill}`;
    if (cefr) url += `&cefrLevel=${cefr}`;
    if (type) url += `&questionType=${type}`;
    if (keyword) url += `&keyword=${encodeURIComponent(keyword)}`;

    const resp = await fetch(url);
    const data = await resp.json();
    renderQueue(data.data);
    renderPagination(data.data);
}

function renderQueue(page) {
    const container = document.getElementById('queueContainer');
    const content = page.content || [];

    if (content.length === 0) {
        container.innerHTML = '<div class="alert alert-info">Không có câu hỏi nào chờ phê duyệt.</div>';
        return;
    }

    container.innerHTML = content.map(q => `
        <div class="card mb-2">
            <div class="card-body">
                <div class="d-flex justify-content-between align-items-start">
                    <div>
                        <span class="badge bg-primary me-1">${q.skill}</span>
                        <span class="badge bg-secondary me-1">${q.questionType}</span>
                        <span class="badge bg-info me-1">${q.cefrLevel}</span>
                        <div class="mt-2">
                            <small class="text-muted">
                                Giáo viên: ${q.user?.fullName || q.user?.email || 'N/A'} |
                                Tạo: ${q.createdAt ? new Date(q.createdAt).toLocaleDateString('vi-VN') : 'N/A'}
                            </small>
                        </div>
                        <p class="mt-1 mb-0" style="max-width:600px">
                            ${(q.content || '').substring(0, 200)}${(q.content || '').length > 200 ? '...' : ''}
                        </p>
                    </div>
                    <div class="btn-group">
                        <button class="btn btn-outline-primary btn-sm" onclick="showPreview(${q.questionId})">Xem</button>
                        <button class="btn btn-success btn-sm" onclick="openApprove(${q.questionId})">✓ Duyệt</button>
                        <button class="btn btn-danger btn-sm" onclick="openReject(${q.questionId})">✗ Từ chối</button>
                    </div>
                </div>
            </div>
        </div>
    `).join('');
}

function renderPagination(page) {
    const nav = document.getElementById('paginationNav');
    const list = document.getElementById('paginationList');
    if (page.totalPages <= 1) { nav.style.display = 'none'; return; }
    nav.style.display = 'block';
    list.innerHTML = '';
    for (let i = 0; i < page.totalPages; i++) {
        list.innerHTML += `<li class="page-item ${i === page.number ? 'active' : ''}">
            <a class="page-link" href="#" onclick="loadQueue(${i});return false">${i + 1}</a>
        </li>`;
    }
}

async function showPreview(questionId) {
    // Fetch full question detail
    const resp = await fetch(`/api/v1/expert/question-review/${questionId}`);
    const data = await resp.json();
    const q = data.data;
    document.getElementById('previewBody').innerHTML = `
        <p><strong>Nội dung:</strong><br>${q.content}</p>
        <p><strong>Loại:</strong> ${q.questionType} | <strong>Kỹ năng:</strong> ${q.skill} | <strong>CEFR:</strong> ${q.cefrLevel}</p>
        <p><strong>Người tạo:</strong> ${q.user?.fullName || q.user?.email}</p>
        ${q.audioUrl ? `<p><audio controls src="${q.audioUrl}" class="w-100"></audio></p>` : ''}
        ${q.imageUrl ? `<p><img src="${q.imageUrl}" style="max-width:300px"></p>` : ''}
        <div id="optionsList"></div>
    `;
    previewModal.show();
}

function openApprove(questionId) {
    currentQuestionId = questionId;
    document.getElementById('approveNote').value = '';
    approveModal.show();
}

document.getElementById('confirmApproveBtn').addEventListener('click', async () => {
    const note = document.getElementById('approveNote').value;
    await fetch(`/api/v1/expert/question-review/${currentQuestionId}/approve`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ reviewNote: note })
    });
    approveModal.hide();
    loadQueue(currentPage);
});

function openReject(questionId) {
    currentQuestionId = questionId;
    document.getElementById('rejectNote').value = '';
    document.getElementById('rejectDelete').checked = false;
    rejectModal.show();
}

document.getElementById('confirmRejectBtn').addEventListener('click', async () => {
    const note = document.getElementById('rejectNote').value;
    if (!note.trim()) { alert('Vui lòng nhập lý do từ chối'); return; }
    const del = document.getElementById('rejectDelete').checked;
    await fetch(`/api/v1/expert/question-review/${currentQuestionId}/reject`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ reviewNote: note, delete: del })
    });
    rejectModal.hide();
    loadQueue(currentPage);
});
</script>
</body>
</html>
```

---

## Chunk 6: Teacher "My Questions" Page

### Task 11: Add view route for teacher's questions

**Files:**
- Modify: `src/main/java/com/example/DoAn/controller/TeacherViewController.java` (or wherever teacher views are)

- [ ] **Step 1: Find the teacher view controller**

Run: `Glob` for `**/Teacher*Controller.java` to find the right file.

- [ ] **Step 2: Add route**

Add a route:

```java
@GetMapping("/teacher/my-questions")
public String myQuestionsPage() {
    return "teacher/my-questions";
}
```

Also add a new API controller for listing teacher's own questions:

**Create:** `src/main/java/com/example/DoAn/controller/TeacherQuestionController.java`

```java
@RestController
@RequestMapping("/api/v1/teacher/questions")
@RequiredArgsConstructor
public class TeacherQuestionController {

    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;

    @GetMapping("/my")
    public ResponseEntity<ResponseData<Page<Question>>> getMyQuestions(
            Authentication auth,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {

        User teacher = userRepository.findByEmail(auth.getName())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Specification<Question> spec = (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.equal(root.get("source"), "TEACHER_PRIVATE"));
            preds.add(cb.equal(root.get("user").get("userId"), teacher.getUserId()));
            if (status != null && !status.isBlank()) {
                preds.add(cb.equal(root.get("status"), status));
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };
        return ResponseData.success(questionRepository.findAll(spec, pageable));
    }
}
```

### Task 12: Create teacher/my-questions.html

**Files:**
- Create: `src/main/resources/templates/teacher/my-questions.html`

- [ ] **Step 1: Write the template**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      th:replace="~{fragments/layout :: layout(~{::title}, ~{::#content})}">
<head><title>Câu hỏi của tôi</title></head>
<body>
<div id="content" class="container mt-4">
    <h3>Câu hỏi của tôi</h3>

    <ul class="nav nav-tabs mb-3" id="statusTabs">
        <li class="nav-item">
            <a class="nav-link active" href="#" data-status="">Tất cả</a>
        </li>
        <li class="nav-item">
            <a class="nav-link" href="#" data-status="PENDING_REVIEW">⏳ Chờ phê duyệt</a>
        </li>
        <li class="nav-item">
            <a class="nav-link" href="#" data-status="PUBLISHED">✓ Đã phê duyệt</a>
        </li>
        <li class="nav-item">
            <a class="nav-link" href="#" data-status="DRAFT">Bị từ chối</a>
        </li>
    </ul>

    <div id="questionsList"></div>

    <nav id="paginationNav" class="mt-3" style="display:none">
        <ul class="pagination justify-content-center" id="paginationList"></ul>
    </nav>
</div>

<script>
let currentStatus = '';
let currentPage = 0;

document.querySelectorAll('#statusTabs .nav-link').forEach(tab => {
    tab.addEventListener('click', (e) => {
        e.preventDefault();
        document.querySelectorAll('#statusTabs .nav-link').forEach(t => t.classList.remove('active'));
        tab.classList.add('active');
        currentStatus = tab.dataset.status;
        loadQuestions(0);
    });
});

async function loadQuestions(page = 0) {
    currentPage = page;
    let url = `/api/v1/teacher/questions/my?page=${page}&size=20`;
    if (currentStatus) url += `&status=${currentStatus}`;
    const resp = await fetch(url);
    const data = await resp.json();
    render(data.data);
}

function render(page) {
    const list = document.getElementById('questionsList');
    const content = page.content || [];
    if (content.length === 0) {
        list.innerHTML = '<div class="alert alert-info">Không có câu hỏi nào.</div>';
        return;
    }
    list.innerHTML = content.map(q => {
        let badge, badgeClass;
        if (q.status === 'PUBLISHED') { badge = 'Đã phê duyệt'; badgeClass = 'bg-success'; }
        else if (q.status === 'PENDING_REVIEW') { badge = 'Chờ phê duyệt'; badgeClass = 'bg-warning text-dark'; }
        else if (q.status === 'DRAFT') { badge = 'Bị từ chối'; badgeClass = 'bg-danger'; }
        else { badge = q.status; badgeClass = 'bg-secondary'; }

        return `
        <div class="card mb-2">
            <div class="card-body">
                <div class="d-flex justify-content-between">
                    <div>
                        <span class="badge ${badgeClass}">${badge}</span>
                        <span class="badge bg-primary">${q.skill}</span>
                        <span class="badge bg-secondary">${q.questionType}</span>
                        <p class="mt-2 mb-1">${(q.content||'').substring(0,150)}...</p>
                        ${q.reviewNote ? `<small class="text-danger">💬 Ghi chú: ${q.reviewNote}</small>` : ''}
                        ${q.reviewedAt ? `<br><small class="text-muted">Đã duyệt: ${new Date(q.reviewedAt).toLocaleDateString('vi-VN')}</small>` : ''}
                    </div>
                </div>
            </div>
        </div>`;
    }).join('');

    const nav = document.getElementById('paginationNav');
    const pl = document.getElementById('paginationList');
    if (page.totalPages <= 1) { nav.style.display = 'none'; return; }
    nav.style.display = 'block';
    pl.innerHTML = '';
    for (let i = 0; i < page.totalPages; i++) {
        pl.innerHTML += `<li class="page-item ${i===page.number?'active':''}">
            <a class="page-link" href="#" onclick="loadQuestions(${i});return false">${i+1}</a>
        </li>`;
    }
}

loadQuestions(0);
</script>
</body>
</html>
```

---

## Spec Reference

See `docs/superpowers/specs/2026-04-04-003-expert-approves-teacher-questions-design.md`.
