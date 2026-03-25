package com.example.DoAn.model;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonBackReference;

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

    @Column(name = "duration")
    private String duration;

    @Column(name = "type")
    private String type;

    @Column(name = "content_text")
    private String content_text;

    @Column(name = "quiz_id")
    private Integer quiz_id;



    @Column(name = "order_index")
    private Integer orderIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id")
    @JsonBackReference(value = "module-lessons")
    private Module module;

}