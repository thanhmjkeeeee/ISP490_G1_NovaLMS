package com.example.DoAn.repository;


import com.example.DoAn.model.Setting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface SettingRepository extends JpaRepository<Setting, Integer> {
    @Query("SELECT s FROM Setting s WHERE s.settingType = 'ROLE' AND s.value = :value")
    Optional<Setting> findRoleByValue(@Param("value") String value);

    @Query("SELECT s FROM Setting s WHERE s.settingType = 'ROLE' AND s.name = :name")
    Optional<Setting> findRoleByName(@Param("name") String name);

    List<Setting> findBySettingTypeAndStatus(String type, String status);

    List<Setting> findBySettingType(String type);

    @Query("SELECT s FROM Setting s WHERE s.settingType IN :types")
    List<Setting> findBySettingTypeIn(@Param("types") List<String> types);
}