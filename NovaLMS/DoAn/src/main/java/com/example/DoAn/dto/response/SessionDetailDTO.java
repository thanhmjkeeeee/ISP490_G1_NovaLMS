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
    private String startTime;
    private String endTime;
    private Integer dayOfWeek; // 1-7 (Mon-Sun)
    private Integer slotNumber; // 1-5
    private String status; // COMPLETED, LEARNING, UPCOMING
    private String topic;
    private String date;
    private String meetLink;
    private String className;
    private String courseName;
    private List<LessonResponseDTO> materials; // For DOC, VIDEO
    private List<LessonResponseDTO> quizzes;   // For QUIZ
}
