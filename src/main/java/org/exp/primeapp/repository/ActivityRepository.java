package org.exp.primeapp.repository;

import org.exp.primeapp.models.entities.Activity;
import org.exp.primeapp.models.enums.EntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {

    List<Activity> findByEntityTypeAndEntityIdOrderByTimestampDesc(EntityType entityType, Long entityId);

    Page<Activity> findByEntityTypeAndEntityIdOrderByTimestampDesc(EntityType entityType, Long entityId, Pageable pageable);

    List<Activity> findByEntityTypeOrderByTimestampDesc(EntityType entityType);

    Page<Activity> findByEntityTypeOrderByTimestampDesc(EntityType entityType, Pageable pageable);

    List<Activity> findByEntityIdOrderByTimestampDesc(Long entityId);

    Page<Activity> findByEntityIdOrderByTimestampDesc(Long entityId, Pageable pageable);

    List<Activity> findByEntityTypeAndEntityIdAndTimestampBetweenOrderByTimestampDesc(
            EntityType entityType, Long entityId, LocalDateTime start, LocalDateTime end);

    List<Activity> findByEntityTypeAndTimestampBetweenOrderByTimestampDesc(
            EntityType entityType, LocalDateTime start, LocalDateTime end);

    @Query("SELECT a FROM Activity a ORDER BY a.timestamp DESC")
    List<Activity> findAllOrderByTimestampDesc();

    @Query("SELECT a FROM Activity a ORDER BY a.timestamp DESC")
    Page<Activity> findAllOrderByTimestampDesc(Pageable pageable);
}


