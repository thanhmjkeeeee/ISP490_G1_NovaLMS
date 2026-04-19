package com.example.DoAn.dto.request;

import com.example.DoAn.dto.response.AIGenerateResponseDTO;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIImportGroupRequestDTO {

    @NotNull(message = "Nội dung passage là bắt buộc")
    private String passage;

    private String audioUrl;
    private String imageUrl;
    private String skill;
    private String cefrLevel;
    private String topic;
    private String explanation;

    @Builder.Default
    private String status = "DRAFT";

    @NotNull(message = "Danh sách câu hỏi là bắt buộc")
    private List<AIGenerateResponseDTO.QuestionDTO> questions;
}
