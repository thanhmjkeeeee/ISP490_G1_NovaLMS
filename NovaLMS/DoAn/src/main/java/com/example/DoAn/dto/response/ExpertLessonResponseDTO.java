package com.example.DoAn.dto.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpertLessonResponseDTO {
    private Integer lessonId;
    private Integer moduleId;
    private String moduleName;
    private String lessonName;
    private String type;
    private String videoUrl;
    private String contentText;
    private Integer quizId;
    private String duration;
    private Integer orderIndex;
}
