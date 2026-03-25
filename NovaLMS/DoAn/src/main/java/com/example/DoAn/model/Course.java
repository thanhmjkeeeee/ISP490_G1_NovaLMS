package com.example.DoAn.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonManagedReference;

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

    @Column(name = "course_name")
    private String courseName;

    @Column(name = "title") // Giả định DB có cột title/name, nếu thiếu bạn tự thêm vào DB
    private String title;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "level_tag", length = 10)
    private String levelTag; // A1, A2, B1, B2, C1, C2

    private String status;

    @Column(name = "price")
    private Double price;

    @Column(name = "sale")
    private Double sale;

    @Column(name = "avatar")
    private String avatar;

    @Transient // Đánh dấu đây là cột ảo, không lưu vào Database
    private Integer studentCount = 0;

    @Column(columnDefinition = "TEXT")
    private String description;

    // KẾT NỐI VỚI CATEGORY (Bảng setting)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id") // Khớp với tên cột trong file abc.sql
    private Setting category;

    // KẾT NỐI VỚI GIẢNG VIÊN (Bảng user)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expert_id") // Khớp với tên cột trong file abc.sql
    private User expert;

    // Nếu bạn muốn đếm số học viên, có thể thêm map với bảng registration
    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    @JsonManagedReference(value = "course-registrations")
    private List<Registration> registrations;

    // 1. Thêm để lấy danh sách các lớp thuộc khóa học này
    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    @JsonManagedReference(value = "course-classes")
    private List<Clazz> classes;

    // Thêm vào trong class Course
    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    @JsonManagedReference(value = "course-modules")
    @OrderBy("orderIndex ASC")
    private List<Module> modules;

    public Integer getStudentCount() { return studentCount; }

    public void setStudentCount(Integer studentCount) { this.studentCount = studentCount; }

    @Override
    public String toString() {
        return "Course(id=" + courseId + ", title=" + title + ", price=" + price + ")";
    }
}