package com.example.DoAn.controller;

import com.example.DoAn.dto.request.EnrollRequestDTO;
import com.example.DoAn.dto.response.PaymentLinkResponseDTO;
import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.model.Payment;
import com.example.DoAn.model.Registration;
import com.example.DoAn.model.User;
import com.example.DoAn.repository.PaymentRepository;
import com.example.DoAn.repository.RegistrationRepository;
import com.example.DoAn.repository.UserRepository;
import com.example.DoAn.service.PayosService;
import com.example.DoAn.service.StudentService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

/**
 * Handles course enrollment with PayOS payment in a single request.
 *
 * Flow:
 *   1. Create Registration (status=PENDING)
 *   2. createPaymentLink: creates Payment record → calls PayOS API → updates Payment + Registration
 *   3. Returns checkoutUrl → frontend redirects user
 *
 * If PayOS fails: Payment.status=FAILED, Registration.status=Submitted (can retry).
 */
@RestController
@RequestMapping("/api/v1")
public class EnrollmentController {

    private final StudentService studentService;
    private final PayosService payosService;
    private final UserRepository userRepository;
    private final RegistrationRepository registrationRepository;
    private final PaymentRepository paymentRepository;
    private final com.example.DoAn.service.EmailService emailService;
    private final com.example.DoAn.service.INotificationService notificationService;
    private static final Logger log = LoggerFactory.getLogger(EnrollmentController.class);

    public EnrollmentController(StudentService studentService, PayosService payosService,
            UserRepository userRepository, RegistrationRepository registrationRepository,
            PaymentRepository paymentRepository,
            com.example.DoAn.service.EmailService emailService,
            com.example.DoAn.service.INotificationService notificationService) {
        this.studentService = studentService;
        this.payosService = payosService;
        this.userRepository = userRepository;
        this.registrationRepository = registrationRepository;
        this.paymentRepository = paymentRepository;
        this.emailService = emailService;
        this.notificationService = notificationService;
    }

    private String getEmail(Principal principal) {
        if (principal == null) return null;
        if (principal instanceof OAuth2AuthenticationToken t) {
            return t.getPrincipal().getAttribute("email");
        }
        return principal.getName();
    }

    /**
     * Enroll in a course and immediately create a PayOS payment link.
     * POST /api/v1/enroll-with-payment
     * Body: EnrollRequestDTO { classId, courseId }
     *
     * Returns PaymentLinkResponseDTO with checkoutUrl on success.
     */
    @PostMapping("/enroll-with-payment")
    @Transactional
    public ResponseData<PaymentLinkResponseDTO> enrollWithPayment(
            @Valid @RequestBody EnrollRequestDTO request,
            Principal principal) {
        String email = getEmail(principal);
        if (email == null) {
            return ResponseData.error(401, "Vui lòng đăng nhập để đăng ký khóa học.");
        }

        // Load user
        Optional<User> optUser = userRepository.findByEmail(email);
        if (optUser.isEmpty()) {
            return ResponseData.error(401, "Không tìm thấy người dùng.");
        }
        User user = optUser.get();

        // ── Kiểm tra đăng ký đang chờ (PENDING/Submitted) — cho phép retry thanh toán ──
        // Nếu user back trình duyệt rồi đăng ký lại, vẫn redirect về PayOS thay vì chặn
        Optional<Registration> existingReg = registrationRepository
                .findByUser_UserIdAndClazz_ClassIdAndStatusIn(
                        user.getUserId(), request.getClassId(), java.util.List.of("PENDING", "Submitted"));
        if (existingReg.isPresent()) {
            Registration reg = existingReg.get();
            // Sync PayOS trạng thái mới nhất
            paymentRepository.findFirstByRegistrationIdOrderByCreatedAtDesc(reg.getRegistrationId())
                    .ifPresent(payment -> {
                        if ("PENDING".equals(payment.getStatus()) || "FAILED".equals(payment.getStatus())) {
                            try {
                                payosService.syncPaymentStatus(payment.getPayosOrderCode());
                            } catch (Exception ignored) {}
                        }
                    });
            // Tìm lại payment sau sync
            Optional<Payment> syncedPayment = paymentRepository
                    .findFirstByRegistrationIdOrderByCreatedAtDesc(reg.getRegistrationId());
            if (syncedPayment.isPresent()) {
                Payment p = syncedPayment.get();
                if ("PAID".equals(p.getStatus())) {
                    // Đã thanh toán rồi → thông báo
                    return ResponseData.error(400, "Bạn đã thanh toán đăng ký này. Vui lòng kiểm tra trang Khóa học của tôi.");
                }
                if (p.getCheckoutUrl() != null && !"CANCELLED".equals(p.getStatus())) {
                    // Vẫn còn link thanh toán → trả về redirect luôn
                    return ResponseData.success("Đăng ký đang chờ thanh toán — đang chuyển hướng...", PaymentLinkResponseDTO.builder()
                            .registrationId(reg.getRegistrationId())
                            .checkoutUrl(p.getCheckoutUrl())
                            .paymentUrl(p.getCheckoutUrl())
                            .status("PENDING")
                            .message("Đăng ký đang chờ thanh toán.")
                            .build());
                }
            }
            // Không có checkoutUrl hoặc đã hủy → cho đăng ký lại (xóa cái cũ)
            reg.setStatus("Cancelled");
            registrationRepository.save(reg);
            log.info("Previous pending registration {} cancelled, allowing new enrollment for user {} class {}",
                    reg.getRegistrationId(), email, request.getClassId());
        }

        // Step 1: Create registration
        ResponseData<Integer> enrollResult = studentService.enrollCourse(email, request);
        if (enrollResult.getStatus() != 200 && enrollResult.getStatus() != 201) {
            return ResponseData.error(enrollResult.getStatus(),
                    enrollResult.getMessage() != null ? enrollResult.getMessage() : "Lỗi đăng ký.");
        }

        Integer registrationId = enrollResult.getData();
        Optional<Registration> optReg = registrationRepository.findWithAssociationsById(registrationId);
        if (optReg.isEmpty()) {
            return ResponseData.error(404, "Không tìm thấy đăng ký.");
        }
        Registration registration = optReg.get();

        // Free course — no payment needed, auto-approve
        boolean isPaid = isPositivePrice(registration.getRegistrationPrice());
        if (!isPaid) {
            registration.setStatus("Approved");
            registrationRepository.save(registration);
            notifyStudentApproved(registration);
            PaymentLinkResponseDTO freeResult = PaymentLinkResponseDTO.builder()
                    .registrationId(registrationId)
                    .status("APPROVED")
                    .message("Khóa học miễn phí — đăng ký thành công!")
                    .build();
            return ResponseData.success("Đăng ký thành công!", freeResult);
        }

        // ── Notify managers of new enrollment ─────────────────────────────────
        notifyManagersOfNewEnrollment(registration);

        // Step 2: Call PayOS — createPaymentLink handles everything:
        //   creates Payment record → calls PayOS → updates Payment + Registration.status=PENDING
        // On PayOS failure: Payment.status=FAILED, Registration.status=Submitted (can retry)
        PaymentLinkResponseDTO paymentResult;
        try {
            paymentResult = payosService.createPaymentLink(registration, user);
        } catch (RuntimeException e) {
            log.warn("PayOS failed for registration {}: {}", registrationId, e.getMessage());
            // PayosServiceImpl already set Payment.status=FAILED and Registration.status=Submitted
            // No additional action needed here
            return ResponseData.error(503, e.getMessage());
        }

        return ResponseData.success("Tạo liên kết thanh toán thành công!", paymentResult);
    }

