package com.example.DoAn.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RescheduleResponseDTO {
    private Integer id;
    private String oldDate;      // Chuyển sang String để Frontend dễ parse Date
    private String oldStartTime;
    private String newDate;      // Chuyển sang String để Frontend dễ parse Date
    private String newStartTime;
    private String reason;
    private String managerNote;
    private String status;
    private LocalDateTime createdAt;

    // Cấu trúc nested (lồng nhau) khớp 100% với file HTML của bạn
    private CreatorDTO createdBy;
    private SessionDTO session;

    @Data
    @Builder
    public static class CreatorDTO {
        private String fullName;
        private String email;
    }

    @Data
    @Builder
    public static class SessionDTO {
        private Integer sessionNumber;
        private ClassDTO clazz;
    }

    @Data
    @Builder
    public static class ClassDTO {
        private String className;
    }
}