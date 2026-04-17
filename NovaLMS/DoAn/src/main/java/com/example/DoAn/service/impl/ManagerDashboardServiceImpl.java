package com.example.DoAn.service.impl;

import com.example.DoAn.dto.response.*;
import com.example.DoAn.repository.*;
import com.example.DoAn.service.ManagerDashboardService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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

        List<RecentRegistrationDTO> recentDto = registrationRepository.findRecentRegistrationsWithAssociations(PageRequest.of(0, 5))
                .stream().map(r -> RecentRegistrationDTO.builder()
                        .registrationId(r.getRegistrationId())
                        .studentName(r.getUser() != null ? r.getUser().getFullName() : "N/A")
                        .studentEmail(r.getUser() != null ? r.getUser().getEmail() : "N/A")
                        .courseName(r.getCourse() != null ? r.getCourse().getCourseName() : "N/A")
                        .enrolledAt(r.getRegistrationTime())
                        .status(r.getStatus())
                        .build())
                .collect(Collectors.toList());

        // Trend (last 7 days)
        java.util.Map<String, Long> trend = new java.util.LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            LocalDateTime startOfDay = LocalDateTime.now().minusDays(i).toLocalDate().atStartOfDay();
            LocalDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);
            long count = registrationRepository.countByRegistrationTimeBetween(startOfDay, endOfDay);
            trend.put(startOfDay.toLocalDate().toString(), count);
        }

        // Status Distribution (Optimized)
        java.util.Map<String, Long> statusDist = new java.util.HashMap<>();
        List<Object[]> statusCounts = registrationRepository.countByStatusDistribution();
        for (Object[] row : statusCounts) {
            String status = (String) row[0];
            if (status == null) status = "Pending";
            statusDist.put(status, (Long) row[1]);
        }

        return ManagerDashboardDTO.builder()
                .totalStudents(studentsFromReg)
                .totalCourses(totalCourses)
                .totalClasses(totalClasses)
                .newRegistrationsThisWeek(newRegs)
                .recentRegistrations(recentDto)
                .registrationTrend(trend)
                .statusDistribution(statusDist)
                .build();
    }
}
