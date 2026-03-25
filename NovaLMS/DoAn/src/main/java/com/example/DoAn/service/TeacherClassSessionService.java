package com.example.DoAn.service;

import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.model.*;
import com.example.DoAn.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TeacherClassSessionService {

    private final ClassSessionRepository classSessionRepository;
    private final QuizRepository quizRepository;
    private final UserRepository userRepository;
    private final ClazzRepository clazzRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // Lấy teacherId từ email
    private Integer getTeacherId(String email) {
        List<Integer> ids = entityManager.createQuery(
                "SELECT u.userId FROM User u WHERE u.email = :email", Integer.class)
                .setParameter("email", email)
                .setMaxResults(1)
                .getResultList();
        return ids.isEmpty() ? null : ids.get(0);
    }

    // Kiểm tra teacher có quyền sở hữu lớp này không
    private boolean isTeacherOfClass(Integer teacherId, Integer classId) {
        Clazz clazz = clazzRepository.findById(classId).orElse(null);
        return clazz != null && clazz.getTeacher() != null
                && Objects.equals(clazz.getTeacher().getUserId(), teacherId);
    }

    @Transactional(readOnly = true)
    public ResponseData<List<Map<String, Object>>> getSessionsByClass(String email, Integer classId) {
        try {
            Integer teacherId = getTeacherId(email);
            if (teacherId == null) return ResponseData.error(401, "Unauthorized");
            if (!isTeacherOfClass(teacherId, classId)) return ResponseData.error(403, "Không có quyền");

            List<ClassSession> sessions = classSessionRepository.findByClazzClassIdOrderBySessionNumberAsc(classId);

            List<Map<String, Object>> result = sessions.stream().map(s -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("sessionId", s.getSessionId());
                m.put("sessionNumber", s.getSessionNumber());
                m.put("sessionDate", s.getSessionDate());
                m.put("startTime", s.getStartTime());
                m.put("endTime", s.getEndTime());
                m.put("topic", s.getTopic());
                m.put("notes", s.getNotes());
                m.put("quizId", s.getQuiz() != null ? s.getQuiz().getQuizId() : null);
                m.put("quizTitle", s.getQuiz() != null ? s.getQuiz().getTitle() : null);
                m.put("quizStatus", s.getQuiz() != null ? s.getQuiz().getStatus() : null);
                return m;
            }).toList();

            return ResponseData.success("Danh sách buổi học", result);
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public ResponseData<Map<String, Object>> getClassSessionsDetail(String email, Integer classId) {
        try {
            Integer teacherId = getTeacherId(email);
            if (teacherId == null) return ResponseData.error(401, "Unauthorized");
            if (!isTeacherOfClass(teacherId, classId)) return ResponseData.error(403, "Không có quyền");

            Clazz clazz = clazzRepository.findById(classId).orElse(null);
            if (clazz == null) return ResponseData.error(404, "Không tìm thấy lớp");

            Map<String, Object> info = new LinkedHashMap<>();
            info.put("classId", clazz.getClassId());
            info.put("className", clazz.getClassName());
            info.put("courseName", clazz.getCourse() != null ? clazz.getCourse().getTitle() : null);
            info.put("startDate", clazz.getStartDate());
            info.put("endDate", clazz.getEndDate());
            info.put("schedule", clazz.getSchedule());
            info.put("slotTime", clazz.getSlotTime());
            info.put("status", clazz.getStatus());

            return ResponseData.success("Chi tiết lớp", info);
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    @Transactional
    public ResponseData<Integer> createSession(String email, Integer classId, Integer sessionNumber,
            LocalDateTime sessionDate, String startTime, String endTime, String topic, String notes, Integer quizId) {
        try {
            Integer teacherId = getTeacherId(email);
            if (teacherId == null) return ResponseData.error(401, "Unauthorized");
            if (!isTeacherOfClass(teacherId, classId)) return ResponseData.error(403, "Không có quyền");

            Clazz clazz = clazzRepository.findById(classId).orElse(null);
            if (clazz == null) return ResponseData.error(404, "Không tìm thấy lớp");

            Quiz quiz = null;
            if (quizId != null) {
                quiz = quizRepository.findById(quizId).orElse(null);
            }

            ClassSession session = ClassSession.builder()
                    .clazz(clazz)
                    .sessionNumber(sessionNumber)
                    .sessionDate(sessionDate)
                    .startTime(startTime)
                    .endTime(endTime)
                    .topic(topic)
                    .notes(notes)
                    .quiz(quiz)
                    .build();

            ClassSession saved = classSessionRepository.save(session);
            return ResponseData.success("Tạo buổi học thành công", saved.getSessionId());
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    @Transactional
    public ResponseData<Void> updateSession(String email, Integer sessionId, Integer sessionNumber,
            LocalDateTime sessionDate, String startTime, String endTime, String topic, String notes, Integer quizId) {
        try {
            Integer teacherId = getTeacherId(email);
            if (teacherId == null) return ResponseData.error(401, "Unauthorized");

            ClassSession session = classSessionRepository.findById(sessionId).orElse(null);
            if (session == null) return ResponseData.error(404, "Không tìm thấy buổi học");
            if (!isTeacherOfClass(teacherId, session.getClazz().getClassId())) return ResponseData.error(403, "Không có quyền");

            if (sessionNumber != null) session.setSessionNumber(sessionNumber);
            if (sessionDate != null) session.setSessionDate(sessionDate);
            if (startTime != null) session.setStartTime(startTime);
            if (endTime != null) session.setEndTime(endTime);
            if (topic != null) session.setTopic(topic);
            if (notes != null) session.setNotes(notes);
            if (quizId != null) {
                Quiz quiz = quizRepository.findById(quizId).orElse(null);
                session.setQuiz(quiz);
            }

            classSessionRepository.save(session);
            return ResponseData.success("Cập nhật thành công");
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    @Transactional
    public ResponseData<Void> deleteSession(String email, Integer sessionId) {
        try {
            Integer teacherId = getTeacherId(email);
            if (teacherId == null) return ResponseData.error(401, "Unauthorized");

            ClassSession session = classSessionRepository.findById(sessionId).orElse(null);
            if (session == null) return ResponseData.error(404, "Không tìm thấy buổi học");
            if (!isTeacherOfClass(teacherId, session.getClazz().getClassId())) return ResponseData.error(403, "Không có quyền");

            classSessionRepository.delete(session);
            return ResponseData.success("Xóa buổi học thành công");
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public ResponseData<List<Map<String, Object>>> getAvailableQuizzes(String email, Integer classId) {
        try {
            Integer teacherId = getTeacherId(email);
            if (teacherId == null) return ResponseData.error(401, "Unauthorized");
            if (!isTeacherOfClass(teacherId, classId)) return ResponseData.error(403, "Không có quyền");

            Clazz clazz = clazzRepository.findById(classId).orElse(null);
            Integer courseId = clazz != null && clazz.getCourse() != null ? clazz.getCourse().getCourseId() : null;

            List<Quiz> quizzes = quizRepository.findAll().stream()
                    .filter(q -> q.getClazz() != null && Objects.equals(q.getClazz().getClassId(), classId))
                    .toList();

            List<Map<String, Object>> result = quizzes.stream().map(q -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("quizId", q.getQuizId());
                m.put("title", q.getTitle());
                m.put("status", q.getStatus());
                m.put("questionCount", q.getQuizQuestions() != null ? q.getQuizQuestions().size() : 0);
                return m;
            }).toList();

            return ResponseData.success("Danh sách quiz", result);
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }
}
