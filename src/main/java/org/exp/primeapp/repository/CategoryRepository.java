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

        long count();

        // Filtering by status
        List<Category> findByStatusOrderByOrderNumberAsc(CategoryStatus status);

        List<Category> findByStatusInOrderByOrderNumberAsc(List<CategoryStatus> statuses);

        List<Category> findBySpotlightNameAndStatusOrderByOrderNumberAsc(String spotlightName, CategoryStatus status);

        List<Category> findBySpotlightNameAndStatusInOrderByOrderNumberAsc(String spotlightName,
                        List<CategoryStatus> statuses);

        // Toggle category status only (without affecting products)
        @Transactional
        @Modifying
        @Query(value = "UPDATE categories SET " +
                        "status = CASE WHEN status = 'ACTIVE' THEN 'INACTIVE' WHEN status = 'INACTIVE' THEN 'ACTIVE' ELSE status END, "
                        +
                        "last_activated_at = CASE WHEN status = 'INACTIVE' THEN NOW() ELSE last_activated_at END, " +
                        "last_deactivated_at = CASE WHEN status = 'ACTIVE' THEN NOW() ELSE last_deactivated_at END " +
                        "WHERE id = :categoryId", nativeQuery = true)
        void toggleCategoryStatusOnly(@Param("categoryId") Long categoryId);

        // Toggle category status with products
        @Transactional
        @Modifying
        @Query(value = "UPDATE categories SET " +
                        "status = CASE WHEN status = 'ACTIVE' THEN 'INACTIVE' WHEN status = 'INACTIVE' THEN 'ACTIVE' ELSE status END, "
                        +
                        "last_activated_at = CASE WHEN status = 'INACTIVE' THEN NOW() ELSE last_activated_at END, " +
                        "last_deactivated_at = CASE WHEN status = 'ACTIVE' THEN NOW() ELSE last_deactivated_at END " +
                        "WHERE id = :categoryId", nativeQuery = true)
        void toggleCategoryStatusWithProducts_Category(@Param("categoryId") Long categoryId);

        @Transactional
        @Modifying
        @Query(value = "UPDATE products SET status = CASE " +
                        "WHEN (SELECT status FROM categories WHERE id = :categoryId) = 'ACTIVE' THEN 'ON_SALE' " +
                        "WHEN (SELECT status FROM categories WHERE id = :categoryId) = 'INACTIVE' THEN 'INACTIVE' " +
                        "ELSE status END " +
                        "WHERE category_id = :categoryId", nativeQuery = true)
        void toggleCategoryStatusWithProducts_Products(@Param("categoryId") Long categoryId);
}