package org.exp.primeapp.repository;

import org.exp.primeapp.models.entities.Product;
import org.exp.primeapp.models.entities.ProductSize;
import org.exp.primeapp.models.enums.Size;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductSizeRepository extends JpaRepository<ProductSize, Long> {
    Optional<ProductSize> findByProductAndSize(Product product, Size size);
}