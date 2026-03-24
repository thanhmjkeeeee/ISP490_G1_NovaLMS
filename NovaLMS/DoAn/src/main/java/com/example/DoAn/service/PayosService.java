package com.example.DoAn.service;

import com.example.DoAn.dto.response.PaymentLinkResponseDTO;
import com.example.DoAn.model.Payment;
import com.example.DoAn.model.Registration;
import com.example.DoAn.model.User;

/**
 * Service interface for PayOS payment gateway operations.
 */
public interface PayosService {

    /**
     * Tạo payment link PayOS cho registration.
     * Tạo Payment record → gọi PayOS API → cập nhật Payment + Registration.
     *
     * @param registration registration đã được tạo (status=Submitted)
     * @param user user đang thanh toán
     * @return PaymentLinkResponseDTO chứa checkoutUrl
     */
    PaymentLinkResponseDTO createPaymentLink(Registration registration, User user) throws RuntimeException;

    /**
     * Retry — tạo payment link mới cho registration đã thất bại.
     * Tạo Payment MỚI (Payment cũ giữ nguyên để audit).
     */
    PaymentLinkResponseDTO retryPaymentLink(Integer registrationId, User user) throws RuntimeException;

    /**
     * Xác minh chữ ký webhook PayOS.
     */
    boolean verifyWebhookSignature(String data, String signature);

    /**
     * Xử lý PAYMENT_SUCCESS từ webhook hoặc return URL.
     * Idempotent — gọi nhiều lần vẫn cho kết quả đúng.
     */
    void handlePaymentSuccess(Long payosPaymentLinkId, String orderCodeStr);

    /**
     * Xử lý PAYMENT_CANCELLED từ webhook hoặc return URL.
     */
    void handlePaymentCancel(Long payosPaymentLinkId, String orderCodeStr);

    /**
     * Đồng bộ trạng thái từ PayOS bằng getPaymentLinkInfo.
     * Dùng cho CANCELLED / EXPIRED (PayOS không gửi webhook cho các case này).
     * Gọi bất đồng bộ hoặc khi student xem danh sách đăng ký.
     */
    void syncPaymentStatus(Long orderCode);

    /**
     * Tạo Payment mới cho retry (internal use).
     * KHÔNG gọi PayOS API — chỉ tạo record và cập nhật registration.
     * Dùng khi muốn tạo Payment PENDING trước, rồi gọi PayOS riêng.
     */
    PaymentLinkResponseDTO createPaymentRecordOnly(Registration registration, User user) throws RuntimeException;
}
