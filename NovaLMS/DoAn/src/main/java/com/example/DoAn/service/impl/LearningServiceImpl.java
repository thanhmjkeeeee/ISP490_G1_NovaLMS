package com.example.DoAn.service.impl;

import com.example.DoAn.dto.response.CourseLearningInfoDTO;
import com.example.DoAn.dto.response.ExpertLessonResponseDTO;
import com.example.DoAn.dto.response.LessonResponseDTO;
import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.dto.response.ChartDataDTO;
import com.example.DoAn.model.*;
import com.example.DoAn.model.Module;
import com.example.DoAn.repository.*;
import com.example.DoAn.model.UserLearningLog;
import com.example.DoAn.service.LearningService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearningServiceImpl implements LearningService {

    private final CourseRepository courseRepository;
    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final UserLessonRepository userLessonRepository;
    private final QuizRepository quizRepository;
    private final QuizResultRepository quizResultRepository;
    private final UserLearningLogRepository userLearningLogRepository;
    private final SessionQuizRepository sessionQuizRepository;

    @Override
    @Transactional(readOnly = true)
    public ResponseData<CourseLearningInfoDTO> getCourseLearningInfo(Long courseId, String email) {
        try {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) return ResponseData.error(401, "Người dùng không tồn tại");

            Course course = courseRepository.getCourseLearningData(courseId.intValue()).orElse(null);
            if (course == null) return ResponseData.error(404, "Không tìm thấy khóa học");

            Registration reg = registrationRepository.findByUser_UserIdAndCourse_CourseIdAndStatus(
                    user.getUserId(), course.getCourseId(), "Approved").orElse(null);

            List<UserLesson> userLessons = userLessonRepository.findByUser_UserId(user.getUserId());
            Set<Integer> completedLessonIds = userLessons.stream()
                    .filter(ul -> "Completed".equalsIgnoreCase(ul.getStatus()))
                    .map(ul -> ul.getLesson().getLessonId())
                    .collect(Collectors.toSet());

            CourseLearningInfoDTO courseInfo = new CourseLearningInfoDTO();
            courseInfo.setCourseId(course.getCourseId().longValue());
            courseInfo.setTitle(course.getCourseName());
            courseInfo.setCourseName(course.getCourseName()); // 🟢 THÊM DÒNG NÀY: Đồng bộ DTO
            courseInfo.setDescription(course.getDescription());

            if (reg != null && reg.getClazz() != null) {
                Clazz clazz = reg.getClazz();
                courseInfo.setClassName(clazz.getClassName());
                courseInfo.setSchedule(clazz.getSchedule());

                if (clazz.getTeacher() != null) {
                    courseInfo.setTeacherName(clazz.getTeacher().getFullName());
                    courseInfo.setTeacherAvatar(clazz.getTeacher().getAvatarUrl());
                }

                // ── Logic: Find nearest session's meet link ─────────────────────────
                String bestMeetLink = clazz.getMeetLink(); 
                String meetLabel = "Link lớp học";

                List<ClassSession> sessions = clazz.getSessions();
                if (sessions != null && !sessions.isEmpty()) {
                    java.time.LocalDateTime now = java.time.LocalDateTime.now();
                    // Filter and sort sessions with valid dates
                    List<ClassSession> validSessions = sessions.stream()
                            .filter(s -> s.getSessionDate() != null)
                            .sorted(Comparator.comparing(ClassSession::getSessionDate))
                            .collect(Collectors.toList());

                    // 1. Try today's sessions (find the first one that hasn't ended)
                    List<ClassSession> todaySessions = validSessions.stream()
                            .filter(s -> s.getSessionDate().toLocalDate().isEqual(now.toLocalDate()))
                            .collect(Collectors.toList());

                    ClassSession activeSessionToday = null;
                    boolean allTodayEnded = false;

                    if (!todaySessions.isEmpty()) {
                        activeSessionToday = todaySessions.stream()
                                .filter(s -> {
                                    if (s.getEndTime() == null || s.getEndTime().isBlank()) return true;
                                    try {
                                        String[] parts = s.getEndTime().split(":");
                                        int hour = Integer.parseInt(parts[0].trim());
                                        int minute = Integer.parseInt(parts[1].trim());
                                        java.time.LocalDateTime sessionEndTime = s.getSessionDate().toLocalDate().atTime(hour, minute);
                                        return now.isBefore(sessionEndTime);
                                    } catch (Exception e) { return true; }
                                })
                                .findFirst().orElse(null);
                        
                        if (activeSessionToday == null) {
                            allTodayEnded = true;
                        }
                    }

                    if (activeSessionToday != null && activeSessionToday.getMeetLink() != null && !activeSessionToday.getMeetLink().isBlank()) {
                        bestMeetLink = activeSessionToday.getMeetLink();
                        meetLabel = "Link học buổi hôm nay (Buổi " + activeSessionToday.getSessionNumber() + ")";
                    } else if (allTodayEnded) {
                        bestMeetLink = null;
                        meetLabel = "Các buổi học hôm nay đã kết thúc";
                    } else {
                        // 2. Try next upcoming session (future days)
                        ClassSession nextSession = validSessions.stream()
                                .filter(s -> s.getSessionDate().toLocalDate().isAfter(now.toLocalDate()))
                                .findFirst().orElse(null);
                        if (nextSession != null && nextSession.getMeetLink() != null && !nextSession.getMeetLink().isBlank()) {
                            bestMeetLink = nextSession.getMeetLink();
                            meetLabel = "Link học buổi tiếp theo (" + nextSession.getSessionDate().toLocalDate().toString() + ")";
                        }
                    }
                }
                courseInfo.setLiveMeetingLink(bestMeetLink);
                courseInfo.setLiveMeetingLabel(meetLabel);
            }

            List<CourseLearningInfoDTO.ModuleDTO> moduleDTOs = new ArrayList<>();
            int totalLessonsCount = 0;
            int completedCount = 0;

            if (course.getModules() != null) {
                List<Module> sortedModules = new ArrayList<>(course.getModules());
                sortedModules.sort(Comparator.comparing(m -> m.getOrderIndex() != null ? m.getOrderIndex() : m.getModuleId()));

                for (Module module : sortedModules) {
                    CourseLearningInfoDTO.ModuleDTO modDTO = new CourseLearningInfoDTO.ModuleDTO();
                    modDTO.setModuleId(module.getModuleId().longValue());
                    modDTO.setModuleTitle(module.getModuleName());
                    modDTO.setModuleName(module.getModuleName()); // 🟢 THÊM DÒNG NÀY: Đồng bộ DTO

                    List<CourseLearningInfoDTO.LessonDTO> lessonDTOs = new ArrayList<>();

                    if (module.getLessons() != null) {
                        List<Lesson> sortedLessons = new ArrayList<>(module.getLessons());
                        sortedLessons.sort(Comparator.comparing(l -> l.getOrderIndex() != null ? l.getOrderIndex() : l.getLessonId()));

                        for (Lesson lesson : sortedLessons) {
                            boolean isCompleted = completedLessonIds.contains(lesson.getLessonId());

                            CourseLearningInfoDTO.LessonDTO lessDTO = new CourseLearningInfoDTO.LessonDTO();
                            lessDTO.setLessonId(lesson.getLessonId().longValue());
                            lessDTO.setLessonTitle(lesson.getLessonName());
                            lessDTO.setLessonName(lesson.getLessonName());
                            lessDTO.setType(lesson.getType());
                            lessDTO.setDuration(lesson.getDuration());
                            lessDTO.setVideoUrl(lesson.getVideoUrl());
                            lessDTO.setVideoEmbedUrl(ExpertLessonResponseDTO.toEmbedUrl(lesson.getVideoUrl()));
                            lessDTO.setAllowDownload(lesson.getAllowDownload() != null ? lesson.getAllowDownload() : true);
                            lessDTO.setCompleted(isCompleted);
                            lessDTO.setLocked(false);

                            lessonDTOs.add(lessDTO);
                            totalLessonsCount++;
                            if (isCompleted) completedCount++;
                        }
                    }
                    modDTO.setLessons(lessonDTOs);
                    modDTO.setTotalLessons(lessonDTOs.size());

                    moduleDTOs.add(modDTO);
                }
            }

            courseInfo.setModules(moduleDTOs);

            int progress = totalLessonsCount == 0 ? 0 : Math.round(((float) completedCount / totalLessonsCount) * 100);
            courseInfo.setProgressPercent(progress);

            // Lấy tất cả quiz COURSE_QUIZ (Global + Class-specific)
            Integer targetClassId = (reg != null && reg.getClazz() != null) ? reg.getClazz().getClassId() : null;
            List<Quiz> studentQuizzes = quizRepository.findQuizzesForStudent(course.getCourseId(), targetClassId);

            // Optimization: Load all session-specific settings for this class to avoid N+1 query
            Map<Integer, SessionQuiz> sessionQuizMap = new HashMap<>();
            if (targetClassId != null) {
                List<SessionQuiz> sqList = sessionQuizRepository.findBySession_Clazz_ClassId(targetClassId);
                for (SessionQuiz sq : sqList) {
                    if (sq.getQuiz() != null) {
                        sessionQuizMap.put(sq.getQuiz().getQuizId(), sq);
                    }
                }
            }

            // Optimization: Load all attempts for this user in these quizzes to avoid N+1 query
            List<Integer> quizIds = studentQuizzes.stream().map(Quiz::getQuizId).collect(Collectors.toList());
            Map<Integer, Long> attemptCountMap = new HashMap<>();
            if (!quizIds.isEmpty()) {
                List<Object[]> attemptCounts = quizResultRepository.countAttemptsByUserPerQuiz(user.getUserId(), quizIds);
                for (Object[] row : attemptCounts) {
                    attemptCountMap.put((Integer) row[0], (Long) row[1]);
                }
            }

            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            List<CourseLearningInfoDTO.QuizInfoDTO> quizList = new ArrayList<>();
            for (Quiz quiz : studentQuizzes) {
                Long attemptCount = attemptCountMap.getOrDefault(quiz.getQuizId(), 0L);
                
                // Get session-specific setting or fallback
                SessionQuiz sq = sessionQuizMap.get(quiz.getQuizId());
                boolean isAssignment = "COURSE_ASSIGNMENT".equals(quiz.getQuizCategory());

                Boolean isOpen = false;
                String openAtStr = null;
                String closeAtStr = null;

                if (isAssignment) {
                    // COURSE_ASSIGNMENT -> giữ nguyên logic cũ
                    if (sq != null) {
                        isOpen = sq.getIsOpen() != null ? sq.getIsOpen() : false;
                        openAtStr = sq.getOpenAt() != null ? sq.getOpenAt().toString() : null;
                        closeAtStr = sq.getCloseAt() != null ? sq.getCloseAt().toString() : null;
                    } else {
                        // Assignment but not in session_quiz for this class -> Fallback to Closed
                        isOpen = false;
                        openAtStr = null;
                        closeAtStr = null;
                    }
                } else {
                    // COURSE_QUIZ (quiz nhỏ) -> trạng thái lấy từ SessionQuiz
                    if (sq != null) {
                        if (Boolean.TRUE.equals(sq.getIsOpen())) {
                            // Check thời gian (open_at, close_at)
                            boolean withinTime =
                                    (sq.getOpenAt() == null || now.isAfter(sq.getOpenAt())) &&
                                    (sq.getCloseAt() == null || now.isBefore(sq.getCloseAt()));
                            isOpen = withinTime;
                        } else {
                            isOpen = false;
                        }
                        openAtStr = sq.getOpenAt() != null ? sq.getOpenAt().toString() : null;
                        closeAtStr = sq.getCloseAt() != null ? sq.getCloseAt().toString() : null;
                    } else {
                        // Fallback cho COURSE_QUIZ chưa gán buổi (hoặc quiz toàn cục)
                        isOpen = quiz.getIsOpen() != null ? quiz.getIsOpen() : false;
                        if (isOpen) {
                            boolean withinTime =
                                    (quiz.getOpenAt() == null || now.isAfter(quiz.getOpenAt())) &&
                                    (quiz.getCloseAt() == null || now.isBefore(quiz.getCloseAt()));
                            isOpen = withinTime;
                        }
                        openAtStr = quiz.getOpenAt() != null ? quiz.getOpenAt().toString() : null;
                        closeAtStr = quiz.getCloseAt() != null ? quiz.getCloseAt().toString() : null;
                    }
                }

                quizList.add(CourseLearningInfoDTO.QuizInfoDTO.builder()
                        .quizId(quiz.getQuizId())
                        .title(quiz.getTitle())
                        .quizCategory(quiz.getQuizCategory())
                        .isAssignment(isAssignment)
                        .totalQuestions(quiz.getQuizQuestions() != null ? quiz.getQuizQuestions().size() : 0)
                        .timeLimitMinutes(quiz.getTimeLimitMinutes())
                        .maxAttempts(quiz.getMaxAttempts())
                        .attemptCount(attemptCount.intValue())
                        .isOpen(isOpen)
                        .openAt(openAtStr)
                        .closeAt(closeAtStr)
                        .deadline(quiz.getDeadline() != null ? quiz.getDeadline().toString() : null)
                        .build());
            }
            courseInfo.setQuizzes(quizList);

            return ResponseData.success("Thành công", courseInfo);

        } catch (Exception e) {
            return ResponseData.error(500, "Lỗi tải dữ liệu: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseData<Map<String, Object>> getLessonViewData(Integer lessonId, String email) {
        try {
            Lesson currentLesson = lessonRepository.findById(lessonId).orElse(null);
            if (currentLesson == null) return ResponseData.error(404, "Không tìm thấy bài học");

            Long courseId = currentLesson.getModule().getCourse().getCourseId().longValue();
            ResponseData<CourseLearningInfoDTO> courseInfoResult = getCourseLearningInfo(courseId, email);

            // Check if current lesson is actually completed by this user
            User currentUser = userRepository.findByEmail(email).orElse(null);
            boolean lessonCompleted = false;
            if (currentUser != null) {
                lessonCompleted = userLessonRepository
                        .existsByUser_UserIdAndLesson_LessonIdAndIsCompletedTrue(
                                currentUser.getUserId(), currentLesson.getLessonId());
            }

            LessonResponseDTO lessonDTO = LessonResponseDTO.builder()
                    .lessonId(currentLesson.getLessonId())
                    .lessonTitle(currentLesson.getLessonName())
                    .lessonName(currentLesson.getLessonName()) // 🟢 THÊM DÒNG NÀY: Đồng bộ DTO
                    .type(currentLesson.getType())
                    .duration(currentLesson.getDuration())
                    .videoUrl(currentLesson.getVideoUrl())
                    .videoEmbedUrl(ExpertLessonResponseDTO.toEmbedUrl(currentLesson.getVideoUrl()))
                    .contentText(currentLesson.getContent_text())
                    .allowDownload(currentLesson.getAllowDownload() != null ? currentLesson.getAllowDownload() : true)
                    .isCompleted(lessonCompleted)
                    .isLocked(false)
                    .build();

            Map<String, Object> data = new HashMap<>();
            data.put("currentLesson", lessonDTO);
            data.put("courseInfo", courseInfoResult.getData());

            return ResponseData.success("Thành công", data);
        } catch (Exception e) {
            return ResponseData.error(500, "Lỗi hệ thống: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ResponseData<Void> markLessonCompleted(Integer lessonId, String email) {
        try {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) return ResponseData.error(401, "Người dùng không tồn tại");

            Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
            if (lesson == null) return ResponseData.error(404, "Không tìm thấy bài học");

            UserLesson userLesson = userLessonRepository.findByUser_UserIdAndLesson_LessonId(user.getUserId(), lessonId).orElse(null);

            if (userLesson == null) {
                userLesson = new UserLesson();
                UserLesson.UserLessonId id = new UserLesson.UserLessonId(user.getUserId(), lessonId);
                userLesson.setId(id);
                userLesson.setUser(user);
                userLesson.setLesson(lesson);
            }

            userLesson.setStatus("Completed");
            userLessonRepository.save(userLesson);

            return ResponseData.success("Đã đánh dấu hoàn thành bài học");
        } catch (Exception e) {
            return ResponseData.error(500, "Lỗi cập nhật tiến độ: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getLessonIdToContinue(Integer courseId, String email) {
        try {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) return null;

            Course course = courseRepository.findById(courseId).orElse(null);
            if (course == null || course.getModules() == null || course.getModules().isEmpty()) return null;

            List<UserLesson> completedLessons = userLessonRepository.findByUser_UserId(user.getUserId());
            Set<Integer> completedIds = completedLessons.stream()
                    .filter(ul -> "Completed".equalsIgnoreCase(ul.getStatus()))
                    .map(ul -> ul.getLesson().getLessonId())
                    .collect(Collectors.toSet());

            List<Module> modules = new ArrayList<>(course.getModules());
            modules.sort(Comparator.comparing(m -> m.getOrderIndex() != null ? m.getOrderIndex() : m.getModuleId()));

            Integer firstLessonId = null;

            for (Module m : modules) {
                if (m.getLessons() != null && !m.getLessons().isEmpty()) {
                    List<Lesson> lessons = new ArrayList<>(m.getLessons());
                    lessons.sort(Comparator.comparing(l -> l.getOrderIndex() != null ? l.getOrderIndex() : l.getLessonId()));

                    for (Lesson l : lessons) {
                        if (firstLessonId == null) firstLessonId = l.getLessonId();
                        if (!completedIds.contains(l.getLessonId())) {
                            return l.getLessonId();
                        }
                    }
                }
            }

            return firstLessonId;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    @Transactional
    public ResponseData<Void> trackTime(String email, int seconds) {
        try {
            // Anti-cheating: Không cho phép ping quá 120s (do FE đang setup 30-90s ping 1 lần)
            if (seconds > 120 || seconds <= 0) {
                return ResponseData.error(400, "Dữ liệu thời gian không hợp lệ");
            }

            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) return ResponseData.error(401, "Người dùng không tồn tại");

            LocalDate today = LocalDate.now();

            // Sử dụng hàm findBy... chuẩn của Hibernate
            Optional<UserLearningLog> existingLogOpt = userLearningLogRepository.findByUser_UserIdAndLearnDate(user.getUserId(), today);

            if (existingLogOpt.isPresent()) {
                // Nếu hôm nay đã có log -> Lấy ra, cộng dồn và Lưu lại
                UserLearningLog log = existingLogOpt.get();
                log.setTimeSpentSeconds(log.getTimeSpentSeconds() + seconds);
                userLearningLogRepository.save(log);
            } else {
                // Nếu hôm nay chưa có log -> Tạo mới
                UserLearningLog newLog = UserLearningLog.builder()
                        .user(user)
                        .learnDate(today)
                        .timeSpentSeconds(seconds)
                        .build();
                userLearningLogRepository.save(newLog);
            }

            return ResponseData.success("Đã ghi nhận thời gian học");
        } catch (Exception e) {
            return ResponseData.error(500, "Lỗi tracking: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseData<ChartDataDTO> getDashboardChartData(String email, int days) {
        try {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) return ResponseData.error(401, "Người dùng không tồn tại");

            LocalDate today = LocalDate.now();
            LocalDate startDate = today.minusDays(days - 1); // e.g. if days=7, starts 6 days ago

            List<UserLearningLog> logs = userLearningLogRepository
                    .findByUser_UserIdAndLearnDateAfterOrderByLearnDate(user.getUserId(), startDate.minusDays(1));

            Map<LocalDate, Double> logMap = new HashMap<>();
            for (UserLearningLog log : logs) {
                double hours = (double) log.getTimeSpentSeconds() / 3600.0;
                logMap.put(log.getLearnDate(), Math.round(hours * 100.0) / 100.0);
            }

            List<String> labels = new ArrayList<>();
            List<Double> values = new ArrayList<>();

            for (int i = 0; i < days; i++) {
                LocalDate current = startDate.plusDays(i);
                if (i == days - 1) {
                    labels.add("Hôm nay");
                } else {
                    labels.add(String.format("%02d/%02d", current.getDayOfMonth(), current.getMonthValue()));
                }
                values.add(logMap.getOrDefault(current, 0.0));
            }

            ChartDataDTO dto = ChartDataDTO.builder()
                    .labels(labels)
                    .values(values)
                    .build();

            return ResponseData.success("Thành công", dto);
        } catch (Exception e) {
            return ResponseData.error(500, "Lỗi tải biểu đồ: " + e.getMessage());
        }
    }
}