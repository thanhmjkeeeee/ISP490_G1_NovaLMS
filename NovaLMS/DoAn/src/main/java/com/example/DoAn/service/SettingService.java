package com.example.DoAn.service;

import com.example.DoAn.model.Setting;
import com.example.DoAn.repository.CourseRepository;
import com.example.DoAn.repository.QuestionRepository;
import com.example.DoAn.repository.SettingRepository;
import com.example.DoAn.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Arrays;
import java.util.Comparator;

@Service
public class SettingService {
    @Autowired
    private SettingRepository settingRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private QuestionRepository questionRepository;

    public List<Setting> getCourseCategories() {
        return settingRepository.findBySettingTypeAndStatus("COURSE_CATEGORY", "Active");
    }

    public List<Setting> getSettingsByType(String type) {
        return getSettingsByType(type, false);
    }

    /**
     * @param activeOnly true: chỉ trả về setting có status {@code Active} (dùng cho dropdown form)
     */
    public List<Setting> getSettingsByType(String type, boolean activeOnly) {
        System.out.println(">>> [SERVICE] getSettingsByType called with: " + type + ", activeOnly=" + activeOnly);
        if (type == null || type.isEmpty()) {
            return settingRepository.findAll();
        }
        List<Setting> result;
        if ("ROLE".equalsIgnoreCase(type)) {
            List<String> types = Arrays.asList("ROLE", "USER_ROLE");
            System.out.println(">>> [SERVICE] Querying for types: " + types);
            result = activeOnly
                    ? settingRepository.findBySettingTypeInAndStatus(types, "Active")
                    : settingRepository.findBySettingTypeIn(types);
            System.out.println(">>> [SERVICE] Query returned " + (result != null ? result.size() : 0) + " items.");
        } else {
            result = activeOnly
                    ? settingRepository.findBySettingTypeAndStatus(type, "Active")
                    : settingRepository.findBySettingType(type);
        }
        if (result == null) {
            return List.of();
        }
        result.sort(Comparator.comparing(Setting::getOrderIndex, Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
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
        String currentStatus = setting.getStatus();
        boolean wasActive = currentStatus == null || "active".equalsIgnoreCase(currentStatus.trim());
        boolean becomingInactive = status != null && !status.isBlank()
                && !"active".equalsIgnoreCase(status.trim());
        if (becomingInactive && wasActive) {
            assertSettingUnusedBeforeDeactivate(setting);
        }

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

    /**
     * Chặn chuyển Active → Inactive nếu setting đang được tham chiếu.
     */
    private void assertSettingUnusedBeforeDeactivate(Setting setting) {
        String type = setting.getSettingType();
        if (type == null) {
            return;
        }
        if ("COURSE_CATEGORY".equals(type)) {
            long n = courseRepository.countByCategory_SettingId(setting.getSettingId());
            if (n > 0) {
                throw new IllegalStateException(
                        "Không thể chuyển sang Inactive: chương trình (category) đang được gán cho " + n + " khóa học.");
            }
            return;
        }
        if ("USER_ROLE".equals(type) || "ROLE".equals(type)) {
            long n = userRepository.countByRole_SettingId(setting.getSettingId());
            if (n > 0) {
                throw new IllegalStateException(
                        "Không thể chuyển sang Inactive: vai trò đang được gán cho " + n + " tài khoản.");
            }
            return;
        }
        String val = setting.getValue();
        if (val == null || val.isBlank()) {
            return;
        }
        if ("QUESTION_TYPE".equals(type)) {
            long n = questionRepository.countByQuestionTypeIgnoreCase(val);
            if (n > 0) {
                throw new IllegalStateException(
                        "Không thể chuyển sang Inactive: loại câu hỏi đang được dùng trong " + n + " câu hỏi.");
            }
            return;
        }
        if ("SKILL".equals(type)) {
            long n = questionRepository.countBySkillIgnoreCase(val);
            if (n > 0) {
                throw new IllegalStateException(
                        "Không thể chuyển sang Inactive: kỹ năng đang được dùng trong " + n + " câu hỏi.");
            }
            return;
        }
        if ("CEFR_LEVEL".equals(type)) {
            long n = questionRepository.countByCefrLevelIgnoreCase(val);
            if (n > 0) {
                throw new IllegalStateException(
                        "Không thể chuyển sang Inactive: mức CEFR đang được dùng trong " + n + " câu hỏi.");
            }
        }
    }

    public void deleteSetting(Integer id) {
        Setting setting = getSettingById(id);
        settingRepository.delete(setting);
    }

    public List<Setting> getRoles() {
        return getSettingsByType("ROLE", true);
    }
}
