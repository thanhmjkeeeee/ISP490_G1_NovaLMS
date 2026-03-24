package com.example.DoAn.dto.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentLinkResponseDTO {

    private Integer registrationId;
    private Long payosPaymentLinkId;
    private String checkoutUrl;   // Trang thanh toán PayOS — chuyển hướng user đến đây
    private String paymentUrl;   // Alias cho checkoutUrl
    private String qrCode;       // URL hình ảnh mã QR
    private String status;
    private String message;
}
