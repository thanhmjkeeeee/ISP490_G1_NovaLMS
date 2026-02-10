package com.example.DoAn.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
@Getter // Sử dụng Getter/Setter thay vì @Data để kiểm soát tốt hơn
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String token;

    @Column(name = "expiry_datetime", nullable = false)
    private LocalDateTime expiryDatetime;

    @Column(name = "is_used")
    private boolean isUsed = false; // Chuyển sang primitive boolean

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDatetime);
    }

    // Lombok sẽ tự tạo:
    // public boolean isUsed() { return isUsed; }
    // public void setUsed(boolean used) { this.isUsed = used; }

    public boolean isUsed() {
        return isUsed;
    }

    public void setUsed(boolean used) {
        isUsed = used;
    }
}