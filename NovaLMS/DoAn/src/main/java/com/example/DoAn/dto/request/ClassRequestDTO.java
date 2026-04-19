package com.example.DoAn.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;

@Getter
@Setter
public class ClassRequestDTO implements Serializable {
    @NotBlank(message = "Tên lớp không được để trống")
    private String className;

    @NotNull(message = "Mã khóa học là bắt buộc")
    private Integer courseId;

    private Integer teacherId;

    @NotBlank(message = "Ngày bắt đầu là bắt buộc")
    private String startDate;

    private String endDate;
    private String status;
    private String schedule;
    private String slotTime;
    private Integer numberOfSessions;
    private String meetLink;
    private String description;
}