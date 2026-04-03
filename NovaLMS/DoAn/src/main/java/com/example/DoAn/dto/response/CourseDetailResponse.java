package com.example.DoAn.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.io.Serializable;

@Getter
@Builder
public class CourseDetailResponse implements Serializable {
    private Integer courseId;
    private String courseCode;
    private String courseName;
    private String status;
    private String description;
    private Double price;
    private Double sale;
    private String avatar;
    private Integer categoryId;
    private String categoryName;
    private Integer expertId;
    private String expertName;
    private Long registrationCount;
    private Integer numberOfSessions;
}