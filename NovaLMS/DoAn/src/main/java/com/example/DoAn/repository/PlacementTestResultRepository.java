package com.example.DoAn.repository;

import com.example.DoAn.model.PlacementTestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlacementTestResultRepository extends JpaRepository<PlacementTestResult, Integer> {
    Optional<PlacementTestResult> findByGuestSessionId(String guestSessionId);
    List<PlacementTestResult> findByGuestEmailOrderBySubmittedAtDesc(String guestEmail);
}
