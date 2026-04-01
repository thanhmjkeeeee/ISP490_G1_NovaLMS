package com.example.DoAn.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "quiz_assignment",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"quiz_id", "lesson_id"}),
        @UniqueConstraint(columnNames = {"quiz_id", "module_id"})
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignment_id")
    private Integer assignmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = true)
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = true)
    private Module module;

    @Column(name = "order_index")
    private Integer orderIndex;
}
