package com.example.DoAn.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EnrollRequestDTO {
    @NotNull(message = "Lớp học không được để trống")
    private Integer classId;

    private Integer courseId;
}