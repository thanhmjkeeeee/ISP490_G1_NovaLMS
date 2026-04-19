package com.example.DoAn.dto.request;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AssignmentScheduleRequestDTO {
    private LocalDateTime openAt;
    private LocalDateTime closeAt;
    private LocalDateTime deadline;
}
