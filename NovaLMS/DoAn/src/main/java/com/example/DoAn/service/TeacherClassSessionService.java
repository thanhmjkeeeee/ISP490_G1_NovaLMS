package com.example.DoAn.service;

import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.model.*;
import com.example.DoAn.repository.*;
import com.example.DoAn.dto.request.AssignmentScheduleRequestDTO;
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
import java.time.LocalDateTime;
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
    private final RegistrationRepository registrationRepository;
    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final SessionLessonRepository sessionLessonRepository;
    private final AssignmentSessionRepository assignmentSessionRepository;
    private final UserLessonRepository userLessonRepository;
    private final INotificationService notificationService;
    private final EmailService emailService;
    private final TeacherScheduleConflictService teacherScheduleConflictService;
    private final TeacherQuizService teacherQuizService;

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
        if (teacherId == null)
            return null;
        ClassSession session = classSessionRepository.findById(sessionId).orElse(null);
        if (session == null)
            return null;
        if (!isTeacherOfClass(teacherId, session.getClazz().getClassId()))
            return null;
        return session;
    }

    private List<String> parseMaterials(String materialsJson) {
        if (materialsJson == null || materialsJson.isBlank())
            return List.of();
        try {
            return objectMapper.readValue(materialsJson, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    private String serializeMaterials(List<String> materials) {
        if (materials == null || materials.isEmpty())
            return null;
        try {
            return objectMapper.writeValueAsString(materials);
        } catch (Exception e) {
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // GET SESSIONS (now includes multi-quiz + materials)
    // ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ResponseData<List<Map<String, Object>>> getSessionsByClass(String email, Integer classId) {
        try {
            Integer teacherId = getTeacherId(email);
            if (teacherId == null)
                return ResponseData.error(401, "Vui lòng đăng nhập.");
            if (!isTeacherOfClass(teacherId, classId))
                return ResponseData.error(403, "Không có quyền");

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

                // New multi-quiz from session_quiz table (ONLY COURSE_QUIZ for the Quiz tab)
                List<SessionQuiz> sqList = sessionQuizRepository
                        .findBySessionSessionIdAndQuiz_QuizCategoryOrderByOrderIndexAsc(s.getSessionId(),
                                "COURSE_QUIZ");
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
            if (teacherId == null)
                return ResponseData.error(401, "Vui lòng đăng nhập.");
            if (!isTeacherOfClass(teacherId, classId))
                return ResponseData.error(403, "Không có quyền");

            Clazz clazz = clazzRepository.findById(classId).orElse(null);
            if (clazz == null)
                return ResponseData.error(404, "Không tìm thấy lớp");

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
    // CREATE / UPDATE / DELETE SESSION
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public ResponseData<Integer> createSession(String email, Integer classId, Integer sessionNumber,
            java.time.LocalDateTime sessionDate, String startTime, String endTime, String topic, String notes,
            Integer quizId) {
        try {
            Integer teacherId = getTeacherId(email);
            if (teacherId == null)
                return ResponseData.error(401, "Vui lòng đăng nhập.");
            if (!isTeacherOfClass(teacherId, classId))
                return ResponseData.error(403, "Không có quyền");

            Clazz clazz = clazzRepository.findById(classId).orElse(null);
            if (clazz == null)
                return ResponseData.error(404, "Không tìm thấy lớp");

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

            Optional<String> conflictCreate = teacherScheduleConflictService.checkSingleSessionConflict(
                    teacherId, sessionDate, startTime, null, null);
            if (conflictCreate.isPresent()) {
                return ResponseData.error(400, conflictCreate.get());
            }

            ClassSession saved = classSessionRepository.save(session);

            // ── Notify enrolled students of new session ───────────────────────
            notifyStudentsSessionCreated(saved, clazz);

            return ResponseData.success("Tạo buổi học thành công", saved.getSessionId());
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    @Transactional
    public ResponseData<Void> updateSession(String email, Integer sessionId, Integer sessionNumber,
            java.time.LocalDateTime sessionDate, String startTime, String endTime, String topic, String notes,
            Integer quizId) {
        try {
            Integer teacherId = getTeacherId(email);
            if (teacherId == null)
                return ResponseData.error(401, "Vui lòng đăng nhập.");

            ClassSession session = classSessionRepository.findById(sessionId).orElse(null);
            if (session == null)
                return ResponseData.error(404, "Không tìm thấy buổi học");
            if (!isTeacherOfClass(teacherId, session.getClazz().getClassId()))
                return ResponseData.error(403, "Không có quyền");

            // Capture old values for reschedule notification
            java.time.LocalDateTime oldDate = session.getSessionDate();
            String oldTime = session.getStartTime();

            boolean dateChanged = sessionDate != null && !sessionDate.equals(session.getSessionDate());
            boolean timeChanged = startTime != null && !startTime.equals(session.getStartTime());

            if (sessionNumber != null)
                session.setSessionNumber(sessionNumber);
            if (sessionDate != null)
                session.setSessionDate(sessionDate);
            if (startTime != null)
                session.setStartTime(startTime);
            if (endTime != null)
                session.setEndTime(endTime);
            if (topic != null)
                session.setTopic(topic);
            if (notes != null)
                session.setNotes(notes);
            if (quizId != null) {
                Quiz quiz = quizRepository.findById(quizId).orElse(null);
                session.setQuiz(quiz);
            }

            LocalDateTime effectiveDate = sessionDate != null ? sessionDate : session.getSessionDate();
            String effectiveStart = startTime != null ? startTime : session.getStartTime();
            Optional<String> conflictUpdate = teacherScheduleConflictService.checkSingleSessionConflict(
                    teacherId, effectiveDate, effectiveStart, null, session.getSessionId());
            if (conflictUpdate.isPresent()) {
                return ResponseData.error(400, conflictUpdate.get());
            }

            classSessionRepository.save(session);

            // ── Notify if session was rescheduled ─────────────────────────────
            if ((dateChanged || timeChanged) && session.getClazz() != null) {
                notifyStudentsSessionRescheduled(session, oldDate, oldTime);
            }

            return ResponseData.success("Cập nhật thành công");
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    @Transactional
    public ResponseData<Void> deleteSession(String email, Integer sessionId) {
        try {
            Integer teacherId = getTeacherId(email);
            if (teacherId == null)
                return ResponseData.error(401, "Vui lòng đăng nhập.");

            ClassSession session = classSessionRepository.findById(sessionId).orElse(null);
            if (session == null)
                return ResponseData.error(404, "Không tìm thấy buổi học");
            if (!isTeacherOfClass(teacherId, session.getClazz().getClassId()))
                return ResponseData.error(403, "Không có quyền");

            // ── Notify enrolled students ─────────────────────────────────
            notifyStudentsSessionCancelled(session);

            classSessionRepository.delete(session);
            return ResponseData.success("Xóa buổi học thành công");
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // QUIZ MANAGEMENT (multi-quiz per session)
    // ─────────────────────────────────────────────────────────────

    /**
     * Gắn quiz vào session.
     */
    @Transactional
    public ResponseData<Map<String, Object>> addQuizToSession(String email, Integer sessionId, Integer quizId) {
        try {
            ClassSession session = getSessionWithAuth(email, sessionId);
            if (session == null)
                return ResponseData.error(401, "Không tìm thấy buổi học hoặc không có quyền");

            Quiz quiz = quizRepository.findById(quizId).orElse(null);
            if (quiz == null)
                return ResponseData.error(404, "Không tìm thấy quiz");

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
            if (session == null)
                return ResponseData.error(401, "Không tìm thấy buổi học hoặc không có quyền");

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
    public ResponseData<Map<String, Object>> toggleQuizOpenInSession(String email, Integer sessionId, Integer quizId, Integer timeLimitMinutes) {
        try {
            ClassSession session = getSessionWithAuth(email, sessionId);
            if (session == null)
                return ResponseData.error(401, "Không tìm thấy buổi học hoặc không có quyền");

            SessionQuiz sq = sessionQuizRepository.findBySessionSessionIdAndQuizQuizId(sessionId, quizId).orElse(null);
            if (sq == null)
                return ResponseData.error(404, "Quiz không tồn tại trong buổi học này");

            Boolean current = sq.getIsOpen();
            // Cùng logic nút workspace: wsToggleQuizOpen(..., newOpen) — API chỉ toggle; khi chuyển sang MỞ thì xuất bản nếu còn nháp
            boolean willOpen = current == null || !Boolean.TRUE.equals(current);
            if (willOpen) {
                Quiz quiz = sq.getQuiz();
                if (quiz != null && "DRAFT".equals(quiz.getStatus())) {
                    var pub = teacherQuizService.publishQuiz(quizId, email);
                    if (pub.getStatus() != 200) {
                        return ResponseData.error(pub.getStatus(), pub.getMessage());
                    }
                }
            }
            Boolean updated = willOpen;
            sq.setIsOpen(updated);
            sessionQuizRepository.save(sq);

            if (updated && timeLimitMinutes != null) {
                if (timeLimitMinutes <= 0) {
                    return ResponseData.error(400, "Thời gian làm bài phải lớn hơn 0");
                }
                Quiz quiz = sq.getQuiz();
                quiz.setTimeLimitMinutes(timeLimitMinutes);
                quizRepository.save(quiz);
            }

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
            if (session == null)
                return ResponseData.error(401, "Không tìm thấy buổi học hoặc không có quyền");

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
            if (session == null)
                return ResponseData.error(401, "Không tìm thấy buổi học hoặc không có quyền");

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
    // MATERIALS (FILE UPLOAD)
    // ─────────────────────────────────────────────────────────────

    /**
     * Upload file tài liệu cho session.
     */
    @Transactional
    public ResponseData<List<String>> uploadMaterials(String email, Integer sessionId, List<MultipartFile> files) {
        try {
            ClassSession session = getSessionWithAuth(email, sessionId);
            if (session == null)
                return ResponseData.error(401, "Không tìm thấy buổi học hoặc không có quyền");

            if (files == null || files.isEmpty()) {
                return ResponseData.error(400, "Không có file nào được chọn");
            }

            // Build upload directory
            Path uploadPath = Paths.get(uploadDirBase, "session_" + sessionId);
            Files.createDirectories(uploadPath);

            List<String> savedFiles = new ArrayList<>(parseMaterials(session.getMaterials()));

            for (MultipartFile file : files) {
                if (file.isEmpty())
                    continue;

                String originalFilename = file.getOriginalFilename();
                if (originalFilename == null || originalFilename.isBlank())
                    continue;

                // Sanitize filename
                String safeFilename = System.currentTimeMillis() + "_"
                        + originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
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
            if (session == null)
                return ResponseData.error(401, "Không tìm thấy buổi học hoặc không có quyền");

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
    // AVAILABLE QUIZZES
    // ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ResponseData<List<Map<String, Object>>> getAvailableQuizzes(String email, Integer classId) {
        try {
            Integer teacherId = getTeacherId(email);
            if (teacherId == null)
                return ResponseData.error(401, "Vui lòng đăng nhập.");
            if (!isTeacherOfClass(teacherId, classId))
                return ResponseData.error(403, "Không có quyền");

            Clazz clazz = clazzRepository.findById(classId).orElse(null);
            Integer courseId = clazz != null && clazz.getCourse() != null ? clazz.getCourse().getCourseId() : null;

            List<Quiz> quizzes = List.of();
            if (courseId != null) {
                quizzes = quizRepository.findAllVisibleForTeacher(List.of(courseId), teacherId);
            }

            List<Map<String, Object>> result = quizzes.stream().map(q -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("quizId", q.getQuizId());
                m.put("title", q.getTitle());
                m.put("description", q.getDescription());
                m.put("status", q.getStatus());
                m.put("timeLimitMinutes", q.getTimeLimitMinutes());
                m.put("passScore", q.getPassScore());
                m.put("maxAttempts", q.getMaxAttempts());
                m.put("createdAt", q.getCreatedAt());
                m.put("createdBy", q.getUser() != null ? q.getUser().getFullName() : null);

                // Question count
                int qCount = q.getQuizQuestions() != null ? q.getQuizQuestions().size() : 0;
                m.put("questionCount", qCount);

                // Skill breakdown: { "READING": 3, "LISTENING": 2, ... }
                if (q.getQuizQuestions() != null && !q.getQuizQuestions().isEmpty()) {
                    Map<String, Long> skillBreakdown = q.getQuizQuestions().stream()
                            .filter(qq -> qq.getQuestion() != null && qq.getQuestion().getSkill() != null)
                            .collect(java.util.stream.Collectors.groupingBy(
                                    qq -> qq.getQuestion().getSkill(),
                                    java.util.stream.Collectors.counting()));
                    m.put("skills", skillBreakdown);

                    // Unique CEFR levels
                    List<String> cefrLevels = q.getQuizQuestions().stream()
                            .filter(qq -> qq.getQuestion() != null && qq.getQuestion().getCefrLevel() != null)
                            .map(qq -> qq.getQuestion().getCefrLevel())
                            .distinct().sorted().toList();
                    m.put("cefrLevels", cefrLevels);
                } else {
                    m.put("skills", Map.of());
                    m.put("cefrLevels", List.of());
                }

                return m;
            }).toList();

            return ResponseData.success("Danh sách quiz", result);
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // WORKSPACE ADDITIONS (Students, Course Content, Mapping)
    // ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ResponseData<List<Map<String, Object>>> getStudentsByClass(String email, Integer classId) {
        try {
            Integer teacherId = getTeacherId(email);
            if (teacherId == null)
                return ResponseData.error(401, "Vui lòng đăng nhập.");
            if (!isTeacherOfClass(teacherId, classId))
                return ResponseData.error(403, "Không có quyền");

            List<Registration> regs = registrationRepository.findApprovedByClassId(classId);
            List<Map<String, Object>> result = new ArrayList<>();

            Clazz clazz = clazzRepository.findById(classId).orElse(null);
            if (clazz == null || clazz.getCourse() == null) {
                return ResponseData.error(404, "Lớp học hoặc khóa học không tồn tại");
            }
            Integer courseId = clazz.getCourse().getCourseId();
            long totalLessons = lessonRepository.countByModuleCourse_CourseId(courseId);

            for (Registration r : regs) {
                Map<String, Object> m = new LinkedHashMap<>();
                User student = r.getUser();
                m.put("studentId", student.getUserId());
                m.put("fullName", student.getFullName());
                m.put("email", student.getEmail());

                // Calculate actual progress for this student in this course
                long completedLessons = userLessonRepository
                        .countCompletedLessonsByUserIdAndCourseId(student.getUserId(), courseId);
                int progress = totalLessons > 0 ? (int) (completedLessons * 100 / totalLessons) : 0;

                m.put("progress", progress + "%");
                result.add(m);
            }

            return ResponseData.success("Danh sách học viên", result);
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public ResponseData<Map<String, Object>> getCourseContentForMapping(String email, Integer classId) {
        try {
            Integer teacherId = getTeacherId(email);
            if (teacherId == null)
                return ResponseData.error(401, "Vui lòng đăng nhập.");
            if (!isTeacherOfClass(teacherId, classId))
                return ResponseData.error(403, "Không có quyền");

            Clazz clazz = clazzRepository.findById(classId).orElse(null);
            if (clazz == null || clazz.getCourse() == null)
                return ResponseData.error(404, "Không tìm thấy khóa học của lớp");

            // Fetch current mappings for this class
            List<SessionLesson> currentMappings = sessionLessonRepository
                    .findByClassSession_Clazz_ClassIdOrderByOrderIndexAsc(classId);
            Map<Integer, Integer> lessonToSessionMap = new HashMap<>(); // lessonId -> sessionId
            for (SessionLesson sl : currentMappings) {
                if (sl.getLesson() != null && sl.getSession() != null) {
                    lessonToSessionMap.put(sl.getLesson().getLessonId(), sl.getSession().getSessionId());
                }
            }

            List<com.example.DoAn.model.Module> modules = moduleRepository
                    .findByCourse_CourseIdOrderByOrderIndexAsc(clazz.getCourse().getCourseId());

            List<Map<String, Object>> moduleList = modules.stream().map(m -> {
                Map<String, Object> mm = new LinkedHashMap<>();
                mm.put("moduleId", m.getModuleId());
                mm.put("moduleName", m.getModuleName());

                List<Map<String, Object>> lessons = m.getLessons().stream().map(l -> {
                    Map<String, Object> lm = new LinkedHashMap<>();
                    lm.put("lessonId", l.getLessonId());
                    lm.put("lessonName", l.getLessonName());
                    lm.put("sessionId", lessonToSessionMap.get(l.getLessonId()));
                    return lm;
                }).toList();
                mm.put("lessons", lessons);
                return mm;
            }).toList();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("modules", moduleList);
            return ResponseData.success("Nội dung khóa học", result);
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    @Transactional
    public ResponseData<Void> saveMapping(String email, Integer classId, List<Map<String, Integer>> mappings) {
        try {
            Integer teacherId = getTeacherId(email);
            if (teacherId == null)
                return ResponseData.error(401, "Vui lòng đăng nhập.");
            if (!isTeacherOfClass(teacherId, classId))
                return ResponseData.error(403, "Không có quyền");

            // 1. Clear old mappings for this class
            sessionLessonRepository.deleteBySession_Clazz_ClassId(classId);

            // 2. Clear topics for all sessions in this class (for refresh)
            List<ClassSession> classSessions = classSessionRepository
                    .findByClazzClassIdOrderBySessionNumberAsc(classId);
            for (ClassSession s : classSessions) {
                s.setTopic("Chưa cập nhật chủ đề...");
                classSessionRepository.save(s);
            }

            // 3. Save new mappings and group topics
            Map<Integer, List<String>> sessionTopicsMap = new HashMap<>();

            for (Map<String, Integer> map : mappings) {
                Integer lessonId = map.get("lessonId");
                Integer sessionId = map.get("sessionId");

                if (lessonId != null && sessionId != null) {
                    ClassSession session = classSessionRepository.findById(sessionId).orElse(null);
                    Lesson lesson = lessonRepository.findById(lessonId).orElse(null);

                    if (session != null && lesson != null && session.getClazz().getClassId().equals(classId)) {
                        // Create persistent link
                        SessionLesson sl = SessionLesson.builder()
                                .session(session)
                                .lesson(lesson)
                                .orderIndex(session.getSessionNumber())
                                .build();
                        sessionLessonRepository.save(sl);

                        // Collect topics
                        sessionTopicsMap.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(lesson.getLessonName());
                    }
                }
            }

            // 4. Update session topics with joined strings
            for (Map.Entry<Integer, List<String>> entry : sessionTopicsMap.entrySet()) {
                ClassSession session = classSessionRepository.findById(entry.getKey()).orElse(null);
                if (session != null) {
                    String joinedTopic = String.join(", ", entry.getValue());
                    session.setTopic(joinedTopic);
                    classSessionRepository.save(session);
                }
            }

            return ResponseData.success("Lưu mapping thành công");
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // MEET LINK UPDATE (per-session override)
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public ResponseData<Void> updateMeetLink(String email, Integer sessionId, String meetLink) {
        try {
            ClassSession session = getSessionWithAuth(email, sessionId);
            if (session == null)
                return ResponseData.error(401, "Không tìm thấy buổi học hoặc không có quyền");

            session.setMeetLink(meetLink != null && !meetLink.isBlank() ? meetLink.trim() : null);
            classSessionRepository.save(session);

            return ResponseData.success("Đã cập nhật link Meet/Zoom");
        } catch (Exception e) {
            return ResponseData.error(500, "Lỗi cập nhật link: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // ASSIGNMENT MANAGEMENT (PRO)
    // ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ResponseData<List<Map<String, Object>>> getExpertAssignmentsByClass(String email, Integer classId) {
        try {
            Integer teacherId = getTeacherId(email);
            if (teacherId == null)
                return ResponseData.error(401, "Vui lòng đăng nhập.");
            if (!isTeacherOfClass(teacherId, classId))
                return ResponseData.error(403, "Không có quyền");

            // 1. Get all expert quizzes for this class's course
            List<Quiz> expertQuizzes = quizRepository.findExpertQuizzesByClassId(classId);

            // 2. Get all session assignments for this class to check status
            List<SessionQuiz> sqList = sessionQuizRepository.findBySession_Clazz_ClassId(classId);

            // 3. Map status
            List<Map<String, Object>> result = expertQuizzes.stream().map(q -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("quizId", q.getQuizId());
                m.put("title", q.getTitle());
                m.put("questionCount", q.getQuizQuestions() != null ? q.getQuizQuestions().size() : 0);

                // Find if this quiz is assigned to any session in this class
                SessionQuiz assignedSq = sqList.stream()
                        .filter(sq -> sq.getQuiz().getQuizId().equals(q.getQuizId()))
                        .findFirst().orElse(null);

                if (assignedSq != null) {
                    m.put("sessionQuizId", assignedSq.getId());
                    m.put("sessionId", assignedSq.getSession().getSessionId());
                    m.put("sessionNumber", assignedSq.getSession().getSessionNumber());
                    m.put("isOpen", assignedSq.getIsOpen() != null ? assignedSq.getIsOpen() : false);
                    m.put("openAt", assignedSq.getOpenAt());
                    m.put("closeAt", assignedSq.getCloseAt());
                    m.put("deadline", assignedSq.getDeadline());
                } else {
                    m.put("sessionQuizId", null);
                    m.put("sessionId", null);
                    m.put("sessionNumber", null);
                    m.put("isOpen", false);
                    m.put("openAt", null);
                    m.put("closeAt", null);
                    m.put("deadline", null);
                }
                return m;
            }).toList();

            return ResponseData.success("Danh sách bài tập chuyên gia", result);
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public ResponseData<List<Map<String, Object>>> getAssignmentsByClass(String email, Integer classId) {
        try {
            Integer teacherId = getTeacherId(email);
            if (teacherId == null)
                return ResponseData.error(401, "Vui lòng đăng nhập.");
            if (!isTeacherOfClass(teacherId, classId))
                return ResponseData.error(403, "Không có quyền");

            // 1. Get all expert assignments for this class's course
            List<Quiz> expertQuizzes = quizRepository.findExpertQuizzesByClassId(classId);

            // 2. Get all session assignments for this class
            List<SessionQuiz> sqList = sessionQuizRepository.findBySession_Clazz_ClassId(classId);

            // 3. Merge status
            List<Map<String, Object>> result = expertQuizzes.stream().map(q -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("quizId", q.getQuizId());
                m.put("title", q.getTitle());
                m.put("status", q.getStatus());
                m.put("questionCount", q.getQuizQuestions() != null ? q.getQuizQuestions().size() : 0);

                // Find if this quiz is assigned to any session in this class
                SessionQuiz assignedSq = sqList.stream()
                        .filter(sq -> sq.getQuiz().getQuizId().equals(q.getQuizId()))
                        .findFirst().orElse(null);

                if (assignedSq != null) {
                    m.put("sessionQuizId", assignedSq.getId());
                    m.put("sessionId", assignedSq.getSession().getSessionId());
                    m.put("sessionNumber", assignedSq.getSession().getSessionNumber());
                    m.put("isOpen", assignedSq.getIsOpen() != null ? assignedSq.getIsOpen() : false);
                    m.put("openAt", assignedSq.getOpenAt());
                    m.put("closeAt", assignedSq.getCloseAt());
                    m.put("deadline", assignedSq.getDeadline());
                } else {
                    m.put("sessionQuizId", null);
                    m.put("sessionId", null);
                    m.put("sessionNumber", null);
                    // For unassigned, take isOpen from the Quiz entity
                    m.put("isOpen", q.getIsOpen() != null ? q.getIsOpen() : false);
                    m.put("openAt", null);
                    m.put("closeAt", null);
                    m.put("deadline", null);
                }
                return m;
            }).toList();

            return ResponseData.success("Danh sách bài tập", result);
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    @Transactional
    public ResponseData<Void> updateAssignmentSchedule(String email, Integer sessionQuizId,
            AssignmentScheduleRequestDTO request) {
        try {
            Integer teacherId = getTeacherId(email);
            if (teacherId == null)
                return ResponseData.error(401, "Vui lòng đăng nhập.");

            SessionQuiz sq = sessionQuizRepository.findById(sessionQuizId).orElse(null);
            if (sq == null)
                return ResponseData.error(404, "Không tìm thấy cấu hình assignment");

            if (!isTeacherOfClass(teacherId, sq.getSession().getClazz().getClassId())) {
                return ResponseData.error(403, "Không có quyền");
            }

            if (request.getOpenAt() != null && request.getOpenAt().isBefore(LocalDateTime.now().minusMinutes(5))) {
                return ResponseData.error(400, "Thời gian mở không được ở trong quá khứ");
            }
            if (request.getCloseAt() != null && request.getCloseAt().isBefore(LocalDateTime.now())) {
                return ResponseData.error(400, "Thời gian đóng không được ở trong quá khứ");
            }
            if (request.getOpenAt() != null && request.getCloseAt() != null
                    && !request.getCloseAt().isAfter(request.getOpenAt())) {
                return ResponseData.error(400, "Thời gian đóng phải sau thời gian mở");
            }
            if (request.getDeadline() != null && request.getOpenAt() != null
                    && !request.getDeadline().isAfter(request.getOpenAt())) {
                return ResponseData.error(400, "Deadline phải sau thời gian mở");
            }

            sq.setOpenAt(request.getOpenAt());
            sq.setCloseAt(request.getCloseAt());
            sq.setDeadline(request.getDeadline());

            sessionQuizRepository.save(sq);
            return ResponseData.success("Cập nhật thời gian thành công");
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    @Transactional
    public ResponseData<Void> resetStudentAttempt(String email, Integer quizId, Long studentId) {
        try {
            Integer teacherId = getTeacherId(email);
            if (teacherId == null)
                return ResponseData.error(401, "Vui lòng đăng nhập.");

            AssignmentSession session = assignmentSessionRepository.findByQuizQuizIdAndUserUserId(quizId, studentId)
                    .orElse(null);
            if (session == null)
                return ResponseData.error(404, "Không tìm thấy bài làm của học viên");

            assignmentSessionRepository.delete(session);
            return ResponseData.success("Đã reset lượt làm bài cho học viên");
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // NOTIFICATIONS
    // ─────────────────────────────────────────────────────────────

    private void notifyStudentsSessionCreated(ClassSession session, Clazz clazz) {
        try {
            List<Registration> registrations = registrationRepository.findApprovedByClassId(clazz.getClassId());
            String dateStr = session.getSessionDate() != null
                    ? session.getSessionDate().toLocalDate().toString()
                    : "";
            String timeStr = session.getStartTime() != null ? session.getStartTime() : "";

            for (Registration reg : registrations) {
                User student = reg.getUser();
                if (student == null || student.getUserId() == null)
                    continue;

                String studentName = student.getFullName() != null ? student.getFullName() : "";
                String email = student.getEmail();

                if (email != null && !email.isBlank()) {
                    emailService.sendSessionReminderEmail(email, studentName, clazz.getClassName(),
                            session.getTopic(), dateStr, timeStr, session.getMeetLink());
                }
                notificationService.sendSessionReminder(
                        Long.valueOf(student.getUserId()),
                        clazz.getClassName(),
                        session.getTopic(),
                        dateStr,
                        session.getMeetLink());
            }
        } catch (Exception e) {
            System.err.println("Lỗi gửi thông báo tạo buổi học: " + e.getMessage());
        }
    }

    private void notifyStudentsSessionRescheduled(ClassSession session, java.time.LocalDateTime oldDate,
            String oldTime) {
        try {
            Clazz clazz = session.getClazz();
            if (clazz == null)
                return;

            List<Registration> registrations = registrationRepository.findApprovedByClassId(clazz.getClassId());
            String oldDateStr = oldDate != null ? oldDate.toLocalDate().toString() : "";
            String newDateStr = session.getSessionDate() != null ? session.getSessionDate().toLocalDate().toString()
                    : "";
            String newTimeStr = session.getStartTime() != null ? session.getStartTime() : "";

            for (Registration reg : registrations) {
                User student = reg.getUser();
                if (student == null || student.getUserId() == null)
                    continue;

                String studentName = student.getFullName() != null ? student.getFullName() : "";
                String email = student.getEmail();

                if (email != null && !email.isBlank()) {
                    emailService.sendSessionRescheduledEmail(email, studentName, clazz.getClassName(),
                            oldDateStr, oldTime, newDateStr, newTimeStr, "Giáo viên thay đổi lịch dạy");
                }
                notificationService.sendSessionRescheduled(
                        Long.valueOf(student.getUserId()),
                        clazz.getClassName(),
                        newDateStr,
                        newTimeStr,
                        "Giảng viên đã cập nhật lịch học buổi " + session.getSessionNumber());
            }
        } catch (Exception e) {
            System.err.println("Lỗi gửi thông báo đổi lịch buổi học: " + e.getMessage());
        }
    }

    private void notifyStudentsSessionCancelled(ClassSession session) {
        try {
            Clazz clazz = session.getClazz();
            if (clazz == null)
                return;

            List<Registration> registrations = registrationRepository.findApprovedByClassId(clazz.getClassId());
            String dateStr = session.getSessionDate() != null ? session.getSessionDate().toLocalDate().toString() : "";
            String timeStr = session.getStartTime() != null ? session.getStartTime() : "";

            for (Registration reg : registrations) {
                User student = reg.getUser();
                if (student == null || student.getUserId() == null)
                    continue;

                String studentName = student.getFullName() != null ? student.getFullName() : "";
                String email = student.getEmail();

                if (email != null && !email.isBlank()) {
                    emailService.sendSessionCancelledEmail(email, studentName, clazz.getClassName(),
                            dateStr, timeStr, "Buổi học đã bị hủy bởi giáo viên");
                }
                notificationService.sendSessionCancelled(
                        Long.valueOf(student.getUserId()),
                        clazz.getClassName(),
                        dateStr,
                        "Buổi học số " + session.getSessionNumber() + " đã bị hủy.");
            }
        } catch (Exception e) {
            System.err.println("Lỗi gửi thông báo hủy buổi học: " + e.getMessage());
        }
    }
}
