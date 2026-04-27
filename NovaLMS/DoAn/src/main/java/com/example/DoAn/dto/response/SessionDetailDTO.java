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
    private Integer sessionNumber;
    private String startTime;
    private String endTime;
    private Integer dayOfWeek; // 1-7 (Mon-Sun)
    private Integer slotNumber; // 1-5
    private String status; // COMPLETED, LEARNING, UPCOMING
    private String topic;
    private String sessionDate;
    private String meetLink;
    private String className;
    private String courseName;
    private boolean isLocked;
    private List<LessonResponseDTO> materials; // For DOC, VIDEO
    private List<LessonResponseDTO> quizzes;   // For QUIZ
}
