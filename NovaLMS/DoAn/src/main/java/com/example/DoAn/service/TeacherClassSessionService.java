package com.example.DoAn.service;

import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.model.*;
import com.example.DoAn.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TeacherClassSessionService {

    private final ClassSessionRepository classSessionRepository;
    private final QuizRepository quizRepository;
    private final UserRepository userRepository;
    private final ClazzRepository clazzRepository;
    private final SessionQuizRepository sessionQuizRepository;
    private final QuizQuestionRepository quizQuestionRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${app.upload.dir:src/main/resources/static/uploads/sessions}")
    private String uploadDirBase;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Integer getTeacherId(String email) {
        List<Integer> ids = entityManager.createQuery(
                "SELECT u.userId FROM User u WHERE u.email = :email", Integer.class)
                .setParameter("email", email)
                .setMaxResults(1)
                .getResultList();
        return ids.isEmpty() ? null : ids.get(0);
    }

    private boolean isTeacherOfClass(Integer teacherId, Integer classId) {
        Clazz clazz = clazzRepository.findById(classId).orElse(null);
        return clazz != null && clazz.getTeacher() != null
                && Objects.equals(clazz.getTeacher().getUserId(), teacherId);
    }

    private ClassSession getSessionWithAuth(String email, Integer sessionId) {
        Integer teacherId = getTeacherId(email);
        if (teacherId == null) return null;
        ClassSession session = classSessionRepository.findById(sessionId).orElse(null);
        if (session == null) return null;
        if (!isTeacherOfClass(teacherId, session.getClazz().getClassId())) return null;
        return session;
    }

    private List<String> parseMaterials(String materialsJson) {
        if (materialsJson == null || materialsJson.isBlank()) return List.of();
        try {
            return objectMapper.readValue(materialsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private String serializeMaterials(List<String> materials) {
        if (materials == null || materials.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(materials);
        } catch (Exception e) {
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  GET SESSIONS (now includes multi-quiz + materials)
    // ─────────────────────────────────────────────────────────────

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
                m.put("materials", parseMaterials(s.getMaterials()));

                // Legacy single quiz (for backward compat)
                m.put("quizId", s.getQuiz() != null ? s.getQuiz().getQuizId() : null);
                m.put("quizTitle", s.getQuiz() != null ? s.getQuiz().getTitle() : null);
                m.put("quizStatus", s.getQuiz() != null ? s.getQuiz().getStatus() : null);

                // New multi-quiz from session_quiz table
                List<SessionQuiz> sqList = sessionQuizRepository.findBySessionSessionIdOrderByOrderIndexAsc(s.getSessionId());
                List<Map<String, Object>> quizzesList = sqList.stream().map(sq -> {
                    Map<String, Object> qm = new LinkedHashMap<>();
                    Quiz q = sq.getQuiz();
                    qm.put("sessionQuizId", sq.getId());
                    qm.put("quizId", q.getQuizId());
                    qm.put("title", q.getTitle());
                    qm.put("status", q.getStatus());
                    qm.put("isOpen", sq.getIsOpen() != null ? sq.getIsOpen() : false);
                    qm.put("orderIndex", sq.getOrderIndex());
                    qm.put("questionCount", q.getQuizQuestions() != null ? q.getQuizQuestions().size() : 0);
                    return qm;
                }).toList();
                m.put("quizzes", quizzesList);

                boolean allOpen = !quizzesList.isEmpty() && quizzesList.stream()
                        .allMatch(q -> Boolean.TRUE.equals(q.get("isOpen")));
                m.put("isAllOpen", allOpen);

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

    // ─────────────────────────────────────────────────────────────
    //  CREATE / UPDATE / DELETE SESSION
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public ResponseData<Integer> createSession(String email, Integer classId, Integer sessionNumber,
            java.time.LocalDateTime sessionDate, String startTime, String endTime, String topic, String notes, Integer quizId) {
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
            java.time.LocalDateTime sessionDate, String startTime, String endTime, String topic, String notes, Integer quizId) {
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

    // ─────────────────────────────────────────────────────────────
    //  QUIZ MANAGEMENT (multi-quiz per session)
    // ─────────────────────────────────────────────────────────────

    /**
     * Gắn quiz vào session.
     */
    @Transactional
    public ResponseData<Map<String, Object>> addQuizToSession(String email, Integer sessionId, Integer quizId) {
        try {
            ClassSession session = getSessionWithAuth(email, sessionId);
            if (session == null) return ResponseData.error(401, "Không tìm thấy buổi học hoặc không có quyền");

            Quiz quiz = quizRepository.findById(quizId).orElse(null);
            if (quiz == null) return ResponseData.error(404, "Không tìm thấy quiz");

            if (sessionQuizRepository.existsBySessionSessionIdAndQuizQuizId(sessionId, quizId)) {
                return ResponseData.error(400, "Quiz này đã được gắn vào buổi học");
            }

            int count = sessionQuizRepository.countBySessionSessionId(sessionId);
            SessionQuiz sq = SessionQuiz.builder()
                    .session(session)
                    .quiz(quiz)
                    .orderIndex(count + 1)
                    .isOpen(false)
                    .build();
            SessionQuiz saved = sessionQuizRepository.save(sq);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("sessionQuizId", saved.getId());
            result.put("quizId", quiz.getQuizId());
            result.put("title", quiz.getTitle());
            result.put("status", quiz.getStatus());
            result.put("isOpen", saved.getIsOpen());
            result.put("orderIndex", saved.getOrderIndex());
            result.put("questionCount", quiz.getQuizQuestions() != null ? quiz.getQuizQuestions().size() : 0);

            return ResponseData.success("Đã gắn quiz vào buổi học", result);
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    /**
     * Xóa quiz khỏi session.
     */
    @Transactional
    public ResponseData<Void> removeQuizFromSession(String email, Integer sessionId, Integer quizId) {
        try {
            ClassSession session = getSessionWithAuth(email, sessionId);
            if (session == null) return ResponseData.error(401, "Không tìm thấy buổi học hoặc không có quyền");

            if (!sessionQuizRepository.existsBySessionSessionIdAndQuizQuizId(sessionId, quizId)) {
                return ResponseData.error(404, "Quiz không tồn tại trong buổi học này");
            }

            sessionQuizRepository.deleteBySessionSessionIdAndQuizQuizId(sessionId, quizId);
            return ResponseData.success("Đã xóa quiz khỏi buổi học");
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    /**
     * Toggle mở/đóng 1 quiz trong session.
     */
    @Transactional
    public ResponseData<Map<String, Object>> toggleQuizOpenInSession(String email, Integer sessionId, Integer quizId) {
        try {
            ClassSession session = getSessionWithAuth(email, sessionId);
            if (session == null) return ResponseData.error(401, "Không tìm thấy buổi học hoặc không có quyền");

            SessionQuiz sq = sessionQuizRepository.findBySessionSessionIdAndQuizQuizId(sessionId, quizId).orElse(null);
            if (sq == null) return ResponseData.error(404, "Quiz không tồn tại trong buổi học này");

            Boolean current = sq.getIsOpen();
            Boolean updated = (current == null || !current);
            sq.setIsOpen(updated);
            sessionQuizRepository.save(sq);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("quizId", sq.getQuiz().getQuizId());
            result.put("title", sq.getQuiz().getTitle());
            result.put("isOpen", updated);
            result.put("allOpen", false);

            return ResponseData.success(updated ? "Quiz đã được mở!" : "Quiz đã được đóng!", result);
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    /**
     * Mở tất cả quiz trong session.
     */
    @Transactional
    public ResponseData<List<Map<String, Object>>> openAllQuizzesInSession(String email, Integer sessionId) {
        try {
            ClassSession session = getSessionWithAuth(email, sessionId);
            if (session == null) return ResponseData.error(401, "Không tìm thấy buổi học hoặc không có quyền");

            List<SessionQuiz> sqList = sessionQuizRepository.findBySessionSessionId(sessionId);
            if (sqList.isEmpty()) {
                return ResponseData.error(400, "Buổi học chưa có quiz nào");
            }

            for (SessionQuiz sq : sqList) {
                sq.setIsOpen(true);
                sessionQuizRepository.save(sq);
            }

            List<Map<String, Object>> result = sqList.stream().map(sq -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("quizId", sq.getQuiz().getQuizId());
                m.put("title", sq.getQuiz().getTitle());
                m.put("isOpen", true);
                return m;
            }).toList();

            return ResponseData.success("Đã mở tất cả quiz!", result);
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    /**
     * Đóng tất cả quiz trong session.
     */
    @Transactional
    public ResponseData<List<Map<String, Object>>> closeAllQuizzesInSession(String email, Integer sessionId) {
        try {
            ClassSession session = getSessionWithAuth(email, sessionId);
            if (session == null) return ResponseData.error(401, "Không tìm thấy buổi học hoặc không có quyền");

            List<SessionQuiz> sqList = sessionQuizRepository.findBySessionSessionId(sessionId);
            for (SessionQuiz sq : sqList) {
                sq.setIsOpen(false);
                sessionQuizRepository.save(sq);
            }

            List<Map<String, Object>> result = sqList.stream().map(sq -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("quizId", sq.getQuiz().getQuizId());
                m.put("title", sq.getQuiz().getTitle());
                m.put("isOpen", false);
                return m;
            }).toList();

            return ResponseData.success("Đã đóng tất cả quiz!", result);
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  MATERIALS (FILE UPLOAD)
    // ─────────────────────────────────────────────────────────────

    /**
     * Upload file tài liệu cho session.
     */
    @Transactional
    public ResponseData<List<String>> uploadMaterials(String email, Integer sessionId, List<MultipartFile> files) {
        try {
            ClassSession session = getSessionWithAuth(email, sessionId);
            if (session == null) return ResponseData.error(401, "Không tìm thấy buổi học hoặc không có quyền");

            if (files == null || files.isEmpty()) {
                return ResponseData.error(400, "Không có file nào được chọn");
            }

            // Build upload directory
            Path uploadPath = Paths.get(uploadDirBase, "session_" + sessionId);
            Files.createDirectories(uploadPath);

            List<String> savedFiles = new ArrayList<>(parseMaterials(session.getMaterials()));

            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;

                String originalFilename = file.getOriginalFilename();
                if (originalFilename == null || originalFilename.isBlank()) continue;

                // Sanitize filename
                String safeFilename = System.currentTimeMillis() + "_" + originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
                Path filePath = uploadPath.resolve(safeFilename);
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                savedFiles.add("session_" + sessionId + "/" + safeFilename);
            }

            session.setMaterials(serializeMaterials(savedFiles));
            classSessionRepository.save(session);

            return ResponseData.success("Đã upload " + savedFiles.size() + " file(s)", savedFiles);
        } catch (IOException e) {
            return ResponseData.error(500, "Lỗi khi lưu file: " + e.getMessage());
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    /**
     * Xóa file tài liệu khỏi session.
     */
    @Transactional
    public ResponseData<List<String>> deleteMaterial(String email, Integer sessionId, String filename) {
        try {
            ClassSession session = getSessionWithAuth(email, sessionId);
            if (session == null) return ResponseData.error(401, "Không tìm thấy buổi học hoặc không có quyền");

            List<String> materials = new ArrayList<>(parseMaterials(session.getMaterials()));
            String target = "session_" + sessionId + "/" + filename;
            if (!materials.contains(target)) {
                return ResponseData.error(404, "File không tồn tại trong buổi học này");
            }

            // Delete physical file
            Path filePath = Paths.get(uploadDirBase, target);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }

            materials.remove(target);
            session.setMaterials(serializeMaterials(materials));
            classSessionRepository.save(session);

            return ResponseData.success("Đã xóa file", materials);
        } catch (IOException e) {
            return ResponseData.error(500, "Lỗi khi xóa file: " + e.getMessage());
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  AVAILABLE QUIZZES
    // ─────────────────────────────────────────────────────────────

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
