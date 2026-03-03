package com.example.DoAn.service;

import com.example.DoAn.dto.EnrollPageDTO;
import com.example.DoAn.dto.MyCourseDTO;
import com.example.DoAn.dto.ServiceResult;
import com.example.DoAn.model.Clazz;
import com.example.DoAn.model.Course;
import com.example.DoAn.model.Registration;
import com.example.DoAn.model.User;
import com.example.DoAn.repository.ClassRepository;
import com.example.DoAn.repository.CourseRepository;
import com.example.DoAn.repository.RegistrationRepository;
import com.example.DoAn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final RegistrationRepository registrationRepository;
    private final CourseRepository courseRepository;
    private final ClassRepository classRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public ServiceResult<EnrollPageDTO> getEnrollPageData(String email, Integer courseId) {
        try {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                return ServiceResult.failure("Vui lòng đăng nhập.");
            }

            Course course = courseRepository.findById(courseId).orElse(null);
            if (course == null) {
                return ServiceResult.failure("Không tìm thấy khóa học.");
            }

            List<Clazz> openClasses = classRepository.findByCourse_CourseIdAndStatus(courseId, "Open");

            EnrollPageDTO dto = new EnrollPageDTO(course, openClasses);
            return ServiceResult.success("Thành công", dto);
        } catch (Exception e) {
            return ServiceResult.failure("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @Transactional
    public ServiceResult<Integer> enrollCourse(String email, Integer classId) {
        try {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                return ServiceResult.failure("Vui lòng đăng nhập.");
            }

            Clazz clazz = classRepository.findById(classId).orElse(null);
            if (clazz == null) {
                return ServiceResult.failure("Không tìm thấy lớp học.");
            }

            Integer courseId = clazz.getCourse().getCourseId();

            boolean exists = registrationRepository.existsByUser_UserIdAndClazz_ClassIdAndStatusNot(
                    user.getUserId(), classId, "Cancelled");

            if (exists) {
                return ServiceResult.failure("Bạn đã đăng ký lớp này rồi!");
            }

            Registration reg = Registration.builder()
                    .user(user)
                    .clazz(clazz)
                    .course(clazz.getCourse())
                    .status("Submitted")
                    .registrationPrice(new BigDecimal("5000000"))
                    .note("Đăng ký trực tuyến")
                    .build();

            registrationRepository.save(reg);

            return ServiceResult.success("Đăng ký thành công! Vui lòng chờ duyệt hoặc thanh toán.", courseId);
        } catch (Exception e) {
            return ServiceResult.failure("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public ServiceResult<List<Registration>> getMyEnrollments(String email) {
        try {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                return ServiceResult.failure("Vui lòng đăng nhập.");
            }

            List<Registration> list = registrationRepository.findByUser_UserIdOrderByRegistrationTimeDesc(user.getUserId());
            return ServiceResult.success("Thành công", list);
        } catch (Exception e) {
            return ServiceResult.failure("Lỗi tải dữ liệu: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public ServiceResult<Page<MyCourseDTO>> getMyCourses(String email, String keyword, Integer categoryId, Pageable pageable) {
        try {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                return ServiceResult.failure("Vui lòng đăng nhập.");
            }

            Page<Registration> regPage = registrationRepository.findMyCoursesWithFilters(user.getUserId(), keyword, categoryId, pageable);

            List<MyCourseDTO> dtoList = regPage.getContent().stream().map(reg ->
                    MyCourseDTO.builder()
                            .courseId(reg.getCourse().getCourseId())
                            .title(reg.getCourse().getTitle())
                            .description(reg.getCourse().getDescription())
                            .imageUrl(reg.getCourse().getImageUrl())
                            .className(reg.getClazz() != null ? reg.getClazz().getClassName() : "N/A")
                            .build()
            ).collect(Collectors.toList());

            Page<MyCourseDTO> page = new PageImpl<>(dtoList, pageable, regPage.getTotalElements());
            return ServiceResult.success("Thành công", page);
        } catch (Exception e) {
            return ServiceResult.failure("Lỗi tải dữ liệu: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public ServiceResult<User> getDashboardData(String email) {
        try {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                return ServiceResult.failure("Vui lòng đăng nhập.");
            }
            return ServiceResult.success("Thành công", user);
        } catch (Exception e) {
            return ServiceResult.failure("Lỗi hệ thống: " + e.getMessage());
        }
    }
}