package com.example.DoAn.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// 1. ENTITY COURSE
@Entity
@Table(name = "course")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private Integer courseId;

    @Column(name = "title") // Giả định DB có cột title/name, nếu thiếu bạn tự thêm vào DB
    private String title;

    @Column(name = "image_url")
    private String imageUrl;

    private String status; // Active, Inactive

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Setting category;
}

