package com.example.DoAn.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
@Data
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
    private Boolean isUsed;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDatetime);
    }
}
