package com.example.DoAn.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

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

    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;

    // Kết nối với danh sách bài học
    @OneToMany(mappedBy = "module", cascade = CascadeType.ALL)
    @OrderBy("orderIndex ASC")
    private List<Lesson> lessons;
}