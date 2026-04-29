package com.example.DoAn.repository;

import com.example.DoAn.model.VisitorLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface VisitorLogRepository extends JpaRepository<VisitorLog, Long> {
    Optional<VisitorLog> findByVisitorToken(String visitorToken);

    @Query("SELECT COUNT(v) FROM VisitorLog v")
    long countTotalVisitors();

    @Query("SELECT COUNT(v) FROM VisitorLog v WHERE v.user IS NOT NULL")
    long countRegisteredVisitors();

    /** Thống kê khách truy cập mới theo ngày trong 30 ngày gần đây */
    @Query(value = "SELECT DATE(first_visit) as date, COUNT(*) as count FROM visitor_log WHERE first_visit >= DATE_SUB(CURDATE(), INTERVAL 30 DAY) GROUP BY DATE(first_visit) ORDER BY date", nativeQuery = true)
    java.util.List<Object[]> getDailyVisitors();

    /** Thống kê khách truy cập mới theo tháng trong năm hiện tại */
    @Query(value = "SELECT MONTH(first_visit) as month, COUNT(*) as count FROM visitor_log WHERE YEAR(first_visit) = YEAR(CURDATE()) GROUP BY MONTH(first_visit) ORDER BY month", nativeQuery = true)
    java.util.List<Object[]> getMonthlyVisitors();
    /** Thống kê số lượng chuyển đổi (Guest -> Student) theo ngày trong 30 ngày gần đây */
    @Query(value = "SELECT DATE(u.created_at) as date, COUNT(v.id) as count FROM visitor_log v JOIN user u ON v.user_id = u.user_id WHERE u.created_at >= DATE_SUB(CURDATE(), INTERVAL 30 DAY) GROUP BY DATE(u.created_at) ORDER BY date", nativeQuery = true)
    java.util.List<Object[]> getDailyConversions();
}
