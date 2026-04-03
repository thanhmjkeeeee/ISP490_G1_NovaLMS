package com.example.DoAn.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "session_lesson")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionLesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Mapping tới bảng class_session
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ClassSession session;

    // Mapping tới bảng lesson
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    // Lưu thứ tự hiển thị của bài học trong buổi học đó
    @Column(name = "order_index")
    private Integer orderIndex;
}
