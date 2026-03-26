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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
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
@RequiredArgsConstructor
@Slf4j
public class EnrollmentController {

    private final StudentService studentService;
    private final PayosService payosService;
    private final UserRepository userRepository;
    private final RegistrationRepository registrationRepository;
    private final PaymentRepository paymentRepository;

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
        Optional<Registration> optReg = registrationRepository.findById(registrationId);
        if (optReg.isEmpty()) {
            return ResponseData.error(404, "Không tìm thấy đăng ký.");
        }
        Registration registration = optReg.get();

        // Free course — no payment needed, auto-approve
        if (registration.getRegistrationPrice() == null ||
                registration.getRegistrationPrice().doubleValue() <= 0) {
            registration.setStatus("Approved");
            registrationRepository.save(registration);
            PaymentLinkResponseDTO freeResult = PaymentLinkResponseDTO.builder()
                    .registrationId(registrationId)
                    .status("APPROVED")
                    .message("Khóa học miễn phí — đăng ký thành công!")
                    .build();
            return ResponseData.success("Đăng ký thành công!", freeResult);
        }

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
}