    private boolean isPositivePrice(Object price) {
        if (price == null) return false;
        if (price instanceof java.math.BigDecimal) return ((java.math.BigDecimal) price).compareTo(java.math.BigDecimal.ZERO) > 0;
        if (price instanceof Double) return (Double) price > 0;
        if (price instanceof Number) return ((Number) price).doubleValue() > 0;
        return false;
    }

    private void notifyManagersOfNewEnrollment(Registration registration) {
        if (registration == null) return;
        List<User> managers = userRepository.findByRole_Value("MANAGER");
        if (managers == null || managers.isEmpty()) return;

        String studentName = registration.getUser() != null
                ? (registration.getUser().getFullName() != null ? registration.getUser().getFullName() : "") : "";
        String studentEmail = registration.getUser() != null
                ? (registration.getUser().getEmail() != null ? registration.getUser().getEmail() : "") : "";
        String className = registration.getClazz() != null
                ? (registration.getClazz().getClassName() != null ? registration.getClazz().getClassName() : "") : "";
        String courseName = registration.getCourse() != null
                ? (registration.getCourse().getCourseName() != null ? registration.getCourse().getCourseName() : "") : "";

        for (User manager : managers) {
            String managerName = manager.getFullName() != null ? manager.getFullName() : "";
            if (manager.getEmail() != null && !manager.getEmail().isBlank()) {
                emailService.sendEnrollmentPendingApprovalEmail(manager.getEmail(), managerName,
                        studentName, studentEmail, className, courseName);
            }
            if (manager.getUserId() != null) {
                notificationService.sendEnrollmentPendingApproval(Long.valueOf(manager.getUserId()),
                        studentName, className, courseName);
            }
        }
    }

    private void notifyStudentApproved(Registration registration) {
        if (registration == null || registration.getUser() == null) return;
        User student = registration.getUser();
        String studentName = student.getFullName() != null ? student.getFullName() : "";
        String className = registration.getClazz() != null
                ? (registration.getClazz().getClassName() != null ? registration.getClazz().getClassName() : "") : "";
        String courseName = registration.getCourse() != null
                ? (registration.getCourse().getCourseName() != null ? registration.getCourse().getCourseName() : "") : "";
        String startDate = registration.getClazz() != null && registration.getClazz().getStartDate() != null
                ? registration.getClazz().getStartDate().toLocalDate().toString() : "";

        if (student.getUserId() != null) {
            notificationService.sendEnrollmentApproved(Long.valueOf(student.getUserId()), className, courseName);
        }
        if (student.getEmail() != null && !student.getEmail().isBlank()) {
            emailService.sendEnrollmentApprovedEmail(student.getEmail(), studentName, className, courseName, startDate);
        }
    }
}
