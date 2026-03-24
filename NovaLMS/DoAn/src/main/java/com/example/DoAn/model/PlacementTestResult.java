package com.example.DoAn.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "placement_test_result")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlacementTestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "guest_session_id", length = 100)
    private String guestSessionId;

    @Column(name = "guest_name", length = 100)
    private String guestName;

    @Column(name = "guest_email", length = 100)
    private String guestEmail;

    @Column(name = "score")
    private Integer score;

    @Column(name = "correct_rate", precision = 5, scale = 2)
    private BigDecimal correctRate;

    @Column(name = "passed")
    private Boolean passed;

    @Column(name = "suggested_level", length = 10)
    private String suggestedLevel;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @OneToMany(mappedBy = "placementTestResult", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PlacementTestAnswer> answers;

    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
    }
}
