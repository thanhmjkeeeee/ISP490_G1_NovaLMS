package com.example.DoAn.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "payos")
@Data
public class PayosConfig {

    private String clientId;
    private String apiKey;
    private String checksumKey;
    private String baseUrl;
    private String frontendUrl;

    /**
     * Return URL after successful payment.
     * Cancel/return URL is handled separately.
     */
    public String getReturnUrl() {
        return frontendUrl + "/payment/success";
    }

    public String getCancelUrl() {
        return frontendUrl + "/payment/cancel";
    }

    /** Đường dẫn getPaymentLinkInfo theo orderCode */
    public String getPaymentLinkInfoUrl(Long orderCode) {
        return baseUrl + "/v2/payment-requests/" + orderCode;
    }
}
