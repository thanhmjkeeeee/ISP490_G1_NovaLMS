package com.example.DoAn.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizRequestDTO {

    @NotBlank(message = "Tên quiz không được để trống")
    private String title;

    private String description;

    @NotBlank(message = "Loại quiz không được để trống")
    private String quizCategory;    // ENTRY_TEST | COURSE_QUIZ

    private Integer courseId;        // bắt buộc nếu COURSE_QUIZ
    private Integer timeLimitMinutes;
    private BigDecimal passScore;
    private Integer maxAttempts;
    private String questionOrder;    // FIXED | RANDOM
    private Boolean showAnswerAfterSubmit;
    private String status;           // DRAFT | PUBLISHED | ARCHIVED
}
