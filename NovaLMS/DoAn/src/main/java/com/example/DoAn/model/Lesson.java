package com.example.DoAn.model;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonBackReference;
import java.util.List;

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

    @Column(name = "content_text", columnDefinition = "TEXT")
    private String content_text;

    @Column(name = "quiz_id")
    private Integer quiz_id;

    @Column(name = "order_index")
    private Integer orderIndex;

    @Column(name = "allow_download", columnDefinition = "boolean default true")
    private Boolean allowDownload = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id")
    @JsonBackReference(value = "module-lessons")
    private Module module;

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SessionLesson> sessionLessons;

    // Helper methods for compatibility with StudentServiceImpl calls
    public String getLessonType() {
        return this.type;
    }

    public String getTitle() {
        return this.lessonName;
    }
    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<QuizAssignment> quizAssignments;

}