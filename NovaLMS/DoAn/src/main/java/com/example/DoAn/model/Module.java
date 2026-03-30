package com.example.DoAn.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Entity
@Table(name = "module")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Module {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "module_id")
    private Integer moduleId;

    @Column(name = "module_name")
    private String moduleName;

    @Column(name = "order_index") // Thứ tự chương: 1, 2, 3...
    private Integer orderIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    @JsonBackReference(value = "course-modules")
    private Course course;

    // Kết nối với danh sách bài học
    @OneToMany(mappedBy = "module", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference(value = "module-lessons")
    @OrderBy("orderIndex ASC")
    @Fetch(FetchMode.SUBSELECT)
    private List<Lesson> lessons;

    @OneToMany(mappedBy = "module", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<QuizAssignment> quizAssignments;
}