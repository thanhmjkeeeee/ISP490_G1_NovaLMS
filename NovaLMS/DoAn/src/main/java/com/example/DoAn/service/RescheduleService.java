package com.example.DoAn.service;

import com.example.DoAn.dto.response.RescheduleResponseDTO;
import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.model.ClassSession;
import com.example.DoAn.dto.request.RescheduleRequest;
import com.example.DoAn.model.User;
import com.example.DoAn.repository.ClassSessionRepository;
import com.example.DoAn.repository.RescheduleRequestRepository;
import com.example.DoAn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RescheduleService {

    private final RescheduleRequestRepository rescheduleRequestRepository;
    private final ClassSessionRepository classSessionRepository;
    private final UserRepository userRepository;
    private final com.example.DoAn.service.EmailService emailService;
    private final com.example.DoAn.service.INotificationService notificationService;

    public Optional<RescheduleRequest> getPendingRequest(Integer sessionId) {
        return rescheduleRequestRepository.findPendingBySessionId(sessionId);
    }

    @Transactional
    public ResponseData<Integer> createRequest(Integer sessionId, String newDateStr, String newStartTime, String reason, String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null) return ResponseData.error(401, "Người dùng không tồn tại");

        ClassSession session = classSessionRepository.findByIdForUpdate(sessionId).orElse(null);
        if (session == null) return ResponseData.error(404, "Không tìm thấy buổi học");

        // Check ownership
        if (session.getClazz().getTeacher() == null || !session.getClazz().getTeacher().getUserId().equals(user.getUserId())) {
            return ResponseData.error(403, "Bạn không có quyền gửi yêu cầu cho buổi học này");
        }

        // Check if there's already a PENDING request
        Optional<RescheduleRequest> pending = rescheduleRequestRepository.findPendingBySessionId(sessionId);
        if (pending.isPresent()) {
            return ResponseData.error(400, "Bạn đã gửi yêu cầu đổi lịch cho buổi này, vui lòng chờ duyệt");
        }

        LocalDateTime newDate;
        try {
            // Frontend sends YYYY-MM-DD
                    newDate = LocalDate.parse(newDateStr).atStartOfDay();
        } catch (Exception e) {
            return ResponseData.error(400, "Định dạng ngày không hợp lệ (YYYY-MM-DD)");
        }

        // --- 1. Chặn đổi lịch buổi học trong quá khứ ---
        try {
            LocalTime oldTime = LocalTime.parse(session.getStartTime(), DateTimeFormatter.ofPattern("HH:mm"));
            LocalDateTime originalSessionTime = session.getSessionDate().withHour(oldTime.getHour()).withMinute(oldTime.getMinute());
            if (originalSessionTime.isBefore(LocalDateTime.now())) {
                return ResponseData.error(400, "Không thể đổi lịch cho buổi học đã diễn ra.");
            }
        } catch (Exception e) {
            log.error("Lỗi parse thời gian buổi học cũ: {}", e.getMessage());
        }

        // --- 2. Chặn đổi lịch trùng với lịch hiện tại ---
        if (newDate.toLocalDate().isEqual(session.getSessionDate().toLocalDate()) && newStartTime.equals(session.getStartTime())) {
            return ResponseData.error(400, "Thời gian đổi lịch không được trùng với lịch hiện tại.");
        }

        // --- 1. Chống trùng lịch với các buổi học ĐÃ XÁC NHẬN (Loại trừ chính nó) ---
        // Sử dụng HQL Range Query để Hibernate tự động xử lý Timezone chuẩn xác
        java.time.LocalDate targetDate = java.time.LocalDate.parse(newDateStr);
        java.time.LocalDateTime startOfDay = targetDate.atStartOfDay();
        java.time.LocalDateTime endOfDay = targetDate.plusDays(1).atStartOfDay();

        long conflictConfirmedCount = classSessionRepository.countConflictsInDateRange(
                user.getUserId(), startOfDay, endOfDay, newStartTime, sessionId);
        if (conflictConfirmedCount > 0) {
            return ResponseData.error(400, "Trùng lịch! Bạn đã có lịch dạy một lớp khác vào ca này.");
        }

        // --- 2. Chống trùng lịch với các yêu cầu ĐỔI LỊCH ĐANG CHỜ DUYỆT (PENDING) ---
        boolean conflictPending = rescheduleRequestRepository.existsByCreatedBy_UserIdAndNewDateAndNewStartTimeAndStatus(
                user.getUserId(), newDate, newStartTime, "PENDING");
        if (conflictPending) {
            return ResponseData.error(400, "Trùng lịch! Bạn đang có một yêu cầu đổi lịch khác vào ca này đang chờ duyệt.");
        }

        RescheduleRequest request = RescheduleRequest.builder()
                .session(session)
                .oldDate(session.getSessionDate())
                .oldStartTime(session.getStartTime())
                .newDate(newDate)
                .newStartTime(newStartTime)
                .reason(reason)
                .status("PENDING")
                .createdBy(user)
                .createdAt(LocalDateTime.now())
                .build();

        rescheduleRequestRepository.save(request);

        // ── Notify all managers ──────────────────────────────────────────────
        try {
            String className = session.getClazz().getClassName();
            String dateLabel = request.getNewDate().toLocalDate().toString();
            String teacherName = user.getFullName();
            
            // Fix: Sử dụng findByRole_Value vì repo đã có method này
            List<User> managers = userRepository.findByRole_Value("ROLE_MANAGER");
            for (User manager : managers) {
                notificationService.sendRescheduleRequestForManager(
                    Long.valueOf(manager.getUserId()), 
                    teacherName, 
                    className, 
                    dateLabel
                );
            }
        } catch (Exception e) {
            log.error("Lỗi gửi thông báo cho Manager: {}", e.getMessage());
        }

        return ResponseData.success("Gửi yêu cầu thành công", request.getId());
    }

    @Transactional(readOnly = true)
    public ResponseData<Page<RescheduleResponseDTO>> getRequestsForManager(String teacherName, String status, int page, int size) {
        // Normalize empty filters to null
        if (teacherName != null && teacherName.trim().isEmpty()) teacherName = null;
        if (status != null && status.trim().isEmpty()) status = null;

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<RescheduleRequest> result = rescheduleRequestRepository.findAllWithFilter(teacherName, status, pageable);

        // Map từ Entity sang DTO để tránh lỗi ByteBuddy Proxy của Hibernate
        Page<RescheduleResponseDTO> dtoPage = result.map(r -> RescheduleResponseDTO.builder()
                .id(r.getId())
                .oldDate(r.getOldDate() != null ? r.getOldDate().toString() : null)
                .oldStartTime(r.getOldStartTime())
                .newDate(r.getNewDate() != null ? r.getNewDate().toString() : null)
                .newStartTime(r.getNewStartTime())
                .reason(r.getReason())
                .managerNote(r.getManagerNote())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .createdBy(RescheduleResponseDTO.CreatorDTO.builder()
                        .fullName(r.getCreatedBy() != null ? r.getCreatedBy().getFullName() : "N/A")
                        .email(r.getCreatedBy() != null ? r.getCreatedBy().getEmail() : "")
                        .build())
                .session(RescheduleResponseDTO.SessionDTO.builder()
                        .sessionNumber(r.getSession() != null ? r.getSession().getSessionNumber() : 0)
                        .clazz(RescheduleResponseDTO.ClassDTO.builder()
                                .className(r.getSession() != null && r.getSession().getClazz() != null ? r.getSession().getClazz().getClassName() : "Lớp học rỗng")
                                .build())
                        .build())
                .build());

        return ResponseData.success("Thành công", dtoPage);
    }

    @Transactional
    public ResponseData<Void> updateStatus(Integer requestId, String status, String managerNote) {
        RescheduleRequest request = rescheduleRequestRepository.findById(requestId).orElse(null);
        if (request == null) return ResponseData.error(404, "Không tìm thấy yêu cầu");

        if (!"PENDING".equals(request.getStatus())) {
            return ResponseData.error(400, "Yêu cầu này đã được xử lý");
        }

        request.setStatus(status);
        request.setManagerNote(managerNote);

        if ("APPROVED".equals(status)) {
            ClassSession session = request.getSession();
            if (session != null) {
                session.setSessionDate(request.getNewDate());
                session.setStartTime(request.getNewStartTime());

                // Recalculate end_time (assume 2 hours)
                String endTime = calculateEndTime(request.getNewStartTime());
                session.setEndTime(endTime);
                session.setSlotNumber(calculateSlotNumber(request.getNewStartTime()));

                classSessionRepository.save(session);
            }
        }

        rescheduleRequestRepository.save(request);

        // ── Notify teacher of manager decision ───────────────────────────────
        notifyTeacherOnDecision(request);

        return ResponseData.success("Cập nhật trạng thái thành công");
    }

    private void notifyTeacherOnDecision(RescheduleRequest request) {
        if (request == null || request.getCreatedBy() == null) return;
        User teacher = request.getCreatedBy();
        Long teacherId = Long.valueOf(teacher.getUserId());
        String teacherName = teacher.getFullName() != null ? teacher.getFullName() : "";

        String className = request.getSession() != null && request.getSession().getClazz() != null
                ? request.getSession().getClazz().getClassName() != null
                        ? request.getSession().getClazz().getClassName() : "" : "";
        String newDate = request.getNewDate() != null ? request.getNewDate().toLocalDate().toString() : "";
        String newTime = request.getNewStartTime() != null ? request.getNewStartTime() : "";
        String managerNote = request.getManagerNote() != null ? request.getManagerNote() : "";
        String status = request.getStatus();

        // ── Internal Notification ───────────────────────────────────────────
        try {
            if ("APPROVED".equals(status)) {
                notificationService.sendSessionRescheduled(teacherId, className, newDate, newTime, "Đã được phê duyệt: " + managerNote);
            } else if ("REJECTED".equals(status)) {
                notificationService.send(teacherId, "SESSION_RESCHEDULE_REJECTED", 
                    "Yêu cầu đổi lịch bị từ chối", 
                    "Yêu cầu đổi lịch lớp " + className + " sang ngày " + newDate + " đã bị từ chối. Lý do: " + managerNote, 
                    "/teacher/workspace");
            }
        } catch (Exception e) {
            log.error("Error sending internal notification to teacher: {}", e.getMessage());
        }

        // ── Email Notification ──────────────────────────────────────────────
        try {
            if (teacher.getEmail() != null && !teacher.getEmail().isBlank()) {
                if ("APPROVED".equals(status)) {
                    emailService.sendSessionRescheduledEmail(teacher.getEmail(), teacherName, className,
                            "", "", newDate, newTime, managerNote);
                } else if ("REJECTED".equals(status)) {
                    emailService.sendSessionCancelledEmail(teacher.getEmail(), teacherName, className, newDate, newTime, managerNote);
                }
            }
        } catch (Exception e) {
            log.error("Error sending email notification to teacher: {}", e.getMessage());
        }
    }

    private String calculateEndTime(String startTime) {
        try {
            LocalTime start = LocalTime.parse(startTime, DateTimeFormatter.ofPattern("HH:mm"));
            LocalTime end = start.plusHours(2);
            return end.format(DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            return "";
        }
    }

    public int calculateSlotNumber(String startTime) {
        if (startTime == null || startTime.length() < 5) return 1;
        int hour;
        try {
            hour = Integer.parseInt(startTime.substring(0, 2));
        } catch (Exception e) {
            return 1;
        }
        if (hour >= 7 && hour < 9) return 1;
        if (hour >= 9 && hour < 11) return 2;
        if (hour >= 13 && hour < 15) return 3;
        if (hour >= 15 && hour < 17) return 4;
        if (hour >= 18 && hour < 20) return 5;
        if (hour < 7) return 1;
        if (hour >= 11 && hour < 13) return 2;
        if (hour >= 17 && hour < 18) return 4;
        return 5;
    }

    public long getPendingCount() {
        return rescheduleRequestRepository.findAll().stream()
                .filter(r -> "PENDING".equals(r.getStatus()))
                .count();
    }

    public List<RescheduleResponseDTO> getTeacherRequests(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return java.util.Collections.emptyList();
        
        List<RescheduleRequest> requests = rescheduleRequestRepository.findByCreatedBy_UserIdOrderByCreatedAtDesc(user.getUserId());
        
        return requests.stream().map(this::mapToDTO).collect(java.util.stream.Collectors.toList());
    }

    private RescheduleResponseDTO mapToDTO(RescheduleRequest r) {
        return RescheduleResponseDTO.builder()
                .id(r.getId())
                .oldDate(r.getOldDate() != null ? r.getOldDate().toString() : null)
                .oldStartTime(r.getOldStartTime())
                .newDate(r.getNewDate() != null ? r.getNewDate().toString() : null)
                .newStartTime(r.getNewStartTime())
                .reason(r.getReason())
                .managerNote(r.getManagerNote())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .createdBy(RescheduleResponseDTO.CreatorDTO.builder()
                        .fullName(r.getCreatedBy() != null ? r.getCreatedBy().getFullName() : "N/A")
                        .email(r.getCreatedBy() != null ? r.getCreatedBy().getEmail() : "")
                        .build())
                .session(RescheduleResponseDTO.SessionDTO.builder()
                        .sessionNumber(r.getSession() != null ? r.getSession().getSessionNumber() : 0)
                        .clazz(RescheduleResponseDTO.ClassDTO.builder()
                                .className(r.getSession() != null && r.getSession().getClazz() != null ? r.getSession().getClazz().getClassName() : "Lớp học rỗng")
                                .build())
                        .build())
                .build();
    }
}
