package org.exp.primeapp.repository;

import org.exp.primeapp.models.entities.UserIpInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserIpInfoRepository extends JpaRepository<UserIpInfo, Long> {
    
    List<UserIpInfo> findByUserId(Long userId);
    
    Optional<UserIpInfo> findByUserIdAndIsRegisterInfoTrue(Long userId);
    
    List<UserIpInfo> findByUserIdAndIsRegisterInfoFalse(Long userId);
    
    boolean existsByUserIdAndIpAndBrowserInfo(Long userId, String ip, String browserInfo);
}

