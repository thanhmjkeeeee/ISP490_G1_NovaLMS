package com.example.DoAn.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;

//UserLesson.java (@Entity)
//→ Là bảng chính chứa dữ liệu (ví dụ: status = Completed).
//→ Dùng @EmbeddedId để nhúng cái “khuôn ID” vào.
//→ Dùng @MapsId để liên kết object User và Lesson với 2 giá trị ID bên trong.

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserLessonId implements Serializable {

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "lesson_id")
    private Integer lessonId;
}