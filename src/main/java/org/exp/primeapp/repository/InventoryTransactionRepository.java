package org.exp.primeapp.repository;

import org.exp.primeapp.models.dto.response.admin.InventoryTransactionActivityResponse;
import org.exp.primeapp.models.entities.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryTransactionRepository
        extends JpaRepository<InventoryTransaction, Long>, InventoryTransactionRepositoryCustom {

    /**
     * Find warehouse activities with flexible filtering using native SQL
     * All filters are optional - null values are ignored
     */
    @Query(value = """
            SELECT
                w.id,
                w.product_id as productId,
                p.name as productName,
                NULL as productImageUrl,
                c.name as categoryName,
                w.stock_quantity as stockQuantity,
                w.unit_price as unitPrice,
                w.total_price as totalPrice,
                w.selling_price as sellingPrice,
                w.is_calculated as isCalculated,
                CONCAT(COALESCE(u.first_name, ''), ' ', COALESCE(u.last_name, '')) as adminUserName,
                TO_CHAR(w.created_at, 'DD.MM.YYYY HH24:MI') as createdAt,
                w.type as activityType
            FROM inventory_transactions w
            INNER JOIN products p ON w.product_id = p.id
            LEFT JOIN categories c ON p.category_id = c.id
            LEFT JOIN app_users u ON w.admin_user_id = u.id
            WHERE 1=1
                AND (:type IS NULL OR w.type = :type)
                AND (:productId IS NULL OR w.product_id = :productId)
                AND (:startDate IS NULL OR w.created_at >= :startDate)
                AND (:endDate IS NULL OR w.created_at <= :endDate)
            ORDER BY w.created_at DESC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<InventoryTransactionActivityResponse> findActivitiesWithFilters(
            @Param("type") String type,
            @Param("productId") Long productId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("limit") int limit,
            @Param("offset") int offset);

    /**
     * Count total activities matching the filters
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM inventory_transactions w
            WHERE 1=1
                AND (:type IS NULL OR w.type = :type)
                AND (:productId IS NULL OR w.product_id = :productId)
                AND (:startDate IS NULL OR w.created_at >= :startDate)
                AND (:endDate IS NULL OR w.created_at <= :endDate)
            """, nativeQuery = true)
    long countActivitiesWithFilters(
            @Param("type") String type,
            @Param("productId") Long productId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Date range bo'yicha income larni topish (createdAt bo'yicha)
    @Query("SELECT w FROM InventoryTransaction w WHERE w.createdAt >= :start AND w.createdAt <= :end AND w.type = org.exp.primeapp.models.enums.TransactionType.IN ORDER BY w.createdAt DESC")
    List<InventoryTransaction> findIncomesByDateRange(@Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}