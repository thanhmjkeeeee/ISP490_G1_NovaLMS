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
    private Integer classId;         // gắn quiz với lớp học (cho teacher tạo quiz)
    private Integer timeLimitMinutes;
    private BigDecimal passScore;
    private Integer maxAttempts;
    private Integer numberOfQuestions;
    private String questionOrder;    // FIXED | RANDOM
    private Boolean showAnswerAfterSubmit;
    private Boolean isHybridEnabled; // true = cho phép dùng trong Hybrid Placement Test
    private String targetSkill;  // nullable — Grammar|Vocabulary|Listening|Reading|Writing|Speaking

    private String status;           // DRAFT | PUBLISHED | ARCHIVED
}
