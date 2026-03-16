package com.example.DoAn.repository;


import com.example.DoAn.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.role WHERE u.email = :email")
    Optional<User> findByEmailWithRole(@Param("email") String email);

    @Query(value = "SELECT role_id FROM user WHERE email = :email", nativeQuery = true)
    Integer findRoleIdByEmail(@Param("email") String email);

    List<User> findByRole_SettingIdAndStatus(Integer settingId, String status);
    boolean existsByEmail(String email);
    List<User> findByRole_Value(String roleValue);

    // thông tin role
    @Transactional
    @Modifying
    @Query(value = "UPDATE user SET status = :status WHERE user_id = :id", nativeQuery = true)
    void updateStatusNative(@Param("status") String status, @Param("id") Integer id);
}
