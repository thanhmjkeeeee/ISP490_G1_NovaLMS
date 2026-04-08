package com.example.DoAn.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettingDTO {
    private Integer settingId;
    private String name;
    private String value;
    private String settingType;
    private Integer orderIndex;
    private String status;
    private String description;
}
