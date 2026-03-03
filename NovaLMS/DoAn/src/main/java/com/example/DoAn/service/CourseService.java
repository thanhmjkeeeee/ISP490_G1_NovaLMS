package com.example.DoAn.service;

import com.example.DoAn.model.Clazz;
import com.example.DoAn.model.Course;
import com.example.DoAn.model.Module;
import com.example.DoAn.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private ClassRepository classRepository;
    @Autowired
    private RegistrationRepository registrationRepository;
    @Autowired
    private ModuleRepository moduleRepository;

    public List<Course> getCoursesByFilter(Integer categoryId) {
        return Optional.ofNullable(categoryId)
                .map(id -> courseRepository.findByCategory_SettingIdAndStatus(id, "Active"))
                .orElseGet(() -> courseRepository.findByStatus("Active"));
    }

    public Optional<Course> getCourseById(Integer id) {
        return courseRepository.findById(id);
    }

    public long getStudentCount(Integer courseId) {
        return registrationRepository.countByCourse_CourseIdAndStatus(courseId, "Approved");
    }

    public List<Clazz> getActiveClasses(Integer courseId) {
        return classRepository.findByCourse_CourseIdAndStatus(courseId, "Open");
    }

    public List<Module> getCurriculum(Integer courseId) {
        return moduleRepository.findByCourse_CourseIdOrderByOrderIndexAsc(courseId);
    }

    public List<Course> searchAndFilterCourses(String keyword, Integer categoryId, String sortBy) {
        String searchKey = (keyword != null && !keyword.trim().isEmpty()) ? keyword : null;

        Sort sort = switch (Optional.ofNullable(sortBy).orElse("newest")) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            default -> Sort.by(Sort.Direction.DESC, "courseId");
        };

        return courseRepository.searchCourses(searchKey, categoryId, "Active", sort);
    }
}