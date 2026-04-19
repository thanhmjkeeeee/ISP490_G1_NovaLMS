package com.example.DoAn.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionGroupRequestDTO {

    @NotBlank(message = "Nội dung nhóm câu hỏi là bắt buộc")
    private String groupContent;

    private String audioUrl;
    private String imageUrl;

    @NotBlank(message = "Kỹ năng là bắt buộc")
    private String skill;

    @NotBlank(message = "Cấp độ CEFR là bắt buộc")
    private String cefrLevel;

    private String topic;
    private String explanation;
    private String status;

    @Valid
    private List<QuestionRequestDTO> questions;
}
