package com.example.DoAn.service.impl;

import com.example.DoAn.dto.request.ClassRequestDTO;
import com.example.DoAn.dto.response.ClassDetailResponse;
import com.example.DoAn.dto.response.PageResponse;
import com.example.DoAn.dto.response.RegistrationResponseDTO;
import com.example.DoAn.model.Clazz;
import com.example.DoAn.model.ClassSession;
import com.example.DoAn.model.Course;
import com.example.DoAn.model.Lesson;
import com.example.DoAn.model.SessionLesson;
import com.example.DoAn.model.Quiz;
import com.example.DoAn.model.SessionQuiz;
import com.example.DoAn.model.User;
import com.example.DoAn.repository.ClassRepository;
import com.example.DoAn.repository.ClassSessionRepository;
import com.example.DoAn.repository.CourseRepository;
import com.example.DoAn.repository.LessonRepository;
import com.example.DoAn.repository.RegistrationRepository;
import com.example.DoAn.repository.SessionLessonRepository;
import com.example.DoAn.repository.QuizRepository;
import com.example.DoAn.repository.SessionQuizRepository;
import com.example.DoAn.repository.UserRepository;
import com.example.DoAn.service.EmailService;
import com.example.DoAn.service.IClassService;
import com.example.DoAn.service.INotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClassServiceImpl implements IClassService {

    private static final List<String> ALL_SLOT_TIMES = List.of(
            "Sáng (7:00 - 9:00)",
            "Sáng (9:00 - 11:00)",
            "Chiều (13:00 - 15:00)",
            "Chiều (15:00 - 17:00)",
            "Tối (18:00 - 20:00)"
    );
    private Integer getSlotNumberFromTime(String slotTime) {
        if (slotTime == null || slotTime.isBlank()) return null;

        if (slotTime.contains("7:00 - 9:00")) return 1;
        if (slotTime.contains("9:00 - 11:00")) return 2;
        if (slotTime.contains("13:00 - 15:00")) return 3;
        if (slotTime.contains("15:00 - 17:00")) return 4;
        if (slotTime.contains("18:00 - 20:00")) return 5;

        return null; // Trả về null nếu không khớp
    }

    private final ClassRepository classRepository;
    private final ClassSessionRepository classSessionRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final SessionLessonRepository sessionLessonRepository;
    private final QuizRepository quizRepository;
    private final SessionQuizRepository sessionQuizRepository;
    private final RegistrationRepository registrationRepository;
    private final EmailService emailService;
    private final INotificationService notificationService;

    @Override
    public List<String> getAvailableSlotTimes(Integer teacherId, String schedule, Integer excludeClassId) {
        if (teacherId == null || schedule == null || schedule.isBlank()) {
            return ALL_SLOT_TIMES;
        }

        List<Clazz> existingClasses = classRepository.findByTeacherAndSchedule(teacherId, schedule.trim(), excludeClassId);
        List<String> occupied = existingClasses.stream()
                .map(Clazz::getSlotTime)
                .filter(s -> s != null && !s.isBlank())
                .map(this::normalizeSlot)
                .toList();

        return ALL_SLOT_TIMES.stream()
                .filter(slot -> !occupied.contains(normalizeSlot(slot)))
                .toList();
    }

    private String normalizeSlot(String slot) {
        return slot == null ? "" : slot.trim().toLowerCase();
    }

    private void validateClassRequest(ClassRequestDTO request, Integer excludeClassId) {
        LocalDateTime startDate = parseDateTime(request.getStartDate(), "Ngày khai giảng không hợp lệ");

        LocalDate today = LocalDate.now();
        if (startDate.toLocalDate().isBefore(today)) {
            throw new RuntimeException("Ngày khai giảng không được ở quá khứ");
        }

        // endDate có thể null — sẽ tự tính từ số buổi + lịch học
        if (request.getEndDate() != null && !request.getEndDate().isBlank()) {
            LocalDateTime endDate = parseDateTime(request.getEndDate(), "Ngày kết thúc không hợp lệ");
            if (endDate.toLocalDate().isBefore(today)) {
                throw new RuntimeException("Ngày kết thúc không được ở quá khứ");
            }
            if (endDate.isBefore(startDate)) {
                throw new RuntimeException("Ngày kết thúc phải lớn hơn hoặc bằng ngày khai giảng");
            }
        }

        // Validate trùng tên lớp học
        if (request.getClassName() != null && !request.getClassName().isBlank()) {
            boolean exists;
            if (excludeClassId != null) {
                exists = classRepository.existsByClassNameIgnoreCaseAndClassIdNot(
                        request.getClassName().trim(), excludeClassId);
            } else {
                exists = classRepository.existsByClassNameIgnoreCase(request.getClassName().trim());
            }
            if (exists) {
                throw new RuntimeException("Tên lớp học đã tồn tại. Vui lòng chọn tên khác.");
            }
        }

        if (request.getTeacherId() != null && request.getSchedule() != null && !request.getSchedule().isBlank()) {
            List<String> availableSlots = getAvailableSlotTimes(request.getTeacherId(), request.getSchedule(), excludeClassId);
            if (!availableSlots.stream().map(this::normalizeSlot).toList().contains(normalizeSlot(request.getSlotTime()))) {
                throw new RuntimeException("Giáo viên đã có lớp trùng lịch học và ca học");
            }
        }

        // Validate number of sessions vs course/lesson count
        if (request.getCourseId() != null && request.getNumberOfSessions() != null) {
            Course course = courseRepository.findById(request.getCourseId()).orElse(null);
            Integer minSessions = 1;
            
            if (course != null && course.getNumberOfSessions() != null && course.getNumberOfSessions() > 0) {
                minSessions = course.getNumberOfSessions();
            } else {
                minSessions = (int) lessonRepository.countByModuleCourse_CourseId(request.getCourseId());
            }

            if (request.getNumberOfSessions() < minSessions) {
                throw new RuntimeException("Số buổi học (" + request.getNumberOfSessions() + ") không được nhỏ hơn yêu cầu tối thiểu của khóa (" + minSessions + ")");
            }
        }
    }

    private LocalDateTime parseDateTime(String value, String errorMessage) {
        if (value == null || value.isBlank()) {
            throw new RuntimeException(errorMessage);
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ex) {
            throw new RuntimeException(errorMessage);
        }
    }

    private String normalizeHourMinute(String time) {
        if (time == null || time.isBlank()) return time;
        String[] parts = time.split(":");
        if (parts.length != 2) return time;
        try {
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            return String.format("%02d:%02d", hour, minute);
        } catch (NumberFormatException ex) {
            return time;
        }
    }

    @Override
    @Transactional
    public Integer saveClass(ClassRequestDTO request) {
        validateClassRequest(request, null);

        LocalDateTime startDate = parseDateTime(request.getStartDate(), "Ngày khai giảng không hợp lệ");
        LocalDateTime endDate = null;
        // Tính endDate tự động từ số buổi + lịch học nếu chưa có
        if (request.getEndDate() != null && !request.getEndDate().isBlank()) {
            endDate = parseDateTime(request.getEndDate(), "Ngày kết thúc không hợp lệ");
        } else if (request.getSchedule() != null && !request.getSchedule().isBlank()
                && request.getSlotTime() != null && !request.getSlotTime().isBlank()
                && request.getNumberOfSessions() != null && request.getNumberOfSessions() > 0) {
            endDate = calculateEndDate(request.getSchedule(), request.getStartDate(), request.getNumberOfSessions());
        }

        Clazz clazz = Clazz.builder()
                .className(request.getClassName())
                .course(courseRepository.findById(request.getCourseId()).orElse(null))
                .teacher(request.getTeacherId() != null ? userRepository.findById(request.getTeacherId()).orElse(null) : null)
                .startDate(startDate)
                .endDate(endDate)
                .status(request.getStatus() != null ? request.getStatus() : "Pending")
                .schedule(request.getSchedule())
                .slotTime(request.getSlotTime())
                .numberOfSessions(request.getNumberOfSessions())
                .meetLink(request.getMeetLink())
                .description(request.getDescription())
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

                // --- Auto mapping lessons to sessions ---
                autoMapLessonsToSessions(clazz.getClassId(), clazz.getCourse().getCourseId());
            }
        }

        log.info("Class created successfully: {}", clazz.getClassName());

        // ── Notify teacher of new class assignment ──────────────────────────────
        notifyTeacherClassCreated(clazz);

        return clazz.getClassId();
    }

    // ── Notification helper ────────────────────────────────────────────────────

    private void notifyTeacherClassCreated(Clazz clazz) {
        if (clazz == null || clazz.getTeacher() == null) return;
        User teacher = clazz.getTeacher();
        String teacherName = teacher.getFullName() != null ? teacher.getFullName() : "";
        String className = clazz.getClassName() != null ? clazz.getClassName() : "";
        String courseName = clazz.getCourse() != null && clazz.getCourse().getCourseName() != null
                ? clazz.getCourse().getCourseName() : "";
        String startDate = clazz.getStartDate() != null ? clazz.getStartDate().toLocalDate().toString() : "";
        String schedule = clazz.getSchedule() != null ? clazz.getSchedule() : "";

        if (teacher.getEmail() != null && !teacher.getEmail().isBlank()) {
            emailService.sendClassEnrollmentEmail(teacher.getEmail(), teacherName, className, courseName, startDate, schedule);
        }
        if (teacher.getUserId() != null) {
            notificationService.sendClassEnrollment(Long.valueOf(teacher.getUserId()), className, courseName);
        }
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
                            .slotNumber(getSlotNumberFromTime(clazz.getSlotTime()))
                            .build();
                    sessions.add(session);
                    break;
                }
            }
            current = current.plusDays(1);
        }

        return sessions;
    }

    /** Tính ngày kết thúc dựa trên số buổi và lịch học */
    private LocalDateTime calculateEndDate(String schedule, String startDateStr, Integer numberOfSessions) {
        if (schedule == null || schedule.isBlank() || startDateStr == null
                || startDateStr.isBlank() || numberOfSessions == null || numberOfSessions <= 0) {
            return null;
        }

        List<DayOfWeekInfo> weekdays = parseScheduleDays(schedule);
        if (weekdays.isEmpty()) return null;

        LocalDate startDate = LocalDate.parse(startDateStr.substring(0, 10));
        LocalDate current = startDate;
        int maxDays = numberOfSessions * 14;

        for (int i = 0; i < maxDays && numberOfSessions > 0; i++) {
            int currentDow = current.getDayOfWeek().getValue();
            if (containsDow(weekdays, currentDow)) {
                numberOfSessions--;
                if (numberOfSessions == 0) {
                    return current.atTime(23, 59);
                }
            }
            current = current.plusDays(1);
        }
        return current.atTime(23, 59);
    }

    private boolean containsDow(List<DayOfWeekInfo> list, int dow) {
        return list.stream().anyMatch(d -> d.targetDow == dow);
    }

    /** Trích xuất giờ bắt đầu hoặc kết thúc từ slotTime như "Tối (18:00 - 20:00)" */
    private String extractTime(String slotTime, boolean isStart) {
        if (slotTime == null) return isStart ? "19:00" : "21:00";
        Pattern p = Pattern.compile("(\\d{1,2}:\\d{2})\\s*-\\s*(\\d{1,2}:\\d{2})");
        Matcher m = p.matcher(slotTime);
        if (m.find()) {
            return normalizeHourMinute(isStart ? m.group(1) : m.group(2));
        }
        // Thử format "18:00 - 20:00"
        p = Pattern.compile("(\\d{1,2}:\\d{2})");
        java.util.regex.Matcher m2 = p.matcher(slotTime);
        if (m2.find()) {
            String start = normalizeHourMinute(m2.group(1));
            if (m2.find()) {
                return isStart ? start : normalizeHourMinute(m2.group(1));
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

    private static class DayOfWeekInfo {
        int targetDow;
        String matched;
        DayOfWeekInfo(int d, String m) { this.targetDow = d; this.matched = m; }
    }

    @Override
    @Transactional
    public void updateClass(Integer id, ClassRequestDTO request) {
        validateClassRequest(request, id);

        Clazz clazz = classRepository.findById(id).orElseThrow(() -> new RuntimeException("Class not found"));

        clazz.setClassName(request.getClassName());
        clazz.setCourse(courseRepository.findById(request.getCourseId()).orElse(null));
        clazz.setTeacher(request.getTeacherId() != null ? userRepository.findById(request.getTeacherId()).orElse(null) : null);

        if (request.getStartDate() != null && !request.getStartDate().isEmpty()) {
            clazz.setStartDate(LocalDateTime.parse(request.getStartDate()));
        }
        if (request.getEndDate() != null && !request.getEndDate().isBlank()) {
            clazz.setEndDate(LocalDateTime.parse(request.getEndDate()));
        } else if (request.getSchedule() != null && !request.getSchedule().isBlank()
                && request.getSlotTime() != null && !request.getSlotTime().isBlank()
                && request.getNumberOfSessions() != null && request.getNumberOfSessions() > 0) {
            clazz.setEndDate(calculateEndDate(request.getSchedule(), request.getStartDate(), request.getNumberOfSessions()));
        }

        clazz.setMeetLink(request.getMeetLink());
        clazz.setStatus(request.getStatus());
        clazz.setSchedule(request.getSchedule());
        clazz.setSlotTime(request.getSlotTime());
        clazz.setNumberOfSessions(request.getNumberOfSessions());
        clazz.setMeetLink(request.getMeetLink());
        clazz.setDescription(request.getDescription());

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
                
                // --- Auto mapping lessons to sessions ---
                autoMapLessonsToSessions(clazz.getClassId(), clazz.getCourse().getCourseId());
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
    public PageResponse<ClassDetailResponse> getAllClasses(int pageNo, int pageSize, String className, String courseName, String teacherName, String status) {
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("classId").descending());

        Specification<Clazz> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (className != null && !className.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("className")), "%" + className.trim().toLowerCase() + "%"));
            }
            if (courseName != null && !courseName.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("course").get("courseName")), "%" + courseName.trim().toLowerCase() + "%"));
            }
            if (teacherName != null && !teacherName.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("teacher").get("fullName")), "%" + teacherName.trim().toLowerCase() + "%"));
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
                .courseImageUrl(clazz.getCourse() != null ? clazz.getCourse().getImageUrl() : null)
                .courseDescription(clazz.getCourse() != null ? clazz.getCourse().getDescription() : null)
                .coursePrice(safeToDecimal(clazz.getCourse() != null ? clazz.getCourse().getPrice() : null))
                .courseSale(safeToDecimal(clazz.getCourse() != null ? clazz.getCourse().getSale() : null))
                .expertAvatar(clazz.getCourse() != null && clazz.getCourse().getExpert() != null
                        ? clazz.getCourse().getExpert().getAvatarUrl() : null)
                .expertName(clazz.getCourse() != null && clazz.getCourse().getExpert() != null
                        ? clazz.getCourse().getExpert().getFullName() : null)
                .teacherId(clazz.getTeacher() != null ? clazz.getTeacher().getUserId() : null)
                .teacherName(clazz.getTeacher() != null ? clazz.getTeacher().getFullName() : "Not assigned")
                .startDate(clazz.getStartDate() != null ? clazz.getStartDate().toString() : "")
                .endDate(clazz.getEndDate() != null ? clazz.getEndDate().toString() : "")
                .status(clazz.getStatus() != null ? clazz.getStatus() : "Pending")
                .schedule(clazz.getSchedule() != null ? clazz.getSchedule() : "N/A")
                .slotTime(clazz.getSlotTime() != null ? clazz.getSlotTime() : "N/A")
                .numberOfSessions(clazz.getNumberOfSessions())
                .meetLink(clazz.getMeetLink())
                .description(clazz.getDescription())
                .registrations(registrationDTOs)
                .build();
    }

    private BigDecimal safeToDecimal(Double value) {
        if (value == null) return null;
        return BigDecimal.valueOf(value);
    }

    /**
     * Tự động mapping Lesson sang ClassSession khi tạo lớp mới.
     * Đảm bảo an toàn: Idempotent (không duplicate), Null safety, Transactional.
     */
    @Transactional
    public void autoMapLessonsToSessions(Integer classId, Integer courseId) {
        if (classId == null || courseId == null) return;

        // 1. Idempotency check: Bỏ qua nếu đã có mapping (tránh duplicate khi update hoặc chạy lại)
        if (sessionLessonRepository.existsBySession_Clazz_ClassId(classId)) {
            log.info("[AutoMap] Mapping already exists for Class ID: {}. Skipping.", classId);
            return;
        }

        // 2. Load học liệu và danh sách buổi học
        List<Lesson> lessons = lessonRepository.findAllByCourseIdSorted(courseId);
        List<ClassSession> sessions = classSessionRepository.findByClazzClassIdOrderBySessionNumberAsc(classId);

        if (lessons.isEmpty() || sessions.isEmpty()) {
            log.warn("[AutoMap] Lessons or Sessions are empty for Course: {}, Class: {}", courseId, classId);
            return;
        }

        log.info("[AutoMap] Starting mapping {} lessons to {} sessions for Class ID: {}", lessons.size(), sessions.size(), classId);

        int sessionCount = sessions.size();
        for (int i = 0; i < lessons.size(); i++) {
            Lesson lesson = lessons.get(i);
            
            // Logic: Phân bổ 1-1 cho đến session cuối cùng. Nếu số bài > số buổi, dồn hết vào buổi cuối.
            int sessionIndex = Math.min(i, sessionCount - 1);
            ClassSession session = sessions.get(sessionIndex);

            // Tính orderIndex trong session (nếu dồn nhiều bài vào 1 buổi cuối)
            int orderInSession = (i >= sessionCount - 1) ? (i - (sessionCount - 1) + 1) : 1;

            // Mapping Lesson
            SessionLesson sl = SessionLesson.builder()
                    .session(session)
                    .lesson(lesson)
                    .orderIndex(orderInSession)
                    .build();
            sessionLessonRepository.save(sl);

            // Mapping Quiz (nếu Lesson có Quiz liên kết)
            if (lesson.getQuiz_id() != null) {
                quizRepository.findById(lesson.getQuiz_id()).ifPresent(quiz -> {
                    SessionQuiz sq = SessionQuiz.builder()
                            .session(session)
                            .quiz(quiz)
                            .orderIndex(orderInSession)
                            .isOpen(false)
                            .build();
                    sessionQuizRepository.save(sq);
                });
            }
        }
        log.info("[AutoMap] Successfully mapped lessons for class id: {}", classId);
    }
}
