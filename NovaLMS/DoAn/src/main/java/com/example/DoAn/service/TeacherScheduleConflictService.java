package com.example.DoAn.service;

import com.example.DoAn.model.ClassSession;
import com.example.DoAn.repository.ClassSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Trùng lịch: cùng giáo viên, cùng ngày (theo {@link LocalDate} của {@code sessionDate}),
 * cùng giờ bắt đầu (chuẩn hóa HH:mm). Bỏ qua lớp {@code Closed}/{@code Cancelled}.
 */
@Service
@RequiredArgsConstructor
public class TeacherScheduleConflictService {

    private final ClassSessionRepository classSessionRepository;

    private String normalizeHourMinute(String time) {
        if (time == null || time.isBlank()) {
            return "";
        }
        String t = time.trim();
        String[] parts = t.split(":");
        if (parts.length != 2) {
            return t;
        }
        try {
            int hour = Integer.parseInt(parts[0].trim());
            int minute = Integer.parseInt(parts[1].trim());
            return String.format("%02d:%02d", hour, minute);
        } catch (NumberFormatException ex) {
            return t;
        }
    }

    private String effectiveStartTime(ClassSession s) {
        if (s.getStartTime() != null && !s.getStartTime().isBlank()) {
            return normalizeHourMinute(s.getStartTime());
        }
        if (s.getSessionDate() != null) {
            return String.format("%02d:%02d",
                    s.getSessionDate().getHour(),
                    s.getSessionDate().getMinute());
        }
        return "";
    }

    private String scheduleKey(ClassSession s) {
        if (s.getSessionDate() == null) {
            return "";
        }
        LocalDate d = s.getSessionDate().toLocalDate();
        String st = effectiveStartTime(s);
        return d.toString() + "|" + st;
    }

    /**
     * Kiểm tra danh sách buổi sắp lưu (cùng một lớp hoặc generate) không trùng buổi đã có của GV.
     *
     * @param excludeClassId loại trừ mọi session thuộc lớp này (khi regenerate / update lớp)
     */
    public void assertProposedSessionsHaveNoTeacherConflict(Integer teacherId, List<ClassSession> proposed,
            Integer excludeClassId) {
        if (teacherId == null || proposed == null || proposed.isEmpty()) {
            return;
        }

        Set<String> seen = new HashSet<>();
        for (ClassSession p : proposed) {
            String key = scheduleKey(p);
            if (key.isEmpty() || key.endsWith("|")) {
                throw new RuntimeException("Buổi học thiếu ngày hoặc giờ bắt đầu hợp lệ.");
            }
            if (!seen.add(key)) {
                throw new RuntimeException("Trùng lịch trong cùng danh sách buổi được tạo: " + key.replace('|', ' '));
            }
        }

        LocalDate min = proposed.stream()
                .map(ClassSession::getSessionDate)
                .filter(Objects::nonNull)
                .map(LocalDateTime::toLocalDate)
                .min(LocalDate::compareTo)
                .orElse(null);
        LocalDate max = proposed.stream()
                .map(ClassSession::getSessionDate)
                .filter(Objects::nonNull)
                .map(LocalDateTime::toLocalDate)
                .max(LocalDate::compareTo)
                .orElse(null);
        if (min == null || max == null) {
            return;
        }

        LocalDateTime from = min.atStartOfDay();
        LocalDateTime to = max.plusDays(1).atStartOfDay();

        List<ClassSession> existing = classSessionRepository.findTeacherSessionsForConflictScan(
                teacherId, from, to, excludeClassId, null);

        Map<String, ClassSession> occupied = new HashMap<>();
        for (ClassSession e : existing) {
            String k = scheduleKey(e);
            if (!k.isEmpty() && !k.endsWith("|")) {
                occupied.putIfAbsent(k, e);
            }
        }
        for (ClassSession p : proposed) {
            String key = scheduleKey(p);
            ClassSession e = occupied.get(key);
            if (e != null) {
                String className = e.getClazz() != null && e.getClazz().getClassName() != null
                        ? e.getClazz().getClassName()
                        : "(lớp)";
                LocalDate d = p.getSessionDate().toLocalDate();
                throw new RuntimeException(String.format(
                        "Trùng lịch dạy: ngày %s, ca %s đã có buổi học trong lớp \"%s\".",
                        d, effectiveStartTime(p), className));
            }
        }
    }

    /**
     * Một buổi đơn lẻ (tạo/sửa tay) có trùng với buổi khác của cùng GV không.
     */
    public Optional<String> checkSingleSessionConflict(Integer teacherId, LocalDateTime sessionDate, String startTime,
            Integer excludeClassId, Integer excludeSessionId) {
        if (teacherId == null || sessionDate == null) {
            return Optional.empty();
        }
        String normalized = normalizeHourMinute(startTime != null ? startTime : "");
        if (normalized.isEmpty()) {
            normalized = String.format("%02d:%02d", sessionDate.getHour(), sessionDate.getMinute());
        }

        LocalDate day = sessionDate.toLocalDate();
        LocalDateTime startOfDay = day.atStartOfDay();
        LocalDateTime endOfDay = day.plusDays(1).atStartOfDay();

        List<ClassSession> existing = classSessionRepository.findTeacherSessionsForConflictScan(
                teacherId, startOfDay, endOfDay, excludeClassId, excludeSessionId);

        String key = day + "|" + normalized;
        for (ClassSession e : existing) {
            if (key.equals(scheduleKey(e))) {
                String className = e.getClazz() != null && e.getClazz().getClassName() != null
                        ? e.getClazz().getClassName()
                        : "(lớp)";
                return Optional.of(String.format(
                        "Trùng lịch dạy: ngày %s, ca %s đã có buổi học trong lớp \"%s\".",
                        day, normalized, className));
            }
        }
        return Optional.empty();
    }
}
