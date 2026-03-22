package com.example.DoAn.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizResultId implements Serializable {
    @Column(name = "result_id")
    private Integer resultId;

    @Column(name = "quiz_id")
    private Integer quizId;

    @Column(name = "user_id")
    private Integer userId;
}
