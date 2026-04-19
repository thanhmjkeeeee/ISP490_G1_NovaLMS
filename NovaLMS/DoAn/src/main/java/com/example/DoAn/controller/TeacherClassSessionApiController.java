package com.example.DoAn.controller;

import com.example.DoAn.dto.request.AssignmentScheduleRequestDTO;
import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.service.TeacherClassSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/teacher/class-sessions")
@RequiredArgsConstructor
public class TeacherClassSessionApiController {

    private final TeacherClassSessionService sessionService;

    private String getEmail(Principal principal) {
        if (principal instanceof OAuth2AuthenticationToken token) {
            return token.getPrincipal().getAttribute("email");
        }
        return principal != null ? principal.getName() : null;
    }

    @GetMapping("/{classId}")
    public ResponseData<List<Map<String, Object>>> getSessions(@PathVariable Integer classId, Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Vui lòng đăng nhập.");
        return sessionService.getSessionsByClass(email, classId);
    }

    @GetMapping("/{classId}/detail")
    public ResponseData<Map<String, Object>> getClassDetail(@PathVariable Integer classId, Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Vui lòng đăng nhập.");
        return sessionService.getClassSessionsDetail(email, classId);
    }

    @GetMapping("/{classId}/quizzes")
    public ResponseData<List<Map<String, Object>>> getQuizzes(@PathVariable Integer classId, Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Vui lòng đăng nhập.");
        return sessionService.getAvailableQuizzes(email, classId);
    }

