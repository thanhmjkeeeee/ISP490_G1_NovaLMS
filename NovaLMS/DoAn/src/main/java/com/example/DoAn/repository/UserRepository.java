package com.example.DoAn.repository;


import com.example.DoAn.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRole_Value(String roleValue);

    // thông tin role
        @Query("SELECT u FROM User u JOIN FETCH u.role")
        Page<User> findAllUsersWithRole(Pageable pageable);

}
