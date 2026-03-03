package com.example.DoAn.repository;

import com.example.DoAn.model.EmailVerification;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Integer> {
    Optional<EmailVerification> findFirstByEmailOrderByExpiryTimeDesc(String email);

    @Transactional
    void deleteByEmail(String email);
}
