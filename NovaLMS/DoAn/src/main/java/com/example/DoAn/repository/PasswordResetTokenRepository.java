package com.example.DoAn.repository;

import com.example.DoAn.model.PasswordResetToken;
import com.example.DoAn.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Integer> {
    Optional<PasswordResetToken> findByToken(String token);
    void deleteByUser(User user);
}