package com.example.DoAn.service.impl;

import com.example.DoAn.model.Course;
import com.example.DoAn.model.User;
import com.example.DoAn.model.Setting;
import com.example.DoAn.repository.CourseRepository;
import com.example.DoAn.repository.UserRepository;
import com.example.DoAn.repository.SettingRepository;
import com.example.DoAn.service.HomeService;
import com.example.DoAn.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HomeServiceImpl implements HomeService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SettingRepository settingRepository;

    @Autowired
    private CourseService courseService;

    @Override
    public List<Course> getFeaturedCourses() {
        List<Course> courses = courseRepository.findByStatus("Active");
        for (Course course : courses) {
            long count = courseService.getStudentCount(course.getCourseId());
            course.setStudentCount((int) count);
        }
        return courses;
    }

    @Override
    public List<User> getFeaturedTeachers() {
        return userRepository.findByRole_SettingIdAndStatus(105, "Active");
    }
}