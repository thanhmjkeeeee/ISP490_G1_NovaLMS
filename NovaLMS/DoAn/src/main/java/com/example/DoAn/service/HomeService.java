package com.example.DoAn.service;

import com.example.DoAn.model.Course;
import com.example.DoAn.model.User;
import com.example.DoAn.model.Setting;
import java.util.List;

public interface HomeService {
    List<Course> getFeaturedCourses();
    List<User> getFeaturedTeachers();
}