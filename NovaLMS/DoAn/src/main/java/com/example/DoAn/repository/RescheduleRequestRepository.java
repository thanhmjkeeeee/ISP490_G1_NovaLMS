package com.example.DoAn.repository;

import com.example.DoAn.dto.request.RescheduleRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RescheduleRequestRepository extends JpaRepository<RescheduleRequest, Integer> {

    @Query("SELECT r FROM RescheduleRequest r WHERE r.session.sessionId = :sessionId AND r.status = 'PENDING'")
    Optional<RescheduleRequest> findPendingBySessionId(@Param("sessionId") Integer sessionId);

    @Query("SELECT r FROM RescheduleRequest r " +
           "LEFT JOIN r.session s " +
           "LEFT JOIN s.clazz c " +
           "LEFT JOIN c.teacher u " +
           "WHERE (:teacherName IS NULL OR :teacherName = '' OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :teacherName, '%'))) " +
           "AND (:status IS NULL OR :status = '' OR r.status = :status)")
    Page<RescheduleRequest> findAllWithFilter(
            @Param("teacherName") String teacherName,
            @Param("status") String status,
            Pageable pageable);

    List<RescheduleRequest> findByCreatedBy_UserIdOrderByCreatedAtDesc(Integer userId);

    boolean existsByCreatedBy_UserIdAndNewDateAndNewStartTimeAndStatus(
            Integer userId,
            java.time.LocalDateTime newDate,
            String newStartTime,
            String status
    );
}
