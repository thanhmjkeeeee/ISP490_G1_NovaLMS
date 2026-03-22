package com.example.DoAn.dto.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModuleResponseDTO {
    private Integer moduleId;
    private Integer courseId;
    private String moduleName;
    private Integer orderIndex;
    private int lessonCount;
    private int questionCount;
}
