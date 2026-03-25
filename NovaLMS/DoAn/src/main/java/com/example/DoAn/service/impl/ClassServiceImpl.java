package com.example.DoAn.service.impl;

import com.example.DoAn.dto.request.ClassRequestDTO;
import com.example.DoAn.dto.response.ClassDetailResponse;
import com.example.DoAn.dto.response.PageResponse;
import com.example.DoAn.dto.response.RegistrationResponseDTO;
import com.example.DoAn.model.Clazz;
import com.example.DoAn.model.ClassSession;
import com.example.DoAn.repository.ClassRepository;
import com.example.DoAn.repository.ClassSessionRepository;
import com.example.DoAn.repository.CourseRepository;
import com.example.DoAn.repository.UserRepository;
import com.example.DoAn.service.IClassService;
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
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClassServiceImpl implements IClassService {

    private final ClassRepository classRepository;
    private final ClassSessionRepository classSessionRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Integer saveClass(ClassRequestDTO request) {
        Clazz clazz = Clazz.builder()
                .className(request.getClassName())
                .course(courseRepository.findById(request.getCourseId()).orElse(null))
                .teacher(request.getTeacherId() != null ? userRepository.findById(request.getTeacherId()).orElse(null) : null)
                .startDate(request.getStartDate() != null && !request.getStartDate().isEmpty() ? LocalDateTime.parse(request.getStartDate()) : null)
                .endDate(request.getEndDate() != null && !request.getEndDate().isEmpty() ? LocalDateTime.parse(request.getEndDate()) : null)
                .status(request.getStatus() != null ? request.getStatus() : "Pending")
                .schedule(request.getSchedule())
                .slotTime(request.getSlotTime())
                .numberOfSessions(request.getNumberOfSessions())
                .build();
        classRepository.save(clazz);

        // Tự động tạo các buổi học dựa trên lịch học và số buổi
        if (request.getStartDate() != null && !request.getStartDate().isEmpty()
                && request.getSchedule() != null && !request.getSchedule().isEmpty()
                && request.getSlotTime() != null && !request.getSlotTime().isEmpty()
                && request.getNumberOfSessions() != null && request.getNumberOfSessions() > 0) {

            List<ClassSession> sessions = generateSessions(clazz, request.getSchedule(), request.getSlotTime(),
                    request.getStartDate(), request.getNumberOfSessions());
            if (!sessions.isEmpty()) {
                classSessionRepository.saveAll(sessions);
                log.info("Created {} sessions for class {}", sessions.size(), clazz.getClassName());
            }
        }

        log.info("Class created successfully: {}", clazz.getClassName());
        return clazz.getClassId();
    }

    /**
     * Tự động tạo các buổi học dựa trên lịch học.
     * Ví dụ: schedule="Thứ 2, 4, 6" với slotTime="Tối (18:00 - 20:00)"
     * sẽ tạo buổi 1, 2, 3... vào đúng ngày thứ 2, 4, 6 trong khoảng startDate -> endDate
     */
    private List<ClassSession> generateSessions(Clazz clazz, String schedule, String slotTime,
            String startDateStr, Integer numberOfSessions) {

        List<ClassSession> sessions = new ArrayList<>();
        if (numberOfSessions == null || numberOfSessions <= 0) return sessions;

        // Parse slot time: "Tối (18:00 - 20:00)" -> start="18:00", end="20:00"
        String startTime = extractTime(slotTime, true);
        String endTime = extractTime(slotTime, false);

        // Parse các ngày trong tuần từ schedule
        // Hỗ trợ: "Thứ 2, 4, 6" | "Mon, Wed, Fri" | "2, 4, 6" | "T2,T4,T6"
        List<DayOfWeekInfo> weekdays = parseScheduleDays(schedule);
        if (weekdays.isEmpty()) {
            log.warn("Could not parse schedule: {}", schedule);
            return sessions;
        }

        LocalDate startDate = LocalDate.parse(startDateStr.substring(0, 10));

        int sessionNumber = 1;
        LocalDate current = startDate;
        int maxDays = numberOfSessions * 14; // safety: max 14 days per session

        for (int i = 0; i < maxDays && sessions.size() < numberOfSessions; i++) {
            int currentDow = current.getDayOfWeek().getValue(); // 1=Monday, 7=Sunday

            for (DayOfWeekInfo dw : weekdays) {
                if (currentDow == dw.targetDow) {
                    LocalDateTime sessionDateTime = current.atTime(LocalTime.parse(startTime));

                    ClassSession session = ClassSession.builder()
                            .clazz(clazz)
                            .sessionNumber(sessionNumber++)
                            .sessionDate(sessionDateTime)
                            .startTime(startTime)
                            .endTime(endTime)
                            .build();
                    sessions.add(session);
                    break;
                }
            }
            current = current.plusDays(1);
        }

        return sessions;
    }

    /** Trích xuất giờ bắt đầu hoặc kết thúc từ slotTime như "Tối (18:00 - 20:00)" */
    private String extractTime(String slotTime, boolean isStart) {
        if (slotTime == null) return isStart ? "19:00" : "21:00";
        Pattern p = Pattern.compile("(\\d{1,2}:\\d{2})\\s*-\\s*(\\d{1,2}:\\d{2})");
        Matcher m = p.matcher(slotTime);
        if (m.find()) {
            return isStart ? m.group(1) : m.group(2);
        }
        // Thử format "18:00 - 20:00"
        p = Pattern.compile("(\\d{1,2}:\\d{2})");
        java.util.regex.Matcher m2 = p.matcher(slotTime);
        if (m2.find()) {
            String start = m2.group(1);
            if (m2.find()) {
                return isStart ? start : m2.group(1);
            }
            return start;
        }
        return isStart ? "19:00" : "21:00";
    }

    /** Parse schedule string thành danh sách các thứ trong tuần (1=Mon...7=Sun) */
    private List<DayOfWeekInfo> parseScheduleDays(String schedule) {
        List<DayOfWeekInfo> result = new ArrayList<>();
        if (schedule == null || schedule.isBlank()) return result;

        String s = schedule.toLowerCase().replaceAll("\\s+", "");

        // Map các pattern
        String[][] dayMappings = {
            {"thứ2", "t2", "mon", "2", "monday"},
            {"thứ3", "t3", "tue", "3", "tuesday"},
            {"thứ4", "t4", "wed", "4", "wednesday"},
            {"thứ5", "t5", "thu", "5", "thursday"},
            {"thứ6", "t6", "fri", "6", "friday"},
            {"thứ7", "t7", "sat", "7", "saturday"},
            {"cn", "chủnhật", "sun", "8", "sunday"}
        };

        // Tìm tất cả các số trong chuỗi (1-7 hoặc 2-8)
        Pattern numP = Pattern.compile("\\b([2-8])\\b");
        Matcher numM = numP.matcher(s);
        while (numM.find()) {
            int num = Integer.parseInt(numM.group(1));
            if (num == 8) num = 7; // Chủ nhật
            if (!containsDow(result, num)) {
                result.add(new DayOfWeekInfo(num, numM.group(1)));
            }
        }

        // Tìm theo tên thứ
        for (int i = 0; i < dayMappings.length; i++) {
            for (String alias : dayMappings[i]) {
                if (s.contains(alias)) {
                    int dow = (i == 6) ? 7 : (i + 2); // CN=7, T2=2...
                    if (!containsDow(result, dow)) {
                        result.add(new DayOfWeekInfo(dow, alias));
                    }
                    break;
                }
            }
        }

        // Nếu vẫn rỗng, thử parse CSV
        if (result.isEmpty()) {
            String[] parts = schedule.split("[,\\-;\\s]+");
            for (String p2 : parts) {
                p2 = p2.trim().toLowerCase();
                if (p2.isEmpty()) continue;
                try {
                    int n = Integer.parseInt(p2.replaceAll("\\D", ""));
                    if (n >= 2 && n <= 8) {
                        int dow = n == 8 ? 7 : n;
                        if (!containsDow(result, dow)) {
                            result.add(new DayOfWeekInfo(dow, p2));
                        }
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        return result;
    }

    private boolean containsDow(List<DayOfWeekInfo> list, int dow) {
        return list.stream().anyMatch(d -> d.targetDow == dow);
    }

    private static class DayOfWeekInfo {
        int targetDow;
        String matched;
        DayOfWeekInfo(int d, String m) { this.targetDow = d; this.matched = m; }
    }

    @Override
    @Transactional
    public void updateClass(Integer id, ClassRequestDTO request) {
        Clazz clazz = classRepository.findById(id).orElseThrow(() -> new RuntimeException("Class not found"));

        clazz.setClassName(request.getClassName());
        clazz.setCourse(courseRepository.findById(request.getCourseId()).orElse(null));
        clazz.setTeacher(request.getTeacherId() != null ? userRepository.findById(request.getTeacherId()).orElse(null) : null);

        if (request.getStartDate() != null && !request.getStartDate().isEmpty()) {
            clazz.setStartDate(LocalDateTime.parse(request.getStartDate()));
        }
        if (request.getEndDate() != null && !request.getEndDate().isEmpty()) {
            clazz.setEndDate(LocalDateTime.parse(request.getEndDate()));
        }

        clazz.setStatus(request.getStatus());
        clazz.setSchedule(request.getSchedule());
        clazz.setSlotTime(request.getSlotTime());
        clazz.setNumberOfSessions(request.getNumberOfSessions());

        classRepository.save(clazz);

        // Tự động tạo sessions nếu chưa có
        int existingCount = classSessionRepository.countByClassId(id);
        if (existingCount == 0
                && request.getStartDate() != null && !request.getStartDate().isEmpty()
                && request.getSchedule() != null && !request.getSchedule().isEmpty()
                && request.getSlotTime() != null && !request.getSlotTime().isEmpty()
                && request.getNumberOfSessions() != null && request.getNumberOfSessions() > 0) {

            List<ClassSession> sessions = generateSessions(clazz, request.getSchedule(), request.getSlotTime(),
                    request.getStartDate(), request.getNumberOfSessions());
            if (!sessions.isEmpty()) {
                classSessionRepository.saveAll(sessions);
                log.info("Created {} sessions for class {} on update", sessions.size(), clazz.getClassName());
            }
        }

        log.info("Class updated successfully, id={}", id);
    }

    @Override
    @Transactional
    public void deleteClass(Integer id) {
        classRepository.deleteById(id);
        log.info("Class deleted, id={}", id);
    }

    @Override
    public ClassDetailResponse getClassById(Integer id) {
        Clazz clazz = classRepository.findById(id).orElseThrow(() -> new RuntimeException("Class not found"));
        return mapToResponse(clazz);
    }

    @Override
    public PageResponse<ClassDetailResponse> getAllClasses(int pageNo, int pageSize, String search, String status) {
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("classId").descending());

        Specification<Clazz> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim() + "%";
                Predicate classNamePredicate = cb.like(cb.lower(root.get("className")), searchPattern.toLowerCase());
                Predicate courseNamePredicate = cb.like(cb.lower(root.get("course").get("courseName")), searchPattern.toLowerCase());
                predicates.add(cb.or(classNamePredicate, courseNamePredicate));
            }

            if (status != null && !status.trim().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("status")), status.toLowerCase()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Clazz> page = classRepository.findAll(spec, pageable);
        List<ClassDetailResponse> list = page.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return PageResponse.<ClassDetailResponse>builder()
                .pageNo(pageNo)
                .pageSize(pageSize)
                .totalPages(page.getTotalPages())
                .totalElements((int) page.getTotalElements())
                .items(list)
                .build();
    }

    private ClassDetailResponse mapToResponse(Clazz clazz) {
        List<RegistrationResponseDTO> registrationDTOs = null;
        if (clazz.getRegistrations() != null) {
            registrationDTOs = clazz.getRegistrations().stream()
                    .map(reg -> RegistrationResponseDTO.builder()
                            .registrationId(reg.getRegistrationId())
                            .userId(reg.getUser() != null ? reg.getUser().getUserId() : null)
                            .userName(reg.getUser() != null ? reg.getUser().getFullName() : null)
                            .userEmail(reg.getUser() != null ? reg.getUser().getEmail() : null)
                            .classId(reg.getClazz() != null ? reg.getClazz().getClassId() : null)
                            .className(reg.getClazz() != null ? reg.getClazz().getClassName() : null)
                            .courseId(reg.getCourse() != null ? reg.getCourse().getCourseId() : null)
                            .courseName(reg.getCourse() != null ? reg.getCourse().getCourseName() : null)
                            .registrationTime(reg.getRegistrationTime() != null ? LocalDateTime.parse(reg.getRegistrationTime().toString()) : null)
                            .status(reg.getStatus() != null ? reg.getStatus() : "Pending")
                            .registrationPrice(reg.getRegistrationPrice())
                            .build())
                    .toList();
        }

        return ClassDetailResponse.builder()
                .classId(clazz.getClassId())
                .className(clazz.getClassName() != null ? clazz.getClassName() : "N/A")
                .courseId(clazz.getCourse() != null ? clazz.getCourse().getCourseId() : null)
                .courseName(clazz.getCourse() != null ? clazz.getCourse().getCourseName() : "N/A")
                .teacherId(clazz.getTeacher() != null ? clazz.getTeacher().getUserId() : null)
                .teacherName(clazz.getTeacher() != null ? clazz.getTeacher().getFullName() : "Not assigned")
                .startDate(clazz.getStartDate() != null ? clazz.getStartDate().toString() : "")
                .endDate(clazz.getEndDate() != null ? clazz.getEndDate().toString() : "")
                .status(clazz.getStatus() != null ? clazz.getStatus() : "Pending")
                .schedule(clazz.getSchedule() != null ? clazz.getSchedule() : "N/A")
                .slotTime(clazz.getSlotTime() != null ? clazz.getSlotTime() : "N/A")
                .numberOfSessions(clazz.getNumberOfSessions())
                .registrations(registrationDTOs)
                .build();
    }
}
