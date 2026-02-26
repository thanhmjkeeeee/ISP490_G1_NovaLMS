package com.example.DoAn.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MyCourseDTO {
    private Integer courseId;
    private String title;
    private String description;
    private String imageUrl;
    private String className;
}