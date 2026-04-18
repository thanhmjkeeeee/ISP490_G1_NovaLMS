package com.example.DoAn.controller;

import com.example.DoAn.dto.response.PaymentLinkResponseDTO;
import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.model.Registration;
import com.example.DoAn.model.User;
import com.example.DoAn.repository.PaymentRepository;
import com.example.DoAn.repository.RegistrationRepository;
import com.example.DoAn.repository.UserRepository;
import com.example.DoAn.service.PayosService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PayosService payosService;
    private final PaymentRepository paymentRepository;
    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // ══════════════════════════════════════════════════════════════
    // 1. RETRY PAYMENT
    // ══════════════════════════════════════════════════════════════
    @Operation(summary = "Retry payment — tạo payment link mới")
    @PostMapping("/api/v1/payment/retry")
    @ResponseBody
    public ResponseData<PaymentLinkResponseDTO> retryPayment(
            @RequestParam Integer registrationId,
            Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Vui lòng đăng nhập.");

        Optional<Registration> optReg = registrationRepository.findWithAssociationsById(registrationId);
        if (optReg.isEmpty()) return ResponseData.error(404, "Không tìm thấy đăng ký.");

        Registration reg = optReg.get();
        if (!reg.getUser().getEmail().equals(email)) return ResponseData.error(403, "Bạn không có quyền.");

        Optional<User> optUser = userRepository.findByEmail(email);
        if (optUser.isEmpty()) return ResponseData.error(401, "Không tìm thấy người dùng.");

        try {
            PaymentLinkResponseDTO result = payosService.retryPaymentLink(registrationId, optUser.get());
            return ResponseData.success(result.getMessage(), result);
        } catch (RuntimeException e) {
            return ResponseData.error(400, e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // 2. PAYOS WEBHOOK
    // ══════════════════════════════════════════════════════════════
    @Operation(summary = "PayOS webhook callback")
    @PostMapping("/api/v1/payment/webhook")
    @ResponseBody
    public ResponseEntity<String> handleWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "x-payos-signature", required = false) String signature,
            HttpServletRequest request) {
        try {
            log.info("PayOS webhook received: {}", rawBody);

            if (signature == null || !payosService.verifyWebhookSignature(rawBody, signature)) {
                log.warn("PayOS webhook signature FAILED");
                return ResponseEntity.status(401).body("{\"success\":false,\"message\":\"Invalid signature\"}");
            }

            JsonNode json = objectMapper.readTree(rawBody);
            JsonNode data = json.has("data") ? json.get("data") : null;
            String eventType = json.has("eventType") ? json.get("eventType").asText() : null;

            if (data == null) {
                return ResponseEntity.ok("{\"success\":true}");
            }

            // PayOS: data.id = paymentLinkId, data.orderCode = orderCode
            Long payosPaymentLinkId = data.has("id") ? data.get("id").asLong() : null;
            String orderCodeStr = data.has("orderCode") ? data.get("orderCode").asText() : null;

            log.info("Webhook: eventType={}, payosPaymentLinkId={}, orderCode={}",
                    eventType, payosPaymentLinkId, orderCodeStr);

            if ("PAYMENT_SUCCESS".equals(eventType)) {
                payosService.handlePaymentSuccess(payosPaymentLinkId, orderCodeStr);
            } else if ("PAYMENT_CANCELLED".equals(eventType)) {
                payosService.handlePaymentCancel(payosPaymentLinkId, orderCodeStr);
            } else if ("PAYMENT_EXPIRED".equals(eventType)) {
                // PayOS không gửi webhook này, nhưng xử lý phòng thân
                payosService.handlePaymentCancel(payosPaymentLinkId, orderCodeStr);
            } else {
                log.info("Unhandled webhook event: {}", eventType);
            }

            return ResponseEntity.ok("{\"success\":true}");

        } catch (Exception e) {
            log.error("PayOS webhook processing error", e);
            return ResponseEntity.status(500).body("{\"success\":false,\"message\":\"Internal error\"}");
        }
    }

    // ══════════════════════════════════════════════════════════════
    // 3. RETURN URL — user quay về từ PayOS
    // ══════════════════════════════════════════════════════════════
    @GetMapping("/payment/success")
    public String paymentSuccess(
            @RequestParam(required = false) Long orderId,      // PayOS gọi là orderId
            @RequestParam(required = false) String orderCode,
            @RequestParam(required = false) String id,          // fallback: paymentLinkId
            Principal principal) {
        try {
            // orderId hoặc orderCode từ PayOS là orderCode
            String orderCodeStr = orderId != null ? String.valueOf(orderId)
                    : orderCode != null ? orderCode : null;
            Long paymentLinkId = null;
            try {
                if (id != null) paymentLinkId = Long.parseLong(id);
            } catch (NumberFormatException ignored) {}

            payosService.handlePaymentSuccess(paymentLinkId, orderCodeStr);

            // Sync latest status from PayOS (in case webhook was delayed)
            if (orderCodeStr != null) {
                try {
                    payosService.syncPaymentStatus(Long.parseLong(orderCodeStr));
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            log.error("Error processing payment success return", e);
        }
        return "payment/payment-success";
    }

    @GetMapping("/payment/cancel")
    public String paymentCancel(
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) String orderCode,
            Principal principal) {
        try {
            String orderCodeStr = orderId != null ? String.valueOf(orderId)
                    : orderCode != null ? orderCode : null;
            Long paymentLinkId = null;
            payosService.handlePaymentCancel(paymentLinkId, orderCodeStr);
        } catch (Exception e) {
            log.error("Error processing payment cancel return", e);
        }
        return "payment/payment-cancel";
    }

    // ══════════════════════════════════════════════════════════════
    // HELPER
    // ══════════════════════════════════════════════════════════════
    private String getEmail(Principal principal) {
        if (principal == null) return null;
        if (principal instanceof OAuth2AuthenticationToken t) {
            return t.getPrincipal().getAttribute("email");
        }
        return principal.getName();
    }
}
