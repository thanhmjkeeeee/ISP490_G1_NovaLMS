package com.example.DoAn.service.impl;

import com.example.DoAn.dto.request.CourseRequestDTO;
import com.example.DoAn.dto.response.CourseDetailResponse;
import com.example.DoAn.dto.response.PageResponse;
import com.example.DoAn.model.Course;
import com.example.DoAn.model.Setting;
import com.example.DoAn.model.User;
import com.example.DoAn.repository.*;
import com.example.DoAn.service.ICourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("courseService")
@Slf4j
@RequiredArgsConstructor
public class CourseServiceImpl implements ICourseService {

    private final CourseRepository courseRepository;
    private final RegistrationRepository registrationRepository;
    private final SettingRepository settingRepository;
    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final QuizRepository quizRepository;
    private final ClazzRepository clazzRepository;
    private final ModuleRepository moduleRepository;

    @Override
    public Integer saveCourse(CourseRequestDTO request) {
        Setting category = null;
        if (request.getCategoryId() != null) {
            category = settingRepository.findById(request.getCategoryId()).orElse(null);
        }
        User expert = null;
        if (request.getExpertId() != null) {
            expert = userRepository.findById(request.getExpertId()).orElse(null);
        }
        User teacher = null;
        if (request.getTeacherId() != null) {
            teacher = userRepository.findById(request.getTeacherId()).orElse(null);
        }

        Course course = Course.builder()
                .title(request.getCourseName())
                .courseName(request.getCourseName())
                .description(request.getDescription())
                .price(request.getPrice())
                .sale(request.getSale())
                .numberOfSessions(request.getNumberOfSessions())
                .avatar(request.getAvatar())
                .status(request.getStatus() != null ? request.getStatus() : "Published")
                .category(category)
                .expert(expert)
                .teacher(teacher)
                .isSelfStudy(request.getIsSelfStudy() != null ? request.getIsSelfStudy() : false)
                .build();

        courseRepository.save(course);
        log.info("Course added successfully, courseId={}", course.getCourseId());
        return course.getCourseId();
    }

    @Override
    public void updateCourse(Integer id, CourseRequestDTO request) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        course.setTitle(request.getCourseName());
        course.setCourseName(request.getCourseName());
        course.setDescription(request.getDescription());
        course.setPrice(request.getPrice());
        course.setSale(request.getSale());
        course.setNumberOfSessions(request.getNumberOfSessions());
        course.setAvatar(request.getAvatar());
        
        String newStatus = request.getStatus();
        if (newStatus == null) newStatus = course.getStatus();
        if ("draft".equalsIgnoreCase(newStatus)) {
            long classCount = clazzRepository.countByCourse_CourseId(id);
            if (classCount > 0) {
                newStatus = "Inactive";
            }
        }
        course.setStatus(newStatus);
        if (request.getIsSelfStudy() != null) {
            course.setIsSelfStudy(request.getIsSelfStudy());
        }

        // Update category if provided
        if (request.getCategoryId() != null) {
            Setting category = settingRepository.findById(request.getCategoryId()).orElse(null);
            course.setCategory(category);
        }

        // Update expert if provided
        if (request.getExpertId() != null) {
            User expert = userRepository.findById(request.getExpertId()).orElse(null);
            course.setExpert(expert);
        }

        // Update teacher if provided
        if (request.getTeacherId() != null) {
            User teacher = userRepository.findById(request.getTeacherId()).orElse(null);
            course.setTeacher(teacher);
        }

