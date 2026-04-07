package com.example.DoAn.controller;

import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.model.Question;
import com.example.DoAn.model.User;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Teacher views and manages their own questions (SPEC 003).
 * GET /api/v1/teacher/questions/my
 */
@RestController
@RequestMapping("/api/v1/teacher/questions")
@RequiredArgsConstructor
public class TeacherQuestionController {

    private final EntityManager entityManager;

    /**
     * List all TEACHER_PRIVATE questions belonging to the authenticated teacher.
     * Supports optional status filter.
     * GET /api/v1/teacher/questions/my?status=PENDING_REVIEW&page=0&size=20
     */
    @GetMapping("/my")
    public ResponseData<Map<String, Object>> getMyQuestions(
            Authentication auth,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Integer teacherId = getTeacherId(auth);
        if (teacherId == null) {
            return ResponseData.error(401, "Unauthorized");
        }

        // Count total
        String countJpql = "SELECT COUNT(q) FROM Question q " +
                "WHERE q.source = 'TEACHER_PRIVATE' AND q.user.userId = :teacherId";
        if (status != null && !status.isBlank()) {
            countJpql += " AND q.status = :status";
        }
        var countQuery = entityManager.createQuery(countJpql, Long.class)
                .setParameter("teacherId", teacherId);
        if (status != null && !status.isBlank()) {
            countQuery.setParameter("status", status);
        }
        long total = countQuery.getSingleResult();

        // Fetch page
        String jpql = "SELECT q FROM Question q " +
                "LEFT JOIN FETCH q.user " +
                "WHERE q.source = 'TEACHER_PRIVATE' AND q.user.userId = :teacherId";
        if (status != null && !status.isBlank()) {
            jpql += " AND q.status = :status";
        }
        jpql += " ORDER BY q.createdAt DESC";
        var query = entityManager.createQuery(jpql, Question.class)
                .setParameter("teacherId", teacherId)
                .setFirstResult(page * size)
                .setMaxResults(size);
        if (status != null && !status.isBlank()) {
            query.setParameter("status", status);
        }
        List<Question> questions = query.getResultList();

        // Resolve reviewer names
        List<Map<String, Object>> dtos = new ArrayList<>();
        for (Question q : questions) {
            Map<String, Object> dto = new HashMap<>();
            dto.put("questionId", q.getQuestionId());
            dto.put("content", q.getContent());
            dto.put("questionType", q.getQuestionType());
            dto.put("skill", q.getSkill());
            dto.put("cefrLevel", q.getCefrLevel());
            dto.put("topic", q.getTopic());
            dto.put("tags", q.getTags());
            dto.put("status", q.getStatus());
            dto.put("source", q.getSource());
            dto.put("reviewerId", q.getReviewerId());
            dto.put("reviewedAt", q.getReviewedAt());
            dto.put("reviewNote", q.getReviewNote());
            dto.put("createdAt", q.getCreatedAt());

            // Resolve reviewer name if reviewerId is set
            if (q.getReviewerId() != null) {
                var reviewer = entityManager.find(User.class, q.getReviewerId());
                if (reviewer != null) {
                    dto.put("reviewerName", reviewer.getFullName());
                    dto.put("reviewerEmail", reviewer.getEmail());
                }
            }

            dtos.add(dto);
        }

        int totalPages = total == 0 ? 1 : (int) Math.ceil((double) total / size);
        Map<String, Object> result = new HashMap<>();
        result.put("content", dtos);
        result.put("totalElements", total);
        result.put("totalPages", totalPages);
        result.put("pageNo", page);
        result.put("pageSize", size);
        result.put("last", page >= totalPages - 1);

        return ResponseData.success(result);
    }

    private Integer getTeacherId(Principal principal) {
        String email = getEmail(principal);
        if (email == null) return null;
        @SuppressWarnings("unchecked")
        List<Integer> ids = entityManager.createQuery(
                        "SELECT u.userId FROM User u WHERE u.email = :email")
                .setParameter("email", email)
                .setMaxResults(1)
                .getResultList();
        return ids.isEmpty() ? null : ids.get(0);
    }

    private String getEmail(Principal principal) {
        if (principal == null) return null;
        if (principal instanceof OAuth2AuthenticationToken token) {
            return token.getPrincipal().getAttribute("email");
        }
        return principal.getName();
    }
}
