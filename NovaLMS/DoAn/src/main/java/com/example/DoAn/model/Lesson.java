package com.example.DoAn.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lesson")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lesson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lesson_id")
    private Integer lessonId;

    @Column(name = "lesson_name")
    private String lessonName;

    @Column(name = "video_url")
    private String videoUrl;

    @Column(name = "duration") // Thời lượng bài học (phút)
    private Integer duration;

    @Column(name = "order_index") // Thứ tự bài học trong chương
    private Integer orderIndex;

    @ManyToOne
    @JoinColumn(name = "module_id")
    private Module module;
}