package com.example.DoAn.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "user_lesson")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLesson {

    @EmbeddedId
    private UserLessonId id = new UserLessonId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("lessonId")
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    @Column(name = "status")
    private String status;

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserLessonId implements Serializable {
        @Column(name = "user_id")
        private Integer userId;

        @Column(name = "lesson_id")
        private Integer lessonId;
    }
}