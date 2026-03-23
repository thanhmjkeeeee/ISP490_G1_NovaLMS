package com.example.DoAn.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "answer_option")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnswerOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "answer_option_id")
    private Integer answerOptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "title", columnDefinition = "TEXT")
    private String title;

    @Column(name = "correct_answer")
    private Boolean correctAnswer;

    @Column(name = "order_index")
    private Integer orderIndex;

    // Dùng cho dạng Matching — chứa nội dung cần ghép nối
    @Column(name = "match_target", length = 500)
    private String matchTarget;
}
