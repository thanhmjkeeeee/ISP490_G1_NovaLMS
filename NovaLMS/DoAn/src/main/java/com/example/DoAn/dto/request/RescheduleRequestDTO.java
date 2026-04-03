package com.example.DoAn.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RescheduleRequestDTO {
    private String newDate;      // From frontend (YYYY-MM-DD)
    private String newStartTime;
    private String reason;
}
