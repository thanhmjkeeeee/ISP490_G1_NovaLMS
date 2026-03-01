package com.example.DoAn.service;

import com.example.DoAn.dto.CourseLearningInfoDTO;
import com.example.DoAn.model.*;
import com.example.DoAn.repository.*;
import com.example.DoAn.service.LearningService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearningServiceImpl implements LearningService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final RegistrationRepository registrationRepository;
    private final UserLessonRepository userLessonRepository;
    private final LessonRepository lessonRepository;

    @Override
    @Transactional(readOnly = true)
    public CourseLearningInfoDTO getCourseLearningInfo(Long courseId, String email) {
        // 1. Lấy thông tin User và Course từ DB
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng: " + email));

        Course course = courseRepository.findById(Math.toIntExact(courseId))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học ID: " + courseId));

        // 2. Tìm lớp học mà User đã đăng ký thành công (status = 'Approved') cho khóa này
        Registration registration = registrationRepository
                .findFirstByUserIdAndCourseIdAndStatus(user.getUserId(), Math.toIntExact(courseId), "Approved")
                .orElse(null);

        Clazz clazz = (registration != null) ? registration.getClazz() : null;

        // 3. Lấy thông tin Giảng viên (Ưu tiên giảng viên đứng lớp, nếu không lấy Expert của khóa)
        User instructor = (clazz != null && clazz.getTeacher() != null)
                ? clazz.getTeacher()
                : course.getExpert();

        // 4. Tính toán tiến độ học tập (%)
        // Tổng số bài học trong tất cả module của khóa học
        long totalLessons = course.getModules().stream()
                .mapToLong(m -> m.getLessons().size()).sum();

        // Số bài học user đã hoàn thành trong khóa học này
        long completedLessons = userLessonRepository.countCompletedLessonsByUserIdAndCourseId(user.getUserId(), Math.toIntExact(courseId));

        int progressPercent = (totalLessons == 0) ? 0 : (int) ((completedLessons * 100) / totalLessons);

        // 5. Map Modules và Lessons từ Entity sang DTO
        List<CourseLearningInfoDTO.ModuleDTO> moduleDTOs = course.getModules().stream()
                .sorted((m1, m2) -> Integer.compare(m1.getOrderIndex(), m2.getOrderIndex()))
                .map(m -> {
                    List<CourseLearningInfoDTO.LessonDTO> lessonDTOs = m.getLessons().stream()
                            .sorted((l1, l2) -> Integer.compare(l1.getOrderIndex(), l2.getOrderIndex()))
                            .map(l -> {
                                boolean isDone = userLessonRepository.existsByUserIdAndLessonIdAndStatus(user.getUserId(), l.getLessonId(), "Completed");

                                return CourseLearningInfoDTO.LessonDTO.builder()
                                        .lessonId((long) l.getLessonId())
                                        .lessonTitle(l.getLessonName())
                                        .type(l.getType()) // VIDEO, DOC, QUIZ
                                        .duration(String.valueOf(l.getDuration()))
                                        .build();
                            }).collect(Collectors.toList());

                    return CourseLearningInfoDTO.ModuleDTO.builder()
                            .moduleId((long) m.getModuleId())
                            .moduleTitle(m.getModuleName())
                            .totalLessons(lessonDTOs.size())
                            .lessons(lessonDTOs)
                            .build();
                }).collect(Collectors.toList());

        // 6. Trả về DTO hoàn chỉnh
        return CourseLearningInfoDTO.builder()
                .courseId((long) course.getCourseId())
                .title(course.getTitle())
                .description(course.getDescription())
                .progressPercent(progressPercent)
                .teacherName(instructor != null ? instructor.getFullName() : "Đang cập nhật")
                .teacherAvatar(instructor != null ? instructor.getAvatarUrl() : "/assets/img/person/person-1.jpg")
                .className(clazz != null ? clazz.getClassName() : null)
                .schedule(clazz != null ? clazz.getSchedule() : "Chưa có lịch cụ thể")
                // Bảng class trong SQL chưa có cột meeting_link, tạm thời để null
                .liveMeetingLink(null)
                .modules(moduleDTOs)
                .build();
    }

    @Override
    public Integer getLessonIdToContinue(Integer courseId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Integer> uncompletedIds = userLessonRepository.findUncompletedLessonIds(user.getUserId(), courseId);

        if (uncompletedIds != null && !uncompletedIds.isEmpty()) {
            return uncompletedIds.get(0);
        }

        List<Integer> allIds = userLessonRepository.findAllLessonIdsOfCourse(courseId);

        if (allIds != null && !allIds.isEmpty()) {
            return allIds.get(0);
        }

        return null;
    }

    @Override
    public Lesson getLessonEntity(Integer lessonId) {
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));
    }
}