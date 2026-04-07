package com.example.DoAn.repository;

import com.example.DoAn.model.UserLearningLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserLearningLogRepository extends JpaRepository<UserLearningLog, Integer> {

    Optional<UserLearningLog> findByUser_UserIdAndLearnDate(Integer userId, LocalDate learnDate);

    @Modifying
    @Query("UPDATE UserLearningLog l SET l.timeSpentSeconds = l.timeSpentSeconds + :seconds " +
           "WHERE l.user.userId = :userId AND l.learnDate = :date")
    int updateTimeSpent(@Param("userId") Integer userId, @Param("date") LocalDate date, @Param("seconds") int seconds);

    List<UserLearningLog> findByUser_UserIdAndLearnDateAfterOrderByLearnDate(Integer userId, LocalDate startDate);
}
