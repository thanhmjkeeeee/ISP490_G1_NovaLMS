package com.example.DoAn.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.List;

import java.time.LocalDateTime;

// 2. ENTITY CLASS
@Entity
@Table(name = "class")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Clazz { // Tránh từ khóa 'Class' của Java
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "class_id")
    private Integer classId;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne
    @JoinColumn(name = "teacher_id")
    private User teacher;

    @Column(name = "class_name") // Tên lớp: ví dụ IELTS-K15
    private String className;


    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    @Column(name = "start_date")
    private LocalDateTime startDate;

    private String status; // Pending, Open, Closed

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    @Column(name = "end_date")
    private LocalDateTime endDate;

    // Thêm quan hệ này để biết lớp này có bao nhiêu học viên đã đăng ký
    @OneToMany(mappedBy = "clazz")
    private List<Registration> registrations;

    @Column(name = "schedule") // Ví dụ: "Mon-Wed-Fri"
    private String schedule;

    @Column(name = "slot_time")
    private String slotTime;
}
