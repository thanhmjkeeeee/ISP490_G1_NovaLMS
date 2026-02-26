package com.example.DoAn.repository;

import com.example.DoAn.model.Registration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Integer> {

    List<Registration> findByUser_UserIdOrderByRegistrationTimeDesc(Integer userId);
    boolean existsByUser_UserIdAndClazz_ClassIdAndStatusNot(Integer userId, Integer classId, String status);


    @Query(value = """
        SELECT r FROM Registration r 
        JOIN FETCH r.course c
        JOIN FETCH r.clazz cl
        LEFT JOIN FETCH c.category cat
        WHERE r.user.userId = :userId 
          AND r.status = 'Approved'
          AND (:keyword IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:categoryId IS NULL OR cat.settingId = :categoryId)
    """, countQuery = """
        SELECT COUNT(r) FROM Registration r 
        JOIN r.course c
        LEFT JOIN c.category cat
        WHERE r.user.userId = :userId 
          AND r.status = 'Approved'
          AND (:keyword IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:categoryId IS NULL OR cat.settingId = :categoryId)
    """)
    Page<Registration> findMyCoursesWithFilters(
            @Param("userId") Integer userId,
            @Param("keyword") String keyword,
            @Param("categoryId") Integer categoryId,
            Pageable pageable
    );
}