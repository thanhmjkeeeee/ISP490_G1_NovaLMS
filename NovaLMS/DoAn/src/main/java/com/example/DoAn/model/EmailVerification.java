package com.example.DoAn.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_verifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 255) // Định nghĩa độ dài cho email
    private String email;

    @Column(name = "verification_code", nullable = false, length = 10) // Mã xác nhận chỉ cần ngắn (6-10 ký tự)
    private String verificationCode;

    @Column(name = "expiry_time", nullable = false)
    private LocalDateTime expiryTime;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryTime);
    }
}