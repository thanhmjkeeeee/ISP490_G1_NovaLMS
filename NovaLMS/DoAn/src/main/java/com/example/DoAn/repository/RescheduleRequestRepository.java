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

    /**
     * Giờ bắt đầu (HH:mm) đã được đặt bởi yêu cầu đổi lịch PENDING của GV trong ngày,
     * loại trừ buổi đang chỉnh (excludeSessionId) để không khóa slot đích của chính yêu cầu đó.
     */
    @Query("SELECT r.newStartTime FROM RescheduleRequest r WHERE r.createdBy.userId = :userId AND r.status = 'PENDING' "
            + "AND r.newDate >= :dayStart AND r.newDate < :dayEnd "
            + "AND (:excludeSessionId IS NULL OR r.session.sessionId <> :excludeSessionId) "
            + "AND r.newStartTime IS NOT NULL AND TRIM(r.newStartTime) <> ''")
    List<String> findPendingNewStartTimesForTeacherOnDay(
            @Param("userId") Integer userId,
            @Param("dayStart") java.time.LocalDateTime dayStart,
            @Param("dayEnd") java.time.LocalDateTime dayEnd,
            @Param("excludeSessionId") Integer excludeSessionId);

    boolean existsByCreatedBy_UserIdAndNewDateAndNewStartTimeAndStatus(
            Integer userId,
            java.time.LocalDateTime newDate,
            String newStartTime,
            String status
    );
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteBySession_Clazz_ClassId(Integer classId);
}
