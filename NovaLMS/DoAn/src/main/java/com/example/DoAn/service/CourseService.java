package com.example.DoAn.service;

import com.example.DoAn.model.Clazz;
import com.example.DoAn.model.Course;
import com.example.DoAn.model.Setting;
import com.example.DoAn.repository.CourseRepository;
import com.example.DoAn.repository.SettingRepository;
import com.example.DoAn.repository.ClassRepository;
import com.example.DoAn.repository.RegistrationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import com.example.DoAn.model.Module;
import com.example.DoAn.repository.ModuleRepository;
import org.springframework.data.domain.Sort;

@Service
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private SettingRepository settingRepository;

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private ModuleRepository moduleRepository;


    public List<Course> getAllPublishedCourses() {
        return courseRepository.findByStatus("Active");
    }

    public List<Setting> getAllCategories() {
        return settingRepository.findBySettingTypeAndStatus("COURSE_CATEGORY", "Active");
    }

    public Course getCourseById(Integer id) {
        return courseRepository.findById(id).orElse(null);
    }

    public List<Clazz> getActiveClassesByCourse(Integer courseId) {
        return classRepository.findByCourse_CourseIdAndStatus(courseId, "Open");
    }

    public long getStudentCount(Integer courseId) {
        return registrationRepository.countByCourse_CourseIdAndStatus(courseId, "Approved");
    }

    public List<Module> getCourseCurriculum(Integer courseId) {
        // Gọi hàm từ Repository vừa tạo ở trên
        return moduleRepository.findByCourse_CourseIdOrderByOrderIndexAsc(courseId);
    }

    public List<Course> getCoursesByCategory(Integer categoryId) {
        // Sử dụng "Active" nếu bạn chưa chạy lệnh UPDATE sang "Published"
        return courseRepository.findByCategory_SettingIdAndStatus(categoryId, "Active");
    }

    public List<Course> searchAndFilterCourses(String keyword, Integer categoryId, String sortBy) {
        // 1. Xử lý từ khóa: Nếu rỗng thì coi như Null để tìm tất cả
        if (keyword != null && keyword.trim().isEmpty()) {
            keyword = null;
        }

        // 2. Xử lý sắp xếp (Sort)
        Sort sort = Sort.by(Sort.Direction.DESC, "courseId"); // Mặc định: Mới nhất lên đầu

        if (sortBy != null) {
            switch (sortBy) {
                case "price_asc":
                    sort = Sort.by(Sort.Direction.ASC, "price"); // Giá tăng dần
                    break;
                case "price_desc":
                    sort = Sort.by(Sort.Direction.DESC, "price"); // Giá giảm dần
                    break;
                // Có thể thêm case "name_asc" nếu muốn sắp xếp theo tên
            }
        }

        // 3. Gọi Repository (Hàm này bạn vừa thêm ở bước trước)
        // Lưu ý: "Active" là trạng thái mặc định bạn đang dùng trong các hàm trên
        return courseRepository.searchCourses(keyword, categoryId, "Active", sort);
    }
}