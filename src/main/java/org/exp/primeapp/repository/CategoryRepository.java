package org.exp.primeapp.repository;

import org.exp.primeapp.models.entities.Category;
import org.exp.primeapp.models.enums.CategoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

        List<Category> findAllByOrderByOrderNumberAsc();

        // List<Category> findAllByActiveTrueOrderByOrderNumberAsc(); // Removed

        // List<Category> findAllByActiveFalseOrderByOrderNumberAsc(); // Removed

        long count();

        /*
         * long countByActiveTrue();
         * 
         * long countByActiveFalse();
         */

        // List<Category> findByActive(boolean active); // Removed

        // List<Category> findAllBySpotlightId(Long spotlightName);

        @Transactional
        @Modifying
        @Query("UPDATE Category c SET c.status = CASE WHEN c.status = 'VISIBLE' THEN 'CREATED' ELSE 'VISIBLE' END WHERE c.id = :categoryId")
        void toggleCategoryActiveStatus(@Param("categoryId") Long categoryId);

        /*
         * @Query("SELECT c FROM Category c " +
         * "WHERE c.spotlight.id = :spotlightName AND c.active = :active " +
         * "ORDER BY c.orderNumber ASC")
         * List<Category> findBySpotlightIdAndActiveSorted(
         * 
         * @Param("spotlightName") Long spotlightName,
         * 
         * @Param("active") Boolean active
         * );
         */

        // List<Category> findBySpotlightNameAndActive(String spotlightName, Boolean
        // active); // Removed

        // Status ga qarab filter qilish
        List<Category> findByStatusOrderByOrderNumberAsc(CategoryStatus status);

        List<Category> findBySpotlightNameAndStatusOrderByOrderNumberAsc(String spotlightName, CategoryStatus status);

        List<Category> findBySpotlightNameAndStatusInOrderByOrderNumberAsc(String spotlightName,
                        List<CategoryStatus> statuses);
}