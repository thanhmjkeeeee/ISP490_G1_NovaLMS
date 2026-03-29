package com.example.DoAn.service.impl;

import com.example.DoAn.dto.response.*;
import com.example.DoAn.dto.request.EnrollRequestDTO;
import com.example.DoAn.model.Clazz;
import com.example.DoAn.model.Course;
import com.example.DoAn.model.Payment;
import com.example.DoAn.model.Registration;
import com.example.DoAn.model.User;
import com.example.DoAn.repository.ClassRepository;
import com.example.DoAn.repository.CourseRepository;
import com.example.DoAn.repository.PaymentRepository;
import com.example.DoAn.repository.RegistrationRepository;
import com.example.DoAn.repository.UserRepository;
import com.example.DoAn.service.PayosService;
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
    private final PaymentRepository paymentRepository;
    private final PayosService payosService;

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

            // Status: Submitted — chờ thanh toán PayOS (hoặc Approved nếu free)
            Registration reg = Registration.builder()
                    .user(user)
                    .clazz(clazz)
                    .course(course)
                    .status("Submitted")
                    .registrationPrice(BigDecimal.valueOf(finalPrice))
                    .note(request.getNote() != null && !request.getNote().trim().isEmpty() ? request.getNote() : "Đăng ký trực tuyến, chờ thanh toán PayOS")                    .build();

            registrationRepository.save(reg);
            return ResponseData.success("Đăng ký thành công! Vui lòng hoàn tất thanh toán.", reg.getRegistrationId());
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

            // ⚡ Sync trạng thái PayOS cho các Payment còn PENDING
            // PayOS không gửi webhook cho CANCELLED/EXPIRED → phải chủ động hỏi
            for (Registration reg : list) {
                paymentRepository.findFirstByRegistrationIdOrderByCreatedAtDesc(reg.getRegistrationId())
                        .ifPresent(payment -> {
                            if ("PENDING".equals(payment.getStatus()) || "FAILED".equals(payment.getStatus())) {
                                payosService.syncPaymentStatus(payment.getPayosOrderCode());
                            }
                        });
            }

            // Reload after sync
            list = registrationRepository.findByUser_UserIdOrderByRegistrationTimeDesc(user.getUserId());

            List<RegistrationResponseDTO> dtoList = list.stream().map(reg ->
                RegistrationResponseDTO.builder()
                    .registrationId(reg.getRegistrationId())
                    .courseName(reg.getCourse().getCourseName())
                    .className(reg.getClazz() != null ? reg.getClazz().getClassName() : "N/A")
                    .status(reg.getStatus())
                    .registrationPrice(reg.getRegistrationPrice())
                    .note(reg.getNote())
                    .paymentStatus(
                        paymentRepository.findFirstByRegistrationIdOrderByCreatedAtDesc(reg.getRegistrationId())
                                .map(Payment::getStatus).orElse(null)
                    )
                    .build()
            ).collect(Collectors.toList());

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
                    .title(reg.getCourse().getCourseName())
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
                    .filter(r -> {
                        if (keyword == null || keyword.isEmpty()) return true;
                        String k = keyword.toLowerCase();
                        if (r.getCourse() != null && r.getCourse().getCourseName() != null && r.getCourse().getCourseName().toLowerCase().contains(k)) return true;
                        if (r.getUser() != null && r.getUser().getFullName() != null && r.getUser().getFullName().toLowerCase().contains(k)) return true;
                        if (r.getUser() != null && r.getUser().getEmail() != null && r.getUser().getEmail().toLowerCase().contains(k)) return true;
                        return false;
                    })
                    .filter(r -> (status == null || status.isEmpty() || r.getStatus().equalsIgnoreCase(status)))
                    .filter(r -> (courseId == null || r.getCourse() == null || r.getCourse().getCourseId().equals(courseId)))
                    .collect(Collectors.toList());

            // Manual pagination
            int start = page * size;
            int end = Math.min(start + size, filtered.size());
            List<Registration> pagedList = start < filtered.size() ? filtered.subList(start, end) : List.of();

            List<RegistrationResponseDTO> dtoList = pagedList.stream()
                    .map(r -> {
                        String courseName = r.getCourse() != null ? r.getCourse().getCourseName() : "N/A";
                        String className = r.getClazz() != null ? r.getClazz().getClassName() : "N/A";
                        String userName = r.getUser() != null ? r.getUser().getFullName() : "N/A";
                        String userEmail = r.getUser() != null ? r.getUser().getEmail() : "N/A";

                        return RegistrationResponseDTO.builder()
                                .registrationId(r.getRegistrationId())
                                .courseName(courseName)
                                .className(className)
                                .status(r.getStatus())
                                .registrationPrice(r.getRegistrationPrice())
                                .note(r.getNote())
                                .userName(userName)
                                .userEmail(userEmail)
                                .registrationTime(r.getRegistrationTime())
                                .paymentStatus(
                                    paymentRepository.findFirstByRegistrationIdOrderByCreatedAtDesc(r.getRegistrationId())
                                            .map(Payment::getStatus).orElse(null)
                                )
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

            // Khóa học có phí → chỉ duyệt được khi Payment.status = PAID
            boolean isPaidCourse = registration.getRegistrationPrice() != null
                    && registration.getRegistrationPrice().doubleValue() > 0;

            if (isPaidCourse && "Approved".equals(status)) {
                Payment payment = paymentRepository.findFirstByRegistrationIdOrderByCreatedAtDesc(registrationId).orElse(null);
                if (payment == null || !"PAID".equals(payment.getStatus())) {
                    String currentPayStatus = payment != null ? payment.getStatus() : "chưa có";
                    return ResponseData.error(403,
                            "Khóa học có phí — chỉ duyệt được khi đã thanh toán qua PayOS. "
                            + "Trạng thái thanh toán hiện tại: " + currentPayStatus);
                }
            }

            // Không cho phép hủy / từ chối khi đã thanh toán thành công
            if (("Cancelled".equals(status) || "Rejected".equals(status)) && isPaidCourse) {
                Payment payment = paymentRepository.findFirstByRegistrationIdOrderByCreatedAtDesc(registrationId).orElse(null);
                if (payment != null && "PAID".equals(payment.getStatus())) {
                    return ResponseData.error(403,
                            "Không thể hủy hoặc từ chối đăng ký đã thanh toán thành công!");
                }
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

    @Override
    @Transactional(readOnly = true)
    public ResponseData<Boolean> checkFirstTime(String email) {
        try {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) return ResponseData.error(401, "Unauthorized");
            List<Registration> registrations = registrationRepository.findByUserEmail(email);

            // get list null
            boolean isFirstTime = registrations.isEmpty();

            return ResponseData.success("Kiểm tra thành công", isFirstTime);
        } catch (Exception e) {
            return ResponseData.error(500, "Lỗi: " + e.getMessage());
        }
    }
}
