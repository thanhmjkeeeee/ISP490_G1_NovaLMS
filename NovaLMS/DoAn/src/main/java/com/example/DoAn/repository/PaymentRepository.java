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
    List<Payment> findAllByRegistrationIdOrderByCreatedAtDesc(Integer registrationId);
}
