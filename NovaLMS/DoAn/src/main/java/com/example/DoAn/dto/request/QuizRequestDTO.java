package com.example.DoAn.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private Integer moduleId;        // cho MODULE_QUIZ
    private Integer lessonId;        // cho LESSON_QUIZ
    private Integer timeLimitMinutes;

    @Min(value = 0, message = "Điểm đạt tối thiểu là 0%")
    @Max(value = 100, message = "Điểm đạt tối đa là 100%")
    private BigDecimal passScore;

    @Min(value = 1, message = "Số lần làm lại tối thiểu là 1")
    private Integer maxAttempts;
    private Integer numberOfQuestions;
    private String questionOrder;    // FIXED | RANDOM
    private Boolean showAnswerAfterSubmit;
    private Boolean isHybridEnabled; // true = cho phép dùng trong Hybrid Placement Test
    private String targetSkill;  // nullable — Grammar|Vocabulary|Listening|Reading|Writing|Speaking

    private String status;           // DRAFT | PUBLISHED | ARCHIVED

    private Boolean isSequential; // true for COURSE_ASSIGNMENT / MODULE_ASSIGNMENT
    private java.util.List<String> skillOrder; // ["LISTENING","READING","SPEAKING","WRITING"]
    private java.util.Map<String, Integer> timeLimitPerSkill; // {"SPEAKING": 2, "WRITING": 30}

    // Schedule fields for COURSE_ASSIGNMENT
    private LocalDateTime openAt;    // null = mở ngay khi isOpen=true
    private LocalDateTime closeAt;   // null = không đóng tự động
    private LocalDateTime deadline; // bắt buộc cho assignment
    private Boolean allowExternalSubmission;
    private String externalSubmissionInstruction;
}