        courseRepository.save(course);
        log.info("Course updated successfully, courseId={}", id);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public CourseDetailResponse getById(Integer id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        return mapToResponse(course);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public PageResponse<?> getAllCourses(int pageNo, int pageSize) {
        Page<Course> page = courseRepository.findAll(PageRequest.of(pageNo, pageSize));

        List<CourseDetailResponse> list = page.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return PageResponse.<CourseDetailResponse>builder()
                .pageNo(pageNo)
                .pageSize(pageSize)
                .totalPages(page.getTotalPages())
                .items(list)
                .build();
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void deleteCourse(Integer id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học"));

        // Check if published
        if ("Published".equalsIgnoreCase(course.getStatus())) {
            throw new RuntimeException("Không thể xóa khóa học ở trạng thái 'Xuất bản'. Vui lòng chuyển sang 'Nháp' trước khi xóa.");
        }

        // Check for registrations
        long registrationCount = registrationRepository.countByCourse_CourseId(id);
        if (registrationCount > 0) {
            throw new RuntimeException("Không thể xóa khóa học đã có học viên đăng ký (" + registrationCount + ")");
        }

        // Check for classes
        long classCount = clazzRepository.countByCourse_CourseId(id);
        if (classCount > 0) {
            throw new RuntimeException("Không thể xóa khóa học đang có lớp học (" + classCount + ")");
        }

        // Check for quizzes
        long quizCount = quizRepository.countByCourse_CourseId(id);
        if (quizCount > 0) {
            throw new RuntimeException("Không thể xóa khóa học đang có bài kiểm tra/assignment (" + quizCount + ")");
        }

        // Check for modules
        long moduleCount = moduleRepository.countByCourse_CourseId(id);
        if (moduleCount > 0) {
            throw new RuntimeException("Không thể xóa khóa học đang có module/chương trình học (" + moduleCount + ")");
        }

        courseRepository.delete(course);
        log.info("Course deleted successfully, id={}", id);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public PageResponse<?> getAllCoursesWithFilter(int pageNo, int pageSize, String search, String status, Boolean isSelfStudy) {
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("courseId").descending());
        Specification<Course> spec = Specification.where(null);

        if (search != null && !search.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.or(
                            cb.like(root.get("courseName"), "%" + search + "%"),
                            cb.like(root.get("title"), "%" + search + "%")
                    )
            );
        }

        if (status != null && !status.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        if (isSelfStudy != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("isSelfStudy"), isSelfStudy));
        }

        Page<Course> page = courseRepository.findAll(spec, pageable);

        List<CourseDetailResponse> list = page.getContent().stream()
                .map(this::mapToResponse) // Sử dụng hàm map đã viết ở các bước trước
                .toList();

        return PageResponse.<CourseDetailResponse>builder()
                .pageNo(pageNo)
                .pageSize(pageSize)
                .totalPages(page.getTotalPages())
                .items(list)
                .build();
    }

    private CourseDetailResponse mapToResponse(Course course) {
        return CourseDetailResponse.builder()
                .courseId(course.getCourseId())
                .courseCode(course.getCourseName())
                .courseName(course.getCourseName())
                .description(course.getDescription())
                .price(course.getPrice())
                .sale(course.getSale())
                .avatar(course.getAvatar())
                .status(course.getStatus())
                .categoryId(course.getCategory() != null ? course.getCategory().getSettingId() : null)
                .categoryName(course.getCategory() != null ? course.getCategory().getName() : null)
                .expertId(course.getExpert() != null ? course.getExpert().getUserId() : null)
                .expertName(course.getExpert() != null ? course.getExpert().getFullName() : null)
                .teacherId(course.getTeacher() != null ? course.getTeacher().getUserId() : null)
                .teacherName(course.getTeacher() != null ? course.getTeacher().getFullName() : null)
                .registrationCount(registrationRepository.countByCourse_CourseId(course.getCourseId()))
                .numberOfSessions(course.getNumberOfSessions())
                .classCount(course.getClasses() != null ? (long) course.getClasses().size() : 0L)
                .teacherNames(course.getClasses() != null 
                        ? course.getClasses().stream()
                            .filter(c -> c.getTeacher() != null)
                            .map(c -> c.getTeacher().getFullName())
                            .distinct()
                            .toList()
                        : java.util.Collections.emptyList())
                .isSelfStudy(course.getIsSelfStudy())
                .build();
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void updateCourseStatus(Integer id, String status) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        String newStatus = status;
        if ("draft".equalsIgnoreCase(newStatus)) {
            long classCount = clazzRepository.countByCourse_CourseId(id);
            if (classCount > 0) {
                newStatus = "Inactive";
            }
        }
        course.setStatus(newStatus);
        courseRepository.save(course);
        log.info("Course status updated to {} for id={}", status, id);
    }

    @Override
    public long getLessonCount(Integer courseId) {
        return lessonRepository.countByModuleCourse_CourseId(courseId);
    }
}