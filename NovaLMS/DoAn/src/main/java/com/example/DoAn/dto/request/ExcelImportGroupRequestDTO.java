package com.example.DoAn.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExcelImportGroupRequestDTO {

    @NotNull
    private ExcelQuestionGroupDTO group;
}
