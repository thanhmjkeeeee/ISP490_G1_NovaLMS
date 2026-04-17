package com.example.DoAn.service.impl;

import com.example.DoAn.dto.response.ClassPublicResponseDTO;
import com.example.DoAn.dto.response.CoursePublicResponseDTO;
import com.example.DoAn.model.Clazz;
import com.example.DoAn.model.Course;
import com.example.DoAn.model.User;
import com.example.DoAn.repository.ClassRepository;
import com.example.DoAn.repository.CourseRepository;
import com.example.DoAn.repository.SettingRepository;
import com.example.DoAn.repository.UserRepository;
import com.example.DoAn.service.ClassPublicService;
import com.example.DoAn.service.CourseService;
import com.example.DoAn.service.HomeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class HomeServiceImpl implements HomeService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SettingRepository settingRepository;

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private CourseService courseService;

    @Override
    public List<CoursePublicResponseDTO> getFeaturedCourses() {
        return courseRepository.findTopFeaturedCourses(PageRequest.of(0, 6))
                .stream()
                .map(course -> courseService.mapToSummaryDTO(course))
                .collect(Collectors.toList());
    }

    @Override
    public List<User> getFeaturedTeachers() {
        return settingRepository.findRoleByValue("ROLE_TEACHER")
                .map(role -> userRepository.findByRole_SettingIdAndStatus(role.getSettingId(), "Active"))
                .orElse(Collections.emptyList());
    }

    @Override
    public List<ClassPublicResponseDTO> getUpcomingClasses() {
        return classRepository.findByStatus("Open").stream()
                .filter(c -> c.getStartDate() != null && c.getStartDate().isAfter(LocalDateTime.now().minusDays(1)))
                .sorted(Comparator.comparing(Clazz::getStartDate))
                .limit(4)
                .map(c -> ClassPublicResponseDTO.builder()
                        .classId(c.getClassId())
                        .courseId(c.getCourse() != null ? c.getCourse().getCourseId() : null)
                        .courseTitle(c.getCourse() != null ? c.getCourse().getCourseName() : "N/A")
                        .className(c.getClassName())
                        .teacherName(c.getTeacher() != null ? c.getTeacher().getFullName() : "TBA")
                        .schedule(c.getSchedule())
                        .slotTime(c.getSlotTime())
                        .startDate(c.getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                        .build())
                .collect(Collectors.toList());
    }
}