package com.example.DoAn.dto.response;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIGenerateGroupResponseDTO {

    private String passage;
    private String audioUrl;
    private String imageUrl;
    private String skill;
    private String cefrLevel;
    private String topic;
    private String explanation;
    private List<AIGenerateResponseDTO.QuestionDTO> questions;
    private String warning;
}
