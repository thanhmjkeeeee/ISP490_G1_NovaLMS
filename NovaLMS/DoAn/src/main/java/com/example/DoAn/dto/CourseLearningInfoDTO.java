package com.example.DoAn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseLearningInfoDTO {

    private Long courseId;
    private String title;
    private String description;
    private int progressPercent;

    private String teacherName;
    private String teacherAvatar;

    private String className;
    private String schedule;
    private String liveMeetingLink;

    private List<ModuleDTO> modules;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModuleDTO {

        private Long moduleId;
        private String moduleTitle;
        private int totalLessons;

        private List<LessonDTO> lessons;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LessonDTO {

        private Long lessonId;
        private String lessonTitle;
        private String type;
        private String duration;
        private boolean completed;
        private boolean locked;
    }
}