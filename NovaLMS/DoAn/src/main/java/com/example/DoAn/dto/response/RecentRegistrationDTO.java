package com.example.DoAn.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class RecentRegistrationDTO {
    private Integer registrationId;
    private String studentName;
    private String studentEmail;
    private String courseName;
    private LocalDateTime enrolledAt;
    private String status;
}
