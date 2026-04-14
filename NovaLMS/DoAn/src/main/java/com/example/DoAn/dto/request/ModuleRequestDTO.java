package com.example.DoAn.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModuleRequestDTO {

    @NotNull(message = "courseId is required")
    private Integer courseId;

    @NotBlank(message = "moduleName is required")
    private String moduleName;

    private Integer orderIndex;

    private String cefrLevel;
}

