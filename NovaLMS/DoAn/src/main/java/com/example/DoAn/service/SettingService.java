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

    public List<Setting> getCourseCategories(){
        return settingRepository.findBySettingTypeAndStatus("COURSE_CATEGORY", "Active");
    }

    public Setting saveCourseCategory(String name, String value) {
        Setting setting = Setting.builder()
                .name(name)
                .value(value)
                .settingType("COURSE_CATEGORY")
                .status("Active")
                .build();
        return settingRepository.save(setting);
    }
}
