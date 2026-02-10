package com.example.DoAn.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(length = 20)
    private String mobile;

    @Column(nullable = false)
    private String password; // DB mới dùng 'password', không phải 'password_hash'

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Setting role; // Trỏ tới bảng setting

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(length = 20)
    private String status; // Active, Inactive, Blocked

    @Column(columnDefinition = "TEXT")
    private String note;

    // --- CÁC TRƯỜNG THÊM MỚI (Hibernate tự sinh thêm) ---
    // Để hỗ trợ Google Login và thông tin bổ sung từ form đăng ký

    @Column(name = "auth_provider")
    private String authProvider; // LOCAL, GOOGLE

    @Column(name = "provider_id")
    private String providerId; // Google ID

    @Column(length = 10)
    private String gender; // Male, Female

    @Column(length = 100)
    private String city;

    // Callbacks
    @PrePersist
    protected void onCreate() {
        if (this.status == null) this.status = "Active";
        if (this.authProvider == null) this.authProvider = "LOCAL";
    }
}