    @PostMapping("/{classId}")
    public ResponseData<Integer> createSession(
            @PathVariable Integer classId,
            @RequestParam Integer sessionNumber,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime sessionDate,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) Integer quizId,
            Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Vui lòng đăng nhập.");
        return sessionService.createSession(email, classId, sessionNumber, sessionDate, startTime, endTime, topic, notes, quizId);
    }

    @PutMapping("/update/{sessionId}")
    public ResponseData<Void> updateSession(
            @PathVariable Integer sessionId,
            @RequestParam(required = false) Integer sessionNumber,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime sessionDate,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) Integer quizId,
            Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Vui lòng đăng nhập.");
        return sessionService.updateSession(email, sessionId, sessionNumber, sessionDate, startTime, endTime, topic, notes, quizId);
    }

    @DeleteMapping("/{sessionId}")
    public ResponseData<Void> deleteSession(@PathVariable Integer sessionId, Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Vui lòng đăng nhập.");
        return sessionService.deleteSession(email, sessionId);
    }

    // ─────────────────────────────────────────────
    //  MULTI-QUIZ MANAGEMENT
    // ─────────────────────────────────────────────

    /**
     * Gắn quiz vào session.
     * POST /api/v1/teacher/class-sessions/{sessionId}/quizzes
     * Body: { "quizId": 123 }
     */
    @PostMapping("/{sessionId}/quizzes")
    public ResponseData<?> addQuizToSession(
            @PathVariable Integer sessionId,
            @RequestBody Map<String, Integer> body,
            Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Vui lòng đăng nhập.");
        Integer quizId = body.get("quizId");
        if (quizId == null) return ResponseData.error(400, "Thiếu mã quiz (quizId)");
        return sessionService.addQuizToSession(email, sessionId, quizId);
    }

    /**
     * Xóa quiz khỏi session.
     * DELETE /api/v1/teacher/class-sessions/{sessionId}/quizzes/{quizId}
     */
    @DeleteMapping("/{sessionId}/quizzes/{quizId}")
    public ResponseData<Void> removeQuizFromSession(
            @PathVariable Integer sessionId,
            @PathVariable Integer quizId,
            Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Vui lòng đăng nhập.");
        return sessionService.removeQuizFromSession(email, sessionId, quizId);
    }

    /**
     * Toggle mở/đóng 1 quiz trong session.
     * PATCH /api/v1/teacher/class-sessions/{sessionId}/quizzes/{quizId}/toggle-open
     */
    @PatchMapping("/{sessionId}/quizzes/{quizId}/toggle-open")
    public ResponseData<?> toggleQuizOpenInSession(
            @PathVariable Integer sessionId,
            @PathVariable Integer quizId,
            @RequestParam(required = false) Integer timeLimitMinutes,
            Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Vui lòng đăng nhập.");
        return sessionService.toggleQuizOpenInSession(email, sessionId, quizId, timeLimitMinutes);
    }

    @PatchMapping("/{oldSessionId}/quizzes/{quizId}/reassign")
    public ResponseData<Void> reassignQuiz(
            @PathVariable Integer oldSessionId,
            @PathVariable Integer quizId,
            @RequestBody Map<String, Integer> body,
            Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Vui lòng đăng nhập.");
        Integer newSessionId = body.get("newSessionId");
        if (newSessionId == null) return ResponseData.error(400, "Thiếu mã buổi học mới (newSessionId)");
        return sessionService.reassignQuiz(email, quizId, oldSessionId, newSessionId);
    }

    @PatchMapping("/{sessionId}/quizzes/{quizId}/toggle-open-with-time")
    public ResponseData<?> toggleQuizOpenInSessionWithTime(
            @PathVariable Integer sessionId,
            @PathVariable Integer quizId,
            @RequestParam Integer timeLimitMinutes,
            Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Vui lòng đăng nhập.");
        return sessionService.toggleQuizOpenInSession(email, sessionId, quizId, timeLimitMinutes);
    }

    /**
     * Mở tất cả quiz trong session.
     * PATCH /api/v1/teacher/class-sessions/{sessionId}/quizzes/open-all
     */
    @PatchMapping("/{sessionId}/quizzes/open-all")
    public ResponseData<?> openAllQuizzesInSession(
            @PathVariable Integer sessionId,
            Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Vui lòng đăng nhập.");
        return sessionService.openAllQuizzesInSession(email, sessionId);
    }

    /**
     * Đóng tất cả quiz trong session.
     * PATCH /api/v1/teacher/class-sessions/{sessionId}/quizzes/close-all
     */
    @PatchMapping("/{sessionId}/quizzes/close-all")
    public ResponseData<?> closeAllQuizzesInSession(
            @PathVariable Integer sessionId,
            Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Vui lòng đăng nhập.");
        return sessionService.closeAllQuizzesInSession(email, sessionId);
    }

    // ─────────────────────────────────────────────
    //  MATERIALS (FILE UPLOAD)
    // ─────────────────────────────────────────────

    /**
     * Upload file tài liệu cho session.
     * POST /api/v1/teacher/class-sessions/{sessionId}/materials
     */
    @PostMapping("/{sessionId}/materials")
    public ResponseData<?> uploadMaterials(
            @PathVariable Integer sessionId,
            @RequestParam("files") List<MultipartFile> files,
            Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Vui lòng đăng nhập.");
        return sessionService.uploadMaterials(email, sessionId, files);
    }

    /**
     * Xóa file tài liệu khỏi session.
     * DELETE /api/v1/teacher/class-sessions/{sessionId}/materials/{filename}
     */
    @DeleteMapping("/{sessionId}/materials/{filename}")
    public ResponseData<?> deleteMaterial(
            @PathVariable Integer sessionId,
            @PathVariable String filename,
            Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Vui lòng đăng nhập.");
        return sessionService.deleteMaterial(email, sessionId, filename);
    }

    // ─────────────────────────────────────────────
    //  WORKSPACE ADDITIONS
    // ─────────────────────────────────────────────

    @GetMapping("/{classId}/students")
    public ResponseData<?> getStudentsByClass(@PathVariable Integer classId, Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Vui lòng đăng nhập.");
        return sessionService.getStudentsByClass(email, classId);
    }

    @GetMapping("/{classId}/course-content")
    public ResponseData<?> getCourseContentForMapping(@PathVariable Integer classId, Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Vui lòng đăng nhập.");
        return sessionService.getCourseContentForMapping(email, classId);
    }

    @PostMapping("/{classId}/mapping")
    public ResponseData<?> saveMapping(
            @PathVariable Integer classId,
            @RequestBody List<Map<String, Integer>> mappings,
            Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Vui lòng đăng nhập.");
        return sessionService.saveMapping(email, classId, mappings);
    }

    // ─────────────────────────────────────────────
    //  MEET LINK
    // ─────────────────────────────────────────────

    /**
     * Cập nhật link Meet/Zoom cho buổi học cụ thể.
     * PATCH /api/v1/teacher/class-sessions/{sessionId}/meet-link
     * Body: { "meetLink": "https://..." }
     */
    @PatchMapping("/{sessionId}/meet-link")
    public ResponseData<Void> updateMeetLink(
            @PathVariable Integer sessionId,
            @RequestBody Map<String, String> body,
            Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Vui lòng đăng nhập.");
        String meetLink = body.get("meetLink");
        return sessionService.updateMeetLink(email, sessionId, meetLink);
    }

    // ─────────────────────────────────────────────
    //  ASSIGNMENT MANAGEMENT ENDPOINTS
    // ─────────────────────────────────────────────

    /**
     * Lấy danh sách assignment của lớp (các quiz trong session).
     */
    @GetMapping("/{classId}/assignments")
    public ResponseData<?> getAssignments(@PathVariable Integer classId, Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Vui lòng đăng nhập.");
        return sessionService.getAssignmentsByClass(email, classId);
    }

    /**
     * Lấy danh sách Expert Assignment (khung chương trình) kèm trạng thái gán buổi.
     */
    @GetMapping("/{classId}/expert-assignments")
    public ResponseData<?> getExpertAssignments(@PathVariable Integer classId, Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Vui lòng đăng nhập.");
        return sessionService.getExpertAssignmentsByClass(email, classId);
    }

    /**
     * Cấu hình thời gian mở/đóng cho 1 assignment trong session.
     */
    @PatchMapping("/session-quizzes/{sessionQuizId}/schedule")
    public ResponseData<Void> updateSchedule(
            @PathVariable Integer sessionQuizId,
            @RequestBody AssignmentScheduleRequestDTO request,
            Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Vui lòng đăng nhập.");
        return sessionService.updateAssignmentSchedule(email, sessionQuizId, request);
    }

    /**
     * Reset lượt làm bài cho student.
     */
    @DeleteMapping("/quizzes/{quizId}/students/{studentId}/reset")
    public ResponseData<Void> resetAttempt(
            @PathVariable Integer quizId,
            @PathVariable Long studentId,
            Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Vui lòng đăng nhập.");
        return sessionService.resetStudentAttempt(email, quizId, studentId);
    }
}
