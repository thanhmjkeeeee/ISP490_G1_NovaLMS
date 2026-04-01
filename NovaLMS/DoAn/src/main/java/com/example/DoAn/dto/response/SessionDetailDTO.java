package com.example.DoAn.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionDetailDTO {
    private Integer sessionId;
    private Integer sessionNo;
    private String topic;
    private String date;
    private String status; // COMPLETED, LEARNING, UPCOMING
    private List<LessonResponseDTO> lessons;
}
