package com.example.DoAn.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import java.io.Serializable;

@Getter
public class CourseRequestDTO implements Serializable {
    private String courseCode; // optional — used as course.title

    @NotBlank(message = "Tên khóa học không được để trống")
    private String courseName;

    private String description;

    @NotNull(message = "Học phí là bắt buộc")
    @Min(value = 0, message = "Học phí phải lớn hơn hoặc bằng 0")
    private Double price;

    private Double sale;

    private Integer numberOfSessions;

    private String avatar;

    private String status;

    private Integer categoryId;

    private Integer expertId;
}