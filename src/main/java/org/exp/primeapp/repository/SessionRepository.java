package org.exp.primeapp.repository;

import org.exp.primeapp.models.entities.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
    
    Optional<Session> findBySessionId(String sessionId);
    
    @Query("SELECT s FROM Session s WHERE s.ip = :ip AND s.browserInfo = :browserInfo AND s.isActive = true AND (s.isDeleted IS NULL OR s.isDeleted = false) ORDER BY s.lastAccessedAt DESC")
    Optional<Session> findByIpAndBrowserInfo(@Param("ip") String ip, @Param("browserInfo") String browserInfo, @Param("now") LocalDateTime now);
    
    @Query("SELECT s FROM Session s WHERE s.user.id = :userId AND s.ip = :ip AND s.browserInfo = :browserInfo AND s.isActive = true AND (s.isDeleted IS NULL OR s.isDeleted = false) ORDER BY s.lastAccessedAt DESC")
    Optional<Session> findByUserIdAndIpAndBrowserInfo(@Param("userId") Long userId, @Param("ip") String ip, @Param("browserInfo") String browserInfo, @Param("now") LocalDateTime now);
    
    // Note: @Param("now") is kept for backward compatibility but not used in query
    
    @Query("SELECT s.isDeleted FROM Session s WHERE s.sessionId = :sessionId")
    Optional<Boolean> findIsDeletedBySessionId(@Param("sessionId") String sessionId);
}

