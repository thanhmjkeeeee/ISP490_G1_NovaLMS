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

    // Getters and Setters
    public Integer getSettingId() { return settingId; }
    public void setSettingId(Integer settingId) { this.settingId = settingId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getSettingType() { return settingType; }
    public void setSettingType(String settingType) { this.settingType = settingType; }

    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}