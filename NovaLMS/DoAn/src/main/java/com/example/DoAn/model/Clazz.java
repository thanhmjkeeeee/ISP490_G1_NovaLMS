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

    @OneToMany(mappedBy = "clazz", fetch = FetchType.LAZY)
    @JsonManagedReference(value = "clazz-sessions")
    private List<ClassSession> sessions;

    @Column(name = "meet_link")
    private String meetLink;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    public Integer getClassId() { return classId; }
    public void setClassId(Integer classId) { this.classId = classId; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public User getTeacher() { return teacher; }
    public void setTeacher(User teacher) { this.teacher = teacher; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

    public List<Registration> getRegistrations() { return registrations; }
    public void setRegistrations(List<Registration> registrations) { this.registrations = registrations; }

    public String getSchedule() { return schedule; }
    public void setSchedule(String schedule) { this.schedule = schedule; }

    public String getSlotTime() { return slotTime; }
    public void setSlotTime(String slotTime) { this.slotTime = slotTime; }

    public Integer getNumberOfSessions() { return numberOfSessions; }
    public void setNumberOfSessions(Integer numberOfSessions) { this.numberOfSessions = numberOfSessions; }

    public List<ClassSession> getSessions() { return sessions; }
    public void setSessions(List<ClassSession> sessions) { this.sessions = sessions; }

    public String getMeetLink() { return meetLink; }
    public void setMeetLink(String meetLink) { this.meetLink = meetLink; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
