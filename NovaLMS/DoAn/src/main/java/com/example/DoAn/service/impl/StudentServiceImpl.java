package com.example.DoAn.service.impl;

import com.example.DoAn.dto.response.*;
import com.example.DoAn.dto.request.EnrollRequestDTO;
import com.example.DoAn.model.Clazz;
import com.example.DoAn.model.Course;
import com.example.DoAn.model.Registration;
import com.example.DoAn.model.User;
import com.example.DoAn.repository.ClassRepository;
import com.example.DoAn.repository.CourseRepository;
import com.example.DoAn.repository.RegistrationRepository;
import com.example.DoAn.repository.UserRepository;
import com.example.DoAn.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final RegistrationRepository registrationRepository;
    private final CourseRepository courseRepository;
    private final ClassRepository classRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public ResponseData<EnrollPageResponseDTO> getEnrollPageData(String email, Integer courseId) {
        try {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) return ResponseData.error(401, "Vui lòng đăng nhập.");

            Course course = courseRepository.findById(courseId).orElse(null);
            if (course == null) return ResponseData.error(404, "Không tìm thấy khóa học.");

            List<Clazz> openClasses = classRepository.findByCourse_CourseIdAndStatus(courseId, "Open");
            return ResponseData.success("Thành công", new EnrollPageResponseDTO(course, openClasses));
        } catch (Exception e) {
            return ResponseData.error(500, "Lỗi hệ thống: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ResponseData<Integer> enrollCourse(String email, EnrollRequestDTO request) {
        try {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) return ResponseData.error(401, "Vui lòng đăng nhập.");

            Clazz clazz = classRepository.findById(request.getClassId()).orElse(null);
            if (clazz == null) return ResponseData.error(404, "Không tìm thấy lớp học.");

            boolean exists = registrationRepository.existsByUser_UserIdAndClazz_ClassIdAndStatusNot(
                    user.getUserId(), request.getClassId(), "Cancelled");

            if (exists) return ResponseData.error(400, "Bạn đã đăng ký lớp này rồi!");

            // Tính giá: price - sale
            Course course = clazz.getCourse();
            Double originalPrice = course.getPrice() != null ? course.getPrice() : 0.0;
            Double saleAmount = course.getSale() != null ? course.getSale() : 0.0;
            Double finalPrice = originalPrice - saleAmount;
            if (finalPrice < 0) finalPrice = 0.0;

            Registration reg = Registration.builder()
                    .user(user)
                    .clazz(clazz)
                    .course(course)
                    .status("Submitted")
                    .registrationPrice(BigDecimal.valueOf(finalPrice))
                    .note("Đăng ký trực tuyến")
                    .build();

            registrationRepository.save(reg);
            return ResponseData.success("Đăng ký thành công!", clazz.getCourse().getCourseId());
        } catch (Exception e) {
            return ResponseData.error(500, "Lỗi hệ thống: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseData<List<RegistrationResponseDTO>> getMyEnrollments(String email) {
        try {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) return ResponseData.error(401, "Vui lòng đăng nhập.");

            List<Registration> list = registrationRepository.findByUser_UserIdOrderByRegistrationTimeDesc(user.getUserId());
            List<RegistrationResponseDTO> dtoList = list.stream().map(reg -> RegistrationResponseDTO.builder()
                    .registrationId(reg.getRegistrationId())
                    .courseName(reg.getCourse().getTitle())
                    .className(reg.getClazz() != null ? reg.getClazz().getClassName() : "N/A")
                    .status(reg.getStatus())
                    .registrationPrice(reg.getRegistrationPrice())
                    .note(reg.getNote())
                    .build()).collect(Collectors.toList());

            return ResponseData.success("Thành công", dtoList);
        } catch (Exception e) {
            return ResponseData.error(500, "Lỗi tải dữ liệu: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseData<PageResponse<MyCourseDTO>> getMyCourses(String email, String keyword, Integer categoryId, int page, int size, String sort) {
        try {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) return ResponseData.error(401, "Vui lòng đăng nhập.");

            String[] sortParams = sort.split("_");
            Sort.Direction direction = sortParams[1].equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));

            Page<Registration> regPage = registrationRepository.findMyCoursesWithFilters(user.getUserId(), keyword, categoryId, pageable);

            List<MyCourseDTO> dtoList = regPage.getContent().stream().map(reg -> MyCourseDTO.builder()
                    .courseId(reg.getCourse().getCourseId())
                    .title(reg.getCourse().getTitle())
                    .description(reg.getCourse().getDescription())
                    .imageUrl(reg.getCourse().getImageUrl())
                    .className(reg.getClazz() != null ? reg.getClazz().getClassName() : "N/A")
                    .build()).collect(Collectors.toList());

            PageResponse<MyCourseDTO> pageResponse = PageResponse.<MyCourseDTO>builder()
                    .items(dtoList)
                    .pageNo(regPage.getNumber())
                    .pageSize(regPage.getSize())
                    .totalElements(regPage.getTotalElements())
                    .totalPages(regPage.getTotalPages())
                    .last(regPage.isLast())
                    .build();

            return ResponseData.success("Thành công", pageResponse);
        } catch (Exception e) {
            return ResponseData.error(500, "Lỗi tải dữ liệu: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseData<DashboardResponseDTO> getDashboardData(String email) {
        try {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) return ResponseData.error(401, "Vui lòng đăng nhập.");

            DashboardResponseDTO dto = DashboardResponseDTO.builder()
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .avatarUrl(user.getAvatarUrl())
                    .roleName(user.getRole().getName())
                    .build();

            return ResponseData.success("Thành công", dto);
        } catch (Exception e) {
            return ResponseData.error(500, "Lỗi hệ thống: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseData<PageResponse<RegistrationResponseDTO>> getAllRegistrations(String keyword, String status, Integer courseId, int page, int size) {
        try {
            // Get all registrations
            List<Registration> allRegistrations = registrationRepository.findAllRegistrations();

            // Apply filters in memory
            List<Registration> filtered = allRegistrations.stream()
                    .filter(r -> (keyword == null || keyword.isEmpty() ||
                            (r.getCourse().getCourseName() != null && r.getCourse().getCourseName().toLowerCase().contains(keyword.toLowerCase())) ||
                            (r.getCourse().getTitle() != null && r.getCourse().getTitle().toLowerCase().contains(keyword.toLowerCase())) ||
                            (r.getUser().getFullName() != null && r.getUser().getFullName().toLowerCase().contains(keyword.toLowerCase())) ||
                            (r.getUser().getEmail() != null && r.getUser().getEmail().toLowerCase().contains(keyword.toLowerCase()))))
                    .filter(r -> (status == null || status.isEmpty() || r.getStatus().equalsIgnoreCase(status)))
                    .filter(r -> (courseId == null || r.getCourse().getCourseId().equals(courseId)))
                    .collect(Collectors.toList());

            // Manual pagination
            int start = page * size;
            int end = Math.min(start + size, filtered.size());
            List<Registration> pagedList = start < filtered.size() ? filtered.subList(start, end) : List.of();

            List<RegistrationResponseDTO> dtoList = pagedList.stream()
                    .map(r -> {
                        String courseName = r.getCourse().getCourseName();
                        if (courseName == null || courseName.isEmpty()) {
                            courseName = r.getCourse().getTitle();
                        }
                        return RegistrationResponseDTO.builder()
                                .registrationId(r.getRegistrationId())
                                .courseName(courseName)
                                .className(r.getClazz().getClassName())
                                .status(r.getStatus())
                                .registrationPrice(r.getRegistrationPrice())
                                .note(r.getNote())
                                .userName(r.getUser().getFullName())
                                .userEmail(r.getUser().getEmail())
                                .registrationTime(r.getRegistrationTime())
                                .build();
                    })
                    .collect(Collectors.toList());

            int totalPages = (int) Math.ceil((double) filtered.size() / size);

            PageResponse<RegistrationResponseDTO> pageResponse = PageResponse.<RegistrationResponseDTO>builder()
                    .items(dtoList)
                    .pageNo(page)
                    .pageSize(size)
                    .totalPages(totalPages)
                    .totalElements(filtered.size())
                    .last(page >= totalPages - 1)
                    .build();

            return ResponseData.success("Thành công", pageResponse);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseData.error(500, "Lỗi hệ thống: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ResponseData<Integer> updateRegistrationStatus(Integer registrationId, String status, String note) {
        try {
            Registration registration = registrationRepository.findById(registrationId)
                    .orElse(null);
            if (registration == null) {
                return ResponseData.error(404, "Không tìm thấy đăng ký!");
            }

            registration.setStatus(status);
            if (note != null) {
                registration.setNote(note);
            }
            registrationRepository.save(registration);

            return ResponseData.success("Cập nhật thành công!", registrationId);
        } catch (Exception e) {
            return ResponseData.error(500, "Lỗi hệ thống: " + e.getMessage());
        }
    }
}