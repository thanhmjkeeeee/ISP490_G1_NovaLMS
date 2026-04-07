package com.example.DoAn.service;

import com.example.DoAn.model.Setting;
import com.example.DoAn.repository.SettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SettingService {
    @Autowired
    private SettingRepository settingRepository;

    public List<Setting> getCourseCategories() {
        return settingRepository.findBySettingTypeAndStatus("COURSE_CATEGORY", "Active");
    }

    public List<Setting> getSettingsByType(String type) {
        if (type == null || type.isEmpty()) {
            return settingRepository.findAll();
        }
        return settingRepository.findBySettingType(type);
    }

    public Setting getSettingById(Integer id) {
        return settingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Setting not found with id: " + id));
    }

    public Setting createSetting(String type, String name, String value, String description, Integer orderIndex) {
        Setting setting = Setting.builder()
                .name(name)
                .value(value)
                .settingType((type != null && !type.isEmpty()) ? type : "COURSE_CATEGORY")
                .status("Active")
                .description(description)
                .orderIndex(orderIndex != null ? orderIndex : 0)
                .build();
        return settingRepository.save(setting);
    }

    public Setting createCourseCategory(String name, String value, String description, Integer orderIndex) {
        Setting setting = Setting.builder()
                .name(name)
                .value(value)
                .settingType("COURSE_CATEGORY") // Luôn ép cứng là COURSE_CATEGORY theo yêu cầu
                .status("Active")
                .description(description)
                .orderIndex(orderIndex != null ? orderIndex : 0)
                .build();
        return settingRepository.save(setting);
    }
    
    // Giữ lại hàm cũ để tương thích với AdminDashboardController
    public Setting saveCourseCategory(String name, String value) {
        return createCourseCategory(name, value, null, 0);
    }

    public Setting updateSetting(Integer id, String name, String value, String description, Integer orderIndex, String status) {
        Setting setting = getSettingById(id);
        if (name != null) {
            setting.setName(name);
        }
        if (value != null && !value.isEmpty()) {
            setting.setValue(value);
        }
        if (description != null) {
            setting.setDescription(description);
        }
        if (orderIndex != null) {
            setting.setOrderIndex(orderIndex);
        }
        if (status != null && !status.isEmpty()) {
            setting.setStatus(status);
        }
        return settingRepository.save(setting);
    }

    public void deleteSetting(Integer id) {
        Setting setting = getSettingById(id);
        settingRepository.delete(setting);
    }
}
