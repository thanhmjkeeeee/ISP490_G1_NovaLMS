package com.example.DoAn.service.impl;

import com.example.DoAn.dto.response.*;
import com.example.DoAn.dto.request.EnrollRequestDTO;
import com.example.DoAn.model.*;
import com.example.DoAn.repository.*;
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
import java.time.LocalDate;
import java.util.ArrayList;
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
    private final UserLessonRepository userLessonRepository;
    private final SessionLessonRepository sessionLessonRepository;

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

            String searchKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;

            Page<Registration> regPage = registrationRepository.findMyCoursesWithFilters(
                    user.getUserId(),
                    searchKeyword,
                    categoryId,
                    pageable
            );

            List<MyCourseDTO> dtoList = regPage.getContent().stream().map(reg -> MyCourseDTO.builder()
                    .courseId(reg.getCourse().getCourseId())
                    .title(reg.getCourse().getCourseName() != null ? reg.getCourse().getCourseName() : reg.getCourse().getTitle())
                    .description(reg.getCourse().getDescription())
                    .imageUrl(reg.getCourse().getImageUrl())
                    .className(reg.getClazz() != null ? reg.getClazz().getClassName() : "Chưa xếp lớp")
                    .build()).collect(Collectors.toList());

            PageResponse<MyCourseDTO> pageResponse = PageResponse.<MyCourseDTO>builder()
                    .items(dtoList)
                    .pageNo(regPage.getNumber())
                    .pageSize(regPage.getSize())
                    .totalElements((int) regPage.getTotalElements())
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
                        String categoryName = (r.getCourse() != null && r.getCourse().getCategory() != null)
                                ? r.getCourse().getCategory().getName()
                                : "N/A";
                        String className = r.getClazz() != null ? r.getClazz().getClassName() : "N/A";
                        String userName = r.getUser() != null ? r.getUser().getFullName() : "N/A";
                        String userEmail = r.getUser() != null ? r.getUser().getEmail() : "N/A";

                        return RegistrationResponseDTO.builder()
                                .registrationId(r.getRegistrationId())
                                .courseName(courseName)
                                .categoryName(categoryName)
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
            boolean isPaidCourse = isPositivePrice(registration.getRegistrationPrice());

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
    private boolean isPositivePrice(Object price) {
        if (price == null) return false;
        if (price instanceof BigDecimal) return ((BigDecimal) price).compareTo(BigDecimal.ZERO) > 0;
        if (price instanceof Double) return (Double) price > 0;
        if (price instanceof Number) return ((Number) price).doubleValue() > 0;
        return false;
    }

    public StudentClassDetailResponse getStudentClassDetail(Integer classId, Integer userId) {
        // 1. Check phân quyền thật
            boolean isEnrolled = registrationRepository.existsByClazz_ClassIdAndUser_UserIdAndStatus(classId, userId, "Approved");
        if (!isEnrolled) {
            throw new RuntimeException("Bạn không có quyền truy cập hoặc chưa thanh toán khóa học này!");
        }

        Clazz clazz = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học"));

        // 2. Gom danh sách Members thật
        List<MemberDTO> members = new ArrayList<>();
        if (clazz.getTeacher() != null) {
            members.add(MemberDTO.builder()
                    .name(clazz.getTeacher().getFullName())
                    .email(clazz.getTeacher().getEmail())
                    .avatar(clazz.getTeacher().getAvatarUrl())
                    .role("TEACHER")
                    .build());
        }

        List<Registration> activeStudents = registrationRepository.findByClazz_ClassIdAndStatus(classId, "Approved");
        for (Registration reg : activeStudents) {
            members.add(MemberDTO.builder()
                    .name(reg.getUser().getFullName())
                    .email(reg.getUser().getEmail())
                    .avatar(reg.getUser().getAvatarUrl())
                    .role("STUDENT")
                    .build());
        }

        // 3. Tối ưu N+1: Kéo toàn bộ SessionLesson của lớp trong 1 query duy nhất
        List<SessionLesson> allLessonsInClass = sessionLessonRepository
                .findByClassSession_Clazz_ClassIdOrderByOrderIndexAsc(classId);
        
        // Nhóm theo sessionId để truy xuất nhanh
        java.util.Map<Integer, List<SessionLesson>> lessonsGroupedBySession = allLessonsInClass.stream()
                .collect(java.util.stream.Collectors.groupingBy(sl -> sl.getSession().getSessionId()));

        // 4. Gom Session và Map Lesson thật
        List<SessionDetailDTO> sessionDTOs = new ArrayList<>();
        LocalDate today = LocalDate.now();

        int totalLessonsInClass = 0;
        int completedLessonsByUser = 0;

        if (clazz.getSessions() != null) {
            for (ClassSession session : clazz.getSessions()) {
                String sessionStatus = "UPCOMING";
                LocalDate sessionDate = session.getSessionDate() != null ? session.getSessionDate().toLocalDate() : null;

                if (sessionDate != null) {
                    if (sessionDate.isBefore(today)) sessionStatus = "COMPLETED";
                    else if (sessionDate.isEqual(today)) sessionStatus = "LEARNING";
                }

                // Lấy nội dung từ Map thay vì query database trong loop
                List<SessionLesson> sessionLessons = lessonsGroupedBySession.getOrDefault(session.getSessionId(), new ArrayList<>());

                List<LessonResponseDTO> materials = new ArrayList<>();
                List<LessonResponseDTO> quizzes = new ArrayList<>();

                for (SessionLesson sl : sessionLessons) {
                    Lesson lesson = sl.getLesson();
                    if (lesson == null) continue;

                    totalLessonsInClass++; 

                    boolean isLessonCompleted = userLessonRepository
                            .existsByUser_UserIdAndLesson_LessonIdAndIsCompletedTrue(userId, lesson.getLessonId());

                    if (isLessonCompleted) completedLessonsByUser++;

                    boolean isLocked = sessionStatus.equals("UPCOMING");

                    // FIX: Sử dụng đúng Getter từ Entity Lesson (lessonName, type, quiz_id)
                    LessonResponseDTO lessonDTO = LessonResponseDTO.builder()
                            .lessonId(lesson.getLessonId())
                            .type(lesson.getType() != null ? lesson.getType() : "DOC")
                            .lessonTitle(lesson.getLessonName())
                            .lessonName(lesson.getLessonName())
                            .duration(lesson.getDuration())
                            .videoUrl(lesson.getVideoUrl())
                            .quizId(lesson.getQuiz_id()) 
                            .isCompleted(isLessonCompleted)
                            .isLocked(isLocked)
                            .build();

                    if ("QUIZ".equalsIgnoreCase(lessonDTO.getType())) {
                        quizzes.add(lessonDTO);
                    } else {
                        materials.add(lessonDTO);
                    }
                }

                sessionDTOs.add(SessionDetailDTO.builder()
                        .sessionId(session.getSessionId())
                        .sessionNo(session.getSessionNumber())
                        .startTime(session.getStartTime())
                        .endTime(session.getEndTime())
                        .dayOfWeek(session.getSessionDate() != null ? session.getSessionDate().getDayOfWeek().getValue() : null)
                        .slotNumber(calculateSlotNumber(session.getStartTime()))
                        .topic(session.getTopic())
                        .date(session.getSessionDate() != null ? session.getSessionDate().toLocalDate().toString() : "")
                        .status(sessionStatus)
                        .materials(materials)
                        .quizzes(quizzes)
                        .build());
            }
        }

        // 4. CÔNG THỨC TÍNH PROGRESS CHUẨN KẾ HOẠCH V2
        // Progress = (Completed Lessons in UserLesson / Total Lessons mapped to Class) * 100
        int progress = 0;
        if (totalLessonsInClass > 0) {
            progress = (completedLessonsByUser * 100) / totalLessonsInClass;
        }

        // 5. Đóng gói DTO Tổng
        return StudentClassDetailResponse.builder()
                .classId(clazz.getClassId())
                .courseId(clazz.getCourse() != null ? clazz.getCourse().getCourseId() : null)
                .className(clazz.getClassName())
                .courseName(clazz.getCourse() != null ? clazz.getCourse().getCourseName() : "")
                .courseImage(clazz.getCourse() != null ? clazz.getCourse().getImageUrl() : "")
                .startDate(clazz.getStartDate() != null ? clazz.getStartDate().toString() : "")
                .endDate(clazz.getEndDate() != null ? clazz.getEndDate().toString() : "")
                .status(clazz.getStatus())
                .meetLink(clazz.getMeetLink())
                .progressPercent(progress)
                .completedSessions((int) sessionDTOs.stream().filter(s -> s.getStatus().equals("COMPLETED")).count())
                .totalSessions(sessionDTOs.size())
                .sessions(sessionDTOs)
                .members(members)
                .build();
    }

    private int calculateSlotNumber(String startTime) {
        if (startTime == null || startTime.length() < 5) return 1;
        int hour;
        try {
            hour = Integer.parseInt(startTime.substring(0, 2));
        } catch (Exception e) {
            return 1;
        }
        if (hour >= 7 && hour < 9) return 1;
        if (hour >= 9 && hour < 11) return 2;
        if (hour >= 13 && hour < 15) return 3;
        if (hour >= 15 && hour < 17) return 4;
        if (hour >= 18 && hour < 20) return 5;
        // Default mappings if outside standard slots
        if (hour < 7) return 1;
        if (hour >= 11 && hour < 13) return 2;
        if (hour >= 17 && hour < 18) return 4;
        return 5;
    }
}
