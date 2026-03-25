package com.example.DoAn.service.impl;

import com.example.DoAn.dto.response.CourseLearningInfoDTO;
import com.example.DoAn.dto.response.ExpertLessonResponseDTO;
import com.example.DoAn.dto.response.LessonResponseDTO;
import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.model.Course;
import com.example.DoAn.model.Lesson;
import com.example.DoAn.model.Module;
import com.example.DoAn.model.Registration;
import com.example.DoAn.model.User;
import com.example.DoAn.model.UserLesson;
import com.example.DoAn.repository.CourseRepository;
import com.example.DoAn.repository.LessonRepository;
import com.example.DoAn.repository.QuizRepository;
import com.example.DoAn.repository.QuizResultRepository;
import com.example.DoAn.repository.RegistrationRepository;
import com.example.DoAn.repository.UserLessonRepository;
import com.example.DoAn.repository.UserRepository;
import com.example.DoAn.service.LearningService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                courseInfo.setClassName(reg.getClazz().getClassName());
                courseInfo.setSchedule(reg.getClazz().getSchedule());

                if (reg.getClazz().getTeacher() != null) {
                    courseInfo.setTeacherName(reg.getClazz().getTeacher().getFullName());
                    courseInfo.setTeacherAvatar(reg.getClazz().getTeacher().getAvatarUrl());
                }
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
                            lessDTO.setLessonName(lesson.getLessonName()); // 🟢 THÊM DÒNG NÀY: Đồng bộ DTO
                            lessDTO.setType(lesson.getType());
                            lessDTO.setDuration(lesson.getDuration());
                            lessDTO.setVideoUrl(lesson.getVideoUrl());     // 🟢 THÊM DÒNG NÀY: Bổ sung Link gốc
                            lessDTO.setVideoEmbedUrl(ExpertLessonResponseDTO.toEmbedUrl(lesson.getVideoUrl()));
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

            // Lấy quiz COURSE_QUIZ của khóa học
            quizRepository.findFirstByCourseCourseIdAndQuizCategoryAndStatus(
                    course.getCourseId(), "COURSE_QUIZ", "PUBLISHED"
            ).ifPresent(quiz -> {
                long attemptCount = quizResultRepository.countByQuizQuizIdAndUserUserId(quiz.getQuizId(), user.getUserId());
                courseInfo.setCourseQuiz(CourseLearningInfoDTO.QuizInfoDTO.builder()
                        .quizId(quiz.getQuizId())
                        .title(quiz.getTitle())
                        .totalQuestions(quiz.getQuizQuestions() != null ? quiz.getQuizQuestions().size() : 0)
                        .timeLimitMinutes(quiz.getTimeLimitMinutes())
                        .maxAttempts(quiz.getMaxAttempts())
                        .attemptCount((int) attemptCount)
                        .build());
            });

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

            LessonResponseDTO lessonDTO = LessonResponseDTO.builder()
                    .lessonId(currentLesson.getLessonId())
                    .lessonTitle(currentLesson.getLessonName())
                    .lessonName(currentLesson.getLessonName()) // 🟢 THÊM DÒNG NÀY: Đồng bộ DTO
                    .type(currentLesson.getType())
                    .duration(currentLesson.getDuration())
                    .videoUrl(currentLesson.getVideoUrl())
                    .videoEmbedUrl(ExpertLessonResponseDTO.toEmbedUrl(currentLesson.getVideoUrl()))
                    .contentText(currentLesson.getContent_text())
                    .quizId(currentLesson.getQuiz_id())
                    .isCompleted(false)
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
}