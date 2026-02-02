package com.example.DoAn.repository;

import com.example.DoAn.model.EmailVerification;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Integer> {
    // Tìm mã mới nhất của email đó
    Optional<EmailVerification> findFirstByEmailOrderByExpiryTimeDesc(String email);

    // Xóa mã sau khi dùng xong
    @Transactional
    void deleteByEmail(String email);
}
