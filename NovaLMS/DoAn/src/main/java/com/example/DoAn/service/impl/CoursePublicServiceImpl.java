package com.example.DoAn.service.impl;

import com.example.DoAn.dto.response.CoursePublicResponseDTO;
import com.example.DoAn.dto.response.PageResponse;
import com.example.DoAn.model.Course;
import com.example.DoAn.repository.*;
import com.example.DoAn.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class CoursePublicServiceImpl implements CourseService {

    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private ClassRepository classRepository;
    @Autowired
    private RegistrationRepository registrationRepository;
    @Autowired
    private ModuleRepository moduleRepository;

    @Override
    public List<CoursePublicResponseDTO> getCoursesByFilter(Integer categoryId) {
        List<Course> courses = Optional.ofNullable(categoryId)
                .map(id -> courseRepository.findByCategory_SettingIdAndStatus(id, "Published"))
                .orElseGet(() -> courseRepository.findByStatus("Published"));
        return courses.stream().map(this::mapToPublicDTO).toList();
    }

    @Override
    public Optional<CoursePublicResponseDTO> getCourseDetail(Integer id) {
        return courseRepository.findById(id).map(this::mapToPublicDTO);
    }

    @Override
    public PageResponse<CoursePublicResponseDTO> searchAndFilterCourses(String keyword, Integer categoryId,
            String sortBy, int page, int size) {
        String searchKey = (keyword != null && !keyword.trim().isEmpty()) ? keyword : null;
        Sort sort = switch (Optional.ofNullable(sortBy).orElse("newest")) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            default -> Sort.by(Sort.Direction.DESC, "courseId");
        };

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Course> coursePage = courseRepository.searchCourses(searchKey, categoryId, "Published", pageable);

        List<CoursePublicResponseDTO> list = coursePage.getContent().stream()
                .map(this::mapToPublicDTO)
                .toList();

        return PageResponse.<CoursePublicResponseDTO>builder()
                .pageNo(page)
                .pageSize(size)
                .totalPages(coursePage.getTotalPages())
                .items(list)
                .build();
    }

    @Override
    public CoursePublicResponseDTO mapToSummaryDTO(Course course) {
        Integer id = course.getCourseId();

        // Map Expert (Giảng viên chính)
        CoursePublicResponseDTO.ExpertResponseDTO expertDTO = null;
        if (course.getExpert() != null) {
            expertDTO = new CoursePublicResponseDTO.ExpertResponseDTO(
                    course.getExpert().getFullName(),
                    course.getExpert().getAvatarUrl() != null ? course.getExpert().getAvatarUrl()
                            : "/assets/img/default-avatar.png");
        }

        // Dữ liệu cơ bản
        String categoryName = (course.getCategory() != null) ? course.getCategory().getName() : "N/A";
        long studentCount = registrationRepository.countByCourse_CourseIdAndStatus(id, "Approved");

        String imgUrl = "/assets/img/default-course.png";
        if (course.getAvatar() != null && !course.getAvatar().isBlank()
                && !course.getAvatar().contains("placeholder")) {
            imgUrl = course.getAvatar();
        }

        return new CoursePublicResponseDTO(
                id,
                course.getCourseName(),
                course.getDescription(),
                course.getPrice(),
                course.getSale(),
                categoryName,
                studentCount,
                imgUrl,
                course.getLevelTag(),
                course.getStatus(),
                expertDTO,
                List.of(), // No curriculum
                List.of() // No classes
        );
    }

    @Override
    public CoursePublicResponseDTO mapToPublicDTO(Course course) {
        Integer id = course.getCourseId();

        // 1. Map Curriculum (Modules + Lessons)
        // Lấy danh sách module từ DB và map sang ModuleResponseDTO kèm danh sách Lesson
        var curriculum = moduleRepository.findByCourse_CourseIdOrderByOrderIndexAsc(id)
                .stream()
                .map(m -> new CoursePublicResponseDTO.ModuleResponseDTO(
                        m.getModuleId(),
                        m.getModuleName(),
                        m.getOrderIndex(),
                        // Map bài học bên trong mỗi module
                        // Sửa lại đoạn mapping bài học bên trong mỗi module
                        m.getLessons() != null ? m.getLessons().stream()
                                .map(lesson -> new CoursePublicResponseDTO.LessonResponseDTO(
                                        lesson.getLessonId(), // Kiểm tra xem trong Lesson.java có phải lessonId không?
                                        lesson.getLessonName(), // Kiểm tra xem trong Lesson.java có phải lessonName
                                                                // không?
                                        lesson.getDuration() // Kiểm tra xem trong Lesson.java có trường duration không?
                                )).toList() : List.of()))
                .toList();

        // 2. Map Active Classes (Bổ sung đầy đủ thông tin: Giảng viên, Lịch học, Ngày
        // tháng + Lọc thời gian)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDateMin = now.minusDays(7);
        LocalDateTime startDateMax = now.plusDays(90); // Relaxed from 7 to 90

        var classes = classRepository
                .findByCourse_CourseIdAndStatusAndStartDateBetween(id, "Open", startDateMin, startDateMax)
                .stream()
                .map(c -> new CoursePublicResponseDTO.ClassResponseDTO(
                        c.getClassId(),
                        c.getClassName(),
                        c.getStatus(),
                        c.getTeacher() != null ? c.getTeacher().getFullName() : "Chưa có GV",
                        c.getSchedule() != null ? c.getSchedule() : "TBD",
                        c.getSlotTime() != null ? c.getSlotTime() : "N/A",
                        c.getStartDate(),
                        c.getEndDate()))
                .toList();

        // 3. Map Expert (Giảng viên chính của khóa học)
        CoursePublicResponseDTO.ExpertResponseDTO expertDTO = null;
        if (course.getExpert() != null) {
            expertDTO = new CoursePublicResponseDTO.ExpertResponseDTO(
                    course.getExpert().getFullName(),
                    course.getExpert().getAvatarUrl() != null ? course.getExpert().getAvatarUrl()
                            : "/assets/img/default-avatar.png");
        }

        // 4. Chuẩn bị dữ liệu cơ bản
        String categoryName = (course.getCategory() != null) ? course.getCategory().getName() : "N/A";
        long studentCount = registrationRepository.countByCourse_CourseIdAndStatus(id, "Approved");
        String rawAvatar = course.getAvatar();
        String rawImageUrl = course.getImageUrl();

        String imgUrl = "/assets/img/default-course.png";
        if (rawAvatar != null && !rawAvatar.isBlank() && !rawAvatar.contains("placeholder")) {
            imgUrl = rawAvatar;
        } else if (rawImageUrl != null && !rawImageUrl.isBlank() && !rawImageUrl.contains("placeholder")) {
            imgUrl = rawImageUrl;
        }

        return new CoursePublicResponseDTO(
                id,
                course.getCourseName(),
                course.getDescription(),
                course.getPrice(),
                course.getSale(),
                categoryName,
                studentCount,
                imgUrl,
                course.getLevelTag(),
                course.getStatus(),
                expertDTO,
                curriculum,
                classes);
    }
}