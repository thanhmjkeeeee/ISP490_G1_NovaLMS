package com.example.DoAn.model;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

@Entity
@Table(name = "quiz_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(QuizConfig.QuizConfigId.class)
public class QuizConfig {

    @Id
    @Column(name = "quiz_id")
    private Integer quizId;

    @Id
    @Column(name = "module_id")
    private Integer moduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", insertable = false, updatable = false)
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", insertable = false, updatable = false)
    private Module module;

    @Column(name = "num_of_question")
    private Integer numOfQuestion;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizConfigId implements Serializable {
        private Integer quizId;
        private Integer moduleId;
    }
}
