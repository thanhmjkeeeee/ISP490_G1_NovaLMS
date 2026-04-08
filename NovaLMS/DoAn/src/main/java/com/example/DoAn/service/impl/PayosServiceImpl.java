package com.example.DoAn.service.impl;

import com.example.DoAn.configuration.PayosConfig;
import com.example.DoAn.dto.response.PaymentLinkResponseDTO;
import com.example.DoAn.model.*;
import com.example.DoAn.repository.*;
import com.example.DoAn.service.EmailService;
import com.example.DoAn.service.INotificationService;
import com.example.DoAn.service.InvoicePdfService;
import com.example.DoAn.service.PayosService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayosServiceImpl implements PayosService {

    private final PayosConfig payosConfig;
    private final PaymentRepository paymentRepository;
    private final RegistrationRepository registrationRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final EmailService emailService;
    private final INotificationService notificationService;
    private final InvoicePdfService invoicePdfService;

    // ══════════════════════════════════════════════════════════════
    // 1. TẠO PAYMENT LINK (tạo Payment → gọi PayOS → cập nhật)
    // ══════════════════════════════════════════════════════════════
    @Override
    @Transactional
    public PaymentLinkResponseDTO createPaymentLink(Registration registration, User user) throws RuntimeException {
        // 1a. Build request
        Course course = registration.getCourse();
        String rawDesc = course.getCourseName() != null ? course.getCourseName() : "NovaLMS";
        String description = rawDesc.length() > 25 ? rawDesc.substring(0, 25) : rawDesc;

        long amount = registration.getRegistrationPrice() != null
                ? registration.getRegistrationPrice().longValue() : 0L;
        if (amount < 1000) amount = 1000;
        int intAmount = (int) amount;

        // orderCode: 6 chữ số duy nhất
        long orderCode = System.currentTimeMillis() % 1_000_000L;

        // 1b. Tạo Payment record TRƯỚC khi gọi PayOS (để webhook có thể match)
        Payment payment = Payment.builder()
                .payosOrderCode(orderCode)
                .registrationId(registration.getRegistrationId())
                .amount(BigDecimal.valueOf(amount))
                .description(description)
                .status("PENDING")
                .build();
        paymentRepository.save(payment);

        // 1c. Cập nhật registration status → PENDING
        registration.setStatus("PENDING");
        registrationRepository.save(registration);

        // 1d. Build signature
        String signature = buildSignature(amount, description, orderCode);

        // 1e. Gọi PayOS
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orderCode", orderCode);
        body.put("amount", intAmount);
        body.put("description", description);
        body.put("cancelUrl", payosConfig.getCancelUrl());
        body.put("returnUrl", payosConfig.getReturnUrl());
        body.put("signature", signature);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-client-id", payosConfig.getClientId());
        headers.set("X-api-key", payosConfig.getApiKey());

        String url = payosConfig.getBaseUrl() + "/v2/payment-requests";
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        log.info("PayOS request: orderCode={}, amount={}, desc={}", orderCode, intAmount, description);

        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
        } catch (Exception e) {
            log.error("PayOS connection FAILED: {}", e.getMessage());
            // Đánh dấu payment thất bại
            payment.setStatus("FAILED");
            paymentRepository.save(payment);
            registration.setStatus("Submitted"); // revert
            registrationRepository.save(registration);
            throw new RuntimeException("Không thể kết nối PayOS: " + e.getMessage(), e);
        }

        // 1f. Parse response
        JsonNode json;
        try {
            json = objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            log.error("PayOS invalid JSON: {}", response.getBody());
            payment.setStatus("FAILED");
            paymentRepository.save(payment);
            registration.setStatus("Submitted");
            registrationRepository.save(registration);
            throw new RuntimeException("Phản hồi PayOS không hợp lệ.");
        }
        log.info("PayOS response: {}", response.getBody());

        String respCode = json.has("code") ? json.get("code").asText() : null;
        String respDesc = json.has("desc") ? json.get("desc").asText() : null;
        if (!"00".equals(respCode)) {
            payment.setStatus("FAILED");
            paymentRepository.save(payment);
            registration.setStatus("Submitted");
            registrationRepository.save(registration);
            throw new RuntimeException("Lỗi PayOS: " + (respDesc != null ? respDesc : "Mã lỗi " + respCode));
        }

        // 1g. Cập nhật Payment với data từ PayOS
        JsonNode data = json.get("data");
        Long payosPaymentLinkId = data.has("id") ? data.get("id").asLong() : null;
        String checkoutUrl = data.has("checkoutUrl") ? data.get("checkoutUrl").asText() : null;
        String qrCode = data.has("qrCode") ? data.get("qrCode").asText() : null;

        payment.setPayosPaymentLinkId(payosPaymentLinkId);
        payment.setCheckoutUrl(checkoutUrl);
        payment.setQrCode(qrCode);
        paymentRepository.save(payment);

        log.info("Payment link created: paymentId={}, orderCode={}, checkoutUrl={}",
                payment.getId(), orderCode, checkoutUrl);

        return PaymentLinkResponseDTO.builder()
                .registrationId(registration.getRegistrationId())
                .payosPaymentLinkId(payosPaymentLinkId)
                .checkoutUrl(checkoutUrl)
                .paymentUrl(checkoutUrl)
                .qrCode(qrCode)
                .status("PENDING")
                .message("Tạo liên kết thanh toán thành công")
                .build();
    }

    // ══════════════════════════════════════════════════════════════
    // 2. RETRY — tạo Payment mới
    // ══════════════════════════════════════════════════════════════
    @Override
    @Transactional
    public PaymentLinkResponseDTO retryPaymentLink(Integer registrationId, User user) throws RuntimeException {
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đăng ký: " + registrationId));

        Payment currentPayment = paymentRepository.findFirstByRegistrationIdOrderByCreatedAtDesc(registrationId).orElse(null);
        String currentStatus = currentPayment != null ? currentPayment.getStatus() : registration.getStatus();

        if ("PAID".equals(currentStatus)) {
            throw new RuntimeException("Đăng ký này đã được thanh toán.");
        }

        // PENDING — vẫn còn link thanh toán → sync rồi trả về checkoutUrl
        if ("PENDING".equals(currentStatus)) {
            // Sync từ PayOS phòng trường hợp đã thanh toán rồi nhưng chưa cập nhật
            try {
                if (currentPayment.getPayosOrderCode() != null) {
                    syncPaymentStatus(currentPayment.getPayosOrderCode());
                    // Reload sau sync
                    currentPayment = paymentRepository.findFirstByRegistrationIdOrderByCreatedAtDesc(registrationId).orElse(currentPayment);
                }
            } catch (Exception ignored) {}

            // Kiểm tra lại sau sync
            if (currentPayment != null && "PAID".equals(currentPayment.getStatus())) {
                throw new RuntimeException("Đăng ký này đã được thanh toán. Vui lòng kiểm tra trang Khóa học của tôi.");
            }
            if (currentPayment != null && currentPayment.getCheckoutUrl() != null) {
                return PaymentLinkResponseDTO.builder()
                        .registrationId(registrationId)
                        .payosPaymentLinkId(currentPayment.getPayosPaymentLinkId())
                        .checkoutUrl(currentPayment.getCheckoutUrl())
                        .paymentUrl(currentPayment.getCheckoutUrl())
                        .status("PENDING")
                        .message("Link thanh toán hiện tại vẫn còn hiệu lực.")
                        .build();
            }
        }

        // EXPIRED / FAILED / CANCELLED / Submitted — tạo payment link mới
        // Registration về Submitted để createPaymentLink tạo lại PENDING
        registration.setStatus("Submitted");
        registrationRepository.save(registration);

        return createPaymentLink(registration, user);
    }

    // ══════════════════════════════════════════════════════════════
    // 3. TẠO PAYMENT RECORD ONLY (không gọi PayOS)
    // Dùng khi muốn tạo record trước rồi gọi PayOS ở nơi khác
    // ══════════════════════════════════════════════════════════════
    @Override
    @Transactional
    public PaymentLinkResponseDTO createPaymentRecordOnly(Registration registration, User user) throws RuntimeException {
        long orderCode = System.currentTimeMillis() % 1_000_000L;

        Payment payment = Payment.builder()
                .payosOrderCode(orderCode)
                .registrationId(registration.getRegistrationId())
                .amount(registration.getRegistrationPrice())
                .description(
                        (registration.getCourse().getCourseName() != null
                                ? registration.getCourse().getCourseName() : "NovaLMS")
                                .substring(0, Math.min(25,
                                registration.getCourse().getCourseName() != null
                                        ? registration.getCourse().getCourseName().length() : 5))
                )
                .status("PENDING")
                .build();
        paymentRepository.save(payment);

        registration.setStatus("PENDING");
        registrationRepository.save(registration);

        return PaymentLinkResponseDTO.builder()
                .registrationId(registration.getRegistrationId())
                .status("PENDING")
                .message("Payment record created")
                .build();
    }

    // ══════════════════════════════════════════════════════════════
    // 4. WEBHOOK — XÁC MINH CHỮ KÝ
    // ══════════════════════════════════════════════════════════════
    @Override
    public boolean verifyWebhookSignature(String data, String signature) {
        try {
            String computed = calculateHmacSha256(data);
            return computed.equals(signature);
        } catch (Exception e) {
            log.error("PayOS webhook signature verification error", e);
            return false;
        }
    }

    // ══════════════════════════════════════════════════════════════
    // 5. WEBHOOK — THÀNH CÔNG
    // PayOS gửi: data.id = paymentLinkId, data.orderCode = orderCode
    // ══════════════════════════════════════════════════════════════
    @Override
    @Transactional
    public void handlePaymentSuccess(Long payosPaymentLinkId, String orderCodeStr) {
        try {
            if (orderCodeStr == null && payosPaymentLinkId == null) {
                log.warn("handlePaymentSuccess: both orderCode and paymentLinkId are null");
                return;
            }

            Payment payment = null;

            // Ưu tiên tìm bằng orderCode (chắc chắn nhất)
            if (orderCodeStr != null) {
                try {
                    Long orderCode = Long.parseLong(orderCodeStr);
                    payment = paymentRepository.findByPayosOrderCode(orderCode).orElse(null);
                } catch (NumberFormatException ignored) {}
            }

            // Fallback bằng payosPaymentLinkId
            if (payment == null && payosPaymentLinkId != null) {
                payment = paymentRepository.findByPayosPaymentLinkId(payosPaymentLinkId).orElse(null);
            }

            if (payment == null) {
                log.warn("No Payment found for orderCode={}, payosPaymentLinkId={}", orderCodeStr, payosPaymentLinkId);
                return;
            }

            // Idempotent: đã thanh toán rồi thì bỏ qua
            if ("PAID".equals(payment.getStatus())) {
                log.info("Payment already PAID: paymentId={}", payment.getId());
                return;
            }

            // Cập nhật Payment → PAID
            payment.setStatus("PAID");
            payment.setPaidAt(LocalDateTime.now());
            if (payosPaymentLinkId != null && payment.getPayosPaymentLinkId() == null) {
                payment.setPayosPaymentLinkId(payosPaymentLinkId);
            }
            paymentRepository.save(payment);

            // Create a final reference for use inside the lambda
            final Payment finalPayment = payment;
            final BigDecimal finalAmount = payment.getAmount();

            // Tự động duyệt đăng ký khi thanh toán thành công
            Integer registrationId = payment.getRegistrationId();
            if (registrationId != null) {
                Payment finalPayment = payment;
                registrationRepository.findById(registrationId).ifPresent(reg -> {
                    reg.setStatus("Approved");
                    registrationRepository.save(reg);
                    log.info("Registration auto-approved: registrationId={}", registrationId);

                    // ── Send email + in-app notification to student ──────────────────
                    User student = reg.getUser();
                    if (student != null) {
                        String studentName = student.getFullName() != null ? student.getFullName() : "";
                        String courseName = reg.getCourse() != null && reg.getCourse().getCourseName() != null
                                ? reg.getCourse().getCourseName() : "";
                        String className = reg.getClazz() != null ? reg.getClazz().getClassName() : "";
                        String amountStr = finalAmount != null ? finalAmount.toString() : "";
                        String startDate = reg.getClazz() != null && reg.getClazz().getStartDate() != null
                                ? reg.getClazz().getStartDate().toString() : "";

                        if (student.getEmail() != null && !student.getEmail().isBlank()) {
                            String pIdStr = String.valueOf(finalPayment.getId());
                            String pOrderCode = finalPayment.getPayosOrderCode() != null
                                    ? finalPayment.getPayosOrderCode().toString() : "";
                            String pPaidAtStr = finalPayment.getPaidAt() != null
                                    ? finalPayment.getPaidAt().toString() : "";

                            byte[] invoicePdf = invoicePdfService.generateInvoice(
                                    studentName,
                                    student.getEmail(),
                                    courseName,
                                    className,
                                    amountStr,
                                    pIdStr,
                                    pOrderCode,
                                    pPaidAtStr
                            );

                            emailService.sendPaymentSuccessEmail(student.getEmail(), studentName,
                                    courseName, className, amountStr,
                                    pIdStr, pOrderCode, pPaidAtStr, invoicePdf);
                        }
                        if (student.getUserId() != null) {
                            notificationService.sendPaymentSuccess(Long.valueOf(student.getUserId()),
                                    courseName, className);
                        }

                        // Also notify enrollment approved
                        if (student.getUserId() != null) {
                            notificationService.sendEnrollmentApproved(Long.valueOf(student.getUserId()),
                                    className, courseName);
                        }
                        if (student.getEmail() != null && !student.getEmail().isBlank()) {
                            emailService.sendEnrollmentApprovedEmail(student.getEmail(), studentName,
                                    className, courseName, startDate);
                        }
                    }
                });
            }

            log.info("✅ Payment SUCCESS + auto-approved: paymentId={}, registrationId={}",
                    payment.getId(), payment.getRegistrationId());

        } catch (Exception e) {
            log.error("Error handling payment success: {}", e.getMessage(), e);
        }
    }

    // ══════════════════════════════════════════════════════════════
    // 6. WEBHOOK — HỦY / HẾT HẠN
    // PayOS KHÔNG gửi webhook cho CANCELLED/EXPIRED
    // ══════════════════════════════════════════════════════════════
    @Override
    @Transactional
    public void handlePaymentCancel(Long payosPaymentLinkId, String orderCodeStr) {
        try {
            Payment payment = null;
            if (orderCodeStr != null) {
                try {
                    payment = paymentRepository.findByPayosOrderCode(Long.parseLong(orderCodeStr)).orElse(null);
                } catch (NumberFormatException ignored) {}
            }
            if (payment == null && payosPaymentLinkId != null) {
                payment = paymentRepository.findByPayosPaymentLinkId(payosPaymentLinkId).orElse(null);
            }
            if (payment == null) return;

            if (!"CANCELLED".equals(payment.getStatus())) {
                payment.setStatus("CANCELLED");
                paymentRepository.save(payment);
                log.info("Payment CANCELLED: paymentId={}", payment.getId());

                // ── Send email + in-app notification to student ──────────────────
                Integer registrationId = payment.getRegistrationId();
                if (registrationId != null) {
                    registrationRepository.findById(registrationId).ifPresent(reg -> {
                        User student = reg.getUser();
                        if (student != null) {
                            String studentName = student.getFullName() != null ? student.getFullName() : "";
                            String courseName = reg.getCourse() != null && reg.getCourse().getCourseName() != null
                                    ? reg.getCourse().getCourseName() : "";
                            String className = reg.getClazz() != null ? reg.getClazz().getClassName() : "";

                            if (student.getEmail() != null && !student.getEmail().isBlank()) {
                                emailService.sendPaymentFailedEmail(student.getEmail(), studentName,
                                        courseName, className);
                            }
                            if (student.getUserId() != null) {
                                notificationService.sendPaymentFailed(Long.valueOf(student.getUserId()),
                                        courseName, className);
                            }
                        }
                    });
                }
            }
        } catch (Exception e) {
            log.error("Error handling payment cancel: {}", e.getMessage(), e);
        }
    }

    // ══════════════════════════════════════════════════════════════
    // 7. SYNC TRẠNG THÁI TỪ PAYOS (polling cho CANCELLED/EXPIRED)
    // ══════════════════════════════════════════════════════════════
    @Override
    @Transactional
    public void syncPaymentStatus(Long orderCode) {
        if (orderCode == null) return;

        Payment payment = paymentRepository.findByPayosOrderCode(orderCode).orElse(null);
        if (payment == null) return;

        // Đã xác định rõ thì bỏ qua
        if ("PAID".equals(payment.getStatus()) || "CANCELLED".equals(payment.getStatus())
                || "EXPIRED".equals(payment.getStatus())) {
            return;
        }

        String url = payosConfig.getPaymentLinkInfoUrl(orderCode);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-client-id", payosConfig.getClientId());
        headers.set("X-api-key", payosConfig.getApiKey());

        HttpEntity<Void> request = new HttpEntity<>(headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            JsonNode json = objectMapper.readTree(response.getBody());
            log.info("PayOS getPaymentLinkInfo orderCode={}: {}", orderCode, response.getBody());

            String payosStatus = json.has("status") ? json.get("status").asText() : null;
            if (payosStatus == null) return;

            switch (payosStatus) {
                case "PAID" -> {
                    // Cập nhật Payment + tự động duyệt đăng ký
                    payment.setStatus("PAID");
                    payment.setPaidAt(LocalDateTime.now());
                    paymentRepository.save(payment);
                    Integer registrationId = payment.getRegistrationId();
                    if (registrationId != null) {
                        registrationRepository.findById(registrationId).ifPresent(reg -> {
                            if (!"Approved".equals(reg.getStatus())) {
                                reg.setStatus("Approved");
                                registrationRepository.save(reg);
                                log.info("✅ Synced + auto-approved: paymentId={}, registrationId={}", payment.getId(), registrationId);
                            }
                        });
                    }
                    log.info("✅ Synced PAID + auto-approved: paymentId={}", payment.getId());
                }
                case "CANCELLED" -> {
                    if (!"CANCELLED".equals(payment.getStatus())) {
                        payment.setStatus("CANCELLED");
                        paymentRepository.save(payment);
                        log.info("Synced CANCELLED: paymentId={}", payment.getId());
                    }
                }
                case "EXPIRED" -> {
                    if (!"EXPIRED".equals(payment.getStatus())) {
                        payment.setStatus("EXPIRED");
                        paymentRepository.save(payment);
                        log.info("Synced EXPIRED: paymentId={}", payment.getId());
                    }
                }
            }
        } catch (Exception e) {
            log.error("syncPaymentStatus failed for orderCode={}: {}", orderCode, e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // HELPER — Build signature
    // ══════════════════════════════════════════════════════════════
    private String buildSignature(long amount, String description, long orderCode) {
        Map<String, String> data = new TreeMap<>();
        data.put("amount", String.valueOf(amount));
        data.put("cancelUrl", payosConfig.getCancelUrl());
        data.put("description", description);
        data.put("orderCode", String.valueOf(orderCode));
        data.put("returnUrl", payosConfig.getReturnUrl());

        String raw = data.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .reduce((a, b) -> a + "&" + b)
                .orElse("");

        log.debug("Signature raw: [{}]", raw);
        return calculateHmacSha256(raw);
    }

    private String calculateHmacSha256(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    payosConfig.getChecksumKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (RuntimeException e) { throw e; }
        catch (Exception e) { throw new RuntimeException("Lỗi tạo chữ ký PayOS", e); }
    }
}
