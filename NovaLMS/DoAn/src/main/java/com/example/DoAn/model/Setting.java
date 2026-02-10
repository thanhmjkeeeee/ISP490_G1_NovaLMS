package com.example.DoAn.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "setting")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Setting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "setting_id")
    private Integer settingId;

    @Column(name = "name")
    private String name; // Ví dụ: "Student", "Admin"

    @Column(name = "value")
    private String value; // Ví dụ: "ROLE_STUDENT", "ROLE_ADMIN"

    @Column(name = "setting_type")
    private String settingType; // Ví dụ: "USER_ROLE"

    @Column(name = "order_index")
    private Integer orderIndex;

    @Column(name = "status")
    private String status;

    @Column(name = "description")
    private String description;
}