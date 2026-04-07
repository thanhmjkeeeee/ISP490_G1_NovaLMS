package com.example.DoAn.controller;

import com.example.DoAn.dto.request.RescheduleRequest;
import com.example.DoAn.dto.request.RescheduleRequestDTO;
import com.example.DoAn.dto.response.LessonResponseDTO;
import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.dto.response.SessionDetailDTO;
import com.example.DoAn.model.*;
import com.example.DoAn.repository.*;
import com.example.DoAn.service.RescheduleService;
import jakarta.persistence.EntityManager;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/teacher")
public class TeacherViewController {

    private final EntityManager entityManager;
    private final ClassSessionRepository classSessionRepository;
    private final RescheduleService rescheduleService;
    private final SessionLessonRepository sessionLessonRepository;
    private final SessionQuizRepository sessionQuizRepository;

    public TeacherViewController(EntityManager entityManager,
                                  ClassSessionRepository classSessionRepository,
                                  RescheduleService rescheduleService,
                                  SessionLessonRepository sessionLessonRepository,
                                  SessionQuizRepository sessionQuizRepository) {
        this.entityManager = entityManager;
        this.classSessionRepository = classSessionRepository;
        this.rescheduleService = rescheduleService;
        this.sessionLessonRepository = sessionLessonRepository;
        this.sessionQuizRepository = sessionQuizRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard() {
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

    @GetMapping("/quiz-bank")
    public String quizBankPage() {
        return "redirect:/teacher/workspace";
    }

    // ══════════════════════════════════════════════════════════════════════
    //  MY GRADING (SPEC 006 — Teacher grading dashboard)
    // ══════════════════════════════════════════════════════════════════════

    @GetMapping("/my-grading")
    public String myGradingPage() {
        return "teacher/my-grading";
    }

    // ══════════════════════════════════════════════════════════════════════
    //  MY QUESTIONS (SPEC 003 — Teacher views their submitted questions)
    // ══════════════════════════════════════════════════════════════════════

    @GetMapping("/my-questions")
    public String myQuestionsPage() {
        return "teacher/my-questions";
    }

    // ══════════════════════════════════════════════════════════════════════
    //  LESSON QUIZ WIZARD (3-Step: Config → Build → Finish)
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
        Integer teacherId = getTeacherId(principal);
        if (teacherId == null) {
            return ResponseData.error(401, "Unauthorized");
        }

        Long total = entityManager.createQuery(
                        "SELECT COUNT(c.classId) FROM Clazz c WHERE c.teacher.userId = :teacherId",
                        Long.class)
                .setParameter("teacherId", teacherId)
                .getSingleResult();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = entityManager.createQuery("""
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
                        WHERE c.teacher.userId = :teacherId
                        ORDER BY c.classId DESC
                        """)
                .setParameter("teacherId", teacherId)
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

        return ResponseData.success("Success", data);
    }

    @GetMapping("/api/schedule")
    @ResponseBody
    public ResponseData<List<Map<String, Object>>> schedule(Principal principal) {
        Integer teacherId = getTeacherId(principal);
        if (teacherId == null) {
            return ResponseData.error(401, "Unauthorized");
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

        return ResponseData.success("Success", items);
    }

    @GetMapping("/api/students")
    @ResponseBody
    public ResponseData<List<Map<String, Object>>> students(Principal principal) {
        Integer teacherId = getTeacherId(principal);
        if (teacherId == null) {
            return ResponseData.error(401, "Unauthorized");
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

        return ResponseData.success("Success", items);
    }

    @GetMapping("/api/quiz-bank")
    @ResponseBody
    public ResponseData<List<Map<String, Object>>> quizBank(Principal principal) {
        Integer teacherId = getTeacherId(principal);
        if (teacherId == null) {
            return ResponseData.error(401, "Unauthorized");
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

        return ResponseData.success("Success", items);
    }

    @GetMapping("/api/timetable")
    @ResponseBody
    public ResponseData<Map<String, Object>> timetable(
            @RequestParam(defaultValue = "2026") int year,
            @RequestParam(defaultValue = "1") int week,
            Principal principal) {
        Integer teacherId = getTeacherId(principal);
        if (teacherId == null) return ResponseData.error(401, "Unauthorized");

        // Calculate week start (Monday) and end (Sunday)
        LocalDate weekStart = LocalDate.of(year, 1, 1)
                .with(WeekFields.ISO.dayOfWeek(), 1)
                .plusWeeks(week - 1);
        LocalDate weekEnd = weekStart.plusDays(7);
        LocalDateTime start = weekStart.atStartOfDay();
        LocalDateTime end = weekEnd.atStartOfDay();

        List<ClassSession> sessions = classSessionRepository.findByTeacherAndDateRange(teacherId, start, end);

        // Build grid: day (1-7) -> slotIndex (0-12) -> list of session summaries
        Map<Integer, Map<Integer, List<Map<String, Object>>>> grid = new LinkedHashMap<>();
        for (int d = 1; d <= 7; d++) {
            grid.put(d, new LinkedHashMap<>());
            for (int s = 0; s < 13; s++) {
                grid.get(d).put(s, new ArrayList<>());
            }
        }

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM");

        for (ClassSession s : sessions) {
            int dayIndex = s.getSessionDate().getDayOfWeek().getValue(); // 1=Mon
            int slotIndex = getSlotIndex(s.getStartTime());
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("sessionId", s.getSessionId());
            summary.put("sessionNumber", s.getSessionNumber());
            summary.put("date", s.getSessionDate().format(dateFmt));
            summary.put("time", (s.getStartTime() != null ? s.getStartTime() : "") + (s.getEndTime() != null ? " - " + s.getEndTime() : ""));
            summary.put("classId", s.getClazz().getClassId());
            summary.put("className", s.getClazz().getClassName());
            summary.put("courseName", s.getClazz().getCourse() != null ? s.getClazz().getCourse().getCourseName() : null);
            summary.put("topic", s.getTopic());
            grid.get(dayIndex).get(slotIndex).add(summary);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("year", year);
        result.put("week", week);
        result.put("weekStart", weekStart.format(dateFmt));
        result.put("weekEnd", weekEnd.minusDays(1).format(dateFmt));
        result.put("grid", grid);

        return ResponseData.success("Timetable", result);
    }

    @GetMapping("/api/session/{sessionId}/detail")
    @ResponseBody
    public ResponseData<SessionDetailDTO> sessionDetail(
            @PathVariable Integer sessionId,
            Principal principal) {
        Integer teacherId = getTeacherId(principal);
        if (teacherId == null) return ResponseData.error(401, "Unauthorized");

        ClassSession session = classSessionRepository.findById(sessionId).orElse(null);
        if (session == null) return ResponseData.error(404, "Không tìm thấy buổi học");
        if (session.getClazz().getTeacher() == null ||
                !session.getClazz().getTeacher().getUserId().equals(teacherId)) {
            return ResponseData.error(403, "Không có quyền");
        }

        // 1. Topic Aggregation from SessionLesson
        List<SessionLesson> sessionLessons = sessionLessonRepository.findBySessionSessionId(sessionId);
        String aggregatedTopic = sessionLessons.stream()
                .map(sl -> sl.getLesson().getLessonName())
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));

        if (aggregatedTopic.isEmpty()) aggregatedTopic = session.getTopic(); // Fallback
        if (aggregatedTopic == null || aggregatedTopic.isEmpty()) aggregatedTopic = "Chưa cập nhật";

        // 2. Classify Lessons into Materials and Quizzes
        List<LessonResponseDTO> materials = new ArrayList<>();
        List<LessonResponseDTO> quizzes = new ArrayList<>();

        for (SessionLesson sl : sessionLessons) {
            Lesson l = sl.getLesson();
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

        // Add additional session materials (attachments)
        if (session.getMaterials() != null && !session.getMaterials().isBlank()) {
            // This assumes the frontend can handle both Lesson objects and raw filenames or similar structure
            // For now, let's keep it consistent with LessonResponseDTO
        }

        // Add actual SessionQuizzes linked to this session
        List<SessionQuiz> sessionQuizzes = sessionQuizRepository.findBySessionSessionId(sessionId);
        for (SessionQuiz sq : sessionQuizzes) {
            quizzes.add(LessonResponseDTO.builder()
                    .quizId(sq.getQuiz().getQuizId())
                    .lessonName(sq.getQuiz().getTitle())
                    .type("QUIZ")
                    .status(sq.getIsOpen() ? "OPEN" : "CLOSED")
                    .build());
        }

        SessionDetailDTO detail = SessionDetailDTO.builder()
                .sessionId(session.getSessionId())
                .sessionNo(session.getSessionNumber())
                .date(session.getSessionDate() != null
                        ? session.getSessionDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : null)
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .slotNumber(session.getSlotNumber())
                .topic(aggregatedTopic)
                .materials(materials)
                .quizzes(quizzes)
                .build();

        return ResponseData.success("Chi tiết buổi học", detail);
    }

    @GetMapping("/api/session/{sessionId}/reschedule-status")
    @ResponseBody
    public ResponseData<Map<String, Object>> rescheduleStatus(@PathVariable Integer sessionId) {
        Optional<RescheduleRequest> pending = rescheduleService.getPendingRequest(sessionId);
        Map<String, Object> data = new HashMap<>();
        data.put("hasPending", pending.isPresent());
        return ResponseData.success("Success", data);
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
                email
        );
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DASHBOARD STATS
    // ══════════════════════════════════════════════════════════════════════

    @GetMapping("/api/dashboard-stats")
    @ResponseBody
    public ResponseData<Map<String, Object>> dashboardStats(Principal principal) {
        Integer teacherId = getTeacherId(principal);
        if (teacherId == null) return ResponseData.error(401, "Unauthorized");

        List<ClassSession> sessions = classSessionRepository.findByTeacherAndDateRange(
            teacherId,
            LocalDate.now().minusDays(30).atStartOfDay(),
            LocalDate.now().plusDays(60).atStartOfDay()
        );
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
        if (startTime == null || startTime.length() < 5) return 6;
        int hour;
        try {
            hour = Integer.parseInt(startTime.substring(0, 2));
        } catch (NumberFormatException e) {
            return 6;
        }
        if (hour >= 7 && hour < 9) return 1;
        if (hour >= 9 && hour < 11) return 2;
        if (hour >= 13 && hour < 15) return 3;
        if (hour >= 15 && hour < 17) return 4;
        if (hour >= 18 && hour < 20) return 5;
        if (hour < 7) return 0;
        return Math.min(12, hour - 7 + 1);
    }

    private Integer getTeacherId(Principal principal) {
        String email = getEmailFromPrincipal(principal);
        if (email == null) return null;

        @SuppressWarnings("unchecked")
        List<Integer> userIds = entityManager.createQuery(
                        "SELECT u.userId FROM User u WHERE u.email = :email")
                .setParameter("email", email)
                .setMaxResults(1)
                .getResultList();

        return userIds.isEmpty() ? null : userIds.get(0);
    }

    private String getEmailFromPrincipal(Principal principal) {
        if (principal == null) return null;
        if (principal instanceof OAuth2AuthenticationToken token) {
            return token.getPrincipal().getAttribute("email");
        }
        return principal.getName();
    }
}
