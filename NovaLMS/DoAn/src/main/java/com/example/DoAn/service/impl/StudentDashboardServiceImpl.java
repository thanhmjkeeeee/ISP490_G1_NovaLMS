package com.example.DoAn.service.impl;

import com.example.DoAn.dto.response.*;
import com.example.DoAn.model.*;
import com.example.DoAn.repository.*;
import com.example.DoAn.service.StudentDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentDashboardServiceImpl implements StudentDashboardService {

    private final UserRepository userRepository;
    private final RegistrationRepository registrationRepository;
    private final LessonRepository lessonRepository;
    private final UserLessonRepository userLessonRepository;

    @Override
    @Transactional(readOnly = true)
    public StudentDashboardDTO getDashboardData(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Registration> registrations = registrationRepository.findByUserEmail(email);
        List<EnrolledCourseDTO> enrolledCourses = new ArrayList<>();

        for (Registration reg : registrations) {
            if (!"Approved".equals(reg.getStatus())) continue;

            Course course = reg.getCourse();
            Integer courseId = course.getCourseId();

            long totalLessons = lessonRepository.countByModuleCourse_CourseId(courseId);
            long completedLessons = userLessonRepository.countCompletedLessonsByUserIdAndCourseId(user.getUserId(), courseId);
            
            double progressPercent = totalLessons > 0 ? (completedLessons * 100.0 / totalLessons) : 0.0;
            String status = (totalLessons > 0 && progressPercent == 100.0) ? "COMPLETED" : "ACTIVE";

            Integer nextLessonId = null;
            String nextLessonTitle = null;

            if (progressPercent < 100.0) {
                List<Integer> uncompletedLessonIds = userLessonRepository.findUncompletedLessonIds(user.getUserId(), courseId);
                if (!uncompletedLessonIds.isEmpty()) {
                    nextLessonId = uncompletedLessonIds.get(0);
                    Lesson nextLesson = lessonRepository.findById(nextLessonId).orElse(null);
                    if (nextLesson != null) {
                        nextLessonTitle = nextLesson.getLessonName();
                    }
                }
            }

            LocalDate enrolledAt = reg.getRegistrationTime() != null ? reg.getRegistrationTime().toLocalDate() : null;

            String thumbnailUrl = course.getImageUrl();
            if (thumbnailUrl == null || thumbnailUrl.isBlank() || thumbnailUrl.contains("placeholder")) {
                thumbnailUrl = "/assets/img/default-course.png";
            }

            enrolledCourses.add(EnrolledCourseDTO.builder()
                    .courseId(courseId)
                    .title(course.getTitle())
                    .thumbnailUrl(thumbnailUrl)
                    .teacherName(reg.getClazz() != null && reg.getClazz().getTeacher() != null ? reg.getClazz().getTeacher().getFullName() : "N/A")
                    .className(reg.getClazz() != null ? reg.getClazz().getClassName() : "N/A")
                    .totalLessons((int) totalLessons)
                    .completedLessons((int) completedLessons)
                    .progressPercent(progressPercent)
                    .enrolledAt(enrolledAt)
                    .status(status)
                    .nextLessonId(nextLessonId)
                    .nextLessonTitle(nextLessonTitle)
                    .build());
        }

        enrolledCourses.sort((a, b) -> {
            boolean aProg = a.getProgressPercent() != null && a.getProgressPercent() > 0 && a.getProgressPercent() < 100;
            boolean bProg = b.getProgressPercent() != null && b.getProgressPercent() > 0 && b.getProgressPercent() < 100;
            if (aProg && !bProg) return -1;
            if (!aProg && bProg) return 1;
            
            boolean aComp = a.getProgressPercent() != null && a.getProgressPercent() == 100;
            boolean bComp = b.getProgressPercent() != null && b.getProgressPercent() == 100;
            if (aComp && !bComp) return 1; // move completed down
            if (!aComp && bComp) return -1;

            return 0;
        });

        return StudentDashboardDTO.builder()
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .roleName(user.getRole() != null ? user.getRole().getName() : "STUDENT")
                .enrolledCourses(enrolledCourses)
                .build();
    }
}
