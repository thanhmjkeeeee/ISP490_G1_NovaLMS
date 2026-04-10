package com.example.DoAn.service.impl;

import com.example.DoAn.dto.response.TeacherDashboardResponseDTO;
import com.example.DoAn.model.*;
import com.example.DoAn.repository.*;
import com.example.DoAn.service.ITeacherDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherDashboardServiceImpl implements ITeacherDashboardService {

    private final ClazzRepository clazzRepository;
    private final ClassSessionRepository classSessionRepository;
    private final RegistrationRepository registrationRepository;
    private final QuizResultRepository quizResultRepository;
    private final UserRepository userRepository;

    @Override
    public TeacherDashboardResponseDTO getDashboardData(String email) {
        TeacherDashboardResponseDTO dto = new TeacherDashboardResponseDTO();

        // 1. Find teacher by email
        User teacher = userRepository.findByEmail(email).orElse(null);
        if (teacher == null) {
            return dto;
        }
        dto.setFullName(teacher.getFullName());
        Integer teacherId = teacher.getUserId();

        // 2. Active Classes
        List<Clazz> activeClasses = clazzRepository.findAllByTeacher_UserId(teacherId);
        dto.setActiveClasses(activeClasses.size());

        // 3. Total Students (Optimized - Avoid loop if possible, but small enough to keep for now)
        int totalStudents = activeClasses.stream()
                .mapToInt(clazz -> registrationRepository.findApprovedByClassId(clazz.getClassId()).size())
                .sum();
        dto.setTotalStudents(totalStudents);

        // 4. Today's Classes
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
        List<ClassSession> todaySessions = classSessionRepository.findByTeacherAndDateRange(teacherId, startOfDay, endOfDay);
        dto.setTodayClasses(todaySessions.size());

        List<Integer> classIds = activeClasses.stream()
                .map(Clazz::getClassId)
                .collect(Collectors.toList());

        // 5 & 6. Pending Grading & Unlock Requests (🔥 TỐI ƯU HÓA: BỎ VÒNG LẶP N+1)
        long pendingGrading = 0;
        long unlockRequests = 0;

        if (!classIds.isEmpty()) {
            // Thay vì lặp qua từng classId, ta chỉ cần sửa Repository để nhận vào 1 List<Integer> classIds
            // Tạm thời dùng vòng lặp nhưng gộp vào 1 khối để dễ kiểm soát (nếu bạn chưa update Repository)
            // Lời khuyên: Hãy vào QuizResultRepository viết thêm hàm IN (classIds) để tối ưu sau.
            for (Integer classId : classIds) {
                pendingGrading += quizResultRepository.countPendingGradingForClass(classId);
                unlockRequests += quizResultRepository.countUnlockRequestsForClass(classId);
            }
        }
        dto.setPendingGrading((int) pendingGrading);
        dto.setUnlockRequests((int) unlockRequests);

        // 7. Alert Students (Mock Data as requested)
        // 7. Alert Students (Real Data: Điểm trung bình dưới 4.0)
        List<TeacherDashboardResponseDTO.AlertStudentItem> alerts = new ArrayList<>();

        if (!classIds.isEmpty()) {
            // Lấy danh sách học viên có điểm TB < 4.0 thông qua 1 câu query duy nhất
            List<Object[]> underperformingStudents = quizResultRepository.findStudentsWithAverageScoreBelow(classIds, 4.0);

            for (Object[] record : underperformingStudents) {
                String studentName = (String) record[0];
                // String studentEmail = (String) record[1]; // Tạm thời chưa dùng email để hiển thị ở đây
                Double avgScore = (Double) record[2];

                String initials = getInitials(studentName);

                // Format điểm lấy 1 chữ số thập phân (VD: 3.5)
                String formattedScore = String.format("%.1f", avgScore);

                alerts.add(createAlert(
                        studentName,
                        initials,
                        "red", // Dùng màu đỏ cảnh báo nguy hiểm
                        "Điểm trung bình các bài tập hiện tại là " + formattedScore,
                        "▼ " + formattedScore + " TB",
                        "score"
                ));
            }
        }
        dto.setAlertStudents(alerts);

        // 8. Today's Schedule
        List<TeacherDashboardResponseDTO.ScheduleItem> scheduleItems = todaySessions.stream()
                .map(session -> {
                    TeacherDashboardResponseDTO.ScheduleItem item = new TeacherDashboardResponseDTO.ScheduleItem();
                    item.setClassName(session.getClazz().getClassName());
                    item.setStartTime(session.getStartTime());
                    item.setEndTime(session.getEndTime());
                    item.setSessionDate(session.getSessionDate());
                    item.setSlotName("Slot " + (session.getSlotNumber() != null ? session.getSlotNumber() : "?"));
                    item.setSessionNumber(session.getSessionNumber());
                    item.setIsToday(true);
                    return item;
                })
                .collect(Collectors.toList());
        dto.setTodaySchedule(scheduleItems);

        List<TeacherDashboardResponseDTO.RecentSubmissionItem> recentSubmissions = new ArrayList<>();
        if (!classIds.isEmpty()) {
            // Chỉ query 5 dòng đầu tiên trên TỔNG CỘNG TẤT CẢ các lớp, không phải 5 dòng / 1 lớp
            // Điều này tránh việc fetch hàng ngàn object rồi mới sort trong RAM
            Pageable pageable = PageRequest.of(0, 5);

            // Loop is still here due to Repo structure, but we limit aggressive fetching
            for (Integer classId : classIds) {
                if (recentSubmissions.size() >= 5) break; // Thoát sớm nếu đã lấy đủ 5 bài

                List<QuizResult> classResults = quizResultRepository.findRecentSubmissionsByClassId(classId, pageable);

                recentSubmissions.addAll(classResults.stream()
                        .map(qr -> {
                            TeacherDashboardResponseDTO.RecentSubmissionItem item = new TeacherDashboardResponseDTO.RecentSubmissionItem();
                            String fullName = qr.getUser() != null ? qr.getUser().getFullName() : "Unknown";
                            item.setStudentName(fullName);
                            item.setStudentEmail(qr.getUser() != null ? qr.getUser().getEmail() : "");
                            item.setInitials(getInitials(fullName));
                            item.setQuizTitle(qr.getQuiz() != null ? qr.getQuiz().getTitle() : "Unknown");
                            item.setClassName(qr.getQuiz() != null && qr.getQuiz().getClazz() != null ? qr.getQuiz().getClazz().getClassName() : "Unknown");
                            item.setSubmittedAt(qr.getSubmittedAt());

                            // 1. Xử lý điểm
                            item.setScore(qr.getScore() != null ? qr.getScore().doubleValue() : null);
                            item.setTotalScore(8); // Default

                            // 🔥 2. BẢN VÁ LỖI TRẠNG THÁI HIỂN THỊ 🔥
                            // Bỏ qua cột status của DB, dựa vào cờ 'passed' làm nguồn chân lý tuyệt đối
                            if (qr.getPassed() != null || "GRADED".equalsIgnoreCase(qr.getStatus()) || "ALL_GRADED".equalsIgnoreCase(qr.getStatus())) {
                                item.setStatus("ALL_GRADED"); // Báo cho JS biết là đã chấm xong
                            } else {
                                item.setStatus("PENDING");    // Báo cho JS biết là cần chấm
                            }

                            item.setResultId(qr.getResultId());
                            return item;
                        })
                        .collect(Collectors.toList()));
            }

            recentSubmissions = recentSubmissions.stream()
                    .sorted((a, b) -> {
                        if (a.getSubmittedAt() == null) return 1;
                        if (b.getSubmittedAt() == null) return -1;
                        return b.getSubmittedAt().compareTo(a.getSubmittedAt());
                    })
                    .limit(5)
                    .collect(Collectors.toList());
        }
        dto.setRecentSubmissions(recentSubmissions);

        return dto;
    }

    private TeacherDashboardResponseDTO.AlertStudentItem createAlert(String name, String initials, String color, String reason, String badge, String type) {
        TeacherDashboardResponseDTO.AlertStudentItem item = new TeacherDashboardResponseDTO.AlertStudentItem();
        item.setName(name);
        item.setInitials(initials);
        item.setAvatarColor(color);
        item.setReason(reason);
        item.setBadgeText(badge);
        item.setType(type);
        return item;
    }

    private String getInitials(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.split(" ");
        if (parts.length == 1) return name.substring(0, Math.min(2, name.length())).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }
}