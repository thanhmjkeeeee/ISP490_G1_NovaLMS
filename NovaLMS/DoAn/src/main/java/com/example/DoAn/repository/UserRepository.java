package com.example.DoAn.repository;


import com.example.DoAn.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.userId = :userId")
    Optional<User> findByIdForUpdate(@Param("userId") Integer userId);

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.role WHERE u.email = :email")
    Optional<User> findByEmailWithRole(@Param("email") String email);

    @Query(value = "SELECT role_id FROM user WHERE email = :email", nativeQuery = true)
    Integer findRoleIdByEmail(@Param("email") String email);

    List<User> findByRole_SettingIdAndStatus(Integer settingId, String status);
    boolean existsByEmail(String email);
    boolean existsByMobile(String mobile);
    List<User> findByRole_Value(String roleValue);

    @Query("SELECT u FROM User u WHERE u.role.settingId = :roleId")
    List<User> findByRoleSettingId(@Param("roleId") Integer roleId);

    long countByRole_SettingId(Integer roleSettingId);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role.name = :roleName")
    long countByRoleName(@Param("roleName") String roleName);

    // thông tin role
    @Transactional
    @Modifying
    @Query(value = "UPDATE user SET status = :status WHERE user_id = :id", nativeQuery = true)
    void updateStatusNative(@Param("status") String status, @Param("id") Integer id);
}
