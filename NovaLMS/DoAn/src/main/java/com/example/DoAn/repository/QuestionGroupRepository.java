package com.example.DoAn.repository;

import com.example.DoAn.model.QuestionGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionGroupRepository extends JpaRepository<QuestionGroup, Integer> {
    List<QuestionGroup> findByUserEmail(String email);

    @Query("SELECT g FROM QuestionGroup g LEFT JOIN FETCH g.questions WHERE " +
           "(:skill IS NULL OR g.skill = :skill) AND " +
           "(:cefrLevel IS NULL OR g.cefrLevel = :cefrLevel) AND " +
           "(:topic IS NULL OR g.topic LIKE %:topic%) AND " +
           "(:status IS NULL OR g.status = :status) AND " +
           "(:keyword IS NULL OR g.groupContent LIKE %:keyword% OR g.topic LIKE %:keyword%) " +
           "ORDER BY g.createdAt DESC")
    List<QuestionGroup> findByFilters(
        @Param("skill") String skill,
        @Param("cefrLevel") String cefrLevel,
        @Param("topic") String topic,
        @Param("status") String status,
        @Param("keyword") String keyword
    );
}
