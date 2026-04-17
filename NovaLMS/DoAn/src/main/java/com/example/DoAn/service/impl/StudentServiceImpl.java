package com.example.DoAn.service.impl;

import com.example.DoAn.dto.response.*;
import com.example.DoAn.dto.request.EnrollRequestDTO;
import com.example.DoAn.model.*;
import com.example.DoAn.repository.*;
import com.example.DoAn.service.EmailService;
import com.example.DoAn.service.INotificationService;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private final SessionQuizRepository sessionQuizRepository;
    private final LessonRepository lessonRepository;
    private final QuizResultRepository quizResultRepository;
    private final ClassSessionRepository classSessionRepository;
    private final QuizRepository quizRepository;
    private final EmailService emailService;
    private final INotificationService notificationService;

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

            // Active Courses Count
            List<Registration> myRegs = registrationRepository.findByUserEmail(user.getEmail());
            int activeCoursesCount = (int) myRegs.stream().filter(r -> "Approved".equals(r.getStatus())).count();

            // Completed Quizzes Count
            List<QuizResult> myQuizzes = quizResultRepository.findByUser_Email(user.getEmail());
            int completedQuizzesCount = myQuizzes.size();

            // Recent Quiz Scores (mapping to List<QuizScoreDTO>)
            Page<QuizResult> recentPage = quizResultRepository.findByUserEmailOrderBySubmittedAtDesc(user.getEmail(), PageRequest.of(0, 5));
            List<DashboardResponseDTO.QuizScoreDTO> recentScores = recentPage.getContent().stream()
                    .map(q -> DashboardResponseDTO.QuizScoreDTO.builder()
                            .quizName(q.getQuiz().getTitle())
                            .score(q.getScore() != null ? q.getScore().doubleValue() : 0.0)
                            .build())
                    .collect(Collectors.toList());

            // Last Lesson (Find latest active enrollment, then find its active lesson)
            DashboardResponseDTO.LastLessonDTO lastLessonDTO = null;
            Registration latestActiveReg = myRegs.stream()
                    .filter(r -> "Approved".equals(r.getStatus()))
                    .max(java.util.Comparator.comparing(Registration::getRegistrationTime))
                    .orElse(null);

            if (latestActiveReg != null) {
                Course course = latestActiveReg.getCourse();
                if (course != null) {
                    List<Integer> uncompleted = userLessonRepository.findUncompletedLessonIds(user.getUserId(), course.getCourseId());
                    Lesson targetLesson = null;

                    if (!uncompleted.isEmpty()) {
                        targetLesson = lessonRepository.findById(uncompleted.get(0)).orElse(null);
                    } else {
                        List<Integer> allIds = userLessonRepository.findAllLessonIdsOfCourse(course.getCourseId());
                        if (!allIds.isEmpty()) {
                            targetLesson = lessonRepository.findById(allIds.get(allIds.size() - 1)).orElse(null);
                        }
                    }

                    if (targetLesson != null) {
                        long completedCount = userLessonRepository.countCompletedLessonsByUserIdAndCourseId(user.getUserId(), course.getCourseId());
                        long totalCount = userLessonRepository.findAllLessonIdsOfCourse(course.getCourseId()).size();
                        int progress = totalCount > 0 ? (int) ((completedCount * 100) / totalCount) : 0;

                        lastLessonDTO = DashboardResponseDTO.LastLessonDTO.builder()
                                .courseName(course.getCourseName() != null ? course.getCourseName() : course.getTitle())
                                .chapterName(targetLesson.getLessonName())
                                .progress(progress)
                                .lessonUrl("/student/lesson/view/" + targetLesson.getLessonId())
                                .courseImage(course.getImageUrl())
                                .build();
                    }
                }
            }

            // ── UPCOMING EVENTS (Sắp diễn ra) ──
            List<DashboardResponseDTO.UpcomingEventDTO> upcomingEvents = new ArrayList<>();
            try {
                LocalDateTime now = LocalDateTime.now();
                // Get all class IDs user is enrolled in (Approved)
                List<Registration> approvedRegs = myRegs.stream()
                        .filter(r -> "Approved".equals(r.getStatus()) && r.getClazz() != null)
                        .collect(Collectors.toList());

                List<ClassSession> allUpcomingSessions = new ArrayList<>();
                for (Registration reg : approvedRegs) {
                    List<ClassSession> sessions = classSessionRepository
                            .findByClazzClassIdOrderBySessionNumberAsc(reg.getClazz().getClassId());
                    for (ClassSession s : sessions) {
                        if (s.getSessionDate() != null && !s.getSessionDate().isBefore(now)) {
                            allUpcomingSessions.add(s);
                        }
                    }
                }
                // Sort by date ascending, take first 3
                allUpcomingSessions.sort((a, b) -> a.getSessionDate().compareTo(b.getSessionDate()));
                int limit = Math.min(3, allUpcomingSessions.size());
                String[] monthNames = {"", "Thg 1", "Thg 2", "Thg 3", "Thg 4", "Thg 5", "Thg 6",
                                       "Thg 7", "Thg 8", "Thg 9", "Thg 10", "Thg 11", "Thg 12"};
                for (int i = 0; i < limit; i++) {
                    ClassSession s = allUpcomingSessions.get(i);
                    String day = String.valueOf(s.getSessionDate().getDayOfMonth());
                    String month = monthNames[s.getSessionDate().getMonthValue()];
                    String className = s.getClazz() != null ? s.getClazz().getClassName() : "Lớp học";
                    String timeRange = (s.getStartTime() != null ? s.getStartTime() : "") +
                                       (s.getEndTime() != null ? " – " + s.getEndTime() : "");
                    String subtitle = timeRange;
                    if (s.getClazz() != null && s.getClazz().getMeetLink() != null) {
                        subtitle += " • Live Zoom";
                    }

                    upcomingEvents.add(DashboardResponseDTO.UpcomingEventDTO.builder()
                            .day(day)
                            .month(month)
                            .title(className + " - Buổi " + s.getSessionNumber())
                            .subtitle(subtitle)
                            .type("NORMAL")
                            .build());
                }
            } catch (Exception ex) {
                // Silently ignore if upcoming events fail
            }

            // ── RECENT QUIZ HISTORY (Lịch sử làm bài) ──
            List<DashboardResponseDTO.RecentQuizHistoryDTO> quizHistoryList = new ArrayList<>();
            try {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
                Page<QuizResult> historyPage = quizResultRepository.findByUserEmailOrderBySubmittedAtDesc(
                        user.getEmail(), PageRequest.of(0, 5));
                for (QuizResult qr : historyPage.getContent()) {
                    String statusLabel;
                    String statusClass;
                    String iconBg, iconColor, iconClass;

                    if (qr.getPassed() == null) {
                        // Chờ chấm điểm
                        statusLabel = "Chờ chấm điểm";
                        statusClass = "badge-warn";
                        iconBg = "#eff6ff"; iconColor = "#1d6de5"; iconClass = "bi-patch-question-fill";
                    } else if (Boolean.TRUE.equals(qr.getPassed())) {
                        statusLabel = "Đạt";
                        statusClass = "badge-success";
                        iconBg = "#dcfce7"; iconColor = "#16a34a"; iconClass = "bi-check-circle-fill";
                    } else {
                        statusLabel = "Không đạt";
                        statusClass = "badge-danger";
                        iconBg = "#fee2e2"; iconColor = "#dc2626"; iconClass = "bi-x-circle-fill";
                    }

                    // Determine max score by summing points of all questions
                    int totalPoints = 0;
                    if (qr.getQuiz() != null && qr.getQuiz().getQuizQuestions() != null) {
                        for (QuizQuestion qq : qr.getQuiz().getQuizQuestions()) {
                            totalPoints += qq.getPoints() != null ? qq.getPoints().intValue() : 1;
                        }
                    }
                    Integer maxScore = totalPoints;

                    String courseName = "";
                    if (qr.getQuiz() != null && qr.getQuiz().getCourse() != null) {
                        courseName = qr.getQuiz().getCourse().getCourseName();
                    }

                    quizHistoryList.add(DashboardResponseDTO.RecentQuizHistoryDTO.builder()
                            .quizId(qr.getQuiz() != null ? qr.getQuiz().getQuizId() : null)
                            .resultId(qr.getResultId())
                            .quizTitle(qr.getQuiz() != null ? qr.getQuiz().getTitle() : "Quiz")
                            .courseName(courseName)
                            .submittedAt(qr.getSubmittedAt() != null ? qr.getSubmittedAt().format(fmt) : "")
                            .score(qr.getScore())
                            .maxScore(maxScore)
                            .statusLabel(statusLabel)
                            .statusClass(statusClass)
                            .iconBg(iconBg)
                            .iconColor(iconColor)
                            .iconClass(iconClass)
                            .build());
                }
            } catch (Exception ex) {
                // Silently ignore if quiz history fails
            }

            DashboardResponseDTO dto = DashboardResponseDTO.builder()
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .avatarUrl(user.getAvatarUrl())
                    .roleName(user.getRole().getName())
                    .activeCourses(activeCoursesCount)
                    .completedQuizzes(completedQuizzesCount)
                    .recentQuizScores(recentScores)
                    .lastLesson(lastLessonDTO)
                    .upcomingEvents(upcomingEvents)
                    .recentQuizHistory(quizHistoryList)
                    .build();

            return ResponseData.success("Thành công", dto);
        } catch (Exception e) {
            e.printStackTrace();
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

            // ── Send email + in-app notification ─────────────────────────────────
            User student = registration.getUser();
            if (student != null) {
                String studentName = student.getFullName() != null ? student.getFullName() : "";
                String courseName = registration.getCourse() != null && registration.getCourse().getCourseName() != null
                        ? registration.getCourse().getCourseName() : "";
                String className = registration.getClazz() != null ? registration.getClazz().getClassName() : "";
                String startDate = registration.getClazz() != null && registration.getClazz().getStartDate() != null
                        ? registration.getClazz().getStartDate().toString() : "";

                if ("Approved".equals(status)) {
                    if (student.getUserId() != null) {
                        notificationService.sendEnrollmentApproved(Long.valueOf(student.getUserId()), className, courseName);
                    }
                    if (student.getEmail() != null && !student.getEmail().isBlank()) {
                        emailService.sendEnrollmentApprovedEmail(student.getEmail(), studentName, className, courseName, startDate);
                    }
                } else if ("Rejected".equals(status)) {
                    if (student.getUserId() != null) {
                        notificationService.sendEnrollmentRejected(Long.valueOf(student.getUserId()), className, courseName, note);
                    }
                    if (student.getEmail() != null && !student.getEmail().isBlank()) {
                        emailService.sendEnrollmentRejectedEmail(student.getEmail(), studentName, className, courseName, note);
                    }
                }
            }

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

    @Override
    @Transactional(readOnly = true)
    public StudentClassDetailResponse getStudentClassDetail(Integer classId, Integer userId) {
        // 1. Check phân quyền thật
        boolean isEnrolled = registrationRepository.existsByClazz_ClassIdAndUser_UserIdAndStatus(classId, userId, "Approved");
        if (!isEnrolled) {
            throw new RuntimeException("Bạn không có quyền truy cập hoặc chưa thanh toán khóa học này!");
        }

        Clazz clazz = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học"));

        // 2. Gom danh sách Members
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

        // 3. Tối ưu N+1: Kéo toàn bộ SessionLesson và SessionQuiz của lớp
        List<SessionLesson> allLessonsInClass = sessionLessonRepository
                .findByClassSession_Clazz_ClassIdOrderByOrderIndexAsc(classId);
        
        java.util.Map<Integer, List<SessionLesson>> lessonsGroupedBySession = allLessonsInClass.stream()
                .collect(Collectors.groupingBy(sl -> sl.getSession().getSessionId()));

        List<SessionQuiz> allQuizzesInClass = sessionQuizRepository.findAll().stream()
                .filter(sq -> sq.getSession() != null && sq.getSession().getClazz() != null 
                        && sq.getSession().getClazz().getClassId().equals(classId))
                .toList();
        
        java.util.Map<Integer, List<SessionQuiz>> sessionQuizzesGrouped = allQuizzesInClass.stream()
                .collect(Collectors.groupingBy(sq -> sq.getSession().getSessionId()));

        // 4. Gom Session và Map nội dung
        List<SessionDetailDTO> sessionDTOs = new ArrayList<>();
        LocalDate today = LocalDate.now();

        int totalLessonsMapped = 0;
        int completedLessonsByUser = 0;

        if (clazz.getSessions() != null) {
            for (ClassSession session : clazz.getSessions()) {
                String sessionStatus = "UPCOMING";
                LocalDate sessionDate = session.getSessionDate() != null ? session.getSessionDate().toLocalDate() : null;

                if (sessionDate != null) {
                    if (sessionDate.isBefore(today)) sessionStatus = "COMPLETED";
                    else if (sessionDate.isEqual(today)) sessionStatus = "LEARNING";
                }

                boolean isLocked = sessionStatus.equals("UPCOMING");

                // --- CATEGORY 1: MATERIALS & EXPERT QUIZZES (from SessionLesson) ---
                List<SessionLesson> sessionLessons = lessonsGroupedBySession.getOrDefault(session.getSessionId(), new ArrayList<>());
                
                // Aggregate Topic: Nối tên tất cả bài học
                String aggregatedTopic = sessionLessons.stream()
                        .map(sl -> sl.getLesson().getLessonName())
                        .collect(Collectors.joining(", "));
                if (aggregatedTopic.isEmpty()) aggregatedTopic = session.getTopic(); // Fallback to database topic

                List<LessonResponseDTO> materials = new ArrayList<>();
                List<LessonResponseDTO> quizzes = new ArrayList<>();

                for (SessionLesson sl : sessionLessons) {
                    Lesson lesson = sl.getLesson();
                    if (lesson == null) continue;

                    totalLessonsMapped++; 
                    boolean isComp = userLessonRepository.existsByUser_UserIdAndLesson_LessonIdAndIsCompletedTrue(userId, lesson.getLessonId());
                    if (isComp) completedLessonsByUser++;

                    QuizResult latestResult = quizResultRepository
                            .findFirstByQuizQuizIdAndUserUserIdOrderBySubmittedAtDesc(lesson.getQuiz_id(), userId)
                            .orElse(null);

                    Quiz qObj = (lesson.getQuiz_id() != null)
                            ? quizRepository.findById(lesson.getQuiz_id()).orElse(null)
                            : null;

                    int qMaxAttempts = (qObj != null && qObj.getMaxAttempts() != null) ? qObj.getMaxAttempts() : 0;
                    long qAttemptsUsed = (qObj != null) ? quizResultRepository.countByQuizQuizIdAndUserUserIdAndStatusNot(qObj.getQuizId(), userId, "IN_PROGRESS") : 0;

                    LessonResponseDTO lDTO = LessonResponseDTO.builder()
                            .lessonId(lesson.getLessonId())
                            .type(lesson.getType() != null ? lesson.getType() : "DOC")
                            .lessonTitle(lesson.getLessonName())
                            .lessonName(lesson.getLessonName())
                            .duration(lesson.getDuration())
                            .videoUrl(lesson.getVideoUrl())
                            .quizId(lesson.getQuiz_id())
                            .isCompleted(isComp)
                            .isLocked(isLocked)
                            .latestResultId(latestResult != null ? latestResult.getResultId() : null)
                            .gradingStatus(latestResult != null ? latestResult.getStatus() : null)
                            .isSequential(qObj != null && Boolean.TRUE.equals(qObj.getIsSequential()))
                            .canRetake(qMaxAttempts == 0 || qAttemptsUsed < qMaxAttempts)
                            .attemptsLeft(qMaxAttempts > 0 ? (int) Math.max(0, qMaxAttempts - qAttemptsUsed) : -1)
                            .maxAttempts(qMaxAttempts > 0 ? qMaxAttempts : null)
                            .build();

                    if ("QUIZ".equalsIgnoreCase(lDTO.getType())) quizzes.add(lDTO);
                    else materials.add(lDTO);
                }

                // --- CATEGORY 2: TEACHER DIRECT QUIZ (ClassSession.quiz) ---
                if (session.getQuiz() != null) {
                    Quiz q = session.getQuiz();
                    QuizResult latestResult = quizResultRepository
                            .findFirstByQuizQuizIdAndUserUserIdOrderBySubmittedAtDesc(q.getQuizId(), userId)
                            .orElse(null);

                    int qMaxAttempts = q.getMaxAttempts() != null ? q.getMaxAttempts() : 0;
                    long qAttemptsUsed = quizResultRepository.countByQuizQuizIdAndUserUserIdAndStatusNot(q.getQuizId(), userId, "IN_PROGRESS");

                    quizzes.add(LessonResponseDTO.builder()
                            .quizId(q.getQuizId())
                            .lessonTitle("[Lớp] " + q.getTitle())
                            .lessonName(q.getTitle())
                            .type("QUIZ")
                            .isCompleted(latestResult != null && Boolean.TRUE.equals(latestResult.getPassed()))
                            .isLocked(isLocked)
                            .latestResultId(latestResult != null ? latestResult.getResultId() : null)
                            .gradingStatus(latestResult != null ? latestResult.getStatus() : null)
                            .isSequential(Boolean.TRUE.equals(q.getIsSequential()))
                            .canRetake(qMaxAttempts == 0 || qAttemptsUsed < qMaxAttempts)
                            .attemptsLeft(qMaxAttempts > 0 ? (int) Math.max(0, qMaxAttempts - qAttemptsUsed) : -1)
                            .maxAttempts(qMaxAttempts > 0 ? qMaxAttempts : null)
                            .build());
                }

                // --- CATEGORY 3: TEACHER MAPPED QUIZZES (SessionQuiz) ---
                List<SessionQuiz> sessionQuizzes = sessionQuizzesGrouped.getOrDefault(session.getSessionId(), new ArrayList<>());
                for (SessionQuiz sq : sessionQuizzes) {
                    Quiz q = sq.getQuiz();
                    // Chỉ hiển thị nếu Teacher đã "Open"
                    if (Boolean.TRUE.equals(sq.getIsOpen())) {
                        QuizResult latestResult = quizResultRepository
                                .findFirstByQuizQuizIdAndUserUserIdOrderBySubmittedAtDesc(q.getQuizId(), userId)
                                .orElse(null);

                        int qMaxAttempts = q.getMaxAttempts() != null ? q.getMaxAttempts() : 0;
                        long qAttemptsUsed = quizResultRepository.countByQuizQuizIdAndUserUserIdAndStatusNot(q.getQuizId(), userId, "IN_PROGRESS");

                        quizzes.add(LessonResponseDTO.builder()
                                .quizId(q.getQuizId())
                                .lessonTitle("[Bổ trợ] " + q.getTitle())
                                .lessonName(q.getTitle())
                                .type("QUIZ")
                                .isCompleted(latestResult != null && Boolean.TRUE.equals(latestResult.getPassed()))
                                .isLocked(isLocked)
                                .latestResultId(latestResult != null ? latestResult.getResultId() : null)
                                .gradingStatus(latestResult != null ? latestResult.getStatus() : null)
                                .isSequential(Boolean.TRUE.equals(q.getIsSequential()))
                                .canRetake(qMaxAttempts == 0 || qAttemptsUsed < qMaxAttempts)
                                .attemptsLeft(qMaxAttempts > 0 ? (int) Math.max(0, qMaxAttempts - qAttemptsUsed) : -1)
                                .maxAttempts(qMaxAttempts > 0 ? qMaxAttempts : null)
                                .build());
                    }
                }

                // Resolve meetLink: session-level overrides class-level
                String resolvedMeetLink = (session.getMeetLink() != null && !session.getMeetLink().isBlank())
                        ? session.getMeetLink()
                        : clazz.getMeetLink();

                sessionDTOs.add(SessionDetailDTO.builder()
                        .sessionId(session.getSessionId())
                        .sessionNo(session.getSessionNumber())
                        .startTime(session.getStartTime())
                        .endTime(session.getEndTime())
                        .dayOfWeek(session.getSessionDate() != null ? session.getSessionDate().getDayOfWeek().getValue() : null)
                        .slotNumber(calculateSlotNumber(session.getStartTime()))
                        .topic(aggregatedTopic)
                        .date(session.getSessionDate() != null ? session.getSessionDate().toLocalDate().toString() : "")
                        .status(sessionStatus)
                        .meetLink(resolvedMeetLink)
                        .materials(materials)
                        .quizzes(quizzes)
                        .build());
            }
        }

        int progress = totalLessonsMapped == 0 ? 0 : (completedLessonsByUser * 100) / totalLessonsMapped;

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

    @Override
    @Transactional(readOnly = true)
    public ResponseData<LearningProgressResponseDTO> getLearningProgress(String email) {
        try {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) return ResponseData.error(401, "Vui lòng đăng nhập.");

            // 1. Lấy tất cả khóa học của học sinh (Approved)
            List<Registration> regs = registrationRepository.findByUserEmail(email).stream()
                    .filter(r -> "Approved".equals(r.getStatus()))
                    .collect(Collectors.toList());

            List<LearningProgressResponseDTO.CourseProgressDTO> courseProgressList = new ArrayList<>();

            for (Registration reg : regs) {
                Course course = reg.getCourse();
                if (course == null) continue;

                // Lessons
                List<Integer> allLessonIds = userLessonRepository.findAllLessonIdsOfCourse(course.getCourseId());
                long completedLessons = userLessonRepository.countCompletedLessonsByUserIdAndCourseId(user.getUserId(), course.getCourseId());
                int totalLessons = allLessonIds.size();
                int progress = totalLessons > 0 ? (int) ((completedLessons * 100) / totalLessons) : 0;

                // Quizzes
                // We need to count quizzes related to the course/class
                // Simple way: Count QuizResults for quizzes belonging to this course
                List<QuizResult> quizResults = quizResultRepository.findByUser_Email(email).stream()
                        .filter(qr -> qr.getQuiz().getCourse() != null && qr.getQuiz().getCourse().getCourseId().equals(course.getCourseId()))
                        .collect(Collectors.toList());
                
                long passedQuizzes = quizResults.stream()
                        .filter(qr -> Boolean.TRUE.equals(qr.getPassed()))
                        .map(qr -> qr.getQuiz().getQuizId()) // unique quizzes? 
                        .distinct()
                        .count();
                
                // Total quizzes in course (from SessionQuiz or Course content)
                // Let's count from session_lesson where lesson type is QUIZ + session_quiz
                long totalQuizzesInCourse = sessionLessonRepository.countByCourseIdAndLessonType(course.getCourseId(), "QUIZ")
                        + sessionQuizRepository.countByCourseId(course.getCourseId());

                double avgScore = quizResults.stream()
                        .filter(qr -> qr.getScore() != null)
                        .mapToDouble(qr -> qr.getScore().doubleValue())
                        .average().orElse(0.0);

                String teacherName = (reg.getClazz() != null && reg.getClazz().getTeacher() != null) 
                        ? reg.getClazz().getTeacher().getFullName() : "N/A";

                courseProgressList.add(LearningProgressResponseDTO.CourseProgressDTO.builder()
                        .courseId(course.getCourseId())
                        .courseName(course.getCourseName() != null ? course.getCourseName() : course.getTitle())
                        .courseImage(course.getImageUrl())
                        .totalLessons(totalLessons)
                        .completedLessons((int) completedLessons)
                        .progressPercent(progress)
                        .totalQuizzes((int) totalQuizzesInCourse)
                        .completedQuizzes((int) passedQuizzes)
                        .averageScore(Math.round(avgScore * 10.0) / 10.0)
                        .teacherName(teacherName)
                        .status(progress >= 100 ? "Hoàn thành" : "Đang học")
                        .build());
            }

            return ResponseData.success("Thành công", LearningProgressResponseDTO.builder()
                    .courses(courseProgressList)
                    .build());
        } catch (Exception e) {
            return ResponseData.error(500, "Lỗi tải tiến độ: " + e.getMessage());
        }
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
