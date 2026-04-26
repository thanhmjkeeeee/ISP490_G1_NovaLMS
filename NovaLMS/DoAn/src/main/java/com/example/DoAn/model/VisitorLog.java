package com.example.DoAn.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "visitor_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitorLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "visitor_token", unique = true, nullable = false, length = 50)
    private String visitorToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "first_visit")
    private LocalDateTime firstVisit;

    @Column(name = "last_visit")
    private LocalDateTime lastVisit;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @PrePersist
    protected void onCreate() {
        this.firstVisit = LocalDateTime.now();
        this.lastVisit = LocalDateTime.now();
    }
}
