package com.example.DoAn.controller;

import com.example.DoAn.dto.response.ResponseData;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/teacher")
public class TeacherViewController {

    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping("/dashboard")
    public String dashboard() {
        return "teacher/dashboard";
    }

    @GetMapping("/my-classes")
    public String myClassesPage() {
        return "teacher/my-classes";
    }

    @GetMapping("/schedule")
    public String schedulePage() {
        return "teacher/schedule";
    }

    @GetMapping("/students")
    public String studentsPage() {
        return "teacher/students";
    }

    @GetMapping("/quiz-bank")
    public String quizBankPage() {
        return "teacher/quiz-bank";
    }

    @GetMapping("/sessions")
    public String classSessionsPage(@RequestParam Integer classId) {
        return "teacher/class-sessions";
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
