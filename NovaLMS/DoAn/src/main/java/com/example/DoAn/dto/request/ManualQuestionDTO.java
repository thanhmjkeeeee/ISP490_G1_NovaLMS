package com.example.DoAn.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManualQuestionDTO {

    @NotBlank(message = "Nội dung câu hỏi là bắt buộc")
    @Size(min = 10, max = 2000, message = "Nội dung phải từ 10 đến 2000 ký tự")
    private String content;

    @NotBlank(message = "Loại câu hỏi là bắt buộc")
    private String questionType; // "MULTIPLE_CHOICE_SINGLE", "MULTIPLE_CHOICE_MULTI", "FILL_IN_BLANK", "MATCHING", "WRITING", "SPEAKING"

    private String explanation;

    // For MC / FILL_IN_BLANK / MATCHING
    private List<AnswerOptionInput> options;

    // For MATCHING pairs: each entry is "leftIndex:rightIndex" e.g. "0:2"
    private List<String> matchingPairs;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AnswerOptionInput {
        private String title;
        private Boolean correct; // true for correct answer(s)
        private String matchTarget; // non-null for left items in MATCHING type
    }
}
