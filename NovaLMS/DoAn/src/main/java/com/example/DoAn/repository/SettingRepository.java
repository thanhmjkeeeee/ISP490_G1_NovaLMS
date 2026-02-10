package com.example.DoAn.repository;


import com.example.DoAn.model.Setting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SettingRepository extends JpaRepository<Setting, Integer> {
    // Tìm Setting Role theo giá trị (ví dụ: value = 'ROLE_STUDENT')
    // và đảm bảo type là 'USER_ROLE'
    @Query("SELECT s FROM Setting s WHERE s.settingType = 'USER_ROLE' AND s.value = :value")
    Optional<Setting> findRoleByValue(String value);
}