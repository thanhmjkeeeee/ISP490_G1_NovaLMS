package com.example.DoAn.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @JsonBackReference(value = "course-classes")
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
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
    @OneToMany(mappedBy = "clazz", fetch = FetchType.LAZY)
    @JsonManagedReference(value = "clazz-registrations")
    private List<Registration> registrations;

    @Column(name = "schedule") // Ví dụ: "Mon-Wed-Fri"
    private String schedule;

    @Column(name = "slot_time")
    private String slotTime;

    @Column(name = "number_of_sessions")
    private Integer numberOfSessions;

    @Column(name = "meet_link")
    private String meetLink;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
