package com.example.DoAn.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "placement_test_answer")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlacementTestAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "placement_result_id", nullable = false)
    private PlacementTestResult placementTestResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "answered_options", columnDefinition = "TEXT")
    private String answeredOptions;

    @Column(name = "is_correct")
    private Boolean isCorrect;
}
