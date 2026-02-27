package com.example.DoAn.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

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

    @Column(name = "title") // Giả định DB có cột title/name, nếu thiếu bạn tự thêm vào DB
    private String title;

    @Column(name = "image_url")
    private String imageUrl;

    private String status;

    @Column(name = "price")
    private Double price;

    @Transient // Đánh dấu đây là cột ảo, không lưu vào Database
    private Integer studentCount = 0;

    @Column(columnDefinition = "TEXT")
    private String description;

    // KẾT NỐI VỚI CATEGORY (Bảng setting)
    @ManyToOne
    @JoinColumn(name = "category_id") // Khớp với tên cột trong file abc.sql
    private Setting category;

    // KẾT NỐI VỚI GIẢNG VIÊN (Bảng user)
    @ManyToOne
    @JoinColumn(name = "expert_id") // Khớp với tên cột trong file abc.sql
    private User expert;

    // Nếu bạn muốn đếm số học viên, có thể thêm map với bảng registration
    @OneToMany(mappedBy = "course")
    private List<Registration> registrations;

    // 1. Thêm để lấy danh sách các lớp thuộc khóa học này
    @OneToMany(mappedBy = "course")
    private List<Clazz> classes;

    // Thêm vào trong class Course
    @OneToMany(mappedBy = "course")
    @OrderBy("orderIndex ASC")
    private List<Module> modules;

    public Integer getStudentCount() { return studentCount; }

    public void setStudentCount(Integer studentCount) { this.studentCount = studentCount; }

    @Override
    public String toString() {
        return "Course(id=" + courseId + ", title=" + title + ", price=" + price + ")";
    }
}