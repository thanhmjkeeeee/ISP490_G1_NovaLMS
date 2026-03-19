package com.example.DoAn.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import java.io.Serializable;

@Getter
public class CourseRequestDTO implements Serializable {
    @NotBlank(message = "Course code must not be blank")
    private String courseCode;

    @NotBlank(message = "Course name must not be blank")
    private String courseName;

    private String description;

    @NotNull(message = "Price must not be null")
    @Min(value = 0, message = "Price must be greater than or equal to 0")
    private Double price;

    private Double sale;

    private String avatar;

    private String status;

    private Integer categoryId;
}