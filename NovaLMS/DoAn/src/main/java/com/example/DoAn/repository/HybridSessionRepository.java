package com.example.DoAn.repository;

import com.example.DoAn.model.HybridSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface HybridSessionRepository extends JpaRepository<HybridSession, Integer> {
    Optional<HybridSession> findByGuestSessionId(String guestSessionId);
}
