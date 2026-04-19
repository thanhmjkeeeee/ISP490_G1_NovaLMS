package com.example.DoAn.controller;

import com.example.DoAn.dto.request.RescheduleRequest;
import com.example.DoAn.dto.request.RescheduleRequestDTO;
import com.example.DoAn.dto.response.LessonResponseDTO;
import org.springframework.transaction.annotation.Transactional;
import com.example.DoAn.dto.response.RescheduleResponseDTO;
import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.dto.response.SessionDetailDTO;
import com.example.DoAn.dto.response.TeacherDashboardResponseDTO;
import com.example.DoAn.model.*;
import com.example.DoAn.repository.*;
import com.example.DoAn.service.ITeacherDashboardService;
import com.example.DoAn.service.RescheduleService;
import org.springframework.ui.Model;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/teacher")
public class TeacherViewController {

    private final EntityManager entityManager;
    private final ClassSessionRepository classSessionRepository;
    private final RescheduleRequestRepository rescheduleRequestRepository;
    private final RescheduleService rescheduleService;
    private final SessionLessonRepository sessionLessonRepository;
    private final SessionQuizRepository sessionQuizRepository;
    private final ITeacherDashboardService teacherDashboardService;

    public TeacherViewController(EntityManager entityManager,
            ClassSessionRepository classSessionRepository,
            RescheduleRequestRepository rescheduleRequestRepository,
            RescheduleService rescheduleService,
            SessionLessonRepository sessionLessonRepository,
            SessionQuizRepository sessionQuizRepository,
            ITeacherDashboardService teacherDashboardService) {
        this.entityManager = entityManager;
        this.classSessionRepository = classSessionRepository;
        this.rescheduleRequestRepository = rescheduleRequestRepository;
        this.rescheduleService = rescheduleService;
        this.sessionLessonRepository = sessionLessonRepository;
        this.sessionQuizRepository = sessionQuizRepository;
        this.teacherDashboardService = teacherDashboardService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        String email = getEmailFromPrincipal(principal);
        if (email != null) {
            TeacherDashboardResponseDTO dashboardData = teacherDashboardService.getDashboardData(email);
            model.addAttribute("dashboardData", dashboardData);
        }
        return "teacher/dashboard";
    }

    @GetMapping("/my-classes")
    public String myClassesPage() {
        return "teacher/my-classes";
    }

    @GetMapping("/workspace")
    public String workspacePage() {
        return "teacher/workspace";
    }

    @GetMapping("/reschedule-requests")
    public String rescheduleRequestsPage() {
        return "teacher/reschedule-requests";
    }

    @GetMapping("/quiz-bank")
    public String quizBankPage() {
        return "redirect:/teacher/workspace";
    }

    // ══════════════════════════════════════════════════════════════════════
    // MY GRADING (SPEC 006 — Teacher grading dashboard)
    // ══════════════════════════════════════════════════════════════════════

    @GetMapping("/my-grading")
    public String myGradingPage() {
        return "teacher/my-grading";
    }

    // ══════════════════════════════════════════════════════════════════════
    // MY QUESTIONS (SPEC 003 — Teacher views their submitted questions)
    // ══════════════════════════════════════════════════════════════════════

    @GetMapping("/my-questions")
    public String myQuestionsPage() {
        return "teacher/my-questions";
    }

    // ══════════════════════════════════════════════════════════════════════
    // LESSON QUIZ WIZARD (3-Step: Config → Build → Finish)
    // ══════════════════════════════════════════════════════════════════════

    @GetMapping("/quiz/{quizId}/build")
    public String quizBuildPage(@PathVariable Integer quizId, org.springframework.ui.Model model) {
        model.addAttribute("quizId", quizId);
        return "teacher/quiz-build";
    }

    @GetMapping("/quiz/{quizId}/finish")
    public String quizFinishPage(@PathVariable Integer quizId, org.springframework.ui.Model model) {
        model.addAttribute("quizId", quizId);
        return "teacher/quiz-finish";
    }

