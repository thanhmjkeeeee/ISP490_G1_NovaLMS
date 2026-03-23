package com.example.DoAn.service.impl;

import com.example.DoAn.dto.response.*;
import com.example.DoAn.repository.*;
import com.example.DoAn.service.ManagerDashboardService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ManagerDashboardServiceImpl implements ManagerDashboardService {

    @PersistenceContext
    private EntityManager em;

    private final CourseRepository courseRepository;
    private final ClassRepository classRepository;
    private final RegistrationRepository registrationRepository;

    @Override
    @Transactional(readOnly = true)
    public ManagerDashboardDTO getDashboardData() {
        Long studentsFromReg = 0L;
        try {
            studentsFromReg = (Long) em.createQuery("SELECT COUNT(DISTINCT r.user) FROM Registration r").getSingleResult();
        } catch (Exception e) {
            // fallback
            studentsFromReg = 0L;
        }

        long totalCourses = courseRepository.count();
        long totalClasses = classRepository.count();
        
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        long newRegs = registrationRepository.countByRegistrationTimeAfter(weekAgo);

        List<RecentRegistrationDTO> recentDto = registrationRepository.findTop10ByOrderByRegistrationTimeDesc()
                .stream().map(r -> RecentRegistrationDTO.builder()
                        .registrationId(r.getRegistrationId())
                        .studentName(r.getUser() != null ? r.getUser().getFullName() : "N/A")
                        .studentEmail(r.getUser() != null ? r.getUser().getEmail() : "N/A")
                        .courseName(r.getCourse() != null ? r.getCourse().getTitle() : "N/A")
                        .enrolledAt(r.getRegistrationTime())
                        .status(r.getStatus())
                        .build())
                .collect(Collectors.toList());

        return ManagerDashboardDTO.builder()
                .totalStudents(studentsFromReg)
                .totalCourses(totalCourses)
                .totalClasses(totalClasses)
                .newRegistrationsThisWeek(newRegs)
                .recentRegistrations(recentDto)
                .build();
    }
}
