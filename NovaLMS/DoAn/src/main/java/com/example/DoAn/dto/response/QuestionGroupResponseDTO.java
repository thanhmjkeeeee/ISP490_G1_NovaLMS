package com.example.DoAn.dto.response;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionGroupResponseDTO {
    private Integer groupId;
    private String groupContent;
    private String audioUrl;
    private String imageUrl;
    private String skill;
    private String cefrLevel;
    private String topic;
    private String explanation;
    private String status;
    private List<QuestionResponseDTO> questions;
    private java.time.LocalDateTime createdAt;
}
