package com.example.DoAn.repository;


import com.example.DoAn.model.Setting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface SettingRepository extends JpaRepository<Setting, Integer> {
    @Query("SELECT s FROM Setting s WHERE s.settingType = 'USER_ROLE' AND s.value = :value")
    Optional<Setting> findRoleByValue(String value);

    List<Setting> findBySettingTypeAndStatus(String type, String status);

}