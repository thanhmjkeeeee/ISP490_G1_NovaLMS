package com.example.DoAn.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 3. ENTITY REGISTRATION
@Entity
@Table(name = "registration")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Registration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "registration_id")
    private Integer registrationId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "class_id", nullable = false)
    private Clazz clazz;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "registration_time")
    private LocalDateTime registrationTime;

    @Column(name = "registration_price")
    private BigDecimal registrationPrice;

    @Column(length = 50)
    private String status; // Submitted, Approved, Cancelled, Rejected

    @Column(columnDefinition = "TEXT")
    private String note;

    @PrePersist
    protected void onCreate() {
        this.registrationTime = LocalDateTime.now();
        if (this.status == null) this.status = "Submitted";
    }
}
