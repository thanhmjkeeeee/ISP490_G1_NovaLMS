package com.example.DoAn.dto.request;

import com.example.DoAn.model.ClassSession;
import com.example.DoAn.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reschedule_request")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RescheduleRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ClassSession session;

    @Column(name = "old_date")
    private LocalDateTime oldDate;

    @Column(name = "old_start_time")
    private String oldStartTime;

    @Column(name = "new_date")
    private LocalDateTime newDate;

    @Column(name = "new_start_time")
    private String newStartTime;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "manager_note", columnDefinition = "TEXT")
    private String managerNote;

    @Column(name = "status", length = 20)
    private String status; // PENDING, APPROVED, REJECTED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
