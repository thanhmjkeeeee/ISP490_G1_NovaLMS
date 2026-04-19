package com.example.DoAn.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionRequestDTO {

    private Integer moduleId;
    private Integer questionId;  // Dùng khi update câu hỏi con trong Group

    @NotBlank(message = "Nội dung câu hỏi là bắt buộc")
    private String content;

    @NotBlank(message = "Loại câu hỏi là bắt buộc")
    private String questionType;

    @NotBlank(message = "Kỹ năng là bắt buộc")
    private String skill;

    @NotBlank(message = "Cấp độ CEFR là bắt buộc")
    private String cefrLevel;

    private String topic;
    private String tags;
    private String explanation;
    private String audioUrl;
    private String imageUrl;
    private String status;

    @Valid
    private List<AnswerOptionDTO> options;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AnswerOptionDTO {
        @NotBlank(message = "Nội dung phương án là bắt buộc")
        private String title;

        private Boolean correct;
        private String matchTarget;
    }
}
