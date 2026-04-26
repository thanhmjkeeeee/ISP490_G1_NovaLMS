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
}
