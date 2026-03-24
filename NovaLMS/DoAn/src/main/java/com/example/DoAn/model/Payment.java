package com.example.DoAn.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** orderCode gửi sang PayOS — unique */
    @Column(name = "payos_order_code", unique = true, nullable = false)
    private Long payosOrderCode;

    /** id từ PayOS trả về trong response (data.id) — set sau khi tạo link thành công */
    @Column(name = "payos_payment_link_id")
    private Long payosPaymentLinkId;

    /** FK sang registration — mỗi registration có tối đa 1 payment */
    @Column(name = "registration_id", nullable = false)
    private Integer registrationId;

    /** Trạng thái thanh toán: PENDING | PAID | CANCELLED | EXPIRED | FAILED */
    @Column(length = 50)
    private String status;

    /** Số tiền VND đã gửi sang PayOS */
    @Column(name = "amount")
    private BigDecimal amount;

    /** Mô tả đã gửi sang PayOS */
    private String description;

    /** URL thanh toán PayOS — redirect user tới đây */
    @Column(name = "checkout_url", length = 500)
    private String checkoutUrl;

    /** QR code từ PayOS */
    @Column(name = "qr_code", length = 500)
    private String qrCode;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = "PENDING";
    }
}
