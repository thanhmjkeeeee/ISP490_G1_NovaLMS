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
        // 'COURSE_CATEGORY' phải khớp với cột setting_type trong file SQL bạn vừa nạp
        return settingRepository.findBySettingTypeAndStatus("COURSE_CATEGORY", "Active");
    }
}
