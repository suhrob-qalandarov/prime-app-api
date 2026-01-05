package org.exp.primeapp.repository;

import jakarta.persistence.LockModeType;
import org.exp.primeapp.models.entities.Product;
import org.exp.primeapp.models.entities.ProductSize;
import org.exp.primeapp.models.enums.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductSizeRepository extends JpaRepository<ProductSize, Long> {
    Optional<ProductSize> findByProductAndSize(Product product, Size size);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ps from ProductSize ps where ps.id = :id")
    Optional<ProductSize> findByIdWithLock(@Param("id") Long id);
}