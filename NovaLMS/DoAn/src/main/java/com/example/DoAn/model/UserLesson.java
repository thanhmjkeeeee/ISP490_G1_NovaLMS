package com.example.DoAn.model;

import jakarta.persistence.*;
import lombok.*;

//UserLesson
//→ Là “khuôn ID”, gom userId và lessonId thành 1 object duy nhất.
//→ JPA hiểu rằng: muốn tìm 1 bản ghi thì phải có đủ 2 giá trị này.

@Entity
@Table(name = "user_lesson")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLesson {

    @EmbeddedId
    private UserLessonId id;


    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @MapsId("lessonId")
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    @Column(name = "status")
    private String status;
}