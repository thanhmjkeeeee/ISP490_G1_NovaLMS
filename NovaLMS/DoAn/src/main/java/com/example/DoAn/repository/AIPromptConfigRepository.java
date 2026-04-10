package com.example.DoAn.repository;

import com.example.DoAn.model.AIPromptConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AIPromptConfigRepository extends JpaRepository<AIPromptConfig, Integer> {
    Optional<AIPromptConfig> findByBucket(String bucket);
}
