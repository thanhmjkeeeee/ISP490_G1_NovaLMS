package com.example.DoAn.repository;

import com.example.DoAn.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    /** Lấy payment mới nhất của 1 registration — dùng cho duyệt/admin */
    Optional<Payment> findFirstByRegistrationIdOrderByCreatedAtDesc(Integer registrationId);
    Optional<Payment> findByPayosOrderCode(Long orderCode);
    Optional<Payment> findByPayosPaymentLinkId(Long payosPaymentLinkId);
    /** Tính tổng doanh thu từ các payment đã PAID */
    @org.springframework.data.jpa.repository.Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'PAID'")
    java.math.BigDecimal sumTotalRevenue();

    /** Doanh thu theo tháng trong năm hiện tại */
    @org.springframework.data.jpa.repository.Query(value = "SELECT MONTH(paid_at) as month, SUM(amount) as total FROM payment WHERE status = 'PAID' AND YEAR(paid_at) = YEAR(CURDATE()) GROUP BY MONTH(paid_at) ORDER BY month", nativeQuery = true)
    List<Object[]> getMonthlyRevenue();

    /** Lấy danh sách payment thành công để xuất Excel */
    List<Payment> findByStatusOrderByPaidAtDesc(String status);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT r.user.userId) FROM Registration r JOIN Payment p ON r.registrationId = p.registrationId WHERE p.status = 'PAID'")
    long countConvertedStudents();

    List<Payment> findAllByRegistrationIdOrderByCreatedAtDesc(Integer registrationId);
}
