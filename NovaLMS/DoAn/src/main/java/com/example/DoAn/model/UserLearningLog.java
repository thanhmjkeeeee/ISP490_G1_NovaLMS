package com.example.DoAn.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "user_learning_log", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_date", columnNames = {"user_id", "learn_date"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLearningLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "learn_date", nullable = false)
    private LocalDate learnDate;

    @Column(name = "time_spent_seconds", columnDefinition = "int default 0")
    private Integer timeSpentSeconds;
}
