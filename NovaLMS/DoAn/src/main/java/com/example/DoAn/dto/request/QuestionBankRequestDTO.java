package com.example.DoAn.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionBankRequestDTO {

    @NotBlank(message = "Nội dung câu hỏi không được để trống")
    private String content;

    @NotBlank(message = "Loại câu hỏi không được để trống")
    private String questionType;   // MULTIPLE_CHOICE_SINGLE, MULTIPLE_CHOICE_MULTI, FILL_IN_BLANK, MATCHING, WRITING, SPEAKING

    @NotBlank(message = "Kỹ năng không được để trống")
    private String skill;          // LISTENING, READING, WRITING, SPEAKING

    @NotBlank(message = "Cấp độ CEFR không được để trống")
    private String cefrLevel;      // A1, A2, B1, B2, C1, C2

    private String topic;
    private String tags;           // comma-separated
    private String explanation;
    private String audioUrl;
    private String imageUrl;
    private String status;         // DRAFT, PUBLISHED, ARCHIVED

    @Valid
    private List<AnswerOptionDTO> options;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AnswerOptionDTO {
        @NotBlank(message = "Nội dung đáp án không được để trống")
        private String title;
        private Boolean correct;
        private Integer orderIndex;
        private String matchTarget;    // cho dạng Matching
    }
}
