package com.example.DoAn.repository;

import com.example.DoAn.model.HybridSessionQuiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HybridSessionQuizRepository extends JpaRepository<HybridSessionQuiz, Integer> {
    List<HybridSessionQuiz> findByHybridSessionIdOrderByQuizOrderAsc(Integer hybridSessionId);
}
