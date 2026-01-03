package org.exp.primeapp.repository;

import org.exp.primeapp.models.entities.Category;
import org.exp.primeapp.models.entities.Product;
import org.exp.primeapp.models.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

        List<Product> findAllByCategory(Category category);

        // List<Product> findAllByCategory(Category category); // Already generic enough

        // List<Product> findAllByActive(boolean active); // REMOVED

        @Modifying
        @Transactional
        @Query("UPDATE Product p SET p.status = :status WHERE p.id = :productId")
        int updateStatus(@Param("status") ProductStatus status, @Param("productId") Long productId);

        @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND p.status = :status")
        List<Product> findByStatusAndCategoryId(@Param("categoryId") Long categoryId,
                        @Param("status") ProductStatus status);

        long countByStatus(ProductStatus status);

        @Transactional
        @Modifying
        @Query("UPDATE Product p SET p.status = CASE WHEN p.status = 'ON_SALE' THEN 'ARCHIVED' ELSE 'ON_SALE' END WHERE p.id = :productId")
        void toggleProductUpdateStatus(@Param("productId") Long productId);

        @Query(value = "SELECT * FROM product p WHERE p.status = 'SALE' ORDER BY RANDOM() LIMIT 4", nativeQuery = true)
        List<Product> findRandom4ActiveProductsStatusSale();

        @Query(value = "SELECT * FROM product p WHERE p.status = 'NEW' ORDER BY RANDOM() LIMIT 4", nativeQuery = true)
        List<Product> findRandom4ActiveProductsStatusNew();

        @Query(value = "SELECT * FROM product p WHERE p.status = 'HOT' ORDER BY RANDOM() LIMIT 4", nativeQuery = true)
        List<Product> findRandom4ActiveProductsStatusHot();

        // List<Product> findAllByCategory_IdAndActive(Long categoryId, Boolean active);

        Page<Product> findAllByStatus(ProductStatus status, Pageable pageable);

        Page<Product> findAllByCategory_IdAndStatus(Long categoryId, ProductStatus status, Pageable pageable);

        // Status ga qarab filter qilish
        Page<Product> findAllByStatusOrderByIdDesc(ProductStatus status, Pageable pageable);

        List<Product> findByStatus(ProductStatus status);

        List<Product> findByCategoryIdAndStatus(Long categoryId, ProductStatus status);

        List<Product> findAllByStatus(ProductStatus status);

        List<Product> findAllByStatusNot(ProductStatus status);

        List<Product> findByStatusNotAndCategoryId(ProductStatus status, Long categoryId);

        Page<Product> findAllByCategoryIdAndStatus(Long categoryId, ProductStatus status, Pageable pageable);

        long countByCategory(Category category);

        long countByCategoryId(Long categoryId);
}