package org.exp.primeapp.repository.setting;

import org.exp.primeapp.models.entities.settings.Setting;
import org.exp.primeapp.models.enums.setting.SettingCategory;
import org.exp.primeapp.models.enums.setting.SettingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SettingRepository extends JpaRepository<Setting, Long> {
    Optional<Setting> findByKey(String key);
    
    Optional<Setting> findByType(SettingType type);
    
    List<Setting> findByCategory(SettingCategory category);
    
    List<Setting> findByIsVisibleTrue();
    
    List<Setting> findByCategoryAndIsVisibleTrue(SettingCategory category);
}