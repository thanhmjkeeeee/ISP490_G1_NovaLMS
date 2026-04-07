package com.example.DoAn.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentPublishDTO {
    private String status; // PUBLISHED or ARCHIVED
}
