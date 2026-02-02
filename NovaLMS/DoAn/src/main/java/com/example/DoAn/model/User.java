package com.example.DoAn.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    // --- CÁC TRƯỜNG MỚI THÊM VÀO ---

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "phone_number")
    private String phone;

    @Column(length = 20)
    private String gender; // Male, Female, Other

    @Column(length = 100)
    private String city;

    @Column(name = "avatar_url")
    private String avatarUrl;

    // --------------------------------

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(length = 20)
    private String status;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Xác định provider (Google hay Local)
    @Column(name = "auth_provider")
    private String authProvider;

    @Column(name = "provider_id")
    private String providerId;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "Active";
        }
        if (this.authProvider == null) {
            this.authProvider = "LOCAL";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}