package com.example.DoAn.repository;

import com.example.DoAn.model.AnswerOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AnswerOptionRepository extends JpaRepository<AnswerOption, Integer> {
    List<AnswerOption> findByQuestionQuestionId(Integer questionId);
    void deleteByQuestionQuestionId(Integer questionId);
}
