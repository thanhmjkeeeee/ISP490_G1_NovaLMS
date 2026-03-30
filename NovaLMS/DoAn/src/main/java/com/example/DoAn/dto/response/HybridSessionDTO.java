package com.example.DoAn.dto.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HybridSessionDTO {
    private Integer sessionId;
    private Integer totalQuizzes;
    private Integer completedQuizzes;
    private String status;
    private String redirectUrl;
}
