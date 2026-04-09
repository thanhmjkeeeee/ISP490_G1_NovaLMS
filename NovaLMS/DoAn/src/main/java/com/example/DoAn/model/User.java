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

    @ManyToOne(fetch = FetchType.LAZY)
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

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Setting getRole() { return role; }
    public void setRole(Setting role) { this.role = role; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getAuthProvider() { return authProvider; }
    public void setAuthProvider(String authProvider) { this.authProvider = authProvider; }

    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    // Callbacks
    @PrePersist
    protected void onCreate() {
        if (this.status == null) this.status = "Active";
        if (this.authProvider == null) this.authProvider = "LOCAL";
    }
}