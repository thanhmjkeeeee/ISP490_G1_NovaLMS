package com.example.DoAn.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "quiz_question")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quiz_question_id")
    private Integer quizQuestionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private QuestionGroup questionGroup;

    @Column(name = "order_index")
    private Integer orderIndex;

    // LISTENING | READING | SPEAKING | WRITING — set for sequential assignments
    @Column(name = "skill", length = 20)
    private String skill;

    // Điểm cho câu hỏi này trong quiz
    @Column(name = "points", precision = 5, scale = 2)
    private BigDecimal points;
}
