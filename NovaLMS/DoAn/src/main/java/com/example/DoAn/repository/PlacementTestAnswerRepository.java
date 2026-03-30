package com.example.DoAn.repository;

import com.example.DoAn.model.PlacementTestAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlacementTestAnswerRepository extends JpaRepository<PlacementTestAnswer, Integer> {

    @Query("SELECT a FROM PlacementTestAnswer a WHERE a.placementTestResult.id = :resultId AND a.question.questionId = :questionId")
    Optional<PlacementTestAnswer> findByResultIdAndQuestionId(
        @Param("resultId") Integer resultId,
        @Param("questionId") Integer questionId);

    @Query("SELECT a FROM PlacementTestAnswer a WHERE a.placementTestResult.id = :resultId")
    List<PlacementTestAnswer> findByResultId(@Param("resultId") Integer resultId);
}