    @GetMapping("/sessions")
    public String classSessionsPage(@RequestParam Integer classId) {
        return "redirect:/teacher/workspace";
    }

    @GetMapping("/api/my-classes")
    @ResponseBody
    public ResponseData<Map<String, Object>> myClasses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Principal principal) {

        String email = getEmailFromPrincipal(principal);
        if (email == null)
            return ResponseData.error(401, "Vui lòng đăng nhập.");

        log.info("Fetching classes for email: {}", email);

        try {
            List<String> roles = entityManager.createQuery("""
                    SELECT s.value FROM User u
                    JOIN u.role s
                    WHERE u.email = :email
                    """, String.class)
                    .setParameter("email", email)
                    .getResultList();

            boolean isAdmin = roles.stream().anyMatch(r -> "ROLE_ADMIN".equals(r) || "ROLE_MANAGER".equals(r));
            log.info("Email: {}, isAdmin: {}", email, isAdmin);

            String whereClause = isAdmin ? "1=1" : "c.teacher.email = :email";

            TypedQuery<Long> totalQuery = entityManager.createQuery(
                    "SELECT COUNT(c.classId) FROM Clazz c WHERE " + whereClause, Long.class);
            if (!isAdmin)
                totalQuery.setParameter("email", email);
            Long total = totalQuery.getSingleResult();

            String queryStr = """
                    SELECT new map(
                        c.classId as classId,
                        c.className as className,
                        co.courseName as courseName,
                        c.status as status,
                        c.startDate as startDate,
                        c.endDate as endDate,
                        c.schedule as schedule,
                        c.slotTime as slotTime,
                        c.numberOfSessions as numberOfSessions,
                        (SELECT COUNT(r.registrationId) FROM Registration r WHERE r.clazz.classId = c.classId) as studentCount
                    )
                    FROM Clazz c
                    LEFT JOIN c.course co
                    WHERE %s
                    ORDER BY c.classId DESC
                    """
                    .formatted(whereClause);

            Query itemsQuery = entityManager.createQuery(queryStr);
            if (!isAdmin)
                itemsQuery.setParameter("email", email);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = itemsQuery
                    .setFirstResult(page * size)
                    .setMaxResults(size)
                    .getResultList();

            int totalPages = total == 0 ? 1 : (int) Math.ceil((double) total / size);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("items", items);
            data.put("pageNo", page);
            data.put("pageSize", size);
            data.put("totalPages", totalPages);
            data.put("totalElements", total);
            data.put("last", page >= totalPages - 1);

            return ResponseData.success("Thành công", data);
        } catch (Exception e) {
            log.error("Error in myClasses: ", e);
            return ResponseData.error(500, "Lỗi máy chủ: " + e.getMessage());
        }
    }

    @GetMapping("/api/schedule")
    @ResponseBody
    public ResponseData<List<Map<String, Object>>> schedule(Principal principal) {
        Integer teacherId = getTeacherId(principal);
        if (teacherId == null) {
            return ResponseData.error(401, "Vui lòng đăng nhập.");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = entityManager.createQuery("""
                SELECT new map(
                    c.classId as classId,
                    c.className as className,
                    co.courseName as courseName,
                    c.startDate as startDate,
                    c.endDate as endDate,
                    c.schedule as schedule,
                    c.slotTime as slotTime,
                    c.status as status
                )
                FROM Clazz c
                LEFT JOIN c.course co
                WHERE c.teacher.userId = :teacherId
                ORDER BY c.startDate ASC
                """)
                .setParameter("teacherId", teacherId)
                .getResultList();

        return ResponseData.success("Thành công", items);
    }

    @GetMapping("/api/students")
    @ResponseBody
    public ResponseData<List<Map<String, Object>>> students(Principal principal) {
        Integer teacherId = getTeacherId(principal);
        if (teacherId == null) {
            return ResponseData.error(401, "Vui lòng đăng nhập.");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = entityManager.createQuery("""
                SELECT new map(
                    u.userId as studentId,
                    u.fullName as studentName,
                    u.email as email,
                    u.mobile as mobile,
                    c.classId as classId,
                    c.className as className,
                    co.courseName as courseName,
                    r.status as registrationStatus,
                    r.registrationTime as registrationTime
                )
                FROM Registration r
                JOIN r.user u
                JOIN r.clazz c
                LEFT JOIN r.course co
                WHERE c.teacher.userId = :teacherId
                ORDER BY r.registrationTime DESC
                """)
                .setParameter("teacherId", teacherId)
                .getResultList();

        return ResponseData.success("Thành công", items);
    }

    @GetMapping("/api/quiz-bank")
    @ResponseBody
    public ResponseData<List<Map<String, Object>>> quizBank(Principal principal) {
        Integer teacherId = getTeacherId(principal);
        if (teacherId == null) {
            return ResponseData.error(401, "Vui lòng đăng nhập.");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = entityManager.createQuery("""
                SELECT new map(
                    c.classId as classId,
                    c.className as className,
                    co.courseName as courseName,
                    (SELECT COUNT(q.quizId) FROM Quiz q WHERE q.clazz.classId = c.classId) as quizCount
                )
                FROM Clazz c
                LEFT JOIN c.course co
                WHERE c.teacher.userId = :teacherId
                ORDER BY c.classId DESC
                """)
                .setParameter("teacherId", teacherId)
                .getResultList();

        return ResponseData.success("Thành công", items);
    }

    @GetMapping("/api/timetable")
    @ResponseBody
    public ResponseData<Map<String, Object>> timetable(
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "2026") int year,
            @RequestParam(defaultValue = "1") int week,
            Principal principal) {
        try {
            Integer teacherId = getTeacherId(principal);
            if (teacherId == null)
                return ResponseData.error(401, "Vui lòng đăng nhập.");

            LocalDate weekStart;
            if (date != null && !date.isEmpty()) {
                weekStart = LocalDate.parse(date);
            } else {
                weekStart = LocalDate.of(year, 1, 1)
                        .with(WeekFields.ISO.dayOfWeek(), 1)
                        .plusWeeks(week - 1);
            }

            LocalDate weekEnd = weekStart.plusDays(7);
            LocalDateTime start = weekStart.atStartOfDay();
            LocalDateTime end = weekEnd.atStartOfDay();

            List<ClassSession> sessions = classSessionRepository.findByTeacherAndDateRange(teacherId, start, end);

            Map<Integer, Map<Integer, List<Map<String, Object>>>> grid = new LinkedHashMap<>();
            for (int d = 1; d <= 7; d++) {
                grid.put(d, new LinkedHashMap<>());
                for (int s = 0; s < 13; s++) {
                    grid.get(d).put(s, new ArrayList<>());
                }
            }

            DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM");
            for (ClassSession s : sessions) {
                if (s.getSessionDate() == null) continue;

                int dayIndex = s.getSessionDate().getDayOfWeek().getValue();
                Integer slotNum = s.getSlotNumber();
                int slotIndex = (slotNum != null) ? slotNum : getSlotIndex(s.getStartTime());

                Map<String, Object> summary = new LinkedHashMap<>();
                summary.put("sessionId", s.getSessionId());
                summary.put("sessionNumber", s.getSessionNumber());
                summary.put("date", s.getSessionDate().format(dateFmt));
                summary.put("time", (s.getStartTime() != null ? s.getStartTime() : "")
                        + (s.getEndTime() != null ? " - " + s.getEndTime() : ""));
                
                if (s.getClazz() == null) continue;
                
                summary.put("classId", s.getClazz().getClassId());
                summary.put("className", s.getClazz().getClassName());
                summary.put("courseName", s.getClazz().getCourse() != null ? s.getClazz().getCourse().getCourseName() : null);
                summary.put("topic", s.getTopic());

                if (grid.containsKey(dayIndex) && grid.get(dayIndex).containsKey(slotIndex)) {
                    grid.get(dayIndex).get(slotIndex).add(summary);
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("year", year);
            result.put("week", week);
            DateTimeFormatter isoFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            result.put("weekStart", weekStart.format(isoFmt));
            result.put("weekEnd", weekEnd.minusDays(1).format(isoFmt));
            result.put("grid", grid);

            return ResponseData.success("Đã tải lịch giảng dạy", result);
        } catch (Exception e) {
            log.error("Timetable error for teacher: ", e);
            return ResponseData.error(500, "Lỗi máy chủ: " + e.getMessage());
        }
    }

    @GetMapping("/api/session/{sessionId}/detail")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseData<SessionDetailDTO> sessionDetail(
            @PathVariable Integer sessionId,
            Principal principal) {
        try {
            Integer teacherId = getTeacherId(principal);
            if (teacherId == null)
                return ResponseData.error(401, "Vui lòng đăng nhập.");

            ClassSession session = classSessionRepository.findWithDetailsById(sessionId).orElse(null);
            if (session == null)
                return ResponseData.error(404, "Không tìm thấy buổi học");

            // Defensive check for teacher ownership and lazy-loading safety
            User teacherUser = (session.getClazz() != null) ? session.getClazz().getTeacher() : null;
            if (teacherUser == null || !teacherUser.getUserId().equals(teacherId)) {
                return ResponseData.error(403, "Không có quyền");
            }

            // 1. Topic Aggregation from SessionLesson
            List<SessionLesson> sessionLessons = sessionLessonRepository.findBySessionSessionIdWithLesson(sessionId);
            String aggregatedTopic = sessionLessons.stream()
                    .map(sl -> (sl.getLesson() != null) ? sl.getLesson().getLessonName() : null)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(", "));

            if (aggregatedTopic.isEmpty())
                aggregatedTopic = (session.getTopic() != null) ? session.getTopic() : "Chưa cập nhật";
            if (aggregatedTopic == null || aggregatedTopic.isEmpty())
                aggregatedTopic = "Chưa cập nhật";

            // 2. Classify Lessons into Materials and Quizzes
            List<LessonResponseDTO> materials = new ArrayList<>();
            List<LessonResponseDTO> quizzes = new ArrayList<>();

            for (SessionLesson sl : sessionLessons) {
                Lesson l = sl.getLesson();
                if (l == null) continue;

                LessonResponseDTO dto = LessonResponseDTO.builder()
                        .lessonId(l.getLessonId())
                        .lessonName(l.getLessonName())
                        .type(l.getType())
                        .build();

                if ("QUIZ".equalsIgnoreCase(l.getType())) {
                    quizzes.add(dto);
                } else {
                    materials.add(dto);
                }
            }

            // Add actual SessionQuizzes linked to this session
            List<SessionQuiz> sessionQuizzes = sessionQuizRepository.findBySessionSessionIdWithQuiz(sessionId);
            for (SessionQuiz sq : sessionQuizzes) {
                if (sq.getQuiz() == null) continue;
                quizzes.add(LessonResponseDTO.builder()
                        .quizId(sq.getQuiz().getQuizId())
                        .lessonName(sq.getQuiz().getTitle())
                        .type("QUIZ")
                        .status((sq.getIsOpen() != null && sq.getIsOpen()) ? "OPEN" : "CLOSED")
                        .build());
            }

            // Resolve meetLink: session-level overrides class-level
            String resolvedMeetLink = (session.getMeetLink() != null && !session.getMeetLink().isBlank())
                    ? session.getMeetLink()
                    : (session.getClazz() != null ? session.getClazz().getMeetLink() : null);

            SessionDetailDTO detail = SessionDetailDTO.builder()
                    .sessionId(session.getSessionId())
                    .sessionNo(session.getSessionNumber())
                    .date(session.getSessionDate() != null
                            ? session.getSessionDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            : null)
                    .startTime(session.getStartTime())
                    .endTime(session.getEndTime())
                    .slotNumber(session.getSlotNumber())
                    .topic(aggregatedTopic)
                    .meetLink(resolvedMeetLink)
                    .className(session.getClazz() != null ? session.getClazz().getClassName() : "N/A")
                    .courseName(
                            (session.getClazz() != null && session.getClazz().getCourse() != null) 
                                ? session.getClazz().getCourse().getCourseName() : "")
                    .materials(materials)
                    .quizzes(quizzes)
                    .build();

            return ResponseData.success("Chi tiết buổi học", detail);
        } catch (Exception e) {
            log.error("Error loading session detail for id {}: ", sessionId, e);
            return ResponseData.error(500, "Lỗi máy chủ: " + e.getMessage());
        }
    }

    @GetMapping("/api/session/{sessionId}/reschedule-status")
    @ResponseBody
    public ResponseData<Map<String, Object>> rescheduleStatus(@PathVariable Integer sessionId) {
        Optional<RescheduleRequest> pending = rescheduleService.getPendingRequest(sessionId);
        Map<String, Object> data = new HashMap<>();
        data.put("hasPending", pending.isPresent());
        return ResponseData.success("Thành công", data);
    }

    /**
     * Các slot giờ bắt đầu (HH:mm) đã bận trong ngày — mọi lớp của GV + yêu cầu đổi lịch PENDING,
     * trừ buổi {@code excludeSessionId} (buổi đang đổi lịch) để không tự khóa slot hiện tại của chính nó.
     */
    @GetMapping("/api/reschedule/busy-slots")
    @ResponseBody
    public ResponseData<List<String>> busySlotsForReschedule(
            @RequestParam String date,
            @RequestParam(required = false) Integer excludeSessionId,
            Principal principal) {
        Integer teacherId = getTeacherId(principal);
        if (teacherId == null) {
            return ResponseData.error(401, "Vui lòng đăng nhập.");
        }
        try {
            LocalDate d = LocalDate.parse(date);
            LocalDateTime dayStart = d.atStartOfDay();
            LocalDateTime dayEnd = d.plusDays(1).atStartOfDay();

            LinkedHashSet<String> busy = new LinkedHashSet<>();
            List<ClassSession> sessions = classSessionRepository.findByTeacherAndDateRange(
                    teacherId, dayStart, dayEnd);
            for (ClassSession s : sessions) {
                if (excludeSessionId != null && excludeSessionId.equals(s.getSessionId())) {
                    continue;
                }
                String n = normalizeStartTimeForSlot(s.getStartTime());
                if (n != null) {
                    busy.add(n);
                }
            }
            List<String> pendingSlots = rescheduleRequestRepository.findPendingNewStartTimesForTeacherOnDay(
                    teacherId, dayStart, dayEnd, excludeSessionId);
            for (String t : pendingSlots) {
                String n = normalizeStartTimeForSlot(t);
                if (n != null) {
                    busy.add(n);
                }
            }
            return ResponseData.success("Thành công", new ArrayList<>(busy));
        } catch (Exception e) {
            log.error("busy-slots error: ", e);
            return ResponseData.error(400, "Ngày không hợp lệ: " + e.getMessage());
        }
    }

    private static String normalizeStartTimeForSlot(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        int colon = s.indexOf(':');
        if (colon <= 0) {
            return null;
        }
        try {
            int h = Integer.parseInt(s.substring(0, colon).trim());
            String after = s.substring(colon + 1);
            String mPart = after.length() >= 2 ? after.substring(0, 2) : after;
            int mi = Integer.parseInt(mPart.replaceAll("\\D", ""));
            return String.format("%02d:%02d", h, mi);
        } catch (Exception e) {
            return null;
        }
    }

    @PostMapping("/api/session/{sessionId}/reschedule")
    @ResponseBody
    public ResponseData<Integer> reschedule(
            @PathVariable Integer sessionId,
            @RequestBody RescheduleRequestDTO request,
            Principal principal) {
        String email = getEmailFromPrincipal(principal);
        return rescheduleService.createRequest(
                sessionId,
                request.getNewDate(),
                request.getNewStartTime(),
                request.getReason(),
                email);
    }

    @GetMapping("/api/my-reschedule-requests")
    @ResponseBody
    public ResponseData<List<RescheduleResponseDTO>> getMyRescheduleRequests(Principal principal) {
        String email = getEmailFromPrincipal(principal);
        return ResponseData.success("Thành công", rescheduleService.getTeacherRequests(email));
    }

    // ══════════════════════════════════════════════════════════════════════
    // DASHBOARD STATS
    // ══════════════════════════════════════════════════════════════════════

    @GetMapping("/api/dashboard-stats")
    @ResponseBody
    public ResponseData<Map<String, Object>> dashboardStats(Principal principal) {
        Integer teacherId = getTeacherId(principal);
        if (teacherId == null)
            return ResponseData.error(401, "Vui lòng đăng nhập.");

        List<ClassSession> sessions = classSessionRepository.findByTeacherAndDateRange(
                teacherId,
                LocalDate.now().minusDays(30).atStartOfDay(),
                LocalDate.now().plusDays(60).atStartOfDay());
        Set<Integer> classIds = sessions.stream()
                .map(s -> s.getClazz().getClassId())
                .collect(java.util.stream.Collectors.toSet());

        long studentCount = entityManager.createQuery(
                "SELECT COUNT(r) FROM Registration r WHERE r.clazz.classId IN :classIds AND r.status = 'APPROVED'",
                Long.class)
                .setParameter("classIds", classIds)
                .getSingleResult();

        long courseCount = entityManager.createQuery(
                "SELECT COUNT(DISTINCT c.course.courseId) FROM Clazz c WHERE c.teacher.userId = :teacherId",
                Long.class)
                .setParameter("teacherId", teacherId)
                .getSingleResult();

        long pendingQuestionCount = entityManager.createQuery(
                "SELECT COUNT(q) FROM Question q WHERE q.user.userId = :teacherId AND q.source = 'TEACHER_PRIVATE' AND q.status = 'PENDING_REVIEW'",
                Long.class)
                .setParameter("teacherId", teacherId)
                .getSingleResult();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("studentCount", studentCount);
        stats.put("courseCount", courseCount);
        stats.put("pendingQuestionCount", pendingQuestionCount);
        return ResponseData.success(stats);
    }

    private int getSlotIndex(String startTime) {
        if (startTime == null || startTime.length() < 4)
            return 0;
        String time = startTime.trim();
        if (time.startsWith("7:") || time.startsWith("07:"))
            return 1;
        if (time.startsWith("9:") || time.startsWith("09:"))
            return 2;
        if (time.startsWith("13:"))
            return 3;
        if (time.startsWith("15:"))
            return 4;
        if (time.startsWith("18:"))
            return 5;
        return 0;
    }

    private Integer getTeacherId(Principal principal) {
        String email = getEmailFromPrincipal(principal);
        if (email == null)
            return null;

        @SuppressWarnings("unchecked")
        List<Integer> userIds = entityManager.createQuery(
                "SELECT u.userId FROM User u WHERE u.email = :email")
                .setParameter("email", email)
                .setMaxResults(1)
                .getResultList();

        return userIds.isEmpty() ? null : userIds.get(0);
    }

    private String getEmailFromPrincipal(Principal principal) {
        if (principal == null)
            return null;
        if (principal instanceof OAuth2AuthenticationToken token) {
            return token.getPrincipal().getAttribute("email");
        }
        return principal.getName();
    }
}
