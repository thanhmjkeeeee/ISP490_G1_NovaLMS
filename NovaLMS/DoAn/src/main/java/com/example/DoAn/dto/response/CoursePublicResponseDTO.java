package com.example.DoAn.dto.response;

import java.time.LocalDate; // Nhớ import thư viện này nếu dùng LocalDate
import java.util.List;

public record CoursePublicResponseDTO(
        Integer courseId,
        String courseName,
        String description,
        Double price,
        Double sale,
        String categoryName,
        long studentCount,
        String imageUrl,
        String status,
        ExpertResponseDTO expert,
        List<ModuleResponseDTO> curriculum,
        List<ClassResponseDTO> activeClasses
) {
    public String getCourseDuration() {
        if (activeClasses == null || activeClasses.isEmpty()) return "N/A";

        var firstClass = activeClasses.get(0);
        if (firstClass.startDate() != null && firstClass.endDate() != null) {
            long days = java.time.Duration.between(firstClass.startDate(), firstClass.endDate()).toDays();
            return days + " ngày";
        }
        return "Trọn đời";
    }

    public record ExpertResponseDTO(
            String fullName,
            String avatarUrl
    ) {}

    public record ModuleResponseDTO(
            Integer moduleId,
            String moduleName,
            Integer orderIndex,
            List<LessonResponseDTO> lessons // THÊM MỚI: Danh sách bài học
    ) {}

    // THÊM MỚI: Record cho bài học
    public record LessonResponseDTO(
            Integer lessonId,
            String lessonName,
            String duration // Đổi từ Integer sang String cho khớp Entity Lesson
    ) {}

    public record ClassResponseDTO(
            Integer classId,
            String className,
            String status,
            String teacherName,
            String schedule,
            String slotTime,
            java.time.LocalDateTime startDate, // Sửa thành LocalDateTime
            java.time.LocalDateTime endDate    // Sửa thành LocalDateTime
    ) {}
}