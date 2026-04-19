package com.example.DoAn.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModuleRequestDTO {

    @NotNull(message = "Mã khóa học là bắt buộc")
    private Integer courseId;

    @NotBlank(message = "Tên chương là bắt buộc")
    private String moduleName;

    private Integer orderIndex;

    private String cefrLevel;
}

