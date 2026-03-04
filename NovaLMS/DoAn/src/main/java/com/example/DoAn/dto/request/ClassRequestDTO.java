package com.example.DoAn.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;

@Getter
@Setter
public class ClassRequestDTO implements Serializable {
    @NotBlank(message = "Class name must not be blank")
    private String className;

    @NotNull(message = "Course ID must not be null")
    private Integer courseId;

    private Integer teacherId;

    @NotBlank(message = "Start date is required")
    private String startDate;

    private String endDate;
    private String status;
    private String schedule;
    private String slotTime;
}