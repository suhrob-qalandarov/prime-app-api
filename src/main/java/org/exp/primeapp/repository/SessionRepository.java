package org.exp.primeapp.repository;

import org.exp.primeapp.models.entities.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
    
    Optional<Session> findBySessionId(String sessionId);
    
    // Note: browserInfo queries removed - browserInfos is now LinkedHashSet (JSON array)
    // Use findBySessionId or filter by IP only if needed
    
    @Query("SELECT s.isDeleted FROM Session s WHERE s.sessionId = :sessionId")
    Optional<Boolean> findIsDeletedBySessionId(@Param("sessionId") String sessionId);
    
    /**
     * User ning barcha o'chirilmagan sessionlarini olish
     */
    @Query("SELECT s FROM Session s WHERE s.user.id = :userId AND (s.isDeleted = false OR s.isDeleted IS NULL)")
    List<Session> findAllByUserIdAndIsDeletedFalse(@Param("userId") Long userId);
}

