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

    @NotBlank(message = "Group content is required")
    private String groupContent;

    private String audioUrl;
    private String imageUrl;

    @NotBlank(message = "Skill is required")
    private String skill;

    @NotBlank(message = "CEFR level is required")
    private String cefrLevel;

    private String topic;
    private String explanation;
    private String status;

    @Valid
    private List<QuestionRequestDTO> questions;
}